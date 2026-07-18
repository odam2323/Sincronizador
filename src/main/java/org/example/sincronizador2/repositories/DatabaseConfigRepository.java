package org.example.sincronizador2.repositories;

import org.example.sincronizador2.entities.DatabaseConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DatabaseConfigRepository extends JpaRepository<DatabaseConfigEntity, UUID> {

}
