const { app, BrowserWindow, dialog, ipcMain } = require("electron");
const fs = require("fs");
const http = require("http");
const path = require("path");
const readline = require("readline");
const { spawn } = require("child_process");

const shellRoot = __dirname;
const projectRoot = path.resolve(shellRoot, "..", "..");
const workerRoot = path.resolve(shellRoot, "..");
const backendRoot = path.resolve(projectRoot, "backend");
const adminDistRoot = path.resolve(projectRoot, "admin-web", "dist");
const defaultConfigPath = path.join(workerRoot, "config.json");
const bridgePath = path.join(workerRoot, "electron_worker.py");
const omniHelperPath = path.join(workerRoot, "electron_omni_helper.py");
const desktopAppPort = 4173;
const desktopAppUrl = `http://127.0.0.1:${desktopAppPort}`;
const backendBaseUrl = "http://127.0.0.1:8080";

let mainWindow = null;
let workerProcess = null;
let workerStopTimer = null;
let backendProcess = null;
let backendStopTimer = null;
let botProcess = null;
let staticServer = null;
let currentConfigPath = defaultConfigPath;
let backendManagedByElectron = false;
let workerUiSuppressedForRpa = false;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1440,
    height: 920,
    minWidth: 1180,
    minHeight: 760,
    autoHideMenuBar: true,
    title: "患者随访桌面版",
    webPreferences: {
      preload: path.join(shellRoot, "preload.js"),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });

  mainWindow.loadFile(path.join(shellRoot, "renderer", "index.html"));

  mainWindow.on("closed", () => {
    mainWindow = null;
  });
}

function sendToRenderer(payload) {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.send("desktop:event", payload);
  }
}

function prepareWindowForRpa() {
  if (!mainWindow || mainWindow.isDestroyed() || workerUiSuppressedForRpa) {
    return;
  }
  workerUiSuppressedForRpa = true;
  mainWindow.setAlwaysOnTop(false);
  sendToRenderer({
    type: "ui:switch-tab",
    tab: "worker",
  });
  sendToRenderer({
    type: "worker:log",
    message: "[ui] 检测到任务开始执行，已取消置顶并切换到工作区。",
  });
}

function restoreWindowAfterRpa() {
  if (!mainWindow || mainWindow.isDestroyed() || !workerUiSuppressedForRpa) {
    return;
  }
  workerUiSuppressedForRpa = false;
  const summary = safeReadConfigSummary(currentConfigPath);
  mainWindow.setAlwaysOnTop(Boolean(summary.alwaysOnTop));
  sendToRenderer({
    type: "worker:log",
    message: "[ui] 任务执行阶段结束，已恢复窗口置顶设置。",
  });
}

function safeReadConfigSummary(configPath) {
  if (!configPath || !fs.existsSync(configPath)) {
    return {
      configPath,
      exists: false,
      backendBaseUrl: "",
      workerId: "",
      jobType: "",
      pythonCommand: "python",
      resolvedPythonCommand: "python",
      sdkRoot: "",
      omniConfigPath: "",
      alwaysOnTop: false,
    };
  }

  const parsed = loadWorkerConfig(configPath);
  const resolvedPythonCommand = resolvePythonCommand(parsed);
  return {
    configPath,
    exists: true,
    backendBaseUrl: String(parsed.backend_base_url || ""),
    workerId: String(parsed.worker_id || ""),
    jobType: String(parsed.job_type || ""),
    pythonCommand: String(parsed.python_command || "python"),
    resolvedPythonCommand,
    sdkRoot: String(parsed.sdk_root || ""),
    omniConfigPath: String(parsed.config_path || ""),
    alwaysOnTop: parsed.always_on_top !== false,
  };
}

function loadWorkerConfig(configPath) {
  const raw = fs.readFileSync(configPath, "utf-8");
  return JSON.parse(raw);
}

function resolvePythonCommand(config) {
  const configured = String(config.python_command || "python").trim() || "python";
  const normalized = configured.toLowerCase();
  if (
    configured &&
    !["python", "python.exe", "py"].includes(normalized) &&
    (path.isAbsolute(configured) || configured.includes("/") || configured.includes("\\"))
  ) {
    return configured;
  }

  const sdkRoot = String(config.sdk_root || "").trim();
  const candidates = [];
  if (sdkRoot) {
    candidates.push(path.join(sdkRoot, ".venv", "Scripts", "python.exe"));
    candidates.push(path.join(sdkRoot, "venv", "Scripts", "python.exe"));
    candidates.push(path.join(sdkRoot, ".env", "Scripts", "python.exe"));
  }
  candidates.push(path.join(workerRoot, ".venv", "Scripts", "python.exe"));
  candidates.push(path.join(projectRoot, ".venv", "Scripts", "python.exe"));

  for (const candidate of candidates) {
    if (fs.existsSync(candidate)) {
      return candidate;
    }
  }
  return configured;
}

function buildWorkerRuntime(configPath) {
  if (!configPath || !fs.existsSync(configPath)) {
    throw new Error(`配置文件不存在: ${configPath}`);
  }
  const config = loadWorkerConfig(configPath);
  return {
    config,
    configPath,
    resolvedPythonCommand: resolvePythonCommand(config),
    sdkRoot: String(config.sdk_root || "").trim(),
    omniConfigPath: String(config.config_path || "").trim(),
  };
}

function applyTopmostFromConfig(configPath) {
  const summary = safeReadConfigSummary(configPath);
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.setAlwaysOnTop(Boolean(summary.alwaysOnTop));
  }
  return summary;
}

function attachLineReader(stream, onLine) {
  const reader = readline.createInterface({ input: stream });
  reader.on("line", onLine);
  return reader;
}

function getContentType(filePath) {
  const extension = path.extname(filePath).toLowerCase();
  const mapping = {
    ".html": "text/html; charset=utf-8",
    ".js": "application/javascript; charset=utf-8",
    ".css": "text/css; charset=utf-8",
    ".json": "application/json; charset=utf-8",
    ".svg": "image/svg+xml",
    ".png": "image/png",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".woff": "font/woff",
    ".woff2": "font/woff2",
  };
  return mapping[extension] || "application/octet-stream";
}

function startAdminStaticServer() {
  if (staticServer) {
    return;
  }

  staticServer = http.createServer((request, response) => {
    const requestPath = decodeURIComponent((request.url || "/").split("?")[0]);
    let filePath = path.join(adminDistRoot, requestPath === "/" ? "index.html" : requestPath);

    if (!filePath.startsWith(adminDistRoot)) {
      response.writeHead(403);
      response.end("Forbidden");
      return;
    }

    if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
      filePath = path.join(adminDistRoot, "index.html");
    }

    try {
      const content = fs.readFileSync(filePath);
      response.writeHead(200, { "Content-Type": getContentType(filePath) });
      response.end(content);
    } catch (error) {
      response.writeHead(500, { "Content-Type": "text/plain; charset=utf-8" });
      response.end(String(error));
    }
  });

  staticServer.listen(desktopAppPort, "127.0.0.1");
}

function stopAdminStaticServer() {
  if (staticServer) {
    staticServer.close();
    staticServer = null;
  }
}

function clearWorkerStopTimer() {
  if (workerStopTimer) {
    clearTimeout(workerStopTimer);
    workerStopTimer = null;
  }
}

function clearBackendStopTimer() {
  if (backendStopTimer) {
    clearTimeout(backendStopTimer);
    backendStopTimer = null;
  }
}

function finalizeWorkerExit(code, signal) {
  clearWorkerStopTimer();
  workerProcess = null;
  restoreWindowAfterRpa();
  sendToRenderer({
    type: "worker:lifecycle",
    status: "STOPPED",
    exitCode: typeof code === "number" ? code : null,
    signal: signal || null,
  });
}

function finalizeBackendExit(code, signal) {
  clearBackendStopTimer();
  backendProcess = null;
  backendManagedByElectron = false;
  sendToRenderer({
    type: "backend:status",
    status: "STOPPED",
    exitCode: typeof code === "number" ? code : null,
    signal: signal || null,
  });
}

function parseBridgeLine(line) {
  try {
    return JSON.parse(line);
  } catch (_error) {
    return { type: "worker:log", message: line };
  }
}

async function wait(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

async function probeBackend(url) {
  try {
    const response = await fetch(`${url}/api/admin/auth/me`, {
      method: "GET",
    });
    return response.status > 0;
  } catch (_error) {
    return false;
  }
}

async function waitBackendReady(timeoutMs = 45000) {
  const startedAt = Date.now();
  while (Date.now() - startedAt < timeoutMs) {
    if (await probeBackend(backendBaseUrl)) {
      sendToRenderer({
        type: "backend:status",
        status: "READY",
      });
      return true;
    }
    await wait(1000);
  }
  return false;
}

function startWorker(configPath) {
  if (workerProcess) {
    throw new Error("worker 已在运行");
  }

  if (!fs.existsSync(bridgePath)) {
    throw new Error(`bridge 脚本不存在: ${bridgePath}`);
  }

  const runtime = buildWorkerRuntime(configPath);
  const summary = safeReadConfigSummary(configPath);
  if (!summary.exists) {
    throw new Error(`配置文件不存在: ${configPath}`);
  }

  currentConfigPath = configPath;
  const child = spawn(runtime.resolvedPythonCommand, ["-u", bridgePath, configPath], {
    cwd: workerRoot,
    stdio: ["pipe", "pipe", "pipe"],
    env: {
      ...process.env,
      PYTHONIOENCODING: "utf-8",
      PYTHONUTF8: "1",
      PYTHONUNBUFFERED: "1",
    },
  });

  workerProcess = child;
  clearWorkerStopTimer();
  child.stdout.setEncoding("utf8");
  child.stderr.setEncoding("utf8");

  attachLineReader(child.stdout, (line) => {
    const payload = parseBridgeLine(line);
    if (payload.type === "event" && payload.event?.type === "claimed") {
      prepareWindowForRpa();
    }
    if (payload.type === "event" && ["success", "failed", "idle"].includes(payload.event?.type)) {
      restoreWindowAfterRpa();
    }
    sendToRenderer(payload.type.startsWith("worker:") ? payload : { ...payload, type: payload.type });
  });

  attachLineReader(child.stderr, (line) => {
    sendToRenderer({
      type: "worker:log",
      message: `[stderr] ${line}`,
    });
  });

  child.on("error", (error) => {
    sendToRenderer({
      type: "worker:error",
      message: String(error.message || error),
    });
  });

  child.on("exit", (code, signal) => {
    finalizeWorkerExit(code, signal);
  });

  sendToRenderer({
    type: "worker:config",
    summary,
  });

  return summary;
}

function stopWorker() {
  if (!workerProcess) {
    return false;
  }

  sendToRenderer({
    type: "worker:lifecycle",
    status: "STOPPING",
  });

  if (workerProcess.stdin && !workerProcess.stdin.destroyed) {
    workerProcess.stdin.write("stop\n");
  }

  clearWorkerStopTimer();
  workerStopTimer = setTimeout(() => {
    if (workerProcess) {
      if (process.platform === "win32") {
        spawn("taskkill", ["/pid", String(workerProcess.pid), "/t", "/f"], {
          windowsHide: true,
        });
      } else {
        workerProcess.kill("SIGKILL");
      }
    }
  }, 5000);

  return true;
}

function ensureOmniRuntime(configPath = currentConfigPath) {
  const runtime = buildWorkerRuntime(configPath);
  if (!runtime.sdkRoot) {
    throw new Error("worker config.json 里没有 sdk_root，无法启动 omni 功能");
  }
  if (!fs.existsSync(runtime.sdkRoot)) {
    throw new Error(`sdk_root 路径不存在: ${runtime.sdkRoot}`);
  }
  if (!runtime.omniConfigPath) {
    throw new Error("worker config.json 里没有 config_path，无法读取 omni 配置");
  }
  if (!fs.existsSync(runtime.omniConfigPath)) {
    throw new Error(`omni config.yaml 不存在: ${runtime.omniConfigPath}`);
  }
  return runtime;
}

function runPythonJson(runtime, scriptPath, args, payload = null) {
  return new Promise((resolve, reject) => {
    const child = spawn(runtime.resolvedPythonCommand, ["-u", scriptPath, ...args], {
      cwd: workerRoot,
      stdio: ["pipe", "pipe", "pipe"],
      windowsHide: true,
    });
    let stdout = "";
    let stderr = "";

    child.stdout.on("data", (chunk) => {
      stdout += chunk.toString("utf-8");
    });
    child.stderr.on("data", (chunk) => {
      stderr += chunk.toString("utf-8");
    });
    child.on("error", (error) => reject(error));
    child.on("exit", (code) => {
      if (code !== 0) {
        try {
          const parsed = JSON.parse(stdout || "{}");
          reject(new Error(parsed.error || stderr || `python helper exited with ${code}`));
          return;
        } catch (_error) {
          reject(new Error(stderr || stdout || `python helper exited with ${code}`));
          return;
        }
      }
      try {
        const parsed = JSON.parse(stdout || "{}");
        if (!parsed.ok) {
          reject(new Error(parsed.error || "helper failed"));
          return;
        }
        resolve(parsed.data || {});
      } catch (error) {
        reject(error);
      }
    });
    if (payload) {
      child.stdin.write(JSON.stringify(payload));
    }
    child.stdin.end();
  });
}

async function loadOmniSettings(configPath = currentConfigPath) {
  const runtime = ensureOmniRuntime(configPath);
  const data = await runPythonJson(runtime, omniHelperPath, ["read", runtime.omniConfigPath]);
  return {
    ...data,
    omniConfigPath: runtime.omniConfigPath,
    resolvedPythonCommand: runtime.resolvedPythonCommand,
  };
}

async function saveOmniSettings(configPath, settings) {
  const runtime = ensureOmniRuntime(configPath);
  return runPythonJson(runtime, omniHelperPath, ["write", runtime.omniConfigPath], settings);
}

function isBotRunning() {
  return botProcess && botProcess.exitCode === null;
}

function startBot(configPath = currentConfigPath) {
  if (isBotRunning()) {
    throw new Error("Bot 已在运行");
  }
  const runtime = ensureOmniRuntime(configPath);
  const runBotPath = path.join(runtime.sdkRoot, "run_bot.py");
  if (!fs.existsSync(runBotPath)) {
    throw new Error(`找不到 run_bot.py: ${runBotPath}`);
  }

  const child = spawn(runtime.resolvedPythonCommand, ["-u", runBotPath, "--config", runtime.omniConfigPath], {
    cwd: runtime.sdkRoot,
    stdio: ["ignore", "pipe", "pipe"],
    windowsHide: true,
    env: {
      ...process.env,
      PYTHONIOENCODING: "utf-8",
      PYTHONUNBUFFERED: "1",
    },
  });

  botProcess = child;
  sendToRenderer({ type: "bot:status", status: "RUNNING" });
  sendToRenderer({ type: "bot:log", message: `启动 Bot: ${runtime.resolvedPythonCommand} -u run_bot.py --config ${runtime.omniConfigPath}` });

  attachLineReader(child.stdout, (line) => {
    sendToRenderer({ type: "bot:log", message: line });
  });
  attachLineReader(child.stderr, (line) => {
    sendToRenderer({ type: "bot:log", message: `[stderr] ${line}` });
  });
  child.on("error", (error) => {
    sendToRenderer({ type: "bot:error", message: String(error.message || error) });
  });
  child.on("exit", (code, signal) => {
    botProcess = null;
    sendToRenderer({ type: "bot:status", status: "STOPPED", exitCode: typeof code === "number" ? code : null, signal: signal || null });
  });
  return {
    running: true,
    resolvedPythonCommand: runtime.resolvedPythonCommand,
    omniConfigPath: runtime.omniConfigPath,
  };
}

function stopBot() {
  if (!isBotRunning()) {
    return false;
  }
  const processToStop = botProcess;
  sendToRenderer({ type: "bot:status", status: "STOPPING" });
  if (process.platform === "win32") {
    spawn("taskkill", ["/pid", String(processToStop.pid), "/t", "/f"], {
      windowsHide: true,
    });
  } else {
    processToStop.kill("SIGTERM");
  }
  return true;
}

async function startBackend() {
  if (backendProcess) {
    throw new Error("backend 已在运行");
  }

  if (await probeBackend(backendBaseUrl)) {
    sendToRenderer({
      type: "backend:status",
      status: "READY",
    });
    sendToRenderer({
      type: "backend:log",
      message: "检测到 8080 已有可用后端，直接复用现有进程",
    });
    backendManagedByElectron = false;
    return { backendBaseUrl, reusedExisting: true };
  }

  sendToRenderer({
    type: "backend:status",
    status: "STARTING",
  });

  const command =
    process.platform === "win32"
      ? "mvn spring-boot:run \"-Dspring-boot.run.profiles=mysql,local\""
      : "mvn spring-boot:run -Dspring-boot.run.profiles=mysql,local";
  const child =
    process.platform === "win32"
      ? spawn("cmd.exe", ["/d", "/s", "/c", command], {
          cwd: backendRoot,
          stdio: ["ignore", "pipe", "pipe"],
          windowsHide: true,
        })
      : spawn("sh", ["-lc", command], {
          cwd: backendRoot,
          stdio: ["ignore", "pipe", "pipe"],
        });

  backendProcess = child;
  backendManagedByElectron = true;
  clearBackendStopTimer();

  attachLineReader(child.stdout, (line) => {
    sendToRenderer({
      type: "backend:log",
      message: line,
    });
    if (line.includes("Tomcat started on port")) {
      sendToRenderer({
        type: "backend:status",
        status: "READY",
      });
    }
  });

  attachLineReader(child.stderr, (line) => {
    sendToRenderer({
      type: "backend:log",
      message: `[stderr] ${line}`,
    });
  });

  child.on("error", (error) => {
    sendToRenderer({
      type: "backend:error",
      message: String(error.message || error),
    });
  });

  child.on("exit", (code, signal) => {
    finalizeBackendExit(code, signal);
  });

  waitBackendReady().then((ready) => {
    if (!ready && backendProcess) {
      sendToRenderer({
        type: "backend:status",
        status: "START_TIMEOUT",
      });
    }
  });

  return { backendBaseUrl, reusedExisting: false };
}

function stopBackend() {
  if (!backendProcess) {
    return false;
  }

  sendToRenderer({
    type: "backend:status",
    status: "STOPPING",
  });

  clearBackendStopTimer();
  backendStopTimer = setTimeout(() => {
    if (backendProcess) {
      backendProcess.kill();
    }
  }, 6000);

  if (!backendManagedByElectron) {
    sendToRenderer({
      type: "backend:log",
      message: "当前后端不是桌面壳启动的，不执行停止操作",
    });
    sendToRenderer({
      type: "backend:status",
      status: "READY",
    });
    return false;
  }

  if (process.platform === "win32") {
    spawn("taskkill", ["/pid", String(backendProcess.pid), "/t", "/f"], {
      windowsHide: true,
    });
  } else {
    backendProcess.kill("SIGTERM");
  }
  return true;
}

ipcMain.handle("desktop:get-initial-state", async () => {
  const summary = applyTopmostFromConfig(currentConfigPath);
  const backendAvailable = await probeBackend(backendBaseUrl);
  return {
    configPath: currentConfigPath,
    workerSummary: summary,
    workerRunning: Boolean(workerProcess),
    backendRunning: Boolean(backendProcess) || backendAvailable,
    backendBaseUrl,
    adminUrl: desktopAppUrl,
    backendManagedByElectron,
    botRunning: Boolean(isBotRunning()),
    botStatus: isBotRunning() ? "RUNNING" : "STOPPED",
  };
});

ipcMain.handle("worker:choose-config", async () => {
  const result = await dialog.showOpenDialog({
    title: "选择 worker 配置文件",
    defaultPath: fs.existsSync(currentConfigPath) ? currentConfigPath : workerRoot,
    properties: ["openFile"],
    filters: [{ name: "JSON", extensions: ["json"] }],
  });

  if (result.canceled || result.filePaths.length === 0) {
    return null;
  }

  currentConfigPath = result.filePaths[0];
  const summary = applyTopmostFromConfig(currentConfigPath);
  sendToRenderer({ type: "worker:config", summary });
  return {
    configPath: currentConfigPath,
    summary,
  };
});

ipcMain.handle("worker:start", async (_event, configPath) => {
  const targetConfigPath = configPath || currentConfigPath;
  const summary = startWorker(targetConfigPath);
  return {
    ok: true,
    summary,
  };
});

ipcMain.handle("worker:stop", async () => {
  return { ok: stopWorker() };
});

ipcMain.handle("omni:load-settings", async (_event, configPath) => {
  return {
    ok: true,
    data: await loadOmniSettings(configPath || currentConfigPath),
  };
});

ipcMain.handle("omni:save-settings", async (_event, payload) => {
  return {
    ok: true,
    data: await saveOmniSettings(payload?.configPath || currentConfigPath, payload?.settings || {}),
  };
});

ipcMain.handle("omni:clear-memory", async (_event, persistPath) => {
  if (!persistPath) {
    return { ok: true, deleted: false };
  }
  if (fs.existsSync(persistPath)) {
    fs.unlinkSync(persistPath);
    return { ok: true, deleted: true };
  }
  return { ok: true, deleted: false };
});

ipcMain.handle("bot:start", async (_event, configPath) => {
  return {
    ok: true,
    data: startBot(configPath || currentConfigPath),
  };
});

ipcMain.handle("bot:stop", async () => {
  return {
    ok: stopBot(),
  };
});

ipcMain.handle("backend:start", async () => {
  return {
    ok: true,
    info: await startBackend(),
  };
});

ipcMain.handle("backend:stop", async () => {
  return {
    ok: stopBackend(),
  };
});

ipcMain.handle("window:set-topmost", async (_event, topmost) => {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.setAlwaysOnTop(Boolean(topmost));
  }
  return { ok: true };
});

app.whenReady().then(() => {
  startAdminStaticServer();
  createWindow();
  applyTopmostFromConfig(currentConfigPath);

  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on("before-quit", () => {
  if (workerProcess) {
    stopWorker();
  }
  if (isBotRunning()) {
    stopBot();
  }
  if (backendProcess) {
    stopBackend();
  }
  stopAdminStaticServer();
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") {
    app.quit();
  }
});
