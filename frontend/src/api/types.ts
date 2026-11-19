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

export type CreatePluginVersionRequest = PluginManifest;

export interface ManifestSchemaVersions {
  versions: string[];
}

export interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
  errors?: string[];
}

export type JsonSchema = Record<string, unknown>;
