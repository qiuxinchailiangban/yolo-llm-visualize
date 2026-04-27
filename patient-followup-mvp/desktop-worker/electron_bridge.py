from __future__ import annotations

import json
import signal
import sys
import threading
import traceback
from pathlib import Path

from worker import DEFAULT_CONFIG_PATH, run_worker_loop


def emit(message_type: str, **payload) -> None:
    sys.stdout.write(json.dumps({"type": message_type, **payload}, ensure_ascii=False) + "\n")
    sys.stdout.flush()


def watch_stdin(stop_event: threading.Event) -> None:
    while not stop_event.is_set():
        line = sys.stdin.readline()
        if not line:
            return
        command = line.strip().lower()
        if command == "stop":
            emit("lifecycle", status="STOPPING")
            stop_event.set()
            return


def main() -> int:
    config_path = Path(sys.argv[1]).expanduser().resolve() if len(sys.argv) > 1 else DEFAULT_CONFIG_PATH
    stop_event = threading.Event()

    def request_stop(*_args) -> None:
        stop_event.set()

    signal.signal(signal.SIGINT, request_stop)
    if hasattr(signal, "SIGTERM"):
        signal.signal(signal.SIGTERM, request_stop)

    threading.Thread(target=watch_stdin, args=(stop_event,), daemon=True).start()
    emit("lifecycle", status="STARTING", configPath=str(config_path))

    try:
        exit_code = run_worker_loop(
            config_path,
            log_func=lambda message: emit("log", message=message),
            should_stop=stop_event.is_set,
            status_callback=lambda status: emit("status", status=status),
            event_callback=lambda event: emit("event", event=event),
        )
    except Exception as exc:  # noqa: BLE001
        emit(
            "error",
            message=str(exc),
            traceback=traceback.format_exc(),
        )
        return 1

    emit("lifecycle", status="STOPPED", exitCode=exit_code)
    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
