"""Run the Kotlin HTTP test against a disposable synthetic Movie Inbox instance."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import socket
import subprocess
import sys
import tempfile
import threading
import time
import xml.etree.ElementTree as ET
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--server", type=Path, required=True)
    args = parser.parse_args()
    server_root = args.server.resolve()
    root = Path(__file__).resolve().parents[1]
    manifest = json.loads((root / "contract/source.json").read_text())
    commit = subprocess.check_output(
        [
            "git",
            "-c",
            f"safe.directory={server_root.as_posix()}",
            "-C",
            str(server_root),
            "rev-parse",
            "HEAD",
        ],
        text=True,
    ).strip()
    if commit != manifest["server_commit"]:
        raise SystemExit(
            "Server commit differs from contract/source.json; update contract deliberately."
        )
    for name, entry in manifest["files"].items():
        for path in (root / "contract" / name, server_root / entry["source"]):
            if hashlib.sha256(path.read_bytes()).hexdigest() != entry["sha256"]:
                raise SystemExit(f"Contract hash mismatch: {name}")
    sys.path.insert(0, str(server_root / "src"))
    import uvicorn

    from movie_inbox.application.auth_service import AuthService
    from movie_inbox.domain.catalog import normalize_item
    from movie_inbox.infrastructure.identity_repository import SqliteIdentityRepository
    from movie_inbox.infrastructure.json_repository import JsonCatalogRepository
    from movie_inbox.web.app import create_app
    from movie_inbox.web.config import ViewerConfig

    with tempfile.TemporaryDirectory(prefix="movie-inbox-kotlin-") as directory:
        data = Path(directory)
        catalog = data / "catalog.json"
        JsonCatalogRepository(catalog, normalize_item).write(
            [
                normalize_item(
                    {
                        "id": "synthetic-film",
                        "title": "Synthetic film",
                        "kind": "pelicula",
                        "year": "2000",
                    }
                )
            ]
        )
        instance = data / "instance.db"
        AuthService(SqliteIdentityRepository(instance)).bootstrap_owner(
            "sync-test",
            "synthetic-long-password",
            catalog_name="Synthetic QA",
            source_paths=[str(catalog)],
            write_path=str(catalog),
        )
        media = data / "media"
        media.mkdir()
        listener = socket.socket()
        listener.bind(("127.0.0.1", 0))
        listener.listen(128)
        port = listener.getsockname()[1]
        config = ViewerConfig(
            patterns=[str(catalog)],
            title="Synthetic QA",
            write_json=str(catalog),
            image_cache=False,
            image_cache_dir=str(data / "images"),
            image_cache_max_bytes=1024,
            port=port,
            api_token="synthetic-test-only",
            instance_db=str(instance),
            member_catalog_dir=str(data / "members"),
            library_allowed_roots=(str(media),),
            library_scheduler_poll_seconds=3600,
        )
        server = uvicorn.Server(
            uvicorn.Config(create_app(config), host="127.0.0.1", port=port, log_level="error")
        )
        thread = threading.Thread(target=lambda: server.run(sockets=[listener]), daemon=True)
        thread.start()
        try:
            deadline = time.monotonic() + 20
            while not server.started:
                if not thread.is_alive() or time.monotonic() > deadline:
                    raise RuntimeError("Synthetic server did not start")
                time.sleep(0.05)
            env = os.environ.copy()
            env["MOVIE_INBOX_TEST_ORIGIN"] = f"http://127.0.0.1:{port}"
            result = subprocess.call(
                [
                    str(root / "gradlew.bat"),
                    "testDebugUnitTest",
                    "assembleDebug",
                    "--offline",
                    "--rerun-tasks",
                    "--console=plain",
                ],
                cwd=root,
                env=env,
            )
            if result:
                return result
            report = (
                root
                / "app/build/test-results/testDebugUnitTest"
                / "TEST-io.github.pasaporten25.movieinbox.sync.RealServerRatingTest.xml"
            )
            suite = ET.parse(report).getroot()
            if (
                any(int(suite.get(key, "0")) for key in ("skipped", "failures", "errors"))
                or int(suite.get("tests", "0")) != 1
            ):
                raise RuntimeError("Real-server Kotlin test did not pass without skips")
            print("Kotlin/server cycle passed; no skipped integration test.")
            return 0
        finally:
            server.should_exit = True
            thread.join(timeout=20)
            listener.close()
            if thread.is_alive():
                raise RuntimeError("Synthetic server failed to stop")


if __name__ == "__main__":
    raise SystemExit(main())
