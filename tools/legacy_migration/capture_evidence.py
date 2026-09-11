#!/usr/bin/env python3
"""Capture the currently visible legacy Edit screen as reviewed migration evidence."""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
from typing import Callable
import xml.etree.ElementTree as ElementTree

LEGACY_PACKAGE = "com.example.favoriteplaces"
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


class EvidenceCaptureError(RuntimeError):
    pass


class AdbClient:
    def __init__(
        self,
        adb: Path,
        serial: str,
        runner: Callable[..., subprocess.CompletedProcess[bytes]] = subprocess.run,
    ) -> None:
        self._adb = adb
        self._serial = serial
        self._runner = runner

    def run(self, *arguments: str) -> bytes:
        result = self._runner(
            [str(self._adb), "-s", self._serial, *arguments],
            check=False,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        if result.returncode != 0:
            error = result.stderr.decode("utf-8", errors="replace").strip()
            raise EvidenceCaptureError(error or f"adb command failed: {' '.join(arguments)}")
        return result.stdout


def capture_evidence(
    client: AdbClient,
    output_root: Path,
    record_id: int,
    repository_root: Path,
) -> Path:
    if record_id <= 0:
        raise EvidenceCaptureError("record ID must be positive")
    root = output_root.resolve(strict=True)
    repository = repository_root.resolve(strict=True)
    if root == repository or root.is_relative_to(repository):
        raise EvidenceCaptureError("personal migration evidence must be stored outside the repository")

    if client.run("get-state").strip() != b"device":
        raise EvidenceCaptureError("the selected Android device is not ready")

    hierarchy = _extract_hierarchy(client.run("exec-out", "uiautomator", "dump", "/dev/tty"))
    _require_legacy_edit_screen(hierarchy)
    screenshot = client.run("exec-out", "screencap", "-p")
    if not screenshot.startswith(PNG_SIGNATURE):
        raise EvidenceCaptureError("adb did not return a valid PNG screenshot")

    target = root / "records" / f"{record_id:04d}"
    target.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
    if target.exists():
        raise EvidenceCaptureError(f"evidence already exists for record {record_id}")

    temporary = Path(tempfile.mkdtemp(prefix=f".{record_id:04d}.", dir=target.parent))
    try:
        _write_private(temporary / "edit.xml", hierarchy)
        _write_private(temporary / "edit.png", screenshot)
        metadata = {
            "format": "places-legacy-evidence",
            "version": 1,
            "legacyPackage": LEGACY_PACKAGE,
            "recordId": record_id,
            "capturedAt": datetime.now(timezone.utc).isoformat(),
            "hierarchySha256": hashlib.sha256(hierarchy).hexdigest(),
            "screenshotSha256": hashlib.sha256(screenshot).hexdigest(),
        }
        _write_private(
            temporary / "capture-metadata.json",
            json.dumps(metadata, indent=2, sort_keys=True).encode("utf-8"),
        )
        temporary.rename(target)
    except Exception:
        shutil.rmtree(temporary, ignore_errors=True)
        raise
    return target


def _extract_hierarchy(output: bytes) -> bytes:
    start = output.find(b"<?xml")
    end_marker = b"</hierarchy>"
    end = output.find(end_marker, start)
    if start < 0 or end < 0:
        raise EvidenceCaptureError("UI Automator did not return an XML hierarchy")
    hierarchy = output[start : end + len(end_marker)].strip()
    try:
        ElementTree.fromstring(hierarchy)
    except ElementTree.ParseError as exception:
        raise EvidenceCaptureError("UI Automator returned invalid XML") from exception
    return hierarchy


def _require_legacy_edit_screen(hierarchy: bytes) -> None:
    root = ElementTree.fromstring(hierarchy)
    packages = {node.attrib.get("package") for node in root.iter("node")}
    if LEGACY_PACKAGE not in packages:
        raise EvidenceCaptureError(
            "the legacy Places app is not the visible foreground screen; open the place's Edit screen first"
        )
    editable_nodes = [
        node
        for node in root.iter("node")
        if node.attrib.get("package") == LEGACY_PACKAGE
        and node.attrib.get("class") == "android.widget.EditText"
    ]
    if len(editable_nodes) < 2:
        raise EvidenceCaptureError(
            "the legacy app is visible, but its Edit screen was not detected; "
            "open one saved place before capturing"
        )


def _write_private(path: Path, content: bytes) -> None:
    path.write_bytes(content)
    path.chmod(0o600)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True, help="Exact serial shown by adb devices")
    parser.add_argument("--record-id", required=True, type=int, help="Positive migration record ID")
    parser.add_argument("--output-root", required=True, type=Path, help="Existing folder outside Git")
    parser.add_argument("--adb", type=Path, help="Path to adb; defaults to adb on PATH")
    args = parser.parse_args()

    adb = args.adb or Path(shutil.which("adb") or "")
    if not str(adb) or not adb.is_file():
        parser.error("adb was not found; pass --adb with the Android SDK platform-tools path")
    try:
        target = capture_evidence(
            client=AdbClient(adb=adb, serial=args.serial),
            output_root=args.output_root,
            record_id=args.record_id,
            repository_root=Path(__file__).resolve().parents[2],
        )
    except (EvidenceCaptureError, OSError) as exception:
        parser.error(str(exception))
    print(f"Captured private migration evidence in {target}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
