const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("desktopWorker", {
  getInitialState: () => ipcRenderer.invoke("desktop:get-initial-state"),
  chooseConfig: () => ipcRenderer.invoke("worker:choose-config"),
  startWorker: (configPath) => ipcRenderer.invoke("worker:start", configPath),
  stopWorker: () => ipcRenderer.invoke("worker:stop"),
  loadOmniSettings: (configPath) => ipcRenderer.invoke("omni:load-settings", configPath),
  saveOmniSettings: (payload) => ipcRenderer.invoke("omni:save-settings", payload),
  clearOmniMemory: (persistPath) => ipcRenderer.invoke("omni:clear-memory", persistPath),
  startBot: (configPath) => ipcRenderer.invoke("bot:start", configPath),
  stopBot: () => ipcRenderer.invoke("bot:stop"),
  startBackend: () => ipcRenderer.invoke("backend:start"),
  stopBackend: () => ipcRenderer.invoke("backend:stop"),
  setTopmost: (topmost) => ipcRenderer.invoke("window:set-topmost", topmost),
  onEvent: (callback) => {
    const listener = (_event, payload) => callback(payload);
    ipcRenderer.on("desktop:event", listener);
    return () => ipcRenderer.removeListener("desktop:event", listener);
  },
});
