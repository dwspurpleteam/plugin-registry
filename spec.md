# Контракт Plugin Registry

Plugin Registry — независимый каталог метаданных и неизменяемых версий плагинов. Нормативный формат новых манифестов задают [спецификация 1.1](https://github.com/dwspurpleteam/specs/blob/main/specs/plugin-service-architecture/plugin-manifest-v1.1.md) и [JSON Schema 1.1](https://github.com/dwspurpleteam/specs/blob/main/specs/plugin-service-architecture/plugin-manifest-v1.1.schema.json).

## Ответственность сервиса

Registry:

- хранит запись плагина и JSONB каждой опубликованной версии;
- проверяет новые документы по JSON Schema Draft 2020-12 и дополнительным синтаксическим ограничениям URL;
- гарантирует неизменяемость пары `plugin-id + plugin-version`;
- возвращает сохранённый JSON без преобразования при чтении.

Registry не запускает plugin backend, не управляет активациями, не выдаёт права, не получает данные звонка, не подключается к LiveKit/StreamAPI, не доставляет events/media/results и не хранит runtime IDs, targets или секреты.

## HTTP API

`GET /v1/manifest-schemas` перечисляет зарегистрированные версии, а `GET /v1/manifest-schemas/{version}` возвращает соответствующую JSON Schema. Пользовательский интерфейс применяет выбранную схему для подсказок и предварительной диагностики; окончательная проверка всегда выполняется сервером при публикации.

```text
GET  /v1/plugins?limit={limit}&page={page}
GET  /v1/plugins/{pluginId}/versions?limit={limit}&page={page}
GET  /v1/plugins/{pluginId}/versions/{version}
GET  /v1/plugins/{pluginId}/versions/{version}/manifest
POST /v1/plugins
POST /v1/plugins/{pluginId}/versions
```

Пагинация начинается с 1, `limit` по умолчанию равен 20.

```json
{
  "items": [],
  "page": 1,
  "limit": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

Тело `POST /v1/plugins/{pluginId}/versions` — полный манифест 1.1. `plugin-id` в документе должен совпасть с `{pluginId}`; `plugin-version` определяет версию новой строки. Неизвестное поле или нарушение схемы даёт HTTP 400, существующая пара идентификаторов — HTTP 409.

## Поток публикации

```text
request JSON
  -> validation by canonical schema 1.1
  -> plugin-id/path match
  -> registry metadata substitution
  -> effective event subscription normalization
  -> validation of final JSON by the same schema
  -> immutable JSONB insert
```

Вставка выполняется только после финальной проверки. Обновление существующей строки блокируют Hibernate `@Immutable` и PostgreSQL trigger. Поля 1.1 находятся внутри существующего `manifest JSONB`; новых колонок и Flyway-миграций не требуется.

Вычисляемые `frontend_sdk_version` и `backend_sdk_version` читают необязательные JSON paths. PostgreSQL возвращает `NULL`, если SDK-версия отсутствует.

## Совместимость

- Новая публикация: только `manifest-version: "1.1"`.
- Существующая запись 0.1/1.0: остаётся без изменений и возвращается исходным GET.
- Автоматическая конвертация запрещена: `control-url` и другие новые обязательные данные нельзя достоверно вывести из старого `event-url`.
- Версия схемы и версия плагина независимы; переход на 1.1 оформляется новой `plugin-version`.

## Server-to-server URL

Registry проверяет HTTPS, URI syntax, hostname, port, отсутствие user-info/fragment и отклоняет очевидные local/private/reserved literal targets для backend URL. Registry не делает DNS lookup и не выполняет эти запросы.

Фактический caller обязан применить SSRF-защиту в момент каждого запроса: разрешить DNS, проверить все A/AAAA адреса, запретить loopback/private/link-local/metadata/reserved ranges, повторить проверку после redirect, использовать egress policy и жёсткие timeout/size limits. Это обязанность Plugin Manager/Result Gateway или другого вызывающего компонента, а не Registry.
# Реестр схем и совместимость

- `GET /v1/manifest-schemas` возвращает `{ "versions": [...] }`.
- `GET /v1/manifest-schemas/{version}` возвращает поставленную JSON Schema или 404.
- Публикация требует зарегистрированной схемы и выбирает её по строковому `manifest-version`.
- Чтение ранее опубликованного манифеста не запускает повторную валидацию и не требует наличия старой схемы.
- Registry не мигрирует JSONB и не преобразует документ между версиями.

Колонки `frontend_sdk_version` и `backend_sdk_version` сохранены для обратной совместимости существующего API. Это вычисляемая проекция манифеста 1.1, а не обязательная часть универсального хранения: публикация и сохранение новых версий не зависят от наличия путей `frontend.sdk-version` и `backend.sdk-version`; для иной структуры значения проекции будут `null`. Удаление или обобщение колонок требует отдельной миграции и анализа потребителей.
