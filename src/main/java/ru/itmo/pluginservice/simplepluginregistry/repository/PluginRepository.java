package ru.itmo.pluginservice.simplepluginregistry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.pluginservice.simplepluginregistry.entity.PluginEntity;

public interface PluginRepository extends JpaRepository<PluginEntity, String> {
}
