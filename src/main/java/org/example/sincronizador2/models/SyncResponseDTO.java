package org.example.sincronizador2.models;

import java.util.UUID;

public record SyncResponseDTO(UUID syncId, String msg) {
}
