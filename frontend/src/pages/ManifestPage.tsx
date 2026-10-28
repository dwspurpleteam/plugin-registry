import { useCallback, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getPluginManifest } from "../api/pluginRegistry";
import { PageHeader } from "../components/PageHeader";
import { RequestState } from "../components/RequestState";
import { useRemoteData } from "../hooks/useRemoteData";

export function ManifestPage() {
  const { pluginId = "", version = "" } = useParams();
  const [copied, setCopied] = useState(false);
  const loader = useCallback(
    (signal: AbortSignal) => getPluginManifest(pluginId, version, signal),
    [pluginId, version],
  );
  const { data, error, loading, reload } = useRemoteData(loader, [loader]);
  const formattedManifest = useMemo(() => (data ? JSON.stringify(data, null, 2) : ""), [data]);

  const copyManifest = async () => {
    await navigator.clipboard.writeText(formattedManifest);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1800);
  };

  return (
    <>
      <div className="breadcrumbs">
        <Link to="/plugins">Плагины</Link>
        <span>/</span>
        <Link to={`/plugins/${encodeURIComponent(pluginId)}/versions`}>{pluginId}</Link>
        <span>/</span>
        <span>{version}</span>
      </div>
      <PageHeader
        eyebrow="JSON-манифест"
        title={`${pluginId} · v${version}`}
        description="Неизменяемая конфигурация опубликованной версии плагина."
        actions={
          data ? (
            <button type="button" className="button secondary" onClick={copyManifest}>
              {copied ? "Скопировано" : "Копировать JSON"}
            </button>
          ) : null
        }
      />

      {loading ? (
        <div className="manifest-card loading-code" aria-label="Загрузка манифеста" aria-busy="true">
          {[95, 62, 78, 88, 45, 72, 54].map((width, index) => (
            <span className="skeleton code-line" style={{ width: `${width}%` }} key={index} />
          ))}
        </div>
      ) : null}

      {!loading && error ? (
        <RequestState
          tone="error"
          title="Не удалось загрузить манифест"
          message={error.message}
          actionLabel="Повторить"
          onAction={reload}
        />
      ) : null}

      {!loading && !error && data ? (
        <section className="manifest-card">
          <div className="code-header">
            <span>manifest.json</span>
            <span>read only</span>
          </div>
          <pre>
            <code>{formattedManifest}</code>
          </pre>
        </section>
      ) : null}
    </>
  );
}
