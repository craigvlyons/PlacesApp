#!/usr/bin/env python3
"""Create a Places backup v4 by adding reviewed Google categories to a v3 backup."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import sys
from typing import Any

from build_backup import (
    BACKUP_FORMAT,
    CURRENT_VERSION as PHONE_VERSION,
    RECORD_FIELDS_V3,
    CaptureError,
    _require_exact_fields,
    _write_new_file_atomically,
    records_digest,
)

GOOGLE_TYPE_VERSION = 4
RECORD_FIELDS_V4 = RECORD_FIELDS_V3 + ("googlePrimaryType",)
METADATA_FIELDS = {
    "importId", "evidenceRecordId", "legacyTitle", "legacyAddress",
    "nationalPhoneNumber", "resolvedBusinessName", "resolvedBusinessAddress",
    "googlePlaceId", "googlePrimaryType", "matchStatus", "retrievedAt", "source",
    "rawResponse", "rawResponseSha256",
}


def _length_prefixed(value: Any) -> bytes:
    if value is None:
        return b"-1:"
    if isinstance(value, bool):
        rendered = "1" if value else "0"
    else:
        rendered = str(value)
    encoded = rendered.encode("utf-8")
    return str(len(encoded)).encode("ascii") + b":" + encoded


def records_digest_v4(records: list[dict[str, Any]]) -> str:
    canonical = bytearray()
    for record in records:
        record_bytes = bytearray()
        for field in RECORD_FIELDS_V4:
            record_bytes.extend(_length_prefixed(record[field]))
        canonical.extend(_length_prefixed(record_bytes.decode("utf-8")))
    return hashlib.sha256(canonical).hexdigest()


def enrich_backup(backup: dict[str, Any], metadata: dict[str, Any]) -> dict[str, Any]:
    _require_exact_fields(
        backup,
        {"format", "version", "recordCount", "recordsSha256", "records"},
        "backup",
    )
    if backup["format"] != BACKUP_FORMAT or backup["version"] != PHONE_VERSION:
        raise CaptureError("Google-category enrichment requires a Places backup v3 source")
    records = backup["records"]
    if not isinstance(records, list) or backup["recordCount"] != len(records):
        raise CaptureError("Source backup record count does not match")
    for index, record in enumerate(records):
        if not isinstance(record, dict):
            raise CaptureError(f"source record {index} must be an object")
        _require_exact_fields(record, set(RECORD_FIELDS_V3), f"source record {index}")
    if records_digest(records, PHONE_VERSION) != backup["recordsSha256"]:
        raise CaptureError("Source backup content digest does not match")

    if not isinstance(metadata, dict) or not isinstance(metadata.get("records"), list):
        raise CaptureError("Google metadata records must be an array")
    metadata_by_id: dict[int, dict[str, Any]] = {}
    for index, record in enumerate(metadata["records"]):
        if not isinstance(record, dict):
            raise CaptureError(f"metadata record {index} must be an object")
        _require_exact_fields(record, METADATA_FIELDS, f"metadata record {index}")
        identifier = record["importId"]
        if not isinstance(identifier, int) or isinstance(identifier, bool) or identifier <= 0:
            raise CaptureError(f"metadata record {index} importId must be positive")
        phone = record["nationalPhoneNumber"]
        google_place_id = record["googlePlaceId"]
        google_type = record["googlePrimaryType"]
        if not all(isinstance(value, str) and value.strip() for value in (phone, google_place_id, google_type)):
            raise CaptureError(f"metadata record {index} required Google values must be non-blank")
        if identifier in metadata_by_id:
            raise CaptureError(f"metadata contains duplicate importId {identifier}")
        metadata_by_id[identifier] = record

    source_ids = {record["id"] for record in records}
    if source_ids != set(metadata_by_id):
        raise CaptureError("Google metadata IDs do not exactly match source backup IDs")

    enriched_records: list[dict[str, Any]] = []
    for record in records:
        metadata_record = metadata_by_id[record["id"]]
        if record["phoneNumber"] != metadata_record["nationalPhoneNumber"].strip():
            raise CaptureError(f"Phone metadata changed for source ID {record['id']}")
        # A reviewed legacy record may intentionally keep a null place ID when the
        # current business moved; never attach that newer listing during enrichment.
        if record["placeId"] is not None and record["placeId"] != metadata_record["googlePlaceId"].strip():
            raise CaptureError(f"Google place ID changed for source ID {record['id']}")
        enriched_records.append(
            {**record, "googlePrimaryType": metadata_record["googlePrimaryType"].strip().lower()}
        )

    return {
        "format": BACKUP_FORMAT,
        "version": GOOGLE_TYPE_VERSION,
        "recordCount": len(enriched_records),
        "recordsSha256": records_digest_v4(enriched_records),
        "records": enriched_records,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("backup_v3", type=Path)
    parser.add_argument("google_metadata", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    try:
        backup = json.loads(args.backup_v3.read_text(encoding="utf-8"))
        metadata = json.loads(args.google_metadata.read_text(encoding="utf-8"))
        enriched = enrich_backup(backup, metadata)
        _write_new_file_atomically(
            args.output,
            json.dumps(enriched, ensure_ascii=False, separators=(",", ":")),
        )
    except (OSError, json.JSONDecodeError, CaptureError) as exception:
        print(f"Google-enriched backup not created: {exception}", file=sys.stderr)
        return 1
    print(
        f"Created {args.output} with {enriched['recordCount']} records; "
        f"record SHA-256 {enriched['recordsSha256']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
