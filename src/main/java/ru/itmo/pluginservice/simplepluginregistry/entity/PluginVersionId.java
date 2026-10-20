package ru.itmo.pluginservice.simplepluginregistry.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PluginVersionId implements Serializable {

    @Column(name = "plugin_id", nullable = false, updatable = false)
    private String pluginId;

    @Column(name = "version", nullable = false, updatable = false)
    private String version;

    protected PluginVersionId() {
    }

    public PluginVersionId(String pluginId, String version) {
        this.pluginId = pluginId;
        this.version = version;
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PluginVersionId that)) {
            return false;
        }
        return Objects.equals(pluginId, that.pluginId) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginId, version);
    }
}
