import copy
import unittest

from build_backup import BACKUP_FORMAT, CURRENT_VERSION, RECORD_FIELDS_V3, records_digest
from enrich_backup_v4 import GOOGLE_TYPE_VERSION, enrich_backup, records_digest_v4


class EnrichBackupV4Test(unittest.TestCase):
    def setUp(self):
        self.record = dict(zip(RECORD_FIELDS_V3, (
            1, "google-1", "Dinner", "1 Main St", "notes", 4, True, -1,
            "Denver", 39.0, -104.0, "Our pick", "303-555-0101",
        )))
        self.backup = {
            "format": BACKUP_FORMAT,
            "version": CURRENT_VERSION,
            "recordCount": 1,
            "recordsSha256": records_digest([self.record], CURRENT_VERSION),
            "records": [self.record],
        }
        self.metadata = {"records": [{
            "importId": 1, "evidenceRecordId": 1, "legacyTitle": "Dinner",
            "legacyAddress": "1 Main St", "nationalPhoneNumber": "303-555-0101",
            "resolvedBusinessName": "Dinner", "resolvedBusinessAddress": "1 Main St",
            "googlePlaceId": "google-1", "googlePrimaryType": "restaurant",
            "matchStatus": "exact-address", "retrievedAt": "2026-08-22T00:00:00-06:00",
            "source": "Google Places Text Search (New)", "rawResponse": "raw.json",
            "rawResponseSha256": "0" * 64,
        }]}

    def test_enriches_without_changing_existing_values(self):
        output = enrich_backup(self.backup, self.metadata)
        self.assertEqual(GOOGLE_TYPE_VERSION, output["version"])
        self.assertEqual("restaurant", output["records"][0]["googlePrimaryType"])
        self.assertEqual(self.record, {k: output["records"][0][k] for k in RECORD_FIELDS_V3})
        self.assertEqual(records_digest_v4(output["records"]), output["recordsSha256"])

    def test_rejects_mismatched_place_id(self):
        metadata = copy.deepcopy(self.metadata)
        metadata["records"][0]["googlePlaceId"] = "wrong"
        with self.assertRaisesRegex(ValueError, "place ID changed"):
            enrich_backup(self.backup, metadata)


if __name__ == "__main__":
    unittest.main()
