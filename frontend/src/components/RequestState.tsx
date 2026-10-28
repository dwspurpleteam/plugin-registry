interface RequestStateProps {
  title: string;
  message: string;
  actionLabel?: string;
  onAction?: () => void;
  tone?: "neutral" | "error";
}

export function RequestState({
  title,
  message,
  actionLabel,
  onAction,
  tone = "neutral",
}: RequestStateProps) {
  return (
    <section className={`state-card ${tone}`} role={tone === "error" ? "alert" : undefined}>
      <span className="state-glyph" aria-hidden="true">
        {tone === "error" ? "!" : "◇"}
      </span>
      <h2>{title}</h2>
      <p>{message}</p>
      {actionLabel && onAction ? (
        <button type="button" className="button secondary" onClick={onAction}>
          {actionLabel}
        </button>
      ) : null}
    </section>
  );
}
