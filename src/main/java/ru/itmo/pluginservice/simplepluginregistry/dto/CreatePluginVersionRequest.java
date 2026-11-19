package ru.itmo.pluginservice.simplepluginregistry.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import tools.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * Запрос на публикацию, содержащий манифест в исходном виде.
 *
 * <p>JSON Schema является источником истины для структуры документа. Сохранение
 * исходного дерева JSON не позволяет промежуточному преобразованию DTO незаметно
 * отбросить необязательные или недавно добавленные поля.</p>
 */
public final class CreatePluginVersionRequest {

    private final JsonNode manifest;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public CreatePluginVersionRequest(JsonNode manifest) {
        this.manifest = Objects.requireNonNull(manifest, "manifest").deepCopy();
    }

    @JsonValue
    public JsonNode manifest() {
        return manifest.deepCopy();
    }

    public String pluginId() {
        return textValue("plugin-id");
    }

    public String pluginVersion() {
        return textValue("plugin-version");
    }

    private String textValue(String fieldName) {
        JsonNode value = manifest.get(fieldName);
        return value != null && value.isString() ? value.asString() : null;
    }
}
