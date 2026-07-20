package org.example.sincronizador2.services;

import org.example.sincronizador2.models.*;

import java.util.List;
import java.util.UUID;

public interface SyncService {
    SyncResponseDTO registerConfiguration(DatabaseConfigDTO config);

    List<DatabaseConfigDTO> getAllConfigurations();

    String testConnection(UUID configId);

    SyncResponseDTO startSync(SyncTaskRequestDTO request);

    SyncProgressDTO getTaskProgress(UUID taskId);

    List<SyncTaskDTO> getAllTasks();

    SyncTaskDTO getTask(UUID taskId);
}