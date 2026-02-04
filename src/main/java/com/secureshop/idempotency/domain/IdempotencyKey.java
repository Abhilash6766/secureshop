package com.secureshop.idempotency.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "idempotency_keys",
        uniqueConstraints = @UniqueConstraint(name = "uq_idem", columnNames = {"user_id", "endpoint", "idem_key"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 120)
    private String endpoint;

    @Column(name="idem_key", nullable = false, length = 120)
    private String idemKey;

    @Column(name="request_hash", nullable = false, length = 255)
    private String requestHash;

    // store JSON response (MySQL JSON column)
    @Column(name="response_json", columnDefinition = "json")
    private String responseJson;

    @Column(nullable = false, length = 30)
    private String status; // IN_PROGRESS, COMPLETED, FAILED

    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}