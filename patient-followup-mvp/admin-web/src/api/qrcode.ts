import { request } from "./client";
import type { QrCodeInfo } from "../types";

export async function createTemplateQrCode(templateId: number, expireDays = 30): Promise<QrCodeInfo> {
  const res = await request.post(`/api/admin/templates/${templateId}/qrcodes`, { expireDays });
  return res.data;
}

// 调微信 getwxacodeunlimit 偶尔会到 5–10s，本地默认 timeout=10s 容易超时；
// 这里单独把超时拉到 45s，并且如果后端实际返回的是 JSON（出错场景），把里面的 message 抛出来。
export async function fetchQrCodeImage(id: number): Promise<string> {
  const imageData = (await request.get(`/api/admin/qrcodes/${id}/image`, {
    responseType: "arraybuffer",
    timeout: 45000,
  })) as ArrayBuffer;

  if (looksLikeJsonError(imageData)) {
    const text = new TextDecoder("utf-8").decode(imageData);
    let message = text;
    try {
      const parsed = JSON.parse(text);
      message = parsed.message || parsed.error || text;
    } catch {
      // ignore parse failure, fall back to raw text
    }
    throw new Error(message || "二维码图片接口返回了异常内容");
  }

  const blob = new Blob([imageData], { type: "image/png" });
  return URL.createObjectURL(blob);
}

function looksLikeJsonError(buf: ArrayBuffer): boolean {
  if (!buf || buf.byteLength === 0) {
    return true;
  }
  const head = new Uint8Array(buf, 0, Math.min(2, buf.byteLength));
  // PNG starts with 0x89 0x50; JPEG with 0xFF 0xD8。
  // JSON 一般以 '{'(0x7B) 开头。
  return head[0] === 0x7b;
}
