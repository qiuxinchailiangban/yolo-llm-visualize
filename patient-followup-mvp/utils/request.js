let BASE_URL = "http://localhost:8080";

try {
  const localConfig = require("../config/local");
  if (localConfig && typeof localConfig.BASE_URL === "string" && localConfig.BASE_URL.trim()) {
    BASE_URL = localConfig.BASE_URL.trim();
  }
} catch (error) {
  // Use default localhost during initial development when no local config exists.
}

function request({ url, method = "GET", data }) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      header: {
        "content-type": "application/json",
      },
      success(res) {
        if (res.data && res.data.success) {
          resolve(res.data.data);
        } else {
          reject(new Error(res.data?.message || "请求失败"));
        }
      },
      fail: reject,
    });
  });
}

module.exports = { request };
