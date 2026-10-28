import type {
  CreatePluginRequest,
  CreatePluginVersionRequest,
  PagedResponse,
  Plugin,
  PluginManifest,
  PluginVersion,
  ProblemDetail,
} from "./types";

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

export class ApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      Accept: "application/json",
      ...options?.headers,
    },
  });

  if (!response.ok) {
    let problem: ProblemDetail | undefined;
    try {
      problem = (await response.json()) as ProblemDetail;
    } catch {
      problem = undefined;
    }

    throw new ApiError(
      problem?.detail ?? problem?.title ?? `Запрос завершился с кодом ${response.status}`,
      response.status,
    );
  }

  return (await response.json()) as T;
}

export function getPlugins(page: number, limit: number, signal?: AbortSignal) {
  return request<PagedResponse<Plugin>>(
    `/v1/plugins?page=${page}&limit=${limit}`,
    { signal },
  );
}

export function getPluginVersions(
  pluginId: string,
  page: number,
  limit: number,
  signal?: AbortSignal,
) {
  return request<PagedResponse<PluginVersion>>(
    `/v1/plugins/${encodeURIComponent(pluginId)}/versions?page=${page}&limit=${limit}`,
    { signal },
  );
}

export function getPluginManifest(pluginId: string, version: string, signal?: AbortSignal) {
  return request<PluginManifest>(
    `/v1/plugins/${encodeURIComponent(pluginId)}/versions/${encodeURIComponent(version)}/manifest`,
    { signal },
  );
}

export function createPlugin(payload: CreatePluginRequest) {
  return request<Plugin>("/v1/plugins", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

export function createPluginVersion(pluginId: string, payload: CreatePluginVersionRequest) {
  return request<PluginVersion>(
    `/v1/plugins/${encodeURIComponent(pluginId)}/versions`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    },
  );
}
