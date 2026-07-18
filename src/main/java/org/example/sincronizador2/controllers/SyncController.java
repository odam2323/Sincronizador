package org.example.sincronizador2.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sincronizador2.models.*;
import org.example.sincronizador2.services.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;

    @PostMapping("/config")
    public ResponseEntity<SyncResponseDTO> configureDatabase(@Valid @RequestBody DatabaseConfigDTO configRequest) {
        return ResponseEntity.ok(syncService.registerConfiguration(configRequest));
    }

    @GetMapping("/configs")
    public ResponseEntity<List<DatabaseConfigDTO>> getAllConfigs() {
        return ResponseEntity.ok(syncService.getAllConfigurations());
    }

    @PostMapping("/test-connection/{configId}")
    public ResponseEntity<String> testConnection(@PathVariable UUID configId) {
        return ResponseEntity.ok(syncService.testConnection(configId));
    }

    @PostMapping("/start")
    public ResponseEntity<SyncResponseDTO> startSync(@Valid @RequestBody SyncTaskRequestDTO request) {
        return ResponseEntity.ok(syncService.startSync(request));
    }

    @GetMapping("/tasks/{taskId}/progress")
    public ResponseEntity<SyncProgressDTO> getProgress(@PathVariable UUID taskId) {
        return ResponseEntity.ok(syncService.getTaskProgress(taskId));
    }
}