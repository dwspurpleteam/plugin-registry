package ru.itmo.pluginservice.simplepluginregistry.dto;

import java.util.List;

public record ManifestSchemaVersionsResponse(List<String> versions) {
}
