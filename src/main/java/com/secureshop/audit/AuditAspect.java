package com.secureshop.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secureshop.audit.domain.AuditLog;
import com.secureshop.audit.service.AuditLogService;
import com.secureshop.user.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Aspect
@Component
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    public AuditAspect(AuditLogService auditLogService, ObjectMapper objectMapper, UserRepository userRepository) {
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
    }

    private Long resolveActorUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .map(u -> u.getId())
                .orElse(null);
    }

    @Around("@annotation(com.secureshop.audit.AuditedAction)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {

        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        AuditedAction ann = method.getAnnotation(AuditedAction.class);

        HttpServletRequest req = currentRequest();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("method", req != null ? req.getMethod() : null);
        meta.put("path", req != null ? req.getRequestURI() : null);
        meta.put("idempotencyKey", req != null ? req.getHeader("Idempotency-Key") : null);

        // Avoid logging secrets like password:
        meta.put("principal", auth != null ? auth.getName() : "anonymous");

        if (req != null) {
            meta.put("ipHash", sha256(req.getRemoteAddr()));
            meta.put("uaHash", sha256(req.getHeader("User-Agent")));
        }

        String entityId = resolveEntityId(sig, pjp.getArgs(), ann.entityIdParam());

        try {
            Object result = pjp.proceed();

            writeLog(ann, auth, entityId, "SUCCESS", meta);
            return result;

        } catch (Throwable ex) {
            meta.put("errorType", ex.getClass().getSimpleName());
            meta.put("errorMessage", safeMsg(ex.getMessage()));

            writeLog(ann, auth, entityId, "FAILURE", meta);
            throw ex;
        }
    }

    private void writeLog(AuditedAction ann, Authentication auth, String entityId, String status, Map<String,Object> meta) {
        try {
            AuditLog log = new AuditLog();
            log.setActorUserId(resolveActorUserId(auth)); // if you have userId in JWT claims, we can wire it later
            log.setEvettype(ann.action());
            log.setEntityType(blankToNull(ann.entityType()));
            log.setEntityId(blankToNull(entityId));
            log.setStatus(status);
            log.setMetadataJson(objectMapper.writeValueAsString(meta));
            auditLogService.write(log);
        } catch (Exception ignored) {
            // audit should never break the business request
        }
    }

    private String resolveEntityId(MethodSignature sig, Object[] args, String entityIdParam) {
        if (entityIdParam == null || entityIdParam.isBlank()) return null;
        String[] names = sig.getParameterNames();
        if (names == null) return null;
        for (int i = 0; i < names.length; i++) {
            if (entityIdParam.equals(names[i]) && i < args.length && args[i] != null) {
                return String.valueOf(args[i]);
            }
        }
        return null;
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        if (ra instanceof ServletRequestAttributes sra) return sra.getRequest();
        return null;
    }

    private String sha256(String v) {
        if (v == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(v.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String safeMsg(String msg) {
        if (msg == null) return null;
        return msg.length() > 300 ? msg.substring(0, 300) : msg;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
