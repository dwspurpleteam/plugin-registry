import { useCallback, useEffect, useState } from "react";

interface RemoteData<T> {
  data: T | null;
  error: Error | null;
  loading: boolean;
  reload: () => void;
}

export function useRemoteData<T>(
  loader: (signal: AbortSignal) => Promise<T>,
  dependencies: readonly unknown[],
): RemoteData<T> {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<Error | null>(null);
  const [loading, setLoading] = useState(true);
  const [reloadToken, setReloadToken] = useState(0);

  const reload = useCallback(() => setReloadToken((current) => current + 1), []);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);

    loader(controller.signal)
      .then((value) => {
        if (!controller.signal.aborted) {
          setData(value);
        }
      })
      .catch((reason: unknown) => {
        if (!controller.signal.aborted) {
          setError(reason instanceof Error ? reason : new Error("Не удалось выполнить запрос"));
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
    // The caller controls when a request should be repeated through dependencies.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...dependencies, reloadToken]);

  return { data, error, loading, reload };
}
