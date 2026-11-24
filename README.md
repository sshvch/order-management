# Order Management System

REST API интернет-магазина на Java 25 + Spring Boot 4: регистрация и JWT, каталог товаров,
корзина, оформление заказа и смена статуса.

## Стек

- Java 25, Spring Boot 4
- Spring Web MVC, Spring Data JPA, Hibernate
- PostgreSQL 18, Flyway
- Spring Security + JWT
- Bean Validation, springdoc-openapi
- JUnit 5, Mockito, H2 (тесты)
- Docker Compose, GitHub Actions

## Запуск

Через Docker:

```bash
docker compose up --build
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

Локально (база в Docker, приложение через Maven):

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

При старте Flyway применяет миграции и создаётся администратор.

| Email | Пароль |
|---|---|
| `admin@orderhub.local` | `admin123` |

Пароль меняется через `APP_ADMIN_EMAIL` и `APP_ADMIN_PASSWORD`.

## Переменные окружения

| Переменная | По умолчанию |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/order_management` |
| `DB_USER` | `orderhub` |
| `DB_PASSWORD` | `orderhub123` |
| `JWT_SECRET` | dev-значение, минимум 32 символа |
| `JWT_EXPIRATION` | `86400000` |
| `APP_ADMIN_EMAIL` | `admin@orderhub.local` |
| `APP_ADMIN_PASSWORD` | `admin123` |
| `APP_ADMIN_ENABLED` | `true` |

## Эндпоинты

### Авторизация

| Метод | URL | Доступ |
|---|---|---|
| POST | `/api/auth/register` | все |
| POST | `/api/auth/login` | все |
| GET | `/api/auth/me` | авторизованные |

### Товары

| Метод | URL | Доступ |
|---|---|---|
| GET | `/api/products?search=&page=0&size=10` | все |
| GET | `/api/products/{id}` | все |
| POST | `/api/products` | ADMIN |
| PUT | `/api/products/{id}` | ADMIN |
| DELETE | `/api/products/{id}` | ADMIN |

`DELETE` скрывает товар из каталога, но оставляет его в старых заказах.

### Категории

| Метод | URL | Доступ |
|---|---|---|
| GET | `/api/categories` | все |
| POST | `/api/categories` | ADMIN |
| PUT | `/api/categories/{id}` | ADMIN |
| DELETE | `/api/categories/{id}` | ADMIN |

### Корзина

| Метод | URL | Доступ |
|---|---|---|
| GET | `/api/cart` | авторизованные |
| POST | `/api/cart/items` | авторизованные |
| PUT | `/api/cart/items/{itemId}` | авторизованные |
| DELETE | `/api/cart/items/{itemId}` | авторизованные |
| DELETE | `/api/cart` | авторизованные |

### Заказы

| Метод | URL | Доступ |
|---|---|---|
| POST | `/api/orders` | авторизованные |
| GET | `/api/orders?page=0&size=10` | авторизованные |
| GET | `/api/orders/{id}` | владелец или ADMIN |
| PATCH | `/api/orders/{id}/status` | ADMIN |
| GET | `/api/orders/all?page=0&size=10` | ADMIN |

Статусы: `CREATED → PAID → SHIPPED → DELIVERED`. Отменить можно из `CREATED` и `PAID`,
при отмене остатки возвращаются на склад.

## Пример

```bash
BASE=http://localhost:8080

TOKEN=$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"ivan@example.com","password":"secret123"}' | jq -r .token)

curl -X POST "$BASE/api/cart/items" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'

curl -X POST "$BASE/api/orders" -H "Authorization: Bearer $TOKEN"
```

Коллекция Bruno: `bruno/order-management`.

## Тесты

```bash
./mvnw test
```

20 тестов: unit-тесты сервисов на Mockito и интеграционные MockMvc-тесты
на регистрацию, JWT, 401/403, оформление и отмену заказа.

## Структура

```
src/main/java/ru/orderhub/order_management
├── controller/   REST-контроллеры
├── service/      бизнес-логика и транзакции
├── repository/   Spring Data JPA
├── entity/       сущности БД
├── dto/          request/response
├── exception/    исключения и общий обработчик
├── security/     JWT и UserDetailsService
└── config/       Security, Swagger, создание админа
```

Схема БД лежит в `src/main/resources/db/migration`, Hibernate работает с `ddl-auto=validate`.
