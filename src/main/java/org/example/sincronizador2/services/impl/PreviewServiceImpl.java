package org.example.sincronizador2.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.sincronizador2.entities.DatabaseConfigEntity;
import org.example.sincronizador2.models.ColumnaDTO;
import org.example.sincronizador2.models.TablaPreviewDTO;
import org.example.sincronizador2.repositories.DatabaseConfigRepository;
import org.example.sincronizador2.services.PreviewService;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PreviewServiceImpl implements PreviewService {

    private final DatabaseConfigRepository configRepository;

    @Override
    public List<String> listarTablas(UUID configId) {
        DatabaseConfigEntity config = findConfigOrThrow(configId);
        List<String> tablas = new ArrayList<>();

        try (Connection connection = abrirConexion(config)) {
            DatabaseMetaData meta = connection.getMetaData();
            // Solo tablas de usuario, no vistas ni tablas del sistema
            try (ResultSet rs = meta.getTables(connection.getCatalog(), esquemaPorDefecto(config), "%",
                    new String[] { "TABLE" })) {
                while (rs.next()) {
                    tablas.add(rs.getString("TABLE_NAME"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar tablas: " + e.getMessage(), e);
        }
        return tablas;
    }

    @Override
    public List<ColumnaDTO> listarColumnas(UUID configId, String tabla) {
        DatabaseConfigEntity config = findConfigOrThrow(configId);
        List<ColumnaDTO> columnas = new ArrayList<>();

        try (Connection connection = abrirConexion(config)) {
            DatabaseMetaData meta = connection.getMetaData();
            try (ResultSet rs = meta.getColumns(connection.getCatalog(), esquemaPorDefecto(config), tabla, "%")) {
                while (rs.next()) {
                    columnas.add(new ColumnaDTO(
                            rs.getString("COLUMN_NAME"),
                            rs.getString("TYPE_NAME")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar columnas: " + e.getMessage(), e);
        }

        if (columnas.isEmpty()) {
            throw new IllegalArgumentException("La tabla '" + tabla + "' no existe o no tiene columnas");
        }
        return columnas;
    }

    @Override
    public TablaPreviewDTO muestra(UUID configId, String tabla, int limite) {
        DatabaseConfigEntity config = findConfigOrThrow(configId);
        List<ColumnaDTO> columnas = listarColumnas(configId, tabla);
        List<List<String>> filas = new ArrayList<>();

        try (Connection connection = abrirConexion(config);
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM " + tabla + " LIMIT " + limite)) {

            int columnCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                List<String> fila = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    fila.add(rs.getString(i));
                }
                filas.add(fila);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener la muestra: " + e.getMessage(), e);
        }

        return new TablaPreviewDTO(tabla, columnas, filas);
    }

    // utils

    // El esquema por defecto difiere entre motores: 'public' en Postgres, el
    // usuario en Oracle
    private String esquemaPorDefecto(DatabaseConfigEntity config) {
        return switch (config.getDbType()) {
            case "POSTGRES" -> "public";
            case "ORACLE" -> config.getUserName().toUpperCase();
            default -> null;
        };
    }

    private Connection abrirConexion(DatabaseConfigEntity config) throws SQLException {
        return DriverManager.getConnection(buildJdbcUrl(config), config.getUserName(), config.getPassword());
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
}