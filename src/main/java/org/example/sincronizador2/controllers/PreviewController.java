package org.example.sincronizador2.controllers;

import lombok.RequiredArgsConstructor;
import org.example.sincronizador2.models.ColumnaDTO;
import org.example.sincronizador2.models.TablaPreviewDTO;
import org.example.sincronizador2.services.PreviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sync/preview")
@RequiredArgsConstructor
public class PreviewController {

    // importante: acá llamo a un servicio que ya tiene una implementación y busco
    // su respuesta para cada endpoint.

    private final PreviewService previewService;

    @GetMapping("/{configId}/tables")
    public ResponseEntity<List<String>> listarTablas(@PathVariable UUID configId) {
        return ResponseEntity.ok(previewService.listarTablas(configId));
    }

    @GetMapping("/{configId}/tables/{tabla}/columns")
    public ResponseEntity<List<ColumnaDTO>> listarColumnas(
            @PathVariable UUID configId,
            @PathVariable String tabla) {
        return ResponseEntity.ok(previewService.listarColumnas(configId, tabla));
    }

    @GetMapping("/{configId}/tables/{tabla}/sample")
    public ResponseEntity<TablaPreviewDTO> muestra(
            @PathVariable UUID configId,
            @PathVariable String tabla,
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(previewService.muestra(configId, tabla, limite));
    }
}