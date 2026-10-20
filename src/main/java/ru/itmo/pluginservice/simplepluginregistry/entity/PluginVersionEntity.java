package ru.itmo.pluginservice.simplepluginregistry.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@Entity
@Immutable
@Table(name = "plugin_versions")
public class PluginVersionEntity {

    @EmbeddedId
    private PluginVersionId id;

    @MapsId("pluginId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plugin_id", nullable = false, updatable = false)
    private PluginEntity plugin;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "manifest", nullable = false, columnDefinition = "jsonb", updatable = false)
    private JsonNode manifest;

    @Column(name = "frontend_sdk_version", insertable = false, updatable = false)
    private String frontendSdkVersion;

    @Column(name = "backend_sdk_version", insertable = false, updatable = false)
    private String backendSdkVersion;

    protected PluginVersionEntity() {
    }

    public PluginVersionEntity(PluginVersionId id, PluginEntity plugin, JsonNode manifest) {
        this.id = id;
        this.plugin = plugin;
        this.manifest = manifest;
    }

    public PluginVersionId getId() {
        return id;
    }

    public PluginEntity getPlugin() {
        return plugin;
    }

    public JsonNode getManifest() {
        return manifest;
    }

    public String getFrontendSdkVersion() {
        return frontendSdkVersion;
    }

    public String getBackendSdkVersion() {
        return backendSdkVersion;
    }
}
