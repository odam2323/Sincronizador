package org.example.sincronizador2.models;

import java.util.List;

public record TablaPreviewDTO(
        String tabla,
        List<ColumnaDTO> columnas,
        List<List<String>> muestra) {
}