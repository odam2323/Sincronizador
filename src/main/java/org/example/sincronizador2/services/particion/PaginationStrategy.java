package org.example.sincronizador2.services.particion;

import org.example.sincronizador2.entities.SyncTaskEntity;
import org.example.sincronizador2.enums.MetodoParticion;
import org.example.sincronizador2.models.Chunk;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class PaginationStrategy implements PartitionStrategy {

    @Override
    public MetodoParticion metodo() {
        return MetodoParticion.PAGINACION;
    }

    @Override
    public List<Chunk> generarChunks(Connection connection, SyncTaskEntity task, String condicionBase)
            throws SQLException {
        String tabla = task.getTableName();
        int chunkSize = task.getTamanoChunk();
        String orderCol = columnaOrden(connection, tabla);
        long total = ParticionUtils.contar(connection, tabla, condicionBase);

        List<Chunk> chunks = new ArrayList<>();
        long offset = 0;
        int i = 0;
        while (offset < total) {
            String sql = sqlPaginado(task.getConfig().getDbType(), tabla, condicionBase, orderCol, chunkSize, offset);
            chunks.add(new Chunk("pagina " + (++i) + " (offset " + offset + ")", sql));
            offset += chunkSize;
        }
        return chunks;
    }

    // Clave primaria si existe; si no, la primera columna
    private String columnaOrden(Connection c, String tabla) throws SQLException {
        DatabaseMetaData meta = c.getMetaData();
        try (ResultSet rs = meta.getPrimaryKeys(c.getCatalog(), null, tabla)) {
            if (rs.next())
                return rs.getString("COLUMN_NAME");
        }
        try (ResultSet rs = meta.getColumns(c.getCatalog(), null, tabla, "%")) {
            if (rs.next())
                return rs.getString("COLUMN_NAME");
        }
        return "1";
    }

    private String sqlPaginado(String dbType, String tabla, String base, String orderCol, int limit, long offset) {
        String sel = "SELECT * FROM " + tabla + ParticionUtils.where(base) + " ORDER BY " + orderCol;
        if ("ORACLE".equals(dbType)) {
            return sel + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
        }
        return sel + " LIMIT " + limit + " OFFSET " + offset;
    }
}