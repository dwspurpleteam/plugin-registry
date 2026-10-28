export interface Plugin {
  id: string;
  name: string;
  description: string | null;
}

export interface PluginVersion {
  pluginId: string;
  version: string;
  name: string;
  description: string | null;
  frontendSdkVersion: string | null;
  backendSdkVersion: string | null;
}

export interface PagedResponse<T> {
  items: T[];
  page: number;
  limit: number;
  totalItems: number;
  totalPages: number;
}

export type PluginManifest = Record<string, unknown>;

export interface CreatePluginRequest {
  id: string;
  name: string;
  description: string | null;
}

export type MediaType = "AUDIO" | "VIDEO" | "SCREEN_SHARE" | "SCREEN_SHARE_AUDIO";

export interface CreatePluginVersionRequest {
  manifestVersion: string;
  pluginVersion: string;
  frontend: {
    sdkVersion: string;
    urlWeb: string;
  };
  backend: {
    sdkVersion: string;
    healthUrl: string;
    events: {
      eventUrl: string;
      availableEventTypes: string[];
      availableEventGroups: string[];
    };
    media: {
      input: MediaType[];
      output: MediaType[];
    };
  };
}

export interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
}
