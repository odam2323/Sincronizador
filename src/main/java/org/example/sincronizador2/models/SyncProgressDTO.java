package org.example.sincronizador2.models;

import org.example.sincronizador2.enums.SyncStatus;

import java.util.UUID;

public record SyncProgressDTO(
        UUID taskId,
        SyncStatus status,
        Long totalRows,
        Long processedRows,
        Integer percentage
) {}