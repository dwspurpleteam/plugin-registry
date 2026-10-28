import { useCallback, useState } from "react";
import { Link } from "react-router-dom";
import { getPlugins } from "../api/pluginRegistry";
import { PageHeader } from "../components/PageHeader";
import { Pagination } from "../components/Pagination";
import { RequestState } from "../components/RequestState";
import { useRemoteData } from "../hooks/useRemoteData";

const PAGE_SIZE = 12;

export function PluginsPage() {
  const [page, setPage] = useState(1);
  const loader = useCallback(
    (signal: AbortSignal) => getPlugins(page, PAGE_SIZE, signal),
    [page],
  );
  const { data, error, loading, reload } = useRemoteData(loader, [loader]);

  return (
    <>
      <PageHeader
        eyebrow="Каталог"
        title="Плагины"
        description="Опубликованные расширения платформы и их краткие описания."
        actions={
          <Link className="button primary" to="/plugins/new">
            <span aria-hidden="true">＋</span>
            Создать плагин
          </Link>
        }
      />

      {loading ? <PluginGridSkeleton /> : null}

      {!loading && error ? (
        <RequestState
          tone="error"
          title="Не удалось загрузить плагины"
          message={error.message}
          actionLabel="Повторить"
          onAction={reload}
        />
      ) : null}

      {!loading && !error && data?.items.length === 0 ? (
        <RequestState
          title="Каталог пока пуст"
          message="Когда в реестре появятся плагины, они будут показаны здесь."
        />
      ) : null}

      {!loading && !error && data && data.items.length > 0 ? (
        <>
          <section className="plugin-grid" aria-label="Список плагинов">
            {data.items.map((plugin, index) => (
              <article className="plugin-card" key={plugin.id}>
                <div className="card-index">{String(index + 1).padStart(2, "0")}</div>
                <div className="plugin-card-body">
                  <span className="technical-id">{plugin.id}</span>
                  <h2>{plugin.name}</h2>
                  <p>{plugin.description || "Описание для этого плагина не указано."}</p>
                </div>
                <Link className="card-link" to={`/plugins/${encodeURIComponent(plugin.id)}/versions`}>
                  Все версии <span aria-hidden="true">→</span>
                </Link>
              </article>
            ))}
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

function PluginGridSkeleton() {
  return (
    <div className="plugin-grid" aria-label="Загрузка плагинов" aria-busy="true">
      {[0, 1, 2].map((item) => (
        <div className="plugin-card skeleton-card" key={item}>
          <span className="skeleton line short" />
          <span className="skeleton line title" />
          <span className="skeleton line" />
          <span className="skeleton line medium" />
        </div>
      ))}
    </div>
  );
}
