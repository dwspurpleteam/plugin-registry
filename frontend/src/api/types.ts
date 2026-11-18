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
  "manifest-version": "1.1";
  "plugin-id": string;
  "plugin-version": string;
  name: string;
  description?: string;
  frontend: {
    "url-web": string;
    "sdk-version"?: string;
  };
  backend: {
    "health-url": string;
    "control-url": string;
    "sdk-version"?: string;
    "initial-state"?: string[];
    events?: {
      "available-event-types"?: string[];
      "available-event-groups"?: string[];
    };
    media?: {
      input?: MediaType[];
      output?: MediaType[];
    };
    results?: {
      keys: string[];
      "delivery-report-url"?: string;
    };
  };
}

export interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
  errors?: string[];
}

export type JsonSchema = Record<string, unknown>;
