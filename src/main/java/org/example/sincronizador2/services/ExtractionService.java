package org.example.sincronizador2.services;

import org.example.sincronizador2.entities.DatabaseConfigEntity;
import org.example.sincronizador2.entities.SyncTaskEntity;

public interface ExtractionService {
    void extractAndUpload(DatabaseConfigEntity config, SyncTaskEntity task);
}