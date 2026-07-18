package org.example.sincronizador2.repositories;

import org.example.sincronizador2.entities.SyncTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SyncTaskRepository extends JpaRepository<SyncTaskEntity, UUID> {
}

