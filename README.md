# SecureShop 🛡️ (Security-Driven E-Commerce Backend)

SecureShop is a security-first e-commerce backend built with **Spring Boot**, **Spring Security**, **JWT**, **MySQL**, **Flyway**, and **Docker Compose**.  
The focus of this project is *production-ready security and reliability patterns*: device-bound sessions, refresh token rotation, RBAC authorization, idempotent workflows, and audit logs with **zero business-logic coupling**.

---

## ✅ What We Built So Far

1) Authentication (JWT + Refresh Tokens + Device Sessions)
We implemented a secure login flow using:
- **Access tokens (JWT)** for normal API authorization
- **Refresh tokens** for renewing access tokens without re-login
- **Device-bound sessions** using an `X-Device-Id` header (each login is tied to a specific device)

Highlights:
- Each login request creates/updates a **session** entry in MySQL
- Tokens are issued based on a valid user + device context
- Refresh token flow supports secure session lifecycle patterns (designed for rotation and revocation)

Example login request:
```bash
curl -s -X POST "http://localhost:8081/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Device-Id: c1-device" \
  -d '{"email":"cust1@secureshop.com","password":"Password123"}'

2) Role-Based Access Control (RBAC)

We built RBAC using these database tables:

users, roles, permissions

user_roles, role_permissions

This supports:

Fine-grained authorization rules (customer vs admin actions)

Clean expansion for future permissions (e.g., ORDER_READ_ALL, REFUND_ANY_ORDER, ADMIN_DASHBOARD)

3) Orders + Refund Workflow

We implemented:

Create order endpoint

Refund endpoint that moves order to REFUNDED state

Example:

curl -s -X POST "$BASE/api/v1/orders" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"totalCents":5555}'


Refund:

curl -s -X POST "$BASE/api/v1/orders/$ORDER/refund" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: refund-order-$ORDER-001"

4) Idempotency Keys (Critical for Payments/Refunds)

We built true idempotency for refund requests so that:

If the client retries the same refund request with the same Idempotency-Key

The server returns the same result safely without re-processing

This prevents:

Duplicate refunds

Double writes

Race conditions in retry scenarios

We store:

idem_key

request hash

status

response JSON

timestamp

And we verified it by calling the same refund endpoint twice with the same key and getting consistent results.

5) Audit Logging via Spring AOP (Zero Business-Logic Coupling)

We added an audit system that logs security-sensitive actions without touching business logic.

✅ Implemented using:

@AuditedAction annotation

AuditAspect with @Around

AuditLog entity + audit_log table

We audit:

AUTH_LOGIN

ORDER_REFUND

(Designed to expand for admin actions, authorization failures, etc.)

Stored fields include:

actor user id

event type

entity type / entity id

status (SUCCESS/FAILURE)

metadata JSON (request path/method, idem key, hashed IP/UA, etc.)

timestamp

Example query to view audits:

docker exec -it secureshop-mysql mysql \
  -u secureshop_user -psecureshop_pass secureshop -e "
SELECT id, actor_user_id, event_type, entity_type, entity_id, status, created_at
FROM audit_log
ORDER BY id DESC
LIMIT 20;
"


✅ We also fixed actor identification so audit rows now show the real actor_user_id.

🧱 Database Tables (Current)

These are the tables currently in the database:

users

roles, permissions, user_roles, role_permissions

sessions

orders, order_events

idempotency_keys

audit_log

flyway_schema_history

🚀 Tech Stack

Java 17

Spring Boot 4.x

Spring Security

JWT

MySQL 8

Flyway migrations

Docker Compose

(Optional: Actuator health/info)

▶️ Run Locally (Docker Compose + Spring Boot)
1) Start MySQL with Docker Compose
docker compose up -d

2) Run the Spring Boot app

From IntelliJ:

Run SecureshopApplication

Or CLI:

./mvnw spring-boot:run


App runs on:

http://localhost:8081

✅ Testing Checklist (What We Verified)

 Login returns JWT access token

 Orders can be created with valid token

 Refund works

 Duplicate refund request with same idempotency key does NOT double-refund

 Idempotency data stored in idempotency_keys

 Audit rows stored in audit_log for login and refunds

 actor_user_id is correctly resolved for authenticated actions
