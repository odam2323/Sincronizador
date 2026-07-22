package org.example.sincronizador2.models;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseConfigDTO {

    @NotBlank(message = "El tipo de base de datos es obligatorio")
    @Pattern(regexp = "^(POSTGRES|ORACLE)$", message = "El tipo debe ser estrictamente POSTGRES u ORACLE")
    private String dbType;

    @NotBlank(message = "El nombre de usuario es oblogatorio")
    private String userName;

    @NotBlank(message = "El password es obligatorio")
    private String password;

    @NotBlank(message = "El host no puede estar vacío")
    private String host;

    @NotNull(message = "El puerto es obligatorio")
    @Min(value = 1, message = "El puerto debe ser un número positivo válido")
    @Max(value = 65535, message = "El puerto excede el límite máximo permitido")
    private Integer port;

    @NotBlank(message = "El nombre de la base de datos es obligatorio")
    private String databaseName;

}
