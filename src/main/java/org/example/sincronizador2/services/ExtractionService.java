package org.example.sincronizador2.services;

import org.example.sincronizador2.entities.DatabaseConfigEntity;
import org.example.sincronizador2.entities.SyncTaskEntity;
import org.springframework.scheduling.annotation.Async;

public interface ExtractionService {

    @Async("syncExecutor")
    void extractAndUpload(DatabaseConfigEntity config, SyncTaskEntity task);
}