package org.example.sincronizador2.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.sincronizador2.enums.MetodoParticion;
import org.example.sincronizador2.enums.ModoSync;

import java.time.LocalDateTime;
import java.util.UUID;

public record SyncTaskRequestDTO(
        @NotNull(message = "El ID de configuración es obligatorio") UUID configId,

        @NotBlank(message = "El nombre de la tabla es obligatorio") String tableName,

        @NotNull(message = "El modo de sincronización es obligatorio") ModoSync modoSync,

        @NotNull(message = "El método de partición es obligatorio") MetodoParticion metodoParticion,

        // Solo para modo INCREMENTAL
        String columnaFecha,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,

        // Para PAGINACION y BISECCION
        Integer tamanoChunk,

        // Solo para BISECCION (columna numérica u ordenable, p. ej. una fecha)
        String columnaBiseccion,

        // Solo para CATEGORICO
        String columnaCategoria) {
}
