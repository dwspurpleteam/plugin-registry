import { type FormEvent, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { createPluginVersion } from "../api/pluginRegistry";
import type {
  CreatePluginVersionRequest,
  PluginManifest,
} from "../api/types";
import { PageHeader } from "../components/PageHeader";

function defaultManifest(pluginId: string) {
  return JSON.stringify(
    {
      "manifest-version": "1.1",
      "plugin-id": pluginId,
      "plugin-version": "1.0.0",
      name: pluginId || "new-plugin-version",
      frontend: {
        "url-web": "https://example.com/plugin-ui/web",
      },
      backend: {
        "health-url": "https://example.com/api/health",
        "control-url": "https://example.com/api/control",
        "initial-state": ["participants"],
        events: {
          "available-event-types": [
            "participant.joined",
            "participant.left",
            "call.ended",
          ],
        },
        media: {
          input: [],
          output: [],
        },
        results: {
          keys: ["attendance.summary"],
          "delivery-report-url": "https://example.com/api/deliveries",
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
      return { valid: true, label: "JSON синтаксически корректен" };
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
              <code>plugin-version</code>. SDK-версии необязательны; название и описание при
              публикации берутся из записи плагина в реестре.
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
  // Отправляем разобранный документ без изменений. Сервер проверяет именно этот JSON
  // по канонической схеме 1.1, поэтому неизвестные поля здесь не потеряются.
  return manifest as unknown as CreatePluginVersionRequest;
}
