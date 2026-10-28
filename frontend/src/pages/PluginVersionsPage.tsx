import { useCallback, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getPluginVersions } from "../api/pluginRegistry";
import { PageHeader } from "../components/PageHeader";
import { Pagination } from "../components/Pagination";
import { RequestState } from "../components/RequestState";
import { useRemoteData } from "../hooks/useRemoteData";

const PAGE_SIZE = 20;

export function PluginVersionsPage() {
  const { pluginId = "" } = useParams();
  const [page, setPage] = useState(1);
  const loader = useCallback(
    (signal: AbortSignal) => getPluginVersions(pluginId, page, PAGE_SIZE, signal),
    [page, pluginId],
  );
  const { data, error, loading, reload } = useRemoteData(loader, [loader]);
  const pluginName = data?.items[0]?.name ?? pluginId;

  return (
    <>
      <div className="breadcrumbs">
        <Link to="/plugins">Плагины</Link>
        <span>/</span>
        <span>{pluginId}</span>
      </div>
      <PageHeader
        eyebrow="Версии плагина"
        title={pluginName}
        description={`Все опубликованные версии · ${pluginId}`}
        actions={
          <Link className="button primary" to={`/plugins/${encodeURIComponent(pluginId)}/versions/new`}>
            <span aria-hidden="true">＋</span> Добавить версию
          </Link>
        }
      />

      {loading ? <VersionTableSkeleton /> : null}

      {!loading && error ? (
        <RequestState
          tone="error"
          title="Не удалось загрузить версии"
          message={error.message}
          actionLabel="Повторить"
          onAction={reload}
        />
      ) : null}

      {!loading && !error && data?.items.length === 0 ? (
        <RequestState
          title="Нет опубликованных версий"
          message="Создайте первую версию и добавьте её JSON-манифест."
          actionLabel="Обновить"
          onAction={reload}
        />
      ) : null}

      {!loading && !error && data && data.items.length > 0 ? (
        <>
          <section className="table-card">
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Версия</th>
                    <th>Frontend SDK</th>
                    <th>Backend SDK</th>
                    <th aria-label="Действия" />
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((item) => (
                    <tr key={item.version}>
                      <td>
                        <span className="version-chip">v{item.version}</span>
                      </td>
                      <td>{item.frontendSdkVersion ?? <span className="muted">—</span>}</td>
                      <td>{item.backendSdkVersion ?? <span className="muted">—</span>}</td>
                      <td className="table-action">
                        <Link
                          to={`/plugins/${encodeURIComponent(pluginId)}/versions/${encodeURIComponent(item.version)}/manifest`}
                        >
                          Манифест <span aria-hidden="true">→</span>
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
          <Pagination
            page={data.page}
            totalPages={data.totalPages}
            totalItems={data.totalItems}
            onChange={setPage}
          />
        </>
      ) : null}
    </>
  );
}

function VersionTableSkeleton() {
  return (
    <div className="table-card skeleton-table" aria-label="Загрузка версий" aria-busy="true">
      {[0, 1, 2, 3].map((item) => (
        <span className="skeleton line" key={item} />
      ))}
    </div>
  );
}
