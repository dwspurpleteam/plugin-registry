import { type FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, createPlugin } from "../api/pluginRegistry";
import { PageHeader } from "../components/PageHeader";

interface FormErrors {
  id?: string;
  name?: string;
  description?: string;
  submit?: string;
}

export function CreatePluginPage() {
  const navigate = useNavigate();
  const [id, setId] = useState("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [errors, setErrors] = useState<FormErrors>({});
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const normalizedId = id.trim();
    const normalizedName = name.trim();
    const normalizedDescription = description.trim();
    const nextErrors: FormErrors = {};

    if (!normalizedId) {
      nextErrors.id = "Укажите ID плагина.";
    } else if (normalizedId.length > 255) {
      nextErrors.id = "ID не должен быть длиннее 255 символов.";
    }

    if (!normalizedName) {
      nextErrors.name = "Укажите название плагина.";
    } else if (normalizedName.length > 255) {
      nextErrors.name = "Название не должно быть длиннее 255 символов.";
    }

    if (normalizedDescription.length > 10_000) {
      nextErrors.description = "Описание не должно быть длиннее 10 000 символов.";
    }

    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors);
      return;
    }

    setSubmitting(true);
    setErrors({});

    try {
      const plugin = await createPlugin({
        id: normalizedId,
        name: normalizedName,
        description: normalizedDescription || null,
      });

      navigate(`/plugins/${encodeURIComponent(plugin.id)}/versions/new`);
    } catch (error) {
      const message =
        error instanceof ApiError && error.status === 409
          ? "Плагин с таким ID уже существует. Выберите другой идентификатор."
          : error instanceof Error
            ? error.message
            : "Не удалось создать плагин.";
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
        <span>Новый плагин</span>
      </div>

      <PageHeader
        eyebrow="Каталог"
        title="Создание плагина"
        description="Добавьте карточку плагина в реестр. Первую версию манифеста можно опубликовать следующим шагом."
      />

      <form className="create-plugin-layout" onSubmit={handleSubmit} noValidate>
        <section className="form-panel create-plugin-form">
          <div className="form-panel-heading">
            <span className="eyebrow">Основная информация</span>
            <h2>Карточка плагина</h2>
            <p>ID используется в API и манифестах, а название и описание видны в каталоге.</p>
          </div>

          <div className="field-group">
            <label htmlFor="plugin-id">ID плагина</label>
            <input
              id="plugin-id"
              value={id}
              onChange={(event) => {
                setId(event.target.value);
                setErrors((current) => ({ ...current, id: undefined, submit: undefined }));
              }}
              placeholder="caption-plugin"
              maxLength={255}
              autoComplete="off"
              autoFocus
              aria-invalid={Boolean(errors.id)}
              aria-describedby={errors.id ? "plugin-id-error" : "plugin-id-hint"}
            />
            {errors.id ? (
              <span className="field-error" id="plugin-id-error">
                {errors.id}
              </span>
            ) : (
              <small id="plugin-id-hint">Выберите стабильный технический идентификатор.</small>
            )}
          </div>

          <div className="field-group">
            <label htmlFor="plugin-name">Название</label>
            <input
              id="plugin-name"
              value={name}
              onChange={(event) => {
                setName(event.target.value);
                setErrors((current) => ({ ...current, name: undefined, submit: undefined }));
              }}
              placeholder="Субтитры звонка"
              maxLength={255}
              autoComplete="off"
              aria-invalid={Boolean(errors.name)}
              aria-describedby={errors.name ? "plugin-name-error" : undefined}
            />
            {errors.name ? (
              <span className="field-error" id="plugin-name-error">
                {errors.name}
              </span>
            ) : null}
          </div>

          <div className="field-group">
            <div className="field-label-row">
              <label htmlFor="plugin-description">Описание</label>
              <span>{description.length} / 10 000</span>
            </div>
            <textarea
              id="plugin-description"
              className="form-textarea"
              value={description}
              onChange={(event) => {
                setDescription(event.target.value);
                setErrors((current) => ({
                  ...current,
                  description: undefined,
                  submit: undefined,
                }));
              }}
              placeholder="Кратко опишите назначение плагина"
              maxLength={10_000}
              rows={6}
              aria-invalid={Boolean(errors.description)}
              aria-describedby={errors.description ? "plugin-description-error" : undefined}
            />
            {errors.description ? (
              <span className="field-error" id="plugin-description-error">
                {errors.description}
              </span>
            ) : null}
          </div>

          {errors.submit ? (
            <div className="inline-alert" role="alert">
              {errors.submit}
            </div>
          ) : null}

          <div className="form-actions create-plugin-actions">
            <Link className="button secondary" to="/plugins">
              Отмена
            </Link>
            <button className="button primary" type="submit" disabled={submitting}>
              {submitting ? "Создание…" : "Создать плагин"}
            </button>
          </div>
        </section>

        <aside className="creation-next-step" aria-label="Следующий шаг">
          <span className="step-number">02</span>
          <div>
            <span className="eyebrow">После создания</span>
            <h2>Опубликуйте первую версию</h2>
            <p>
              Мы сразу откроем редактор манифеста. Там можно указать версии SDK, URL сервисов и
              медиавозможности плагина.
            </p>
          </div>
        </aside>
      </form>
    </>
  );
}
