package com.secureshop.audit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditedAction {
    String action();                 // e.g. ORDER_REFUND, AUTH_LOGIN, ADMIN_ACTION
    String entityType() default "";  // e.g. order, user
    String entityIdParam() default "";// method param name (e.g. "orderId")
}
