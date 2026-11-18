# Простой реестр плагинов

Plugin Registry хранит метаданные плагинов и неизменяемые JSON-манифесты их версий, проверяет новые публикации по формату 1.1 и отдаёт сохранённые документы другим сервисам.

Первичные источники контракта:

- [спецификация манифеста 1.1](https://github.com/dwspurpleteam/specs/blob/main/specs/plugin-service-architecture/plugin-manifest-v1.1.md);
- [JSON Schema 1.1](https://github.com/dwspurpleteam/specs/blob/main/specs/plugin-service-architecture/plugin-manifest-v1.1.schema.json).

Локальная копия схемы находится в `src/main/resources/schema/plugin-manifest-v1.1.schema.json` и используется только для воспроизводимой автономной валидации. Источником контракта остаётся репозиторий `specs`.

## Локальный запуск

Локальная база `plugin_registry` и роль `plugin_registry_app` создаются отдельно. Приложение читает настройки из игнорируемого файла `.env`; шаблон находится в `.env.example`.

```shell
./gradlew bootRun
```

При старте Flyway создаёт таблицы `plugins` и `plugin_versions`. Hibernate проверяет схему и не изменяет её.

## Docker

```shell
./gradlew dockerBuild
```

По умолчанию образ получает имя `simple-plugin-registry:0.0.1-SNAPSHOT`. Имя и тег можно переопределить:

```shell
./gradlew dockerBuild -PdockerImageName=simple-plugin-registry:local
```

Настройки PostgreSQL передаются через `DB_URL`, `DB_USERNAME` и `DB_PASSWORD`.

## HTTP API

Swagger UI доступен по адресу `http://localhost:8080/swagger-ui`, OpenAPI — по адресам `http://localhost:8080/v3/api-docs` и `http://localhost:8080/v3/api-docs.yaml`.

```text
GET  /health
GET  /v1/plugins?page=1&limit=20
GET  /v1/plugins/{pluginId}/versions?page=1&limit=20
GET  /v1/plugins/{pluginId}/versions/{version}
GET  /v1/plugins/{pluginId}/versions/{version}/manifest
POST /v1/plugins
POST /v1/plugins/{pluginId}/versions
```

Пагинация начинается с 1, лимит по умолчанию равен 20:

```json
{
  "items": [],
  "page": 1,
  "limit": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

### Создание плагина

```json
{
  "id": "plugin-1",
  "name": "Тестовый плагин",
  "description": "Описание плагина"
}
```

### Публикация версии

`POST /v1/plugins/plugin-1/versions` принимает сам JSON-манифест 1.1 без промежуточного camelCase-представления. Минимальный валидный запрос:

```json
{
  "manifest-version": "1.1",
  "plugin-id": "plugin-1",
  "plugin-version": "1.0.0",
  "name": "Тестовый плагин",
  "frontend": {
    "url-web": "https://plugins.example.com/plugin-1/"
  },
  "backend": {
    "health-url": "https://plugins.example.com/plugin-1/health",
    "control-url": "https://plugins.example.com/plugin-1/control"
  }
}
```

`plugin-id` обязан совпадать с `{pluginId}` в URL. `plugin-version` становится второй частью ключа опубликованной версии. Итоговые `plugin-id`, `name` и `description` берутся из записи плагина в реестре; если описание там отсутствует, поле `description` не записывается. Полный пример со всеми поддержанными необязательными блоками приведён в [plugin-manifest.md](plugin-manifest.md).

Перед сохранением серверная часть:

1. проверяет исходный JSON по JSON Schema Draft 2020-12;
2. сверяет `plugin-id` с параметром пути;
3. формирует итоговый документ из данных реестра и запроса;
4. повторно проверяет уже итоговый JSON той же схемой;
5. сохраняет документ только после успешного завершения всех проверок.

Неизвестные поля, `event-url`, старые `manifest-version`, неизвестные enum-значения, повторяющиеся элементы массивов и некорректные URL возвращают HTTP 400 с путём проблемного поля. Повторная публикация той же пары `plugin-id + plugin-version` возвращает HTTP 409.

## Неизменяемость и старые версии

Строка `plugin_versions` защищена одновременно `@Immutable` и PostgreSQL trigger `plugin_versions_are_immutable`. Изменение конфигурации требует новой `plugin-version`; `manifest-version` и `plugin-version` независимы.

Новые публикации принимаются только с `manifest-version: "1.1"`. Ранее сохранённые 0.1/1.0 документы не мигрируются и не валидируются повторно при чтении: GET возвращает исходный JSON, включая старый `event-url`. Автоматической конвертации нет, потому что для 1.1 нужны новые обязательные данные, прежде всего `control-url`.

Манифест по-прежнему хранится в JSONB, поэтому миграция БД для полей 1.1 не требуется. Вычисляемые колонки `frontend_sdk_version` и `backend_sdk_version` возвращают SQL `NULL`, если соответствующее необязательное поле `sdk-version` отсутствует.

## URL и SSRF

Все URL должны быть абсолютными HTTPS URI с корректным именем узла и портом, без данных пользователя и фрагмента. Для `backend.health-url`, `backend.control-url` и `backend.results.delivery-report-url` реестр дополнительно отклоняет явно заданные частные, локальные, зарезервированные и многоадресные IP, а также имена `localhost` и `.local`.

Реестр сам не выполняет исходящие запросы и не разрешает DNS при публикации. Компонент, который позднее вызывает межсерверный URL, обязан на своей границе повторно проверить результат DNS, все полученные адреса и каждое перенаправление, запретить частные, локальные и служебные адреса, ограничить исходящий трафик, время ожидания и размер ответа. Проверка только строки URL в реестре не защищает от подмены адреса через DNS.

## Пользовательский интерфейс

Интерфейс на React и TypeScript находится в `frontend`. JSON-редактор отправляет разобранный документ без выборочного преобразования полей, поэтому новые или неизвестные свойства не теряются: серверная часть либо принимает весь поддержанный документ, либо возвращает ошибку схемы.

```shell
cd frontend
npm ci
npm run dev
```

Vite открывает пользовательский интерфейс по адресу `http://localhost:5173` и перенаправляет запросы `/v1` в Spring-приложение. `VITE_API_PROXY_TARGET` меняет локальный адрес назначения, а `VITE_API_BASE_URL` задаёт отдельный URL API на этапе сборки.

Plugin Registry не активирует плагины, не проверяет права конкретной активации, не получает участников из LiveKit, не доставляет события или медиа и не отправляет результаты.
