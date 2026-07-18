package org.example.sincronizador2.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.sincronizador2.enums.SyncStatus;
import org.example.sincronizador2.enums.SyncStrategy;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sync_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SyncTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private DatabaseConfigEntity config;

    @Column(nullable = false)
    private String tableName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SyncStrategy strategy;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SyncStatus status;

    @Column
    private Long totalRows;

    @Column
    private Long processedRows;

    @Column
    private String minioPath;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime finishedAt;

}





