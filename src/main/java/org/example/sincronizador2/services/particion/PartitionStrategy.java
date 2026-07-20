package org.example.sincronizador2.services.particion;

import org.example.sincronizador2.entities.SyncTaskEntity;
import org.example.sincronizador2.enums.MetodoParticion;
import org.example.sincronizador2.models.Chunk;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface PartitionStrategy {

    // Qué método de partición implementa esta estrategia
    MetodoParticion metodo();

    // Genera los chunks. condicionBase es el filtro del modo (vacío en
    // REINDEXACION,
    // rango de fechas en INCREMENTAL), sin la palabra WHERE.
    List<Chunk> generarChunks(Connection connection, SyncTaskEntity task, String condicionBase) throws SQLException;
}