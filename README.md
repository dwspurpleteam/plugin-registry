# Simple Plugin Registry

Небольшой read-only реестр метаданных и неизменяемых версий плагинов на Spring Boot и PostgreSQL.

## Локальный запуск

Локальная база `plugin_registry` и роль `plugin_registry_app` создаются отдельно. Приложение читает настройки из игнорируемого файла `.env`; шаблон находится в `.env.example`.

```shell
./gradlew bootRun
```

При старте Flyway создаёт таблицы `plugins` и `plugin_versions`. Hibernate только проверяет схему и не изменяет её.

## HTTP API

```text
GET  /v1/plugins?page=1&limit=20
GET  /v1/plugins/{pluginId}/versions?page=1&limit=20
GET  /v1/plugins/{pluginId}/versions/{version}
GET  /v1/plugins/{pluginId}/versions/{version}/manifest
POST /v1/plugins                              # заглушка: 501
POST /v1/plugins/{pluginId}/versions          # заглушка: 501
```

Коллекции имеют единый формат:

```json
{
  "items": [],
  "page": 1,
  "limit": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

Опубликованная версия идентифицируется парой `plugin_id + version`. Её изменение запрещено на уровне Hibernate и триггером PostgreSQL; новая конфигурация должна публиковаться под новым номером версии.

Колонки `frontend_sdk_version` и `backend_sdk_version` автоматически вычисляются PostgreSQL из полей `frontend.sdk-version` и `backend.sdk-version` JSON-манифеста. В API они возвращаются как `frontendSdkVersion` и `backendSdkVersion`.
