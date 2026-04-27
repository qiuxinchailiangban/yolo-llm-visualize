from __future__ import annotations

import json
import sys
from dataclasses import asdict
from pathlib import Path

from omni_config_io import DEFAULT_PROMPT, read_openai_plugin, write_openai_plugin


def load_payload() -> dict:
    raw = sys.stdin.read().strip()
    if not raw:
        return {}
    return json.loads(raw)


def emit_ok(data: dict | None = None) -> int:
    sys.stdout.write(json.dumps({"ok": True, "data": data or {}}, ensure_ascii=False))
    return 0


def emit_error(message: str) -> int:
    sys.stdout.write(json.dumps({"ok": False, "error": message}, ensure_ascii=False))
    return 1


def read_action(config_path: Path) -> int:
    view = read_openai_plugin(config_path)
    payload = asdict(view)
    payload["default_prompt"] = DEFAULT_PROMPT
    return emit_ok(payload)


def write_action(config_path: Path) -> int:
    payload = load_payload()
    write_openai_plugin(
        config_path,
        enabled=bool(payload.get("enabled")),
        only_private=bool(payload.get("only_private")),
        allowed_targets=list(payload.get("allowed_targets") or []),
        prompt=payload.get("prompt"),
        memory_enabled=bool(payload.get("memory_enabled", True)),
        memory_max_turns=int(payload.get("memory_max_turns", 10) or 0),
        memory_ttl_minutes=int(payload.get("memory_ttl_minutes", 60) or 0),
        memory_persist_path=str(payload.get("memory_persist_path") or "").strip(),
    )
    return emit_ok({"saved": True})


def main() -> int:
    if len(sys.argv) < 3:
        return emit_error("用法: python electron_omni_helper.py <read|write> <config_path>")

    action = sys.argv[1].strip().lower()
    config_path = Path(sys.argv[2]).expanduser()

    try:
        if action == "read":
            return read_action(config_path)
        if action == "write":
            return write_action(config_path)
        return emit_error(f"不支持的动作: {action}")
    except Exception as exc:  # noqa: BLE001
        return emit_error(str(exc))


if __name__ == "__main__":
    raise SystemExit(main())
