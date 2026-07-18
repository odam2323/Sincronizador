package org.example.sincronizador2.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.sincronizador2.entities.DatabaseConfigEntity;
import org.example.sincronizador2.entities.SyncTaskEntity;
import org.example.sincronizador2.enums.SyncStatus;
import org.example.sincronizador2.repositories.SyncTaskRepository;
import org.example.sincronizador2.services.ExtractionService;
import org.example.sincronizador2.services.MinioService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ExtractionServiceImpl implements ExtractionService {

    private final MinioService minioService;
    private final SyncTaskRepository syncTaskRepository;

    @Override
    public void extractAndUpload(DatabaseConfigEntity config, SyncTaskEntity task) {
        String url = buildJdbcUrl(config);

        try (Connection connection = DriverManager.getConnection(url, config.getUserName(), config.getPassword())) {

            long totalRows = countRows(connection, task.getTableName());
            task.setTotalRows(totalRows);
            task.setProcessedRows(0L);
            task.setStatus(SyncStatus.RUNNING);
            task.setStartedAt(LocalDateTime.now());
            syncTaskRepository.save(task);

            String csv = extractTableToCsv(connection, task);

            String objectName = buildObjectName(config, task);
            byte[] csvBytes = csv.getBytes();
            ByteArrayInputStream stream = new ByteArrayInputStream(csvBytes);
            minioService.uploadFile(objectName, stream, csvBytes.length, "text/csv");

            task.setMinioPath(objectName);
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

    private long countRows(Connection connection, String tableName) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private String extractTableToCsv(Connection connection, SyncTaskEntity task) throws SQLException {
        StringWriter writer = new StringWriter();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM" + task.getTableName())) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) writer.append(",");
                writer.append(meta.getColumnName(i));
            }
            writer.append("\n");

            long processed = 0;
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) writer.append(",");
                    String value = rs.getString(i);
                    if (value != null) {
                        writer.append("\"").append(value.replace("\"", "\"\"")).append("\"");
                    }
                }
                writer.append("\n");

                processed++;
                if (processed % 1000 == 0) {
                    task.setProcessedRows(processed);
                    syncTaskRepository.save(task);
                }
            }

            task.setProcessedRows(processed);
            syncTaskRepository.save(task);
        }

        return writer.toString();
    }

    private String buildObjectName(DatabaseConfigEntity config, SyncTaskEntity task) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("%s/%s/%s_%s.csv",
                config.getDatabaseName(),
                task.getTableName(),
                task.getStrategy().name().toLowerCase(),
                timestamp);
    }

    private String buildJdbcUrl(DatabaseConfigEntity config) {
        return switch (config.getDbType()) {
            case "POSTGRES" -> String.format("jdbc:postgresql://%s:%d/%s", config.getHost(), config.getPort(), config.getDatabaseName());
            case "ORACLE" -> String.format("jdbc:oracle:thin:@%s:%d:%s", config.getHost(), config.getPort(), config.getDatabaseName());
            default -> throw new RuntimeException("Tipo de base de datos no soportado: " + config.getDbType());
        };
    }
}