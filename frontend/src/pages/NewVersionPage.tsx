import { type FormEvent, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { createPluginVersion } from "../api/pluginRegistry";
import type {
  CreatePluginVersionRequest,
  MediaType,
  PluginManifest,
} from "../api/types";
import { PageHeader } from "../components/PageHeader";

function defaultManifest(pluginId: string) {
  return JSON.stringify(
    {
      "manifest-version": "0.1",
      "plugin-id": pluginId,
      "plugin-version": "1.0.0",
      name: "new-plugin-version",
      description: "",
      frontend: {
        "sdk-version": "1.0.0",
        "url-web": "https://example.com/plugin-ui/web",
      },
      backend: {
        "sdk-version": "1.0.0",
        "health-url": "https://example.com/api/healthzz",
        events: {
          "event-url": "https://example.com/api/events",
          "available-event-groups": ["participant", "media", "interaction"],
        },
        media: {
          input: ["AUDIO"],
          output: [],
        },
      },
    },
    null,
    2,
  );
}

interface FormErrors {
  version?: string;
  manifest?: string;
  submit?: string;
}

export function NewVersionPage() {
  const { pluginId = "" } = useParams();
  const navigate = useNavigate();
  const [version, setVersion] = useState("1.0.0");
  const [manifestText, setManifestText] = useState(() => defaultManifest(pluginId));
  const [errors, setErrors] = useState<FormErrors>({});
  const [submitting, setSubmitting] = useState(false);

  const jsonStatus = useMemo(() => {
    try {
      JSON.parse(manifestText);
      return { valid: true, label: "JSON корректен" };
    } catch {
      return { valid: false, label: "JSON содержит ошибку" };
    }
  }, [manifestText]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextErrors: FormErrors = {};
    let manifest: PluginManifest | null = null;
    let request: CreatePluginVersionRequest | null = null;

    if (!version.trim()) {
      nextErrors.version = "Укажите номер версии.";
    }

    try {
      const parsed: unknown = JSON.parse(manifestText);
      if (!parsed || Array.isArray(parsed) || typeof parsed !== "object") {
        nextErrors.manifest = "Манифест должен быть JSON-объектом.";
      } else {
        manifest = parsed as PluginManifest;
        if (manifest["plugin-id"] !== pluginId) {
          nextErrors.manifest = `Поле plugin-id должно быть равно «${pluginId}».`;
        } else if (manifest["plugin-version"] !== version.trim()) {
          nextErrors.manifest = "Поле plugin-version должно совпадать с номером версии формы.";
        } else {
          request = toCreateVersionRequest(manifest);
        }
      }
    } catch (error) {
      nextErrors.manifest = error instanceof Error
        ? error.message
        : "Исправьте синтаксис JSON перед публикацией.";
    }

    if (Object.keys(nextErrors).length > 0 || !manifest || !request) {
      setErrors(nextErrors);
      return;
    }

    setSubmitting(true);
    setErrors({});
    try {
      await createPluginVersion(pluginId, request);
      navigate(
        `/plugins/${encodeURIComponent(pluginId)}/versions/${encodeURIComponent(version.trim())}/manifest`,
      );
    } catch (error) {
      const message = error instanceof Error ? error.message : "Не удалось опубликовать версию.";
      setErrors({ submit: message });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <div className="breadcrumbs">
        <Link to="/plugins">Плагины</Link>
        <span>/</span>
        <Link to={`/plugins/${encodeURIComponent(pluginId)}/versions`}>{pluginId}</Link>
        <span>/</span>
        <span>Новая версия</span>
      </div>
      <PageHeader
        eyebrow="Публикация"
        title="Новая версия манифеста"
        description="Версия станет неизменяемой после успешной публикации."
      />

      <form className="editor-layout" onSubmit={handleSubmit} noValidate>
        <aside className="form-panel">
          <div className="field-group">
            <label htmlFor="plugin-id">ID плагина</label>
            <input id="plugin-id" value={pluginId} disabled />
            <small>Версия будет добавлена к этому плагину.</small>
          </div>

          <div className="field-group">
            <label htmlFor="plugin-version">Версия</label>
            <input
              id="plugin-version"
              value={version}
              onChange={(event) => setVersion(event.target.value)}
              placeholder="1.0.0"
              aria-invalid={Boolean(errors.version)}
            />
            {errors.version ? <span className="field-error">{errors.version}</span> : null}
          </div>

          <div className="publish-note">
            <strong>Перед публикацией</strong>
            <p>
              Проверьте SDK-версии, URL сервисов и соответствие полей <code>plugin-id</code> и{" "}
              <code>plugin-version</code>.
            </p>
          </div>

          {errors.submit ? <div className="inline-alert">{errors.submit}</div> : null}

          <div className="form-actions">
            <Link className="button secondary" to={`/plugins/${encodeURIComponent(pluginId)}/versions`}>
              Отмена
            </Link>
            <button className="button primary" type="submit" disabled={submitting}>
              {submitting ? "Публикация…" : "Опубликовать версию"}
            </button>
          </div>
        </aside>

        <section className="json-editor-panel">
          <div className="editor-toolbar">
            <div>
              <strong>manifest.json</strong>
              <span>JSON</span>
            </div>
            <span className={`json-status ${jsonStatus.valid ? "valid" : "invalid"}`}>
              {jsonStatus.label}
            </span>
          </div>
          <label className="sr-only" htmlFor="manifest-json">
            JSON-манифест
          </label>
          <textarea
            id="manifest-json"
            className="json-editor"
            value={manifestText}
            onChange={(event) => setManifestText(event.target.value)}
            spellCheck={false}
            aria-invalid={Boolean(errors.manifest)}
          />
          {errors.manifest ? <div className="editor-error">{errors.manifest}</div> : null}
        </section>
      </form>
    </>
  );
}

function toCreateVersionRequest(manifest: PluginManifest): CreatePluginVersionRequest {
  const frontend = asObject(manifest.frontend, "frontend");
  const backend = asObject(manifest.backend, "backend");
  const events = asObject(backend.events, "backend.events");
  const media = asObject(backend.media, "backend.media");

  return {
    manifestVersion: asString(manifest["manifest-version"], "manifest-version"),
    pluginVersion: asString(manifest["plugin-version"], "plugin-version"),
    frontend: {
      sdkVersion: asString(frontend["sdk-version"], "frontend.sdk-version"),
      urlWeb: asString(frontend["url-web"], "frontend.url-web"),
    },
    backend: {
      sdkVersion: asString(backend["sdk-version"], "backend.sdk-version"),
      healthUrl: asString(backend["health-url"], "backend.health-url"),
      events: {
        eventUrl: asString(events["event-url"], "backend.events.event-url"),
        availableEventTypes: asStringArray(
          events["available-event-types"],
          "backend.events.available-event-types",
        ),
        availableEventGroups: asStringArray(
          events["available-event-groups"],
          "backend.events.available-event-groups",
        ),
      },
      media: {
        input: asMediaTypes(media.input, "backend.media.input"),
        output: asMediaTypes(media.output, "backend.media.output"),
      },
    },
  };
}

function asObject(value: unknown, field: string): PluginManifest {
  if (!value || Array.isArray(value) || typeof value !== "object") {
    throw new Error(`Поле ${field} должно быть JSON-объектом.`);
  }
  return value as PluginManifest;
}

function asString(value: unknown, field: string): string {
  if (typeof value !== "string" || !value.trim()) {
    throw new Error(`Поле ${field} должно быть непустой строкой.`);
  }
  return value;
}

function asStringArray(value: unknown, field: string): string[] {
  if (value === undefined) {
    return [];
  }
  if (!Array.isArray(value) || value.some((item) => typeof item !== "string" || !item.trim())) {
    throw new Error(`Поле ${field} должно быть массивом непустых строк.`);
  }
  return value;
}

function asMediaTypes(value: unknown, field: string): MediaType[] {
  const values = asStringArray(value, field);
  const supported = new Set<MediaType>(["AUDIO", "VIDEO", "SCREEN_SHARE", "SCREEN_SHARE_AUDIO"]);
  if (values.some((item) => !supported.has(item as MediaType))) {
    throw new Error(`Поле ${field} содержит неподдерживаемый тип медиа.`);
  }
  return values as MediaType[];
}
