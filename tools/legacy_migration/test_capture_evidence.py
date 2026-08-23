import subprocess
import tempfile
import unittest
from pathlib import Path

from capture_evidence import AdbClient, EvidenceCaptureError, capture_evidence


LEGACY_XML = b"""<?xml version='1.0'?><hierarchy><node package='com.example.favoriteplaces' class='android.view.View'><node package='com.example.favoriteplaces' class='android.widget.EditText' text='Title'/><node package='com.example.favoriteplaces' class='android.widget.EditText' text='Notes'/></node></hierarchy>"""
OTHER_XML = b"""<?xml version='1.0'?><hierarchy><node package='com.android.settings' text='Settings'/></hierarchy>"""
LEGACY_HOME_XML = b"""<?xml version='1.0'?><hierarchy><node package='com.example.favoriteplaces' class='android.widget.TextView' text='Favorites'/></hierarchy>"""
PNG = b"\x89PNG\r\n\x1a\nsynthetic"


class EvidenceCaptureTest(unittest.TestCase):
    def test_captures_private_atomic_evidence_bundle(self) -> None:
        with tempfile.TemporaryDirectory() as directory, tempfile.TemporaryDirectory() as repository:
            root = Path(directory)
            target = capture_evidence(
                client=self._client(LEGACY_XML, PNG),
                output_root=root,
                record_id=7,
                repository_root=Path(repository),
            )
            self.assertEqual(root.resolve() / "records" / "0007", target)
            self.assertEqual(LEGACY_XML, (target / "edit.xml").read_bytes())
            self.assertEqual(PNG, (target / "edit.png").read_bytes())
            self.assertTrue((target / "capture-metadata.json").is_file())
            self.assertEqual(0o600, (target / "edit.xml").stat().st_mode & 0o777)

    def test_ignores_uiautomator_status_text_after_xml(self) -> None:
        with tempfile.TemporaryDirectory() as directory, tempfile.TemporaryDirectory() as repository:
            target = capture_evidence(
                client=self._client(LEGACY_XML + b"\nUI hierarchy dumped to: /dev/tty\n", PNG),
                output_root=Path(directory),
                record_id=1,
                repository_root=Path(repository),
            )
            self.assertEqual(LEGACY_XML, (target / "edit.xml").read_bytes())

    def test_refuses_non_legacy_foreground(self) -> None:
        with tempfile.TemporaryDirectory() as directory, tempfile.TemporaryDirectory() as repository:
            with self.assertRaisesRegex(EvidenceCaptureError, "not the visible foreground"):
                capture_evidence(
                    self._client(OTHER_XML, PNG), Path(directory), 1, Path(repository)
                )

    def test_refuses_invalid_screenshot(self) -> None:
        with tempfile.TemporaryDirectory() as directory, tempfile.TemporaryDirectory() as repository:
            with self.assertRaisesRegex(EvidenceCaptureError, "valid PNG"):
                capture_evidence(
                    self._client(LEGACY_XML, b"not-png"), Path(directory), 1, Path(repository)
                )

    def test_refuses_legacy_home_screen(self) -> None:
        with tempfile.TemporaryDirectory() as directory, tempfile.TemporaryDirectory() as repository:
            with self.assertRaisesRegex(EvidenceCaptureError, "Edit screen was not detected"):
                capture_evidence(
                    self._client(LEGACY_HOME_XML, PNG), Path(directory), 1, Path(repository)
                )

    def test_refuses_repository_output(self) -> None:
        with tempfile.TemporaryDirectory() as repository:
            root = Path(repository)
            with self.assertRaisesRegex(EvidenceCaptureError, "outside the repository"):
                capture_evidence(self._client(LEGACY_XML, PNG), root, 1, root)

    def test_refuses_overwrite(self) -> None:
        with tempfile.TemporaryDirectory() as directory, tempfile.TemporaryDirectory() as repository:
            root = Path(directory)
            client = self._client(LEGACY_XML, PNG)
            capture_evidence(client, root, 1, Path(repository))
            with self.assertRaisesRegex(EvidenceCaptureError, "already exists"):
                capture_evidence(client, root, 1, Path(repository))

    @staticmethod
    def _client(hierarchy: bytes, screenshot: bytes) -> AdbClient:
        def runner(command: list[str], **_: object) -> subprocess.CompletedProcess[bytes]:
            if command[-1] == "get-state":
                output = b"device\n"
            elif "uiautomator" in command:
                output = hierarchy
            elif "screencap" in command:
                output = screenshot
            else:
                return subprocess.CompletedProcess(command, 1, b"", b"unexpected command")
            return subprocess.CompletedProcess(command, 0, output, b"")

        return AdbClient(Path("/synthetic/adb"), "serial", runner=runner)


if __name__ == "__main__":
    unittest.main()
