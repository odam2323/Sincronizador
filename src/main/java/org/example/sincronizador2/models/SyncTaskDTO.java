package org.example.sincronizador2.models;

import org.example.sincronizador2.enums.MetodoParticion;
import org.example.sincronizador2.enums.ModoSync;
import org.example.sincronizador2.enums.SyncStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SyncTaskDTO(
        UUID id,
        UUID configId,
        String databaseName,
        String tableName,
        ModoSync modoSync,
        MetodoParticion metodoParticion,
        SyncStatus status,
        Long totalRows,
        Long processedRows,
        Integer percentage,
        String minioPath,
        LocalDateTime startedAt,
        LocalDateTime finishedAt) {
}