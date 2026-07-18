import csv
import os
import sys
import io
from datetime import datetime

import psycopg2


def is_int(v):
    if v is None or v == "":
        return False
    try:
        int(v)
        return True
    except ValueError:
        return False


def is_float(v):
    if v is None or v == "":
        return False
    try:
        float(v)
        return True
    except ValueError:
        return False


def is_bool(v):
    return v.strip().lower() in ("true", "false", "t", "f")


TS_FORMATS = ["%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S", "%Y-%m-%d"]


def is_ts(v):
    for fmt in TS_FORMATS:
        try:
            datetime.strptime(v.strip(), fmt)
            return True
        except ValueError:
            continue
    return False


def infer_column_type(values):
    non_empty = [v for v in values if v is not None and v.strip() != ""]
    if not non_empty:
        return "TEXT"
    if all(is_int(v) for v in non_empty):
        return "BIGINT"
    if all(is_float(v) for v in non_empty):
        return "DOUBLE PRECISION"
    if all(is_bool(v) for v in non_empty):
        return "BOOLEAN"
    if all(is_ts(v) for v in non_empty):
        return "TIMESTAMP"
    return "TEXT"


def sanitize(name):
    # Nombre de columna/tabla seguro para SQL
    clean = "".join(c if c.isalnum() else "_" for c in name.strip().lower())
    if clean and clean[0].isdigit():
        clean = "_" + clean
    return clean or "col"


def connect():
    return psycopg2.connect(
        host=os.environ.get("DB_HOST", "postgres-db"),
        port=os.environ.get("DB_PORT", "5432"),
        dbname=os.environ["DB_NAME"],
        user=os.environ["DB_USER"],
        password=os.environ["DB_PASSWORD"],
    )


def main():
    csv_path = os.environ["CSV_PATH"]

    if not os.path.exists(csv_path):
        print(f"[seeder] No se encontró el CSV en {csv_path}", flush=True)
        sys.exit(1)

    # Nombre de tabla: variable de entorno o nombre del archivo
    table_name = os.environ.get("TABLE_NAME")
    if not table_name:
        base = os.path.basename(csv_path)
        table_name = os.path.splitext(base)[0]
    table_name = sanitize(table_name)

    with open(csv_path, newline="", encoding="utf-8") as f:
        reader = csv.reader(f)
        header = next(reader)
        rows = list(reader)

    columns = [sanitize(h) for h in header]

    # Inferir tipo por columna
    col_values = list(zip(*rows)) if rows else [[] for _ in columns]
    col_types = [infer_column_type(list(vals)) for vals in col_values]

    conn = connect()
    conn.autocommit = False
    try:
        with conn.cursor() as cur:
            # Idempotente: si ya existe, no la recrea
            cur.execute(
                "SELECT to_regclass(%s)", (table_name,)
            )
            exists = cur.fetchone()[0] is not None
            if exists:
                print(f"[seeder] La tabla '{table_name}' ya existe, no se recarga.", flush=True)
                return

            cols_ddl = ",\n  ".join(
                f"{name} {ctype}" for name, ctype in zip(columns, col_types)
            )
            ddl = (
                f"CREATE TABLE {table_name} (\n"
                f"  id BIGSERIAL PRIMARY KEY,\n  {cols_ddl}\n)"
            )
            print(f"[seeder] Creando tabla:\n{ddl}", flush=True)
            cur.execute(ddl)

            # Carga masiva con COPY desde memoria
            buffer = io.StringIO()
            writer = csv.writer(buffer)
            writer.writerows(rows)
            buffer.seek(0)

            col_list = ", ".join(columns)
            cur.copy_expert(
                f"COPY {table_name} ({col_list}) FROM STDIN WITH (FORMAT csv)",
                buffer,
            )

        conn.commit()
        print(f"[seeder] Cargados {len(rows)} registros en '{table_name}'.", flush=True)
    except Exception as e:
        conn.rollback()
        print(f"[seeder] Error: {e}", flush=True)
        sys.exit(1)
    finally:
        conn.close()


if __name__ == "__main__":
    main()