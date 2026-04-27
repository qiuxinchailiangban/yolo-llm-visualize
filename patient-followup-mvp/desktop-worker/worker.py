from __future__ import annotations

import json
import locale
import os
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parent
DEFAULT_CONFIG_PATH = ROOT / "config.json"


def emit(log_func, message: str) -> None:
    if log_func is not None:
        log_func(message)
    else:
        print(message)


def emit_event(event_callback, event_type: str, **payload) -> None:
    if event_callback is None:
        return
    event_callback({"type": event_type, **payload})


def resolve_python_command(config: dict) -> str:
    configured = str(config.get("python_command") or "python").strip() or "python"
    normalized = configured.lower()
    if configured and normalized not in {"python", "python.exe", "py"}:
        return configured

    sdk_root = str(config.get("sdk_root") or "").strip()
    candidates: list[Path] = []
    if sdk_root:
        sdk_root_path = Path(sdk_root).expanduser().resolve()
        candidates.extend(
            [
                sdk_root_path / ".venv" / "Scripts" / "python.exe",
                sdk_root_path / "venv" / "Scripts" / "python.exe",
                sdk_root_path / ".env" / "Scripts" / "python.exe",
            ]
        )
    candidates.extend(
        [
            ROOT / ".venv" / "Scripts" / "python.exe",
            ROOT.parent / ".venv" / "Scripts" / "python.exe",
        ]
    )

    for candidate in candidates:
        if candidate.exists():
            return str(candidate)
    return configured


def load_config(config_path: Path) -> dict:
    with config_path.open("r", encoding="utf-8") as fh:
        config = json.load(fh)
    config.setdefault("backend_base_url", "http://localhost:8080")
    config.setdefault("worker_token", "followup-worker-token")
    config.setdefault("worker_id", f"{socket.gethostname()}-desktop-worker")
    config.setdefault("job_type", "WECHAT_RPA_SEND")
    config.setdefault("poll_interval_seconds", 3)
    config.setdefault("python_command", "python")
    config.setdefault("send_script", "send_once.py")
    config.setdefault("delayed_send_script", "send_later.py")
    config.setdefault("send_mode", "enter")
    config.setdefault("wait_seconds", 8)
    config.setdefault("timeout_seconds", 120)
    config["python_command"] = resolve_python_command(config)
    return config


def decode_text(data: bytes | None) -> str:
    if not data:
        return ""
    encodings: list[str] = []
    preferred = locale.getpreferredencoding(False)
    if preferred:
        encodings.append(preferred)
    encodings.extend(["utf-8", "utf-8-sig", "gbk", "cp936", "gb18030"])
    for encoding in encodings:
        try:
            return data.decode(encoding)
        except UnicodeDecodeError:
            continue
    return data.decode("utf-8", errors="replace")


def api_request(config: dict, method: str, path: str, payload: dict | None = None) -> dict:
    url = config["backend_base_url"].rstrip("/") + path
    data = None if payload is None else json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=data,
        method=method,
        headers={
            "Content-Type": "application/json",
            "X-Worker-Token": config["worker_token"],
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            body = decode_text(response.read())
    except urllib.error.HTTPError as exc:
        error_body = decode_text(exc.read())
        raise RuntimeError(f"http {exc.code} {exc.reason}: {error_body}") from exc
    parsed = json.loads(body)
    if not parsed.get("success", False):
        raise RuntimeError(parsed.get("message") or f"request failed: {path}")
    return parsed.get("data")


def claim_job(config: dict) -> dict | None:
    return api_request(
        config,
        "POST",
        "/api/worker/automation-jobs/claim",
        {
            "workerId": config["worker_id"],
            "jobType": config["job_type"],
        },
    )


def resolve_script_name(config: dict, payload: dict) -> str:
    countdown_seconds = int(payload.get("countdownSeconds") or 0)
    return config["delayed_send_script"] if countdown_seconds > 0 else config["send_script"]


def describe_job(config: dict, payload: dict) -> str:
    countdown_seconds = int(payload.get("countdownSeconds") or 0)
    image_paths = payload.get("imagePaths") or ([] if not payload.get("qrImagePath") else [payload.get("qrImagePath")])
    image_requested = bool(image_paths)
    return (
        f"target={payload.get('targetConversation') or '-'} | "
        f"script={resolve_script_name(config, payload)} | "
        f"countdown={countdown_seconds}s | "
        f"images={len(image_paths) if image_requested else 0}"
    )


def build_command(config: dict, payload: dict) -> list[str]:
    sdk_root = Path(config["sdk_root"]).expanduser().resolve()
    countdown_seconds = int(payload.get("countdownSeconds") or 0)
    script_name = resolve_script_name(config, payload)
    command = [
        config["python_command"],
        str((sdk_root / script_name).resolve()),
        "--target",
        payload["targetConversation"],
        "--content",
        payload["content"],
        "--config",
        str(Path(config["config_path"]).expanduser().resolve()),
        "--send-mode",
        config["send_mode"],
        "--wait-seconds",
        str(config["wait_seconds"]),
    ]
    if countdown_seconds > 0:
        command.extend(["--countdown-seconds", str(countdown_seconds)])

    image_paths = payload.get("imagePaths")
    if not image_paths:
        qr_image_path = payload.get("qrImagePath")
        image_paths = [] if not qr_image_path else [qr_image_path]
    for raw_image_path in image_paths:
        image_file = Path(str(raw_image_path)).expanduser()
        if image_file.exists():
            command.extend(["--image", str(image_file.resolve())])
        else:
            print(f"[worker] 警告: 提醒图片不存在，将跳过该图片: {image_file}")
    return command


def execute_job(config: dict, job: dict) -> tuple[str, str, str]:
    payload = json.loads(job["payloadJson"])
    command = build_command(config, payload)
    sdk_root = Path(config["sdk_root"]).expanduser().resolve()
    env = os.environ.copy()
    env.setdefault("PYTHONIOENCODING", "utf-8")
    env.setdefault("PYTHONUTF8", "1")
    completed = subprocess.run(
        command,
        cwd=str(sdk_root),
        capture_output=True,
        timeout=int(config["timeout_seconds"]),
        env=env,
    )
    stdout_text = decode_text(completed.stdout)
    stderr_text = decode_text(completed.stderr)
    output = "\n".join(part for part in [stdout_text, stderr_text] if part).strip()
    command_line = " ".join(command)
    result_json = json.dumps(
        {
            "exitCode": completed.returncode,
            "taskNo": payload.get("taskNo"),
            "reminderTaskId": payload.get("reminderTaskId"),
        },
        ensure_ascii=False,
    )
    if completed.returncode != 0:
        raise RuntimeError(output or f"command exited with code {completed.returncode}")
    return command_line, output, result_json


def report_success(config: dict, job_no: str, command_line: str, output: str, result_json: str) -> None:
    api_request(
        config,
        "POST",
        f"/api/worker/automation-jobs/{job_no}/complete",
        {
            "workerId": config["worker_id"],
            "commandLine": command_line,
            "executionLog": output,
            "resultJson": result_json,
            "errorMessage": None,
        },
    )


def report_failure(config: dict, job_no: str, command_line: str, output: str, error_message: str) -> None:
    api_request(
        config,
        "POST",
        f"/api/worker/automation-jobs/{job_no}/fail",
        {
            "workerId": config["worker_id"],
            "commandLine": command_line,
            "executionLog": output,
            "resultJson": None,
            "errorMessage": error_message,
        },
    )


def run_worker_loop(
    config_path: Path,
    log_func=None,
    should_stop=None,
    status_callback=None,
    event_callback=None,
) -> int:
    if not config_path.exists():
        emit(log_func, f"worker config not found: {config_path}")
        if status_callback is not None:
            status_callback("CONFIG_MISSING")
        return 1

    config = load_config(config_path)
    emit(log_func, f"desktop worker started: {config['worker_id']}")
    emit(log_func, f"config path: {config_path}")
    emit(log_func, f"backend: {config['backend_base_url']}")
    emit(log_func, f"job type: {config['job_type']}")
    emit(log_func, f"python: {config['python_command']}")
    emit(log_func, f"sdk root: {config.get('sdk_root') or '-'}")
    emit(log_func, f"omni config: {config.get('config_path') or '-'}")
    emit(log_func, f"scripts: send={config['send_script']} delayed={config['delayed_send_script']}")
    if status_callback is not None:
        status_callback("IDLE")

    while not _should_stop(should_stop):
        try:
            if status_callback is not None:
                status_callback("POLLING")
            job = claim_job(config)
            if not job:
                _sleep_with_stop(float(config["poll_interval_seconds"]), should_stop)
                if status_callback is not None and not _should_stop(should_stop):
                    status_callback("IDLE")
                continue

            emit(log_func, f"[claim] {job['jobNo']} {job['jobType']}")
            payload = json.loads(job["payloadJson"])
            emit(log_func, f"[task] {job['jobNo']} {describe_job(config, payload)}")
            emit_event(
                event_callback,
                "claimed",
                jobNo=job["jobNo"],
                jobType=job["jobType"],
                targetConversation=payload.get("targetConversation"),
                countdownSeconds=int(payload.get("countdownSeconds") or 0),
                scriptName=resolve_script_name(config, payload),
                imageRequested=bool(payload.get("qrImagePath")),
            )
            if status_callback is not None:
                status_callback(f"RUNNING {job['jobNo']}")
            command_line = ""
            output = ""
            try:
                command_line, output, result_json = execute_job(config, job)
                emit(log_func, f"[exec] {job['jobNo']} {command_line}")
                report_success(config, job["jobNo"], command_line, output, result_json)
                emit_event(
                    event_callback,
                    "success",
                    jobNo=job["jobNo"],
                    targetConversation=payload.get("targetConversation"),
                    scriptName=resolve_script_name(config, payload),
                    imageRequested=bool(payload.get("qrImagePath")),
                )
                emit(log_func, f"[success] {job['jobNo']}")
                if output:
                    emit(log_func, output)
            except Exception as exc:  # noqa: BLE001
                error_message = str(exc)
                if not output:
                    output = error_message
                if command_line:
                    emit(log_func, f"[exec] {job['jobNo']} {command_line}")
                report_failure(config, job["jobNo"], command_line, output, error_message)
                emit_event(
                    event_callback,
                    "failed",
                    jobNo=job["jobNo"],
                    targetConversation=payload.get("targetConversation"),
                    errorMessage=error_message,
                    scriptName=resolve_script_name(config, payload),
                    imageRequested=bool(payload.get("qrImagePath")),
                )
                emit(log_func, f"[failed] {job['jobNo']} {error_message}")
                if output:
                    emit(log_func, output)
            finally:
                emit_event(event_callback, "idle")
                if status_callback is not None and not _should_stop(should_stop):
                    status_callback("IDLE")
        except urllib.error.URLError as exc:
            emit(log_func, f"[backend-unreachable] {exc.reason}")
            if status_callback is not None:
                status_callback("BACKEND_UNREACHABLE")
            _sleep_with_stop(float(config["poll_interval_seconds"]), should_stop)
        except KeyboardInterrupt:
            emit(log_func, "worker stopped")
            if status_callback is not None:
                status_callback("STOPPED")
            return 0
        except Exception as exc:  # noqa: BLE001
            emit(log_func, f"[worker-error] {exc}")
            if status_callback is not None:
                status_callback("WORKER_ERROR")
            _sleep_with_stop(float(config["poll_interval_seconds"]), should_stop)

    emit(log_func, "worker stopped")
    if status_callback is not None:
        status_callback("STOPPED")
    return 0


def _should_stop(should_stop) -> bool:
    if should_stop is None:
        return False
    return bool(should_stop())


def _sleep_with_stop(seconds: float, should_stop) -> None:
    end_at = time.time() + max(0.0, seconds)
    while time.time() < end_at:
        if _should_stop(should_stop):
            return
        time.sleep(min(0.2, end_at - time.time()))


def main() -> int:
    config_path = Path(sys.argv[1]).expanduser().resolve() if len(sys.argv) > 1 else DEFAULT_CONFIG_PATH
    return run_worker_loop(config_path)


if __name__ == "__main__":
    raise SystemExit(main())
