import { RequestState } from "../components/RequestState";

export function NotFoundPage() {
  return (
    <RequestState
      title="Страница не найдена"
      message="Проверьте адрес или вернитесь в каталог плагинов."
      actionLabel="В каталог"
      onAction={() => {
        window.location.assign("/plugins");
      }}
    />
  );
}
