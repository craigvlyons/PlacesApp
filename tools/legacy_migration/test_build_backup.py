import copy
import json
import tempfile
import unittest
from pathlib import Path

from build_backup import (
    CaptureError,
    _write_new_file_atomically,
    build_backup,
    validate_evidence_files,
)
from enrich_backup_v3 import enrich_backup

VERSION_TWO_DIGEST = "b73f3e0e5f52a0034357514582a11c1c2b08782dbb172d20c10487529085a52a"


class BuildBackupTest(unittest.TestCase):
    def setUp(self) -> None:
        fixture_path = Path(__file__).parent / "fixtures" / "complete-capture.json"
        self.capture = json.loads(fixture_path.read_text(encoding="utf-8"))
        fixture_v2_path = Path(__file__).parent / "fixtures" / "complete-capture-v2.json"
        self.capture_v2 = json.loads(fixture_v2_path.read_text(encoding="utf-8"))

    def test_complete_capture_matches_kotlin_golden_digest(self) -> None:
        backup = build_backup(self.capture)
        self.assertEqual(1, backup["version"])
        self.assertEqual(2, backup["recordCount"])
        self.assertEqual(
            "24abac7d58c12bc018d0df4528f557aacaecc3fde570e1d0a5ae3086fc1fcf25",
            backup["recordsSha256"],
        )

    def test_version_two_capture_preserves_default_place_type(self) -> None:
        backup = build_backup(self.capture_v2)

        self.assertEqual(2, backup["version"])
        self.assertEqual("Restaurant", backup["records"][0]["placeType"])
        self.assertIsNone(backup["records"][1]["placeType"])
        self.assertEqual(VERSION_TWO_DIGEST, backup["recordsSha256"])

    def test_version_two_capture_requires_place_type_field(self) -> None:
        capture = copy.deepcopy(self.capture)
        capture["version"] = 2
        for record in capture["records"]:
            record["placeType"] = None
            record["provenance"]["placeType"] = "resolved-from-address"
        del capture["records"][0]["placeType"]

        with self.assertRaisesRegex(CaptureError, "fields do not match"):
            build_backup(capture)

    def test_version_two_rejects_blank_place_type(self) -> None:
        capture = copy.deepcopy(self.capture)
        capture["version"] = 2
        for record in capture["records"]:
            record["placeType"] = "Restaurant"
            record["provenance"]["placeType"] = "resolved-from-address"
        capture["records"][0]["placeType"] = "   "

        with self.assertRaisesRegex(CaptureError, "placeType cannot be blank"):
            build_backup(capture)

    def test_incomplete_location_is_rejected(self) -> None:
        capture = copy.deepcopy(self.capture)
        capture["records"][0]["latitude"] = None
        with self.assertRaisesRegex(CaptureError, "latitude"):
            build_backup(capture)

    def test_missing_evidence_is_rejected(self) -> None:
        capture = copy.deepcopy(self.capture)
        capture["records"][0]["evidence"] = []
        with self.assertRaisesRegex(CaptureError, "evidence"):
            build_backup(capture)

    def test_incomplete_provenance_is_rejected(self) -> None:
        capture = copy.deepcopy(self.capture)
        del capture["records"][0]["provenance"]["longitude"]
        with self.assertRaisesRegex(CaptureError, "provenance fields do not match"):
            build_backup(capture)

    def test_unsafe_evidence_path_is_rejected(self) -> None:
        capture = copy.deepcopy(self.capture)
        capture["records"][0]["evidence"] = ["../outside.png", "records/edit.xml"]
        with self.assertRaisesRegex(CaptureError, "capture folder"):
            build_backup(capture)

    def test_capture_must_be_an_object(self) -> None:
        with self.assertRaisesRegex(CaptureError, "capture must be an object"):
            build_backup([])  # type: ignore[arg-type]

    def test_capture_timestamp_requires_timezone(self) -> None:
        capture = copy.deepcopy(self.capture)
        capture["capturedAt"] = "2026-08-21T12:00:00"
        with self.assertRaisesRegex(CaptureError, "timezone"):
            build_backup(capture)

    def test_empty_capture_is_rejected(self) -> None:
        capture = copy.deepcopy(self.capture)
        capture["records"] = []
        with self.assertRaisesRegex(CaptureError, "must not be empty"):
            build_backup(capture)

    def test_evidence_files_must_exist(self) -> None:
        capture = copy.deepcopy(self.capture)
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for record in capture["records"]:
                record["evidence"] = [f"records/{record['id']:04d}/edit.xml"]
                path = root / record["evidence"][0]
                path.parent.mkdir(parents=True)
                path.write_text("reviewed", encoding="utf-8")
            validate_evidence_files(capture, root)
            (root / capture["records"][0]["evidence"][0]).unlink()
            with self.assertRaisesRegex(CaptureError, "does not exist"):
                validate_evidence_files(capture, root)

    def test_duplicate_ids_are_rejected(self) -> None:
        capture = copy.deepcopy(self.capture)
        capture["records"][1]["id"] = capture["records"][0]["id"]
        with self.assertRaisesRegex(CaptureError, "unique"):
            build_backup(capture)

    def test_output_writer_refuses_to_overwrite_a_backup(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "backup.json"
            _write_new_file_atomically(output, "first")
            with self.assertRaises(FileExistsError):
                _write_new_file_atomically(output, "second")
            self.assertEqual("first", output.read_text(encoding="utf-8"))

    def test_phone_enrichment_is_exact_and_produces_version_three(self) -> None:
        backup = build_backup(self.capture_v2)
        metadata = {
            "records": [
                {"importId": record["id"], "nationalPhoneNumber": f"555-010{index}"}
                for index, record in enumerate(backup["records"])
            ]
        }

        enriched = enrich_backup(backup, metadata)

        self.assertEqual(3, enriched["version"])
        self.assertEqual(backup["recordCount"], enriched["recordCount"])
        self.assertEqual("555-0100", enriched["records"][0]["phoneNumber"])
        self.assertEqual(
            backup["records"][0],
            {key: value for key, value in enriched["records"][0].items() if key != "phoneNumber"},
        )

    def test_phone_enrichment_rejects_incomplete_metadata(self) -> None:
        backup = build_backup(self.capture_v2)
        with self.assertRaisesRegex(CaptureError, "exactly match"):
            enrich_backup(
                backup,
                {"records": [{"importId": backup["records"][0]["id"], "nationalPhoneNumber": "555"}]},
            )


if __name__ == "__main__":
    unittest.main()
