#!/usr/bin/env python3
"""Create a Places backup v3 by adding reviewed phone metadata to a v2 backup."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys
from typing import Any

from build_backup import (
    BACKUP_FORMAT,
    CURRENT_VERSION,
    PLACE_TYPE_VERSION,
    RECORD_FIELDS_V2,
    RECORD_FIELDS_V3,
    CaptureError,
    _require_exact_fields,
    _write_new_file_atomically,
    records_digest,
)


def enrich_backup(backup: dict[str, Any], metadata: dict[str, Any]) -> dict[str, Any]:
    _require_exact_fields(
        backup,
        {"format", "version", "recordCount", "recordsSha256", "records"},
        "backup",
    )
    if backup["format"] != BACKUP_FORMAT or backup["version"] != PLACE_TYPE_VERSION:
        raise CaptureError("Phone enrichment requires a Places backup v2 source")
    records = backup["records"]
    if not isinstance(records, list) or backup["recordCount"] != len(records):
        raise CaptureError("Source backup record count does not match")
    for index, record in enumerate(records):
        if not isinstance(record, dict):
            raise CaptureError(f"source record {index} must be an object")
        _require_exact_fields(record, set(RECORD_FIELDS_V2), f"source record {index}")
    if records_digest(records, PLACE_TYPE_VERSION) != backup["recordsSha256"]:
        raise CaptureError("Source backup content digest does not match")

    if not isinstance(metadata, dict) or not isinstance(metadata.get("records"), list):
        raise CaptureError("Phone metadata records must be an array")
    phones_by_id: dict[int, str] = {}
    for index, record in enumerate(metadata["records"]):
        if not isinstance(record, dict):
            raise CaptureError(f"metadata record {index} must be an object")
        identifier = record.get("importId")
        phone = record.get("nationalPhoneNumber")
        if not isinstance(identifier, int) or isinstance(identifier, bool) or identifier <= 0:
            raise CaptureError(f"metadata record {index} importId must be positive")
        if not isinstance(phone, str) or not phone.strip():
            raise CaptureError(f"metadata record {index} phone must be non-blank")
        if identifier in phones_by_id:
            raise CaptureError(f"metadata contains duplicate importId {identifier}")
        phones_by_id[identifier] = phone.strip()

    source_ids = {record["id"] for record in records}
    if source_ids != set(phones_by_id):
        raise CaptureError("Phone metadata IDs do not exactly match source backup IDs")

    enriched_records = [
        {**record, "phoneNumber": phones_by_id[record["id"]]}
        for record in records
    ]
    for index, record in enumerate(enriched_records):
        _require_exact_fields(record, set(RECORD_FIELDS_V3), f"enriched record {index}")
    return {
        "format": BACKUP_FORMAT,
        "version": CURRENT_VERSION,
        "recordCount": len(enriched_records),
        "recordsSha256": records_digest(enriched_records, CURRENT_VERSION),
        "records": enriched_records,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("backup_v2", type=Path)
    parser.add_argument("phone_metadata", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    try:
        backup = json.loads(args.backup_v2.read_text(encoding="utf-8"))
        metadata = json.loads(args.phone_metadata.read_text(encoding="utf-8"))
        enriched = enrich_backup(backup, metadata)
        _write_new_file_atomically(
            args.output,
            json.dumps(enriched, ensure_ascii=False, separators=(",", ":")),
        )
    except (OSError, json.JSONDecodeError, CaptureError) as exception:
        print(f"Phone-enriched backup not created: {exception}", file=sys.stderr)
        return 1
    print(
        f"Created {args.output} with {enriched['recordCount']} records; "
        f"record SHA-256 {enriched['recordsSha256']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
