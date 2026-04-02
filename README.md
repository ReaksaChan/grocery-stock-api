# Multi-Branch Grocery API

Spring Boot 3.2 · PostgreSQL · JWT Authentication

---

## Quick Start

```bash
# 1. Create database
createdb grocery_db

# 2. Configure environment variables
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
export JWT_SECRET=<base64-encoded-256-bit-key>

# 3. Run
./mvnw spring-boot:run
```

---

## Authentication

All protected endpoints require:
```
Authorization: Bearer <access_token>
```

### Roles & Access

| Role       | Can Do |
|------------|--------|
| `OWNER`    | Everything — full system access |
| `ADMIN`    | Products, inventory, orders, reports — no branch delete |
| `CUSTOMER` | Browse products/branches, place & view own orders |

---

## API Endpoints

### Auth  `/api/v1/auth`

| Method | Path        | Body                                    | Auth     |
|--------|-------------|-----------------------------------------|----------|
| POST   | `/register` | `{fullName, email, password, phone}`    | Public   |
| POST   | `/login`    | `{email, password}`                     | Public   |
| POST   | `/refresh`  | `{refreshToken}`                        | Public   |

---

### Products  `/api/v1/products`

| Method | Path              | Params / Body                         | Auth          |
|--------|-------------------|---------------------------------------|---------------|
| GET    | `/`               | `?categoryId&keyword&page&size`       | Public        |
| GET    | `/{id}`           | —                                     | Public        |
| GET    | `/barcode/{code}` | —                                     | Public        |
| POST   | `/`               | `ProductRequest`                      | OWNER / ADMIN |
| POST   | `/with-stock`     | `ProductWithStockRequest`             | OWNER / ADMIN |
| PUT    | `/{id}`           | `ProductRequest`                      | OWNER / ADMIN |
| DELETE | `/{id}`           | —                                     | OWNER         |

---

### Branches  `/api/v1/branches`

| Method | Path          | Params / Body                             | Auth   |
|--------|---------------|-------------------------------------------|--------|
| GET    | `/`           | —                                         | Public |
| GET    | `/{id}`       | —                                         | Public |
| GET    | `/nearby`     | `?lat&lng&radiusKm`                       | Public |
| POST   | `/`           | `BranchRequest`                           | OWNER  |
| PUT    | `/{id}`       | `BranchRequest`                           | OWNER  |
| DELETE | `/{id}`       | —                                         | OWNER  |

---

### Inventory  `/api/v1/inventory`  _(OWNER / ADMIN only)_

| Method | Path                   | Params / Body                                    |
|--------|------------------------|--------------------------------------------------|
| GET    | `/branch/{branchId}`   | `?page&size`                                     |
| POST   | `/`                    | `InventoryRequest`                               |
| POST   | `/add-stock`           | `AddStockRequest` (increment stock)              |
| PATCH  | `/adjust`              | `?branchId&productId&delta&reason`               |
| POST   | `/transfer`            | `?fromBranchId&toBranchId&productId&quantity`    |
| GET    | `/low-stock`           | `?branchId`                                      |
| GET    | `/expiring-soon`       | `?branchId&daysAhead` (default 30)               |
| GET    | `/expired`             | —                                                |

---

### Orders  `/api/v1/orders`

| Method | Path                    | Auth              |
|--------|-------------------------|-------------------|
| POST   | `/`                     | Authenticated     |
| GET    | `/my`                   | Authenticated     |
| GET    | `/{id}`                 | Authenticated     |
| GET    | `/branch/{branchId}`    | OWNER / ADMIN     |
| PATCH  | `/{id}/confirm`         | OWNER / ADMIN     |
| PATCH  | `/{id}/deliver`         | OWNER / ADMIN     |
| PATCH  | `/{id}/cancel`          | Authenticated     |

**Order status flow:**
```
PENDING → CONFIRMED → DELIVERED
    └──────────────────→ CANCELLED
```
Inventory is only decremented on `DELIVERED`.

---

### Reports  `/api/v1/reports`  _(OWNER / ADMIN only)_

| Method | Path              | Params                          |
|--------|-------------------|---------------------------------|
| GET    | `/summary`        | `?from&to&branchId`             |
| GET    | `/daily-revenue`  | `?from&to&branchId`             |
| GET    | `/top-products`   | `?from&to&branchId&limit`       |
| GET    | `/by-branch`      | `?from&to`                      |

Date format: `YYYY-MM-DD`

---

## Scheduled Jobs

| Job | Schedule | Purpose |
|-----|----------|---------|
| Expiry check | Daily 07:00 | Alerts for items expiring in ≤7 days & already expired |
| Low stock check | Every 30 min | Alerts for items at or below `low_stock_threshold` |

---

## Project Structure

```
src/main/java/com/grocery/
├── GroceryApiApplication.java
├── config/          SecurityConfig
├── controller/      AuthController, ProductController, BranchController,
│                    InventoryController, OrderController,
│                    SalesReportController, CategoryController
├── dto/
│   ├── request/     RegisterRequest, LoginRequest, ProductRequest,
│   │                BranchRequest, InventoryRequest, OrderRequest
│   └── response/    AuthResponse
├── entity/          Role, User, Branch, Category, Product,
│                    Inventory, Order, OrderItem, Payment
├── enums/           RoleName, OrderStatus, PaymentMethod, PaymentStatus
├── exception/       ResourceNotFoundException, DuplicateResourceException,
│                    InsufficientStockException, GlobalExceptionHandler
├── repository/      All JPA repositories with custom queries
├── security/        JwtUtil, JwtAuthenticationFilter, UserDetailsServiceImpl
└── service/impl/    AuthService, ProductService, BranchService,
                     InventoryService, OrderService,
                     SalesReportService, CategoryService, AlertScheduler
```
