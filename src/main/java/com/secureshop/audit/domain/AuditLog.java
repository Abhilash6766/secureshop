package com.secureshop.audit.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "audit_log")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="actor_user_id")
    private Long actorUserId;

    @Column(name = "event_type", nullable=false)
    private String evettype;

    @Column(name="entity_type")
    private String entityType;

    @Column(name="entity_id")
    private String entityId;

    @Column(name="status", nullable = false)
    private String status;

    @Column(name="metadata_json", columnDefinition = "JSON")
    private String metadataJson;

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    // getters/setters (or Lombok if you use it)
    // ... generate in IDE
}
