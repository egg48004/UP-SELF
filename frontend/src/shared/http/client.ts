import axios from "axios";
import type { AxiosRequestConfig } from "axios";
import { session } from "../auth/session";
import type { Envelope, DataResult, TableResult } from "./contracts";
export class ApiError extends Error {
  constructor(
    public code: number,
    message: string,
  ) {
    super(message);
  }
}
let unauthorized: () => void = () => {};
export function onUnauthorized(fn: () => void) {
  unauthorized = fn;
}
const client = axios.create({
  baseURL: "/api",
  timeout: 20000,
  validateStatus: () => true,
});
export function assertSuccess(value: unknown): asserts value is Envelope {
  if (
    !value ||
    typeof value !== "object" ||
    !("code" in value) ||
    typeof value.code !== "number"
  )
    throw new ApiError(500, "The server returned an unreadable response.");
  const r = value as Envelope;
  if (r.code !== 200)
    throw new ApiError(
      r.code,
      typeof r.msg === "string" ? r.msg : "The request could not be completed.",
    );
}
export async function request<T extends Envelope>(
  url: string,
  config: AxiosRequestConfig = {},
): Promise<T> {
  try {
    const response = await client.request({
      url,
      ...config,
      headers: {
        ...(session.token ? { Authorization: "Bearer " + session.token } : {}),
        ...config.headers,
      },
    });
    assertSuccess(response.data);
    return response.data as T;
  } catch (e) {
    if (e instanceof ApiError) {
      if (e.code === 401) unauthorized();
      throw e;
    }
    if (axios.isCancel(e)) throw e;
    throw new ApiError(0, "Connection lost. Please try again.");
  }
}
export async function data<T>(url: string, config: AxiosRequestConfig = {}) {
  return (await request<DataResult<T>>(url, config)).data;
}
export function table<T>(url: string, config: AxiosRequestConfig = {}) {
  return request<TableResult<T>>(url, config);
}
export function mediaUrl(path: string) {
  if (!/^\/(client\/)?learning\/media\/[a-f0-9]{32}$/.test(path))
    throw new ApiError(400, "Invalid media address.");
  return "/api" + path;
}

// Binary requests keep the same authenticated client and business-error handling.
export async function download(url: string): Promise<Blob> {
  try {
    const response = await client.request<Blob>({
      url,
      responseType: "blob",
      headers: session.token
        ? { Authorization: "Bearer " + session.token }
        : {},
    });
    const mime = String(
      response.headers["content-type"] || response.data.type || "",
    );
    if (mime.includes("application/json")) {
      assertSuccess(JSON.parse(await response.data.text()));
      throw new ApiError(500, "The file could not be downloaded.");
    }
    if (response.status < 200 || response.status >= 300)
      throw new ApiError(response.status, "The file could not be downloaded.");
    return response.data;
  } catch (e) {
    if (e instanceof ApiError) {
      if (e.code === 401) unauthorized();
      throw e;
    }
    throw new ApiError(0, "Connection lost. Please try again.");
  }
}
