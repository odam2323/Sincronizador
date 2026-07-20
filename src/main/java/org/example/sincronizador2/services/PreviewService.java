package org.example.sincronizador2.services;

import org.example.sincronizador2.models.ColumnaDTO;
import org.example.sincronizador2.models.TablaPreviewDTO;

import java.util.List;
import java.util.UUID;

public interface PreviewService {
    List<String> listarTablas(UUID configId);

    List<ColumnaDTO> listarColumnas(UUID configId, String tabla);

    TablaPreviewDTO muestra(UUID configId, String tabla, int limite);
}