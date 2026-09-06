"""Build an installable archive with an actual deployment URL, without modifying source config."""
import argparse
import json
from pathlib import Path
from urllib.parse import urlparse
from zipfile import ZipFile, ZIP_DEFLATED


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", required=True, help="Actual PUBLIC_BASE_URL; HTTPS required for remote ChatGPT")
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    base = args.base_url.rstrip("/")
    uri = urlparse(base)
    if (not uri.hostname or uri.username or uri.password or uri.query or uri.fragment or uri.path
            or not (uri.scheme == "https" or (uri.scheme == "http" and uri.hostname in ("127.0.0.1", "localhost", "::1")))):
        parser.error("Use an HTTPS origin, or HTTP loopback for local development")
    root = Path(__file__).resolve().parent
    output = args.output.resolve()
    if output.exists():
        parser.error("Output already exists; choose a new archive name")
    output.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(output, "x", ZIP_DEFLATED) as archive:
        for file in sorted(root.rglob("*")):
            if not file.is_file() or file.suffix == ".zip" or "__pycache__" in file.parts or file.name == "package_plugin.py":
                continue
            name = file.relative_to(root).as_posix()
            if name == ".mcp.json":
                config = json.loads(file.read_text(encoding="utf-8"))
                config["mcpServers"]["jobsight"]["url"] = base + "/mcp"
                archive.writestr(name, json.dumps(config, ensure_ascii=False, indent=2).encode("utf-8"))
            else:
                archive.write(file, name)
    print(f"Created {output}; MCP endpoint: {base}/mcp")


if __name__ == "__main__":
    main()
