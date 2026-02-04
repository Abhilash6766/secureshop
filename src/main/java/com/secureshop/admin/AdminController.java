package com.secureshop.admin;

import com.secureshop.audit.AuditedAction;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @AuditedAction(action = "ADMIN_ACTION")
    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public Map<String, Object> auditRead() {
        return Map.of("ok", true, "message", "You have AUDIT_READ permission");
    }

    @AuditedAction(action = "ADMIN_ACTION")
    @PostMapping("/refund")
    @PreAuthorize("hasAuthority('ORDER_REFUND')")
    public Map<String, Object> refund(@RequestParam String orderId) {
        return Map.of("ok", true, "refundedOrderId", orderId);
    }
}