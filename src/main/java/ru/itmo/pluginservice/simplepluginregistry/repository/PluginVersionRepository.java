package ru.itmo.pluginservice.simplepluginregistry.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.pluginservice.simplepluginregistry.entity.PluginVersionEntity;
import ru.itmo.pluginservice.simplepluginregistry.entity.PluginVersionId;

import java.util.Optional;

public interface PluginVersionRepository extends JpaRepository<PluginVersionEntity, PluginVersionId> {

    Page<PluginVersionEntity> findAllByIdPluginId(String pluginId, Pageable pageable);

    @EntityGraph(attributePaths = "plugin")
    Optional<PluginVersionEntity> findDetailedById(PluginVersionId id);
}
