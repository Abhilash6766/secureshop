package com.secureshop.idempotency.service;

import com.secureshop.idempotency.domain.IdempotencyKey;
import com.secureshop.idempotency.repo.IdempotencyKeyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository repo;
    private final ObjectMapper mapper;

    public IdempotencyService(IdempotencyKeyRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public record IdemResult<T>(T body, boolean cached) {}

    @Transactional
    public <T> IdemResult<T> execute(
            Long userId, String endpoint, String idemKey, String requestHash, Supplier<T> action) {

        int inserted = repo.tryInsertIgnore(userId, endpoint, idemKey, requestHash);

        // If not inserted => row exists => handle duplicate path
        if (inserted == 0) {
            IdempotencyKey existing = repo.findByUserIdAndEndpointAndIdemKey(userId, endpoint, idemKey)
                    .orElseThrow();

            if (!existing.getRequestHash().equals(requestHash)) {
                throw new IllegalArgumentException("Idempotency-Key reused with different request payload");
            }

            if ("COMPLETED".equals(existing.getStatus())) {
                @SuppressWarnings("unchecked")
                T cachedBody = (T) read(existing.getResponseJson());
                return new IdemResult<>(cachedBody, true);
            }

            throw new IllegalArgumentException("Request already in progress. Retry with same Idempotency-Key.");
        }

        // First request => execute
        T result = action.get();

        IdempotencyKey saved = repo.findByUserIdAndEndpointAndIdemKey(userId, endpoint, idemKey)
                .orElseThrow();

        saved.setStatus("COMPLETED");
        saved.setResponseJson(write(result));
        repo.save(saved);

        return new IdemResult<>(result, false);
    }

    private Object read(String json) {
        try { return mapper.readValue(json, Object.class); }
        catch (Exception e) { throw new IllegalStateException("Failed to deserialize cached response", e); }
    }

    private String write(Object obj) {
        try { return mapper.writeValueAsString(obj); }
        catch (Exception e) { throw new IllegalStateException("Failed to serialize response", e); }
    }
}
