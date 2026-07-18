package org.example.sincronizador2.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.sincronizador2.enums.SyncStrategy;

import java.util.UUID;

public record SyncTaskRequestDTO(
        @NotNull(message = "El ID de configuración es obligatorio")
        UUID configId,

        @NotBlank(message = "El nombre de la tabla es obligatorio")
        String tableName,

        @NotNull(message = "La estrategia de sincronización es obligatoria")
        SyncStrategy strategy
) {}