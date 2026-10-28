interface PaginationProps {
  page: number;
  totalPages: number;
  totalItems: number;
  onChange: (page: number) => void;
}

export function Pagination({ page, totalPages, totalItems, onChange }: PaginationProps) {
  if (totalPages <= 1) {
    return totalItems > 0 ? (
      <div className="pagination-summary">Всего записей: {totalItems}</div>
    ) : null;
  }

  return (
    <nav className="pagination" aria-label="Пагинация">
      <span>
        Страница {page} из {totalPages} · записей {totalItems}
      </span>
      <div>
        <button
          type="button"
          className="button secondary compact"
          disabled={page <= 1}
          onClick={() => onChange(page - 1)}
        >
          Назад
        </button>
        <button
          type="button"
          className="button secondary compact"
          disabled={page >= totalPages}
          onClick={() => onChange(page + 1)}
        >
          Далее
        </button>
      </div>
    </nav>
  );
}
