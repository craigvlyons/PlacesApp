#!/usr/bin/env python3
"""Validate a reviewed legacy capture and emit a compatible Places backup file."""

from __future__ import annotations

import argparse
from datetime import datetime
import hashlib
import json
import math
import os
import sys
import tempfile
from pathlib import Path
from typing import Any

CAPTURE_FORMAT = "places-legacy-capture"
LEGACY_VERSION = 1
PLACE_TYPE_VERSION = 2
CURRENT_VERSION = 3
SUPPORTED_VERSIONS = {LEGACY_VERSION, PLACE_TYPE_VERSION, CURRENT_VERSION}
BACKUP_FORMAT = "places-app-backup"
RECORD_FIELDS_V1 = (
    "id",
    "placeId",
    "title",
    "address",
    "content",
    "rating",
    "isFavorite",
    "color",
    "city",
    "latitude",
    "longitude",
)
RECORD_FIELDS_V2 = RECORD_FIELDS_V1 + ("placeType",)
RECORD_FIELDS_V3 = RECORD_FIELDS_V2 + ("phoneNumber",)
PROVENANCE_VALUES = {
    "legacy-ui",
    "resolved-from-address",
    "migration-generated",
}


class CaptureError(ValueError):
    pass


def _require_exact_fields(value: dict[str, Any], expected: set[str], label: str) -> None:
    actual = set(value)
    if actual != expected:
        raise CaptureError(
            f"{label} fields do not match; "
            f"missing={sorted(expected - actual)}, unexpected={sorted(actual - expected)}"
        )


def _require_type(value: Any, expected_type: type, label: str) -> Any:
    if expected_type is int and (not isinstance(value, int) or isinstance(value, bool)):
        raise CaptureError(f"{label} must be an integer")
    if expected_type is float and (
        not isinstance(value, (int, float)) or isinstance(value, bool) or not math.isfinite(value)
    ):
        raise CaptureError(f"{label} must be a finite number")
    if expected_type is bool and not isinstance(value, bool):
        raise CaptureError(f"{label} must be a boolean")
    if expected_type is str and not isinstance(value, str):
        raise CaptureError(f"{label} must be a string")
    return value


def _record_fields(version: int) -> tuple[str, ...]:
    if version == LEGACY_VERSION:
        return RECORD_FIELDS_V1
    if version == PLACE_TYPE_VERSION:
        return RECORD_FIELDS_V2
    return RECORD_FIELDS_V3


def _validate_record(record: dict[str, Any], index: int, version: int) -> dict[str, Any]:
    fields = _record_fields(version)
    if not isinstance(record, dict):
        raise CaptureError(f"record {index} must be an object")
    _require_exact_fields(
        record,
        set(fields) | {"evidence", "provenance"},
        f"record {index}",
    )
    identifier = _require_type(record["id"], int, f"record {index} id")
    if identifier <= 0:
        raise CaptureError(f"record {index} id must be positive")

    place_id = record["placeId"]
    if place_id is not None:
        _require_type(place_id, str, f"record {index} placeId")
    content = record["content"]
    if content is not None:
        _require_type(content, str, f"record {index} content")
    if version >= PLACE_TYPE_VERSION:
        place_type = record["placeType"]
        if place_type is not None:
            _require_type(place_type, str, f"record {index} placeType")
            if not place_type.strip():
                raise CaptureError(f"record {index} placeType cannot be blank")
    if version >= CURRENT_VERSION:
        phone_number = record["phoneNumber"]
        if phone_number is not None:
            _require_type(phone_number, str, f"record {index} phoneNumber")
            if not phone_number.strip():
                raise CaptureError(f"record {index} phoneNumber cannot be blank")
    rating = record["rating"]
    if rating is not None:
        _require_type(rating, int, f"record {index} rating")
        if rating not in range(0, 6):
            raise CaptureError(f"record {index} rating must be from 0 through 5")

    title = _require_type(record["title"], str, f"record {index} title")
    address = _require_type(record["address"], str, f"record {index} address")
    city = _require_type(record["city"], str, f"record {index} city")
    if not title.strip() or not address.strip():
        raise CaptureError(f"record {index} title and address cannot be blank")
    if not city.strip():
        raise CaptureError(f"record {index} city cannot be blank")

    _require_type(record["isFavorite"], bool, f"record {index} isFavorite")
    color = _require_type(record["color"], int, f"record {index} color")
    if not -(2**31) <= color < 2**31:
        raise CaptureError(f"record {index} color is outside the signed 32-bit range")
    latitude = float(_require_type(record["latitude"], float, f"record {index} latitude"))
    longitude = float(_require_type(record["longitude"], float, f"record {index} longitude"))
    if not -90.0 <= latitude <= 90.0:
        raise CaptureError(f"record {index} latitude is outside -90 through 90")
    if not -180.0 <= longitude <= 180.0:
        raise CaptureError(f"record {index} longitude is outside -180 through 180")

    evidence = record["evidence"]
    if not isinstance(evidence, list) or not evidence or not all(
        isinstance(item, str) and item.strip() for item in evidence
    ):
        raise CaptureError(f"record {index} evidence must contain at least one artifact path")
    evidence_suffixes = {Path(item).suffix.lower() for item in evidence}
    if ".xml" not in evidence_suffixes or ".png" not in evidence_suffixes:
        raise CaptureError(
            f"record {index} evidence must include an accessibility XML and screenshot PNG"
        )
    for item in evidence:
        evidence_path = Path(item)
        if evidence_path.is_absolute() or ".." in evidence_path.parts:
            raise CaptureError(
                f"record {index} evidence paths must stay inside the capture folder"
            )

    provenance = record["provenance"]
    if not isinstance(provenance, dict):
        raise CaptureError(f"record {index} provenance must be an object")
    _require_exact_fields(provenance, set(fields), f"record {index} provenance")
    for field, source in provenance.items():
        _require_type(source, str, f"record {index} provenance for {field}")
        if source not in PROVENANCE_VALUES:
            raise CaptureError(
                f"record {index} provenance for {field} must be one of "
                f"{sorted(PROVENANCE_VALUES)}"
            )

    return {field: record[field] for field in fields}


def _length_prefixed(value: Any) -> bytes:
    if value is None:
        return b"-1:"
    if isinstance(value, bool):
        rendered = "1" if value else "0"
    elif isinstance(value, float):
        rendered = str(value)
    else:
        rendered = str(value)
    encoded = rendered.encode("utf-8")
    return str(len(encoded)).encode("ascii") + b":" + encoded


def records_digest(records: list[dict[str, Any]], version: int) -> str:
    fields = _record_fields(version)
    canonical = bytearray()
    for record in records:
        record_bytes = bytearray()
        for field in fields:
            record_bytes.extend(_length_prefixed(record[field]))
        canonical.extend(_length_prefixed(record_bytes.decode("utf-8")))
    return hashlib.sha256(canonical).hexdigest()


def build_backup(capture: dict[str, Any]) -> dict[str, Any]:
    if not isinstance(capture, dict):
        raise CaptureError("capture must be an object")
    _require_exact_fields(
        capture,
        {
            "captureFormat",
            "version",
            "deviceLabel",
            "capturedAt",
            "reviewedBy",
            "records",
        },
        "capture",
    )
    if capture["captureFormat"] != CAPTURE_FORMAT or capture["version"] not in SUPPORTED_VERSIONS:
        raise CaptureError("Unsupported legacy capture format or version")
    version = capture["version"]
    device_label = _require_type(capture["deviceLabel"], str, "deviceLabel")
    captured_at = _require_type(capture["capturedAt"], str, "capturedAt")
    reviewed_by = _require_type(capture["reviewedBy"], str, "reviewedBy")
    if not device_label.strip() or not captured_at.strip() or not reviewed_by.strip():
        raise CaptureError("deviceLabel, capturedAt, and reviewedBy cannot be blank")
    try:
        captured_timestamp = datetime.fromisoformat(captured_at)
    except ValueError as exception:
        raise CaptureError("capturedAt must be an ISO-8601 timestamp") from exception
    if captured_timestamp.tzinfo is None:
        raise CaptureError("capturedAt must include a timezone offset")
    if not isinstance(capture["records"], list):
        raise CaptureError("records must be an array")
    if not capture["records"]:
        raise CaptureError("records must not be empty")

    records = [
        _validate_record(record, index, version)
        for index, record in enumerate(capture["records"])
    ]
    identifiers = [record["id"] for record in records]
    if len(identifiers) != len(set(identifiers)):
        raise CaptureError("record IDs must be unique")
    records.sort(key=lambda record: (record["id"], _record_sort_value(record, version)))
    return {
        "format": BACKUP_FORMAT,
        "version": version,
        "recordCount": len(records),
        "recordsSha256": records_digest(records, version),
        "records": records,
    }


def _record_sort_value(record: dict[str, Any], version: int) -> bytes:
    result = bytearray()
    for field in _record_fields(version):
        result.extend(_length_prefixed(record[field]))
    return bytes(result)


def validate_evidence_files(capture: dict[str, Any], capture_directory: Path) -> None:
    root = capture_directory.resolve(strict=True)
    for index, record in enumerate(capture["records"]):
        for item in record["evidence"]:
            evidence_path = (root / item).resolve(strict=False)
            if not evidence_path.is_relative_to(root):
                raise CaptureError(
                    f"record {index} evidence path escapes the capture folder: {item}"
                )
            if not evidence_path.is_file():
                raise CaptureError(f"record {index} evidence file does not exist: {item}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("capture", type=Path, help="Reviewed legacy capture JSON")
    parser.add_argument("output", type=Path, help="Destination Places backup JSON")
    args = parser.parse_args()
    try:
        capture_text = args.capture.read_text(encoding="utf-8")
        capture = json.loads(capture_text)
        backup = build_backup(capture)
        validate_evidence_files(capture, args.capture.parent)
        _write_new_file_atomically(
            args.output,
            json.dumps(backup, ensure_ascii=False, separators=(",", ":")),
        )
    except (OSError, json.JSONDecodeError, CaptureError) as exception:
        print(f"Migration artifact not created: {exception}", file=sys.stderr)
        return 1
    print(
        f"Created {args.output} with {backup['recordCount']} records; "
        f"record SHA-256 {backup['recordsSha256']}; "
        f"capture SHA-256 {hashlib.sha256(capture_text.encode('utf-8')).hexdigest()}"
    )
    return 0


def _write_new_file_atomically(output: Path, content: str) -> None:
    if not output.parent.is_dir():
        raise OSError(f"Output directory does not exist: {output.parent}")
    temporary_path: Path | None = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="w",
            encoding="utf-8",
            dir=output.parent,
            prefix=f".{output.name}.",
            delete=False,
        ) as temporary:
            temporary_path = Path(temporary.name)
            temporary.write(content)
            temporary.flush()
            os.fsync(temporary.fileno())
        os.link(temporary_path, output)
    finally:
        if temporary_path is not None:
            temporary_path.unlink(missing_ok=True)


if __name__ == "__main__":
    raise SystemExit(main())
