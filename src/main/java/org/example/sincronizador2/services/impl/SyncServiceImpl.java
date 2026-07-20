package org.example.sincronizador2.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.sincronizador2.entities.DatabaseConfigEntity;
import org.example.sincronizador2.entities.SyncTaskEntity;
import org.example.sincronizador2.enums.SyncStatus;
import org.example.sincronizador2.models.*;
import org.example.sincronizador2.repositories.DatabaseConfigRepository;
import org.example.sincronizador2.repositories.SyncTaskRepository;
import org.example.sincronizador2.services.ExtractionService;
import org.example.sincronizador2.services.SyncService;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SyncServiceImpl implements SyncService {

    private final DatabaseConfigRepository configRepository;
    private final SyncTaskRepository taskRepository;
    private final ExtractionService extractionService;

    @Override
    public SyncResponseDTO registerConfiguration(DatabaseConfigDTO configDTO) {
        DatabaseConfigEntity entity = new DatabaseConfigEntity();
        entity.setDbType(configDTO.getDbType());
        entity.setUserName(configDTO.getUserName());
        entity.setPassword(configDTO.getPassword());
        entity.setHost(configDTO.getHost());
        entity.setPort(configDTO.getPort());
        entity.setDatabaseName(configDTO.getDatabaseName());

        DatabaseConfigEntity saved = configRepository.save(entity);
        String msg = "Configuración guardada exitosamente para: " + saved.getDatabaseName();
        return new SyncResponseDTO(saved.getId(), msg);
    }

    @Override
    public List<DatabaseConfigDTO> getAllConfigurations() {
        return configRepository.findAll().stream()
                .map(e -> new DatabaseConfigDTO(
                        e.getDbType(), e.getUserName(), e.getPassword(),
                        e.getHost(), e.getPort(), e.getDatabaseName()))
                .toList();
    }

    @Override
    public String testConnection(UUID configId) {
        DatabaseConfigEntity config = findConfigOrThrow(configId);
        String url = buildJdbcUrl(config);

        try (Connection connection = DriverManager.getConnection(url, config.getUserName(), config.getPassword())) {
            Statement stmt = connection.createStatement();
            stmt.execute("SELECT 1");
            return "Conexión exitosa a " + config.getDatabaseName();
        } catch (SQLException e) {
            return "Error al conectar: " + e.getMessage();
        }
    }

    @Override
    public SyncResponseDTO startSync(SyncTaskRequestDTO request) {
        validarRequest(request);

        DatabaseConfigEntity config = findConfigOrThrow(request.configId());

        SyncTaskEntity task = new SyncTaskEntity();
        task.setConfig(config);
        task.setTableName(request.tableName());
        task.setModoSync(request.modoSync());
        task.setMetodoParticion(request.metodoParticion());
        task.setColumnaFecha(request.columnaFecha());
        task.setFechaInicio(request.fechaInicio());
        task.setFechaFin(request.fechaFin());
        task.setTamanoChunk(request.tamanoChunk());
        task.setColumnaBiseccion(request.columnaBiseccion());
        task.setColumnaCategoria(request.columnaCategoria());
        task.setStatus(SyncStatus.PENDING);
        task.setProcessedRows(0L);
        taskRepository.save(task);

        // Se lanza en segundo plano; el método devuelve de inmediato
        extractionService.extractAndUpload(config, task);

        String msg = "Sincronización iniciada para tabla: " + request.tableName();
        return new SyncResponseDTO(task.getId(), msg);
    }

    @Override
    public SyncProgressDTO getTaskProgress(UUID taskId) {
        SyncTaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada: " + taskId));

        int percentage = 0;
        if (task.getTotalRows() != null && task.getTotalRows() > 0) {
            percentage = (int) ((task.getProcessedRows() * 100) / task.getTotalRows());
        }

        return new SyncProgressDTO(
                task.getId(),
                task.getStatus(),
                task.getTotalRows(),
                task.getProcessedRows(),
                percentage);
    }

    // Valida que cada combinación modo/método traiga los parámetros que necesita
    private void validarRequest(SyncTaskRequestDTO request) {
        switch (request.modoSync()) {
            case INCREMENTAL -> {
                if (request.columnaFecha() == null || request.columnaFecha().isBlank()) {
                    throw new IllegalArgumentException("El modo INCREMENTAL requiere 'columnaFecha'");
                }
                if (request.fechaInicio() == null || request.fechaFin() == null) {
                    throw new IllegalArgumentException("El modo INCREMENTAL requiere 'fechaInicio' y 'fechaFin'");
                }
            }
            case REINDEXACION -> {
                // No requiere parámetros adicionales de fecha
            }
        }

        switch (request.metodoParticion()) {
            case PAGINACION -> {
                if (request.tamanoChunk() == null || request.tamanoChunk() <= 0) {
                    throw new IllegalArgumentException("El método PAGINACION requiere un 'tamanoChunk' positivo");
                }
            }
            case BISECCION -> {
                if (request.tamanoChunk() == null || request.tamanoChunk() <= 0) {
                    throw new IllegalArgumentException("El método BISECCION requiere un 'tamanoChunk' positivo");
                }
                if (request.columnaBiseccion() == null || request.columnaBiseccion().isBlank()) {
                    throw new IllegalArgumentException("El método BISECCION requiere 'columnaBiseccion'");
                }
            }
            case CATEGORICO -> {
                if (request.columnaCategoria() == null || request.columnaCategoria().isBlank()) {
                    throw new IllegalArgumentException("El método CATEGORICO requiere 'columnaCategoria'");
                }
            }
        }
    }

    private DatabaseConfigEntity findConfigOrThrow(UUID configId) {
        return configRepository.findById(configId)
                .orElseThrow(() -> new RuntimeException("Configuración no encontrada: " + configId));
    }

    private String buildJdbcUrl(DatabaseConfigEntity config) {
        return switch (config.getDbType()) {
            case "POSTGRES" -> String.format("jdbc:postgresql://%s:%d/%s", config.getHost(), config.getPort(),
                    config.getDatabaseName());
            case "ORACLE" -> String.format("jdbc:oracle:thin:@%s:%d:%s", config.getHost(), config.getPort(),
                    config.getDatabaseName());
            default -> throw new RuntimeException("Tipo de base de datos no soportado: " + config.getDbType());
        };
    }

    @Override
    public List<SyncTaskDTO> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public SyncTaskDTO getTask(UUID taskId) {
        SyncTaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada: " + taskId));
        return mapToDTO(task);
    }

    private SyncTaskDTO mapToDTO(SyncTaskEntity task) {
        int percentage = 0;
        if (task.getTotalRows() != null && task.getTotalRows() > 0) {
            percentage = (int) ((task.getProcessedRows() * 100) / task.getTotalRows());
        }
        return new SyncTaskDTO(
                task.getId(),
                task.getConfig().getId(),
                task.getConfig().getDatabaseName(),
                task.getTableName(),
                task.getModoSync(),
                task.getMetodoParticion(),
                task.getStatus(),
                task.getTotalRows(),
                task.getProcessedRows(),
                percentage,
                task.getMinioPath(),
                task.getStartedAt(),
                task.getFinishedAt());
    }

}
