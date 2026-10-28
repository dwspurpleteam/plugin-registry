# Simple Plugin Registry

Небольшой read-only реестр метаданных и неизменяемых версий плагинов на Spring Boot и PostgreSQL.

## Локальный запуск

Локальная база `plugin_registry` и роль `plugin_registry_app` создаются отдельно. Приложение читает настройки из игнорируемого файла `.env`; шаблон находится в `.env.example`.

```shell
./gradlew bootRun
```

При старте Flyway создаёт таблицы `plugins` и `plugin_versions`. Hibernate только проверяет схему и не изменяет её.

## HTTP API

Интерактивная документация Swagger UI доступна по адресу
`http://localhost:8080/swagger-ui`, а OpenAPI-спецификация — по
`http://localhost:8080/v3/api-docs` (JSON) и
`http://localhost:8080/v3/api-docs.yaml` (YAML).

```text
GET  /health
GET  /v1/plugins?page=1&limit=20
GET  /v1/plugins/{pluginId}/versions?page=1&limit=20
GET  /v1/plugins/{pluginId}/versions/{version}
GET  /v1/plugins/{pluginId}/versions/{version}/manifest
POST /v1/plugins
POST /v1/plugins/{pluginId}/versions
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

### Создание плагина

```json
{
  "id": "plugin-1",
  "name": "Test plugin",
  "description": "Plugin description"
}
```

### Создание версии

`POST /v1/plugins/plugin-1/versions` принимает типизированную конфигурацию:

```json
{
  "manifestVersion": "0.1",
  "pluginVersion": "1.0.0",
  "frontend": {
    "sdkVersion": "1.0.0",
    "urlWeb": "https://example.com/plugin-ui/web"
  },
  "backend": {
    "sdkVersion": "1.0.0",
    "healthUrl": "https://example.com/api/healthzz",
    "events": {
      "eventUrl": "https://example.com/api/events",
      "availableEventTypes": [],
      "availableEventGroups": ["participant", "media", "interaction"]
    },
    "media": {
      "input": ["AUDIO", "VIDEO"],
      "output": []
    }
  }
}
```

Backend добавляет в итоговый JSON поля `plugin-id`, `name` и `description` из таблицы `plugins` и преобразует DTO в формат манифеста с kebab-case ключами. Если одновременно переданы `availableEventTypes` и `availableEventGroups`, сохраняются только конкретные типы событий.

## Frontend

React + TypeScript интерфейс находится в каталоге `frontend`. Для локального запуска сначала запустите backend, затем:

```shell
cd frontend
npm install
npm run dev
```

Vite откроет frontend на `http://localhost:5173` и проксирует запросы `/v1` в Spring-приложение. Для нестандартного локального порта задайте `VITE_API_PROXY_TARGET`, а для отдельного размещения API — `VITE_API_BASE_URL`, используя `.env.example` как шаблон.
