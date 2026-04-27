from __future__ import annotations

import json
import locale
import os
import signal
import socket
import subprocess
import sys
import threading
import time
import traceback
import urllib.error
import urllib.request
import ctypes
from ctypes import wintypes
from pathlib import Path


ROOT = Path(__file__).resolve().parent
DEFAULT_CONFIG_PATH = ROOT / "config.json"

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")


def emit(message_type: str, **payload) -> None:
    sys.stdout.write(json.dumps({"type": message_type, **payload}, ensure_ascii=False) + "\n")
    sys.stdout.flush()


def emit_event(event_type: str, **payload) -> None:
    emit("event", event={"type": event_type, **payload})


def log(message: str) -> None:
    emit("log", message=message)


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
    config.setdefault("startup_claim_delay_seconds", 10)
    config.setdefault("manual_takeover_on_mouse", True)
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
            log(f"[worker] 警告: 提醒图片不存在，将跳过该图片: {image_file}")
    return command


def terminate_process_tree(process: subprocess.Popen) -> None:
    if process.poll() is not None:
        return
    try:
        if os.name == "nt":
            subprocess.run(
                ["taskkill", "/pid", str(process.pid), "/t", "/f"],
                capture_output=True,
                check=False,
                text=True,
            )
        else:
            process.kill()
    except Exception as exc:  # noqa: BLE001
        log(f"[worker] 终止子进程失败: {exc}")


class ManualMouseTakeoverGuard:
    def __init__(self, enabled: bool) -> None:
        self.enabled = enabled and os.name == "nt"
        self.triggered = threading.Event()
        self.stop_requested = threading.Event()
        self.thread: threading.Thread | None = None
        self.thread_id: int | None = None
        self.reason = ""
        self.startup_error = ""
        self._hook = None
        self._callback = None

    def start(self) -> None:
        if not self.enabled or self.thread is not None:
            return
        self.thread = threading.Thread(target=self._run, daemon=True)
        self.thread.start()

    def stop(self) -> None:
        self.stop_requested.set()
        if self.enabled and self.thread_id:
            ctypes.windll.user32.PostThreadMessageW(self.thread_id, 0x0012, 0, 0)
        if self.thread is not None:
            self.thread.join(timeout=1)
        self.thread = None

    def is_triggered(self) -> bool:
        return self.triggered.is_set()

    def message(self) -> str:
        return self.reason or "检测到人工鼠标输入，已中止当前 RPA 任务。"

    def _run(self) -> None:
        user32 = ctypes.WinDLL("user32", use_last_error=True)
        kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)

        HC_ACTION = 0
        WH_MOUSE_LL = 14
        WM_MOUSEMOVE = 0x0200
        WM_LBUTTONDOWN = 0x0201
        WM_RBUTTONDOWN = 0x0204
        WM_MBUTTONDOWN = 0x0207
        WM_MOUSEWHEEL = 0x020A
        WM_MOUSEHWHEEL = 0x020E
        WM_XBUTTONDOWN = 0x020B
        LLMHF_INJECTED = 0x00000001
        interesting = {
            WM_MOUSEMOVE,
            WM_LBUTTONDOWN,
            WM_RBUTTONDOWN,
            WM_MBUTTONDOWN,
            WM_MOUSEWHEEL,
            WM_MOUSEHWHEEL,
            WM_XBUTTONDOWN,
        }

        class POINT(ctypes.Structure):
            _fields_ = [("x", wintypes.LONG), ("y", wintypes.LONG)]

        class MSLLHOOKSTRUCT(ctypes.Structure):
            _fields_ = [
                ("pt", POINT),
                ("mouseData", wintypes.DWORD),
                ("flags", wintypes.DWORD),
                ("time", wintypes.DWORD),
                ("dwExtraInfo", ctypes.c_void_p),
            ]

        class MSG(ctypes.Structure):
            _fields_ = [
                ("hwnd", wintypes.HWND),
                ("message", wintypes.UINT),
                ("wParam", wintypes.WPARAM),
                ("lParam", wintypes.LPARAM),
                ("time", wintypes.DWORD),
                ("pt", POINT),
                ("lPrivate", wintypes.DWORD),
            ]

        low_level_proc = ctypes.WINFUNCTYPE(wintypes.LPARAM, ctypes.c_int, wintypes.WPARAM, wintypes.LPARAM)

        kernel32.GetCurrentThreadId.argtypes = []
        kernel32.GetCurrentThreadId.restype = wintypes.DWORD
        kernel32.GetModuleHandleW.argtypes = [wintypes.LPCWSTR]
        kernel32.GetModuleHandleW.restype = wintypes.HMODULE

        user32.SetWindowsHookExW.argtypes = [
            ctypes.c_int,
            low_level_proc,
            wintypes.HINSTANCE,
            wintypes.DWORD,
        ]
        user32.SetWindowsHookExW.restype = wintypes.HHOOK
        user32.CallNextHookEx.argtypes = [
            wintypes.HHOOK,
            ctypes.c_int,
            wintypes.WPARAM,
            wintypes.LPARAM,
        ]
        user32.CallNextHookEx.restype = wintypes.LPARAM
        user32.PostThreadMessageW.argtypes = [
            wintypes.DWORD,
            wintypes.UINT,
            wintypes.WPARAM,
            wintypes.LPARAM,
        ]
        user32.PostThreadMessageW.restype = wintypes.BOOL
        user32.GetMessageW.argtypes = [
            ctypes.POINTER(MSG),
            wintypes.HWND,
            wintypes.UINT,
            wintypes.UINT,
        ]
        user32.GetMessageW.restype = ctypes.c_int
        user32.TranslateMessage.argtypes = [ctypes.POINTER(MSG)]
        user32.TranslateMessage.restype = wintypes.BOOL
        user32.DispatchMessageW.argtypes = [ctypes.POINTER(MSG)]
        user32.DispatchMessageW.restype = wintypes.LPARAM
        user32.UnhookWindowsHookEx.argtypes = [wintypes.HHOOK]
        user32.UnhookWindowsHookEx.restype = wintypes.BOOL

        def callback(code, w_param, l_param):
            if code == HC_ACTION and w_param in interesting:
                info = ctypes.cast(l_param, ctypes.POINTER(MSLLHOOKSTRUCT)).contents
                if not info.flags & LLMHF_INJECTED:
                    self.reason = f"检测到人工鼠标输入，位置=({info.pt.x}, {info.pt.y})，已切换为人工接管。"
                    self.triggered.set()
                    user32.PostThreadMessageW(self.thread_id, 0x0012, 0, 0)
            return user32.CallNextHookEx(self._hook, code, w_param, l_param)

        self._callback = low_level_proc(callback)
        self.thread_id = kernel32.GetCurrentThreadId()
        module_handle = kernel32.GetModuleHandleW(None)
        self._hook = user32.SetWindowsHookExW(WH_MOUSE_LL, self._callback, module_handle, 0)
        if not self._hook:
            last_error = ctypes.get_last_error()
            self.startup_error = f"人工接管保护启动失败，未能安装鼠标钩子。win32={last_error}"
            return

        try:
            msg = MSG()
            while not self.stop_requested.is_set():
                result = user32.GetMessageW(ctypes.byref(msg), 0, 0, 0)
                if result in (0, -1):
                    break
                user32.TranslateMessage(ctypes.byref(msg))
                user32.DispatchMessageW(ctypes.byref(msg))
        finally:
            if self._hook:
                user32.UnhookWindowsHookEx(self._hook)
                self._hook = None


def stream_reader(stream, sink, stop_event: threading.Event) -> None:
    buffer = ""
    try:
        while not stop_event.is_set():
            chunk = stream.read(1)
            if chunk == "":
                break
            if chunk in {"\r", "\n"}:
                line = buffer.strip()
                if line:
                    sink(line)
                buffer = ""
                continue
            buffer += chunk
    finally:
        line = buffer.strip()
        if line:
            sink(line)


def execute_job_streaming(config: dict, job: dict, stop_event: threading.Event) -> tuple[str, str, str]:
    payload = json.loads(job["payloadJson"])
    command = build_command(config, payload)
    sdk_root = Path(config["sdk_root"]).expanduser().resolve()
    env = os.environ.copy()
    env.setdefault("PYTHONIOENCODING", "utf-8")
    env.setdefault("PYTHONUTF8", "1")
    env.setdefault("PYTHONUNBUFFERED", "1")
    command_line = " ".join(command)
    log(f"[exec-start] {job['jobNo']} {command_line}")
    output_lines: list[str] = []
    stream_stop = threading.Event()
    takeover_guard = ManualMouseTakeoverGuard(bool(config.get("manual_takeover_on_mouse", True)))
    if takeover_guard.enabled:
        log("[safety] 已启用人工接管保护，任务执行期间手动触碰鼠标将立即中止当前 RPA。")
    else:
        log("[safety] 当前平台未启用人工接管保护。")

    def sink_stdout(line: str) -> None:
        output_lines.append(line)
        log(f"[script] {line}")

    def sink_stderr(line: str) -> None:
        tagged = f"[stderr] {line}"
        output_lines.append(tagged)
        log(f"[script] {tagged}")

    process = subprocess.Popen(
        command,
        cwd=str(sdk_root),
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        stdin=subprocess.DEVNULL,
        text=True,
        encoding="utf-8",
        errors="replace",
        env=env,
        bufsize=1,
    )

    assert process.stdout is not None
    assert process.stderr is not None
    stdout_thread = threading.Thread(target=stream_reader, args=(process.stdout, sink_stdout, stream_stop), daemon=True)
    stderr_thread = threading.Thread(target=stream_reader, args=(process.stderr, sink_stderr, stream_stop), daemon=True)
    stdout_thread.start()
    stderr_thread.start()
    takeover_guard.start()
    if takeover_guard.startup_error:
        log(f"[safety] {takeover_guard.startup_error}")

    started_at = time.time()
    timeout_seconds = int(config["timeout_seconds"])
    try:
        while process.poll() is None:
            if stop_event.is_set():
                terminate_process_tree(process)
                raise RuntimeError("worker stopped")
            if takeover_guard.is_triggered():
                terminate_process_tree(process)
                raise RuntimeError(takeover_guard.message())
            if time.time() - started_at > timeout_seconds:
                terminate_process_tree(process)
                raise RuntimeError(f"command timed out after {timeout_seconds} seconds")
            time.sleep(0.2)
    finally:
        stream_stop.set()
        takeover_guard.stop()
        stdout_thread.join(timeout=1)
        stderr_thread.join(timeout=1)

    output = "\n".join(output_lines).strip()
    result_json = json.dumps(
        {
            "exitCode": process.returncode,
            "taskNo": payload.get("taskNo"),
            "reminderTaskId": payload.get("reminderTaskId"),
        },
        ensure_ascii=False,
    )
    if process.returncode != 0:
        raise RuntimeError(output or f"command exited with code {process.returncode}")
    return command_line, output, result_json


def run_worker_loop(config_path: Path, stop_event: threading.Event) -> int:
    if not config_path.exists():
        log(f"worker config not found: {config_path}")
        emit("status", status="CONFIG_MISSING")
        return 1

    config = load_config(config_path)
    log(f"desktop worker started: {config['worker_id']}")
    log(f"config path: {config_path}")
    log(f"backend: {config['backend_base_url']}")
    log(f"job type: {config['job_type']}")
    log(f"python: {config['python_command']}")
    log(f"sdk root: {config.get('sdk_root') or '-'}")
    log(f"omni config: {config.get('config_path') or '-'}")
    log(f"scripts: send={config['send_script']} delayed={config['delayed_send_script']}")
    startup_delay_seconds = max(0, int(config.get("startup_claim_delay_seconds") or 0))
    if startup_delay_seconds > 0:
        log(f"[startup-delay] worker 已启动，{startup_delay_seconds}s 后开始抢任务，请先把微信切到前台。")
        while startup_delay_seconds > 0 and not stop_event.is_set():
            emit("status", status=f"STARTUP_DELAY {startup_delay_seconds}s")
            time.sleep(1)
            startup_delay_seconds -= 1
        if stop_event.is_set():
            log("worker stopped")
            emit("status", status="STOPPED")
            return 0
        log("[startup-delay] 启动等待结束，开始抢任务。")
    emit("status", status="IDLE")

    while not stop_event.is_set():
        try:
            emit("status", status="POLLING")
            job = claim_job(config)
            if not job:
                sleep_seconds = float(config["poll_interval_seconds"])
                end_at = time.time() + max(0.0, sleep_seconds)
                while time.time() < end_at and not stop_event.is_set():
                    time.sleep(min(0.2, end_at - time.time()))
                if not stop_event.is_set():
                    emit("status", status="IDLE")
                continue

            log(f"[claim] {job['jobNo']} {job['jobType']}")
            payload = json.loads(job["payloadJson"])
            log(f"[task] {job['jobNo']} {describe_job(config, payload)}")
            emit_event(
                "claimed",
                jobNo=job["jobNo"],
                jobType=job["jobType"],
                targetConversation=payload.get("targetConversation"),
                countdownSeconds=int(payload.get("countdownSeconds") or 0),
                scriptName=resolve_script_name(config, payload),
                imageRequested=bool(payload.get("qrImagePath")),
            )
            emit("status", status=f"RUNNING {job['jobNo']}")

            command_line = ""
            output = ""
            try:
                command_line, output, result_json = execute_job_streaming(config, job, stop_event)
                log(f"[exec] {job['jobNo']} {command_line}")
                report_success(config, job["jobNo"], command_line, output, result_json)
                emit_event(
                    "success",
                    jobNo=job["jobNo"],
                    targetConversation=payload.get("targetConversation"),
                    scriptName=resolve_script_name(config, payload),
                    imageRequested=bool(payload.get("qrImagePath")),
                )
                log(f"[success] {job['jobNo']}")
            except Exception as exc:  # noqa: BLE001
                error_message = str(exc)
                if command_line:
                    log(f"[exec] {job['jobNo']} {command_line}")
                if not output:
                    output = error_message
                report_failure(config, job["jobNo"], command_line, output, error_message)
                emit_event(
                    "failed",
                    jobNo=job["jobNo"],
                    targetConversation=payload.get("targetConversation"),
                    errorMessage=error_message,
                    scriptName=resolve_script_name(config, payload),
                    imageRequested=bool(payload.get("qrImagePath")),
                )
                log(f"[failed] {job['jobNo']} {error_message}")
            finally:
                emit_event("idle")
                if not stop_event.is_set():
                    emit("status", status="IDLE")
        except urllib.error.URLError as exc:
            log(f"[backend-unreachable] {exc.reason}")
            emit("status", status="BACKEND_UNREACHABLE")
            time.sleep(float(config["poll_interval_seconds"]))
        except KeyboardInterrupt:
            log("worker stopped")
            emit("status", status="STOPPED")
            return 0
        except Exception as exc:  # noqa: BLE001
            log(f"[worker-error] {exc}")
            emit("status", status="WORKER_ERROR")
            time.sleep(float(config["poll_interval_seconds"]))

    log("worker stopped")
    emit("status", status="STOPPED")
    return 0


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
        exit_code = run_worker_loop(config_path, stop_event)
    except Exception as exc:  # noqa: BLE001
        emit("error", message=str(exc), traceback=traceback.format_exc())
        return 1

    emit("lifecycle", status="STOPPED", exitCode=exit_code)
    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
