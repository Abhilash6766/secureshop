package com.secureshop.auth.repo;

import com.secureshop.auth.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {
    Optional<Session> findByUserIdAndDeviceId(Long userId, String deviceId);
}
