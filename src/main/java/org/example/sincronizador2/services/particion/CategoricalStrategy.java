package org.example.sincronizador2.services.particion;

import org.example.sincronizador2.entities.SyncTaskEntity;
import org.example.sincronizador2.enums.MetodoParticion;
import org.example.sincronizador2.models.Chunk;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Component
public class CategoricalStrategy implements PartitionStrategy {

    @Override
    public MetodoParticion metodo() {
        return MetodoParticion.CATEGORICO;
    }

    @Override
    public List<Chunk> generarChunks(Connection c, SyncTaskEntity task, String condicionBase) throws SQLException {
        String tabla = task.getTableName();
        String col = task.getColumnaCategoria();

        List<Object> valores = new ArrayList<>();
        String sqlDistinct = "SELECT DISTINCT " + col + " FROM " + tabla + ParticionUtils.where(condicionBase)
                + " ORDER BY 1";
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sqlDistinct)) {
            while (rs.next())
                valores.add(rs.getObject(1));
        }

        List<Chunk> chunks = new ArrayList<>();
        for (Object v : valores) {
            String cond = (v == null) ? col + " IS NULL" : col + " = " + ParticionUtils.literal(v);
            String condFull = ParticionUtils.combinar(condicionBase, cond);
            String sql = "SELECT * FROM " + tabla + ParticionUtils.where(condFull);
            chunks.add(new Chunk((v == null) ? "categoria NULL" : "categoria " + v, sql));
        }
        return chunks;
    }
}