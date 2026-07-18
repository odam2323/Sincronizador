package org.example.sincronizador2.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "database_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column
    private String dbType;
    @Column
    private String userName;
    @Column
    private String password;
    @Column
    private String host;
    @Column
    private Integer port;
    @Column
    private String databaseName;

}


