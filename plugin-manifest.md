# Манифест плагина 1.1

Этот файл поясняет, как Plugin Registry принимает и хранит контракт. Нормативными источниками являются [спецификация 1.1](https://github.com/dwspurpleteam/specs/blob/main/specs/plugin-service-architecture/plugin-manifest-v1.1.md) и [JSON Schema 1.1](https://github.com/dwspurpleteam/specs/blob/main/specs/plugin-service-architecture/plugin-manifest-v1.1.schema.json); перечни enum и обязательность полей берутся из схемы, а не определяются здесь независимо.

## Полный валидный пример

```json
{
  "manifest-version": "1.1",
  "plugin-id": "attendance",
  "plugin-version": "1.0.0",
  "name": "Посещаемость",
  "description": "Учёт присутствия участников звонка",
  "frontend": {
    "url-web": "https://plugins.example.com/attendance/",
    "sdk-version": "1.3.0"
  },
  "backend": {
    "health-url": "https://plugins.example.com/attendance/actuator/health",
    "control-url": "https://plugins.example.com/attendance/platform/control",
    "sdk-version": "1.4.0",
    "initial-state": ["participants"],
    "events": {
      "available-event-types": [
        "participant.joined",
        "participant.left",
        "call.ended"
      ]
    },
    "media": {
      "input": [],
      "output": []
    },
    "results": {
      "keys": ["attendance.summary"],
      "delivery-report-url": "https://plugins.example.com/attendance/platform/deliveries"
    }
  }
}
```

`frontend.sdk-version` и `backend.sdk-version` в примере указаны как метаданные, но оба поля необязательны. Минимальный документ может также не содержать `description`, `initial-state`, `events`, `media` и `results`.

## Правила 1.1

- `manifest-version` строго равен `1.1`.
- `plugin-id`, `plugin-version`, `name`, `frontend` и `backend` обязательны.
- `frontend.url-web`, `backend.health-url` и `backend.control-url` обязательны и используют HTTPS.
- Отсутствующее `description` не сериализуется как `null`.
- `backend.initial-state` необязателен; единственное значение версии 1.1 — `participants`, повторы запрещены.
- `backend.events` необязателен. Если он есть, минимум один из списков `available-event-types` или `available-event-groups` должен быть непустым. При наличии обоих действуют конкретные types; реестр сохраняет только этот эффективный список. В 1.1 поддержан `call.ended`. `event-url` запрещён: plugin backend сам подписывается на StreamAPI по gRPC, а environment-specific адрес StreamAPI не является частью манифеста.
- `backend.media` необязателен; `input` и `output` также необязательны и эквивалентны пустым спискам. Пустой список не означает доступ ко всем потокам.
- `backend.results` необязателен. Если блок есть, `keys` — непустой массив уникальных непустых строк; `delivery-report-url` необязателен.
- Все массивы, для которых схема задаёт `uniqueItems`, отклоняют повторы. Неизвестные поля и enum-значения отклоняются.

Точные допустимые event groups, event types и media types нужно смотреть в JSON Schema. Registry не поддерживает отдельную копию этих каталогов в Java-коде: схема является единым источником правил.

## Публикация и хранение

Запрос публикации содержит JSON в том же kebab-case формате, что и сохранённый манифест. Registry проверяет исходный документ, подставляет `plugin-id`, `name` и `description` из таблицы `plugins`, применяет приоритет конкретных event types и повторно валидирует итоговый JSON.

После публикации пара `plugin-id + plugin-version` неизменяема. Новый URL, набор событий, начальные данные или ключ результатов требуют новой версии плагина. `manifest-version` описывает структуру документа и не заменяет `plugin-version`.

Старые записи 0.1/1.0 читаются как сохранены. Registry не меняет их номер формата и не преобразует `event-url` в `control-url`. Для публикации старого плагина по 1.1 разработчик готовит новый документ с обязательным `control-url` и новой `plugin-version`.

## Границы ответственности

Registry хранит манифесты, проверяет их структурную корректность и отдаёт другим сервисам. Он не:

- создаёт активации и не хранит `activationId`/`callId`;
- проверяет права или адресатов конкретной активации;
- получает список участников и не подключается к LiveKit;
- доставляет события, медиа или результаты;
- хранит StreamAPI target, access token, секреты и настройки окружения.

URL-проверки Registry описаны в [README.md](README.md#url-и-ssrf). Защита от DNS rebinding и redirects обязательно повторяется компонентом, который фактически выполняет server-to-server запрос.
# Поставка схем в Registry

Нормативная схема согласуется в репозитории `specs`, а Plugin Registry исполняет и публикует её копию. Схемы Registry не являются вторым независимо развивающимся контрактом. Утверждённый файл размещается как `src/main/resources/plugin-manifest/<version>.schema.json`; значение `properties.manifest-version.const` обязано совпадать с именем файла.

При старте Registry проверяет JSON, диалект draft 2020-12, соответствие версии и компилирует схемы. Ошибка останавливает запуск. При публикации версия берётся только из `manifest-version`, после чего документ проверяется выбранной схемой и сохраняется без удаления или подмены полей.

Форматы `https-uri` и `public-https-uri` реализуют общую HTTPS-политику. Схема назначает их нужным полям, поэтому Java не содержит список путей версии 1.1. `public-https-uri` также отклоняет локальные имена и непубличные IP-литералы. Это ранняя защита, но не замена SSRF-проверке DNS-результата и фактического адреса соединения в компоненте, который выполняет серверный запрос.
