package org.example.sincronizador2.services.particion;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ParticionUtils {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ParticionUtils() {
    }

    // Combina dos condiciones SQL (sin la palabra WHERE). Cualquiera puede estar
    // vacía.
    public static String combinar(String a, String b) {
        boolean va = a == null || a.isBlank();
        boolean vb = b == null || b.isBlank();
        if (va && vb)
            return "";
        if (va)
            return b;
        if (vb)
            return a;
        return "(" + a + ") AND (" + b + ")";
    }

    // Antepone WHERE solo si hay condición
    public static String where(String cond) {
        return (cond == null || cond.isBlank()) ? "" : " WHERE " + cond;
    }

    // Formatea un valor como literal SQL seguro para tipos básicos
    public static String literal(Object v) {
        if (v == null)
            return "NULL";
        if (v instanceof Number)
            return v.toString();
        if (v instanceof Timestamp ts)
            return "'" + FMT.format(ts.toLocalDateTime()) + "'";
        if (v instanceof LocalDateTime ldt)
            return "'" + FMT.format(ldt) + "'";
        return "'" + v.toString().replace("'", "''") + "'";
    }

    // Cuenta filas de una tabla aplicando una condición (sin WHERE)
    public static long contar(Connection c, String tabla, String cond) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tabla + where(cond);
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}