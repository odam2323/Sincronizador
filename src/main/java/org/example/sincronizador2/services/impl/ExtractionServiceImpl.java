package org.example.sincronizador2.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.sincronizador2.entities.DatabaseConfigEntity;
import org.example.sincronizador2.entities.SyncTaskEntity;
import org.example.sincronizador2.enums.ModoSync;
import org.example.sincronizador2.enums.SyncStatus;
import org.example.sincronizador2.models.Chunk;
import org.example.sincronizador2.repositories.SyncTaskRepository;
import org.example.sincronizador2.services.ExtractionService;
import org.example.sincronizador2.services.MinioService;
import org.example.sincronizador2.services.particion.ParticionUtils;
import org.example.sincronizador2.services.particion.PartitionStrategy;
import org.example.sincronizador2.services.particion.PartitionStrategyResolver;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExtractionServiceImpl implements ExtractionService {

    private final MinioService minioService;
    private final SyncTaskRepository syncTaskRepository;
    private final PartitionStrategyResolver resolver;

    @Override
    @Async("syncExecutor")
    public void extractAndUpload(DatabaseConfigEntity config, SyncTaskEntity task) {
        String url = buildJdbcUrl(config);

        try (Connection connection = DriverManager.getConnection(url, config.getUserName(), config.getPassword())) {

            String condicionBase = construirCondicionBase(task);

            long total = ParticionUtils.contar(connection, task.getTableName(), condicionBase);
            task.setTotalRows(total);
            task.setProcessedRows(0L);
            task.setStatus(SyncStatus.RUNNING);
            task.setStartedAt(LocalDateTime.now());
            syncTaskRepository.save(task);

            PartitionStrategy estrategia = resolver.resolver(task.getMetodoParticion());
            List<Chunk> chunks = estrategia.generarChunks(connection, task, condicionBase);

            String carpeta = construirCarpeta(config, task);
            long procesadas = 0;
            int idx = 0;
            for (Chunk chunk : chunks) {
                idx++;
                ResultadoChunk r = ejecutarChunk(connection, chunk.sql());

                String objeto = String.format("%s/chunk_%04d.csv", carpeta, idx);
                byte[] bytes = r.csv().getBytes();
                minioService.uploadFile(objeto, new ByteArrayInputStream(bytes), bytes.length, "text/csv");

                procesadas += r.filas();
                task.setProcessedRows(procesadas);
                syncTaskRepository.save(task);
            }

            task.setMinioPath(carpeta);
            task.setStatus(SyncStatus.COMPLETED);
            task.setFinishedAt(LocalDateTime.now());
            syncTaskRepository.save(task);

        } catch (Exception e) {
            task.setStatus(SyncStatus.FAILED);
            task.setFinishedAt(LocalDateTime.now());
            syncTaskRepository.save(task);
            throw new RuntimeException("Error en la extracción: " + e.getMessage(), e);
        }
    }

    // El modo define el filtro base: vacío en REINDEXACION, rango de fechas en
    // INCREMENTAL
    private String construirCondicionBase(SyncTaskEntity task) {
        if (task.getModoSync() == ModoSync.INCREMENTAL) {
            return task.getColumnaFecha() + " BETWEEN "
                    + ParticionUtils.literal(task.getFechaInicio()) + " AND "
                    + ParticionUtils.literal(task.getFechaFin());
        }
        return "";
    }

    private record ResultadoChunk(String csv, long filas) {
    }

    private ResultadoChunk ejecutarChunk(Connection connection, String sql) throws SQLException {
        StringWriter writer = new StringWriter();
        long filas = 0;

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                if (i > 1)
                    writer.append(",");
                writer.append(meta.getColumnName(i));
            }
            writer.append("\n");

            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1)
                        writer.append(",");
                    String value = rs.getString(i);
                    if (value != null) {
                        writer.append("\"").append(value.replace("\"", "\"\"")).append("\"");
                    }
                }
                writer.append("\n");
                filas++;
            }
        }
        return new ResultadoChunk(writer.toString(), filas);
    }

    private String construirCarpeta(DatabaseConfigEntity config, SyncTaskEntity task) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("%s/%s/%s_%s_%s",
                config.getDatabaseName(),
                task.getTableName(),
                task.getModoSync().name().toLowerCase(),
                task.getMetodoParticion().name().toLowerCase(),
                timestamp);
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
}