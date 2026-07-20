package org.example.sincronizador2.services.particion;

import org.example.sincronizador2.entities.SyncTaskEntity;
import org.example.sincronizador2.enums.MetodoParticion;
import org.example.sincronizador2.models.Chunk;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class BisectionStrategy implements PartitionStrategy {

    private static final int MAX_PROFUNDIDAD = 60;

    @Override
    public MetodoParticion metodo() {
        return MetodoParticion.BISECCION;
    }

    @Override
    public List<Chunk> generarChunks(Connection c, SyncTaskEntity task, String condicionBase) throws SQLException {
        String tabla = task.getTableName();
        String col = task.getColumnaBiseccion();

        Object min, max;
        String sqlMinMax = "SELECT MIN(" + col + "), MAX(" + col + ") FROM " + tabla
                + ParticionUtils.where(condicionBase);
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sqlMinMax)) {
            rs.next();
            min = rs.getObject(1);
            max = rs.getObject(2);
        }

        List<Chunk> chunks = new ArrayList<>();
        if (min == null || max == null)
            return chunks; // sin datos
        dividir(c, task, condicionBase, col, min, false, max, task.getTamanoChunk(), chunks, 0);
        return chunks;
    }

    private void dividir(Connection c, SyncTaskEntity task, String base, String col,
            Object lo, boolean loStrict, Object hi, int chunkSize,
            List<Chunk> chunks, int depth) throws SQLException {

        String opLo = loStrict ? " > " : " >= ";
        String cond = col + opLo + ParticionUtils.literal(lo) + " AND " + col + " <= " + ParticionUtils.literal(hi);
        String condFull = ParticionUtils.combinar(base, cond);
        long count = ParticionUtils.contar(c, task.getTableName(), condFull);

        if (count == 0)
            return;

        if (count <= chunkSize || depth >= MAX_PROFUNDIDAD) {
            emitir(task, condFull, count, chunks, false);
            return;
        }

        Object mid = puntoMedio(lo, hi);
        if (mid == null || !puedeProgresar(lo, mid, hi)) {
            emitir(task, condFull, count, chunks, true);
            return;
        }

        dividir(c, task, base, col, lo, loStrict, mid, chunkSize, chunks, depth + 1); // [lo, mid]
        dividir(c, task, base, col, mid, true, hi, chunkSize, chunks, depth + 1); // (mid, hi]
    }

    private void emitir(SyncTaskEntity task, String condFull, long count, List<Chunk> chunks, boolean indivisible) {
        String sql = "SELECT * FROM " + task.getTableName() + ParticionUtils.where(condFull);
        String desc = (indivisible ? "rango indivisible (" : "rango (") + count + " filas)";
        chunks.add(new Chunk(desc, sql));
    }

    private Object puntoMedio(Object lo, Object hi) {
        if (lo instanceof Date a && hi instanceof Date b) {
            return new Timestamp(a.getTime() + (b.getTime() - a.getTime()) / 2);
        }
        if (lo instanceof Number a && hi instanceof Number b) {
            return a.longValue() + (b.longValue() - a.longValue()) / 2;
        }
        return null; // tipo no soportado para bisección
    }

    private boolean puedeProgresar(Object lo, Object mid, Object hi) {
        if (lo instanceof Date a && mid instanceof Date m && hi instanceof Date b) {
            return m.getTime() > a.getTime() && m.getTime() < b.getTime();
        }
        if (lo instanceof Number a && mid instanceof Number m && hi instanceof Number b) {
            return m.longValue() > a.longValue() && m.longValue() < b.longValue();
        }
        return false;
    }
}