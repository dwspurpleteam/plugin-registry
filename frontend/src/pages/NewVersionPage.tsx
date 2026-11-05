import { type FormEvent, type ReactNode, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { createPluginVersion } from "../api/pluginRegistry";
import type { CreatePluginVersionRequest, MediaType, PluginManifest } from "../api/types";
import { PageHeader } from "../components/PageHeader";

const EVENT_GROUPS = ["participant", "media", "interaction"];
const EVENT_TYPES = [
  "participant.joined", "participant.left", "participant.connection_aborted",
  "participant.reconnecting", "participant.reconnected", "participant.kicked",
  "media.audio_muted", "media.audio_unmuted", "media.video_enabled", "media.video_disabled",
  "media.screen_share_started", "media.screen_share_stopped", "media.track_published",
  "media.track_unpublished", "interaction.chat_message_received",
];
const MEDIA_TYPES: MediaType[] = ["AUDIO", "VIDEO", "SCREEN_SHARE", "SCREEN_SHARE_AUDIO"];
const EVENT_DESCRIPTIONS: Record<string, string> = {
  participant: "События жизненного цикла участников звонка.",
  media: "События состояния и публикации медиа-потоков.",
  interaction: "События взаимодействия участников.",
  "participant.joined": "Участник подключился к звонку.",
  "participant.left": "Участник покинул звонок.",
  "participant.connection_aborted": "Подключение участника было отменено.",
  "participant.reconnecting": "Участник начал переподключение.",
  "participant.reconnected": "Участник успешно переподключился.",
  "participant.kicked": "Участник был удалён из звонка.",
  "media.audio_muted": "Участник выключил микрофон.",
  "media.audio_unmuted": "Участник включил микрофон.",
  "media.video_enabled": "Участник включил камеру.",
  "media.video_disabled": "Участник выключил камеру.",
  "media.screen_share_started": "Участник начал демонстрацию экрана.",
  "media.screen_share_stopped": "Участник завершил демонстрацию экрана.",
  "media.track_published": "Участник опубликовал медиа-поток.",
  "media.track_unpublished": "Участник прекратил публикацию медиа-потока.",
  "interaction.chat_message_received": "Получено сообщение чата.",
};
type EventSelectionMode = "groups" | "types";

interface VersionForm {
  name: string; description: string; frontendSdkVersion: string; frontendUrl: string;
  backendSdkVersion: string; healthUrl: string; eventUrl: string; eventMode: EventSelectionMode;
  eventGroups: string[]; eventTypes: string[]; mediaInput: MediaType[]; mediaOutput: MediaType[];
}
interface FormErrors { version?: string; frontendSdkVersion?: string; backendSdkVersion?: string; healthUrl?: string; submit?: string; }

function updateList<T>(values: T[], value: T, checked: boolean): T[] {
  return checked ? [...values, value] : values.filter((item) => item !== value);
}

export function NewVersionPage() {
  const { pluginId = "" } = useParams();
  const navigate = useNavigate();
  const [version, setVersion] = useState("1.0.0");
  const [form, setForm] = useState<VersionForm>({
    name: "new-plugin-version", description: "", frontendSdkVersion: "1.0.0",
    frontendUrl: "https://example.com/plugin-ui/web", backendSdkVersion: "1.0.0",
    healthUrl: "https://example.com/api/healthzz", eventUrl: "https://example.com/api/events",
    eventMode: "groups", eventGroups: EVENT_GROUPS, eventTypes: [], mediaInput: ["AUDIO"], mediaOutput: [],
  });
  const [errors, setErrors] = useState<FormErrors>({});
  const [submitting, setSubmitting] = useState(false);

  const request = useMemo((): CreatePluginVersionRequest => ({
    manifestVersion: "0.1", pluginVersion: version.trim(),
    frontend: { sdkVersion: form.frontendSdkVersion.trim(), urlWeb: form.frontendUrl.trim() },
    backend: {
      sdkVersion: form.backendSdkVersion.trim(), healthUrl: form.healthUrl.trim(),
      events: { eventUrl: form.eventUrl.trim(), availableEventTypes: form.eventMode === "types" ? form.eventTypes : [], availableEventGroups: form.eventMode === "groups" ? form.eventGroups : [] },
      media: { input: form.mediaInput, output: form.mediaOutput },
    },
  }), [form, version]);
  const manifestText = useMemo(() => JSON.stringify(toManifest(pluginId, form, request), null, 2), [form, pluginId, request]);
  const setField = <K extends keyof VersionForm>(key: K, value: VersionForm[K]) => setForm((current) => ({ ...current, [key]: value }));

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextErrors: FormErrors = {};
    if (!version.trim()) nextErrors.version = "Укажите номер версии.";
    if (!form.frontendSdkVersion.trim()) nextErrors.frontendSdkVersion = "Укажите версию frontend SDK.";
    if (!form.backendSdkVersion.trim()) nextErrors.backendSdkVersion = "Укажите версию backend SDK.";
    if (!form.healthUrl.trim()) nextErrors.healthUrl = "Укажите health URL.";
    if (Object.keys(nextErrors).length) { setErrors(nextErrors); return; }
    setSubmitting(true); setErrors({});
    try {
      await createPluginVersion(pluginId, request);
      navigate(`/plugins/${encodeURIComponent(pluginId)}/versions/${encodeURIComponent(version.trim())}/manifest`);
    } catch (error) {
      setErrors({ submit: error instanceof Error ? error.message : "Не удалось опубликовать версию." });
    } finally { setSubmitting(false); }
  };

  const checkboxList = <T extends string>(name: string, options: T[], values: T[], onChange: (next: T[]) => void) => (
    <div className="choice-list">{options.map((option) => <label className="choice" key={option}>
      <input type="checkbox" name={name} checked={values.includes(option)} onChange={(event) => onChange(updateList(values, option, event.target.checked))} /><span>{option}</span>{EVENT_DESCRIPTIONS[option] ? <Hint text={EVENT_DESCRIPTIONS[option]} /> : null}
    </label>)}</div>
  );

  return <>
    <div className="breadcrumbs"><Link to="/plugins">Плагины</Link><span>/</span><Link to={`/plugins/${encodeURIComponent(pluginId)}/versions`}>{pluginId}</Link><span>/</span><span>Новая версия</span></div>
    <PageHeader eyebrow="Публикация" title="Новая версия манифеста" description="Версия станет неизменяемой после успешной публикации." />
    <form className="editor-layout" onSubmit={handleSubmit} noValidate>
      <section className="form-panel version-form">
        <div className="form-panel-heading"><span className="eyebrow accent">Параметры манифеста</span><h2>Настройте версию</h2><p>JSON справа формируется из этих полей и недоступен для редактирования.</p></div>
        <div className="form-section"><h3>Основное</h3>
          <Field label="ID плагина" hint="Идентификатор уже созданного плагина. Его нельзя изменить при регистрации версии."><input value={pluginId} disabled /></Field>
          <Field label="Версия" hint="Уникальный номер публикуемой версии, например 1.0.0." error={errors.version}><input value={version} onChange={(event) => setVersion(event.target.value)} aria-invalid={Boolean(errors.version)} required /></Field>
          <Field label="Название" hint="Отображаемое имя этой версии плагина."><input value={form.name} onChange={(event) => setField("name", event.target.value)} required /></Field>
          <Field label="Описание" hint="Кратко опишите назначение и возможности плагина."><textarea className="form-textarea compact" value={form.description} onChange={(event) => setField("description", event.target.value)} /></Field>
        </div>
        <div className="form-section"><h3>Frontend</h3>
          <Field label="Версия frontend SDK" hint="Версия SDK, с которой совместим пользовательский интерфейс плагина." required error={errors.frontendSdkVersion}><input value={form.frontendSdkVersion} onChange={(event) => setField("frontendSdkVersion", event.target.value)} aria-invalid={Boolean(errors.frontendSdkVersion)} required /></Field>
          <Field label="URL web" hint="Адрес веб-интерфейса плагина, который будет открыт в iframe."><input type="url" value={form.frontendUrl} onChange={(event) => setField("frontendUrl", event.target.value)} required /></Field>
        </div>
        <div className="form-section"><h3>Backend</h3>
          <Field label="Версия backend SDK" hint="Версия SDK, с которой совместим backend плагина." required error={errors.backendSdkVersion}><input value={form.backendSdkVersion} onChange={(event) => setField("backendSdkVersion", event.target.value)} aria-invalid={Boolean(errors.backendSdkVersion)} required /></Field>
          <Field label="Health URL" hint="URL endpoint, по которому проверяется работоспособность backend плагина." required error={errors.healthUrl}><input type="url" value={form.healthUrl} onChange={(event) => setField("healthUrl", event.target.value)} aria-invalid={Boolean(errors.healthUrl)} required /></Field>
          <Field label="URL событий" hint="Endpoint backend плагина для доставки событий звонка."><input type="url" value={form.eventUrl} onChange={(event) => setField("eventUrl", event.target.value)} required /></Field>
        </div>
        <div className="form-section"><h3>События</h3><p className="section-hint">Выберите группы либо отдельные события.</p>
          <div className="radio-row"><label><input type="radio" name="event-mode" checked={form.eventMode === "groups"} onChange={() => setField("eventMode", "groups")} /> Группы событий</label><label><input type="radio" name="event-mode" checked={form.eventMode === "types"} onChange={() => setField("eventMode", "types")} /> Отдельные события</label></div>
          {form.eventMode === "groups" ? checkboxList("event-groups", EVENT_GROUPS, form.eventGroups, (next) => setField("eventGroups", next)) : checkboxList("event-types", EVENT_TYPES, form.eventTypes, (next) => setField("eventTypes", next))}
        </div>
        <div className="form-section"><h3>Медиа</h3>
          <span className="choice-label">Входящие потоки</span>{checkboxList("media-input", MEDIA_TYPES, form.mediaInput, (next) => setField("mediaInput", next))}
          <span className="choice-label">Исходящие потоки</span>{checkboxList("media-output", MEDIA_TYPES, form.mediaOutput, (next) => setField("mediaOutput", next))}
        </div>
        {errors.submit ? <div className="inline-alert">{errors.submit}</div> : null}
        <div className="form-actions"><Link className="button secondary" to={`/plugins/${encodeURIComponent(pluginId)}/versions`}>Отмена</Link><button className="button primary" type="submit" disabled={submitting}>{submitting ? "Публикация…" : "Опубликовать версию"}</button></div>
      </section>
      <section className="json-editor-panel"><div className="editor-toolbar"><div><strong>manifest.json</strong><span>только чтение</span></div><span className="json-status valid">Сформирован из формы</span></div><pre className="json-preview" aria-label="JSON-манифест только для чтения">{manifestText}</pre></section>
    </form>
  </>;
}

function Field({ label, hint, required, error, children }: { label: string; hint?: string; required?: boolean; error?: string; children: ReactNode }) {
  return <div className="field-group"><label>{label}{required ? <span className="required-mark"> *</span> : null}{hint ? <Hint text={hint} /> : null}</label>{children}{error ? <span className="field-error">{error}</span> : null}</div>;
}

function Hint({ text }: { text: string }) {
  return <span className="hint" tabIndex={0} role="img" aria-label={text} data-tooltip={text}>?</span>;
}

function toManifest(pluginId: string, form: VersionForm, request: CreatePluginVersionRequest): PluginManifest {
  const events: Record<string, unknown> = { "event-url": request.backend.events.eventUrl };
  if (form.eventMode === "groups") events["available-event-groups"] = request.backend.events.availableEventGroups;
  else events["available-event-types"] = request.backend.events.availableEventTypes;
  return { "manifest-version": request.manifestVersion, "plugin-id": pluginId, "plugin-version": request.pluginVersion, name: form.name, description: form.description, frontend: { "sdk-version": request.frontend.sdkVersion, "url-web": request.frontend.urlWeb }, backend: { "sdk-version": request.backend.sdkVersion, "health-url": request.backend.healthUrl, events, media: { input: request.backend.media.input, output: request.backend.media.output } } };
}
