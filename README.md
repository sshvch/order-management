# Order Management System

Учебный REST API интернет-магазина на **Java 25 + Spring Boot 4**.
Проект показывает полный цикл работы с заказом: регистрация и JWT-авторизация, каталог товаров,
корзина, оформление заказа и смена его статуса администратором.

---

## Стек

| Технология | Зачем |
|---|---|
| Java 25, Spring Boot 4.2 | основа приложения |
| Spring Web MVC | REST API |
| Spring Data JPA + Hibernate | работа с базой |
| PostgreSQL 18 | основная база данных |
| Flyway | миграции схемы |
| Spring Security + JWT (jjwt) | авторизация и роли |
| Bean Validation | проверка входных данных |
| springdoc-openapi | Swagger UI |
| JUnit 5 + Mockito | тесты |
| Docker / Docker Compose | запуск приложения и базы одной командой |
| Bruno | ручная проверка эндпоинтов |

---

## Что умеет проект

- Регистрация и вход, пароль хранится в виде BCrypt-хеша.
- JWT-токен, защита эндпоинтов, роли `USER` и `ADMIN`.
- CRUD категорий и товаров, поиск по названию, пагинация.
- Корзина: добавление, изменение количества, удаление, очистка.
- Оформление заказа из корзины в одной транзакции: создаются заказ и позиции,
  уменьшаются остатки, корзина очищается.
- История заказов пользователя и просмотр всех заказов для админа.
- Смена статуса заказа с проверкой допустимых переходов.
- При отмене заказа товары возвращаются на склад.
- Единый формат ошибок, DTO, `@ControllerAdvice`, миграции Flyway, тесты, Swagger.

---

## Быстрый старт (Docker)

Нужен установленный Docker Desktop.

```bash
docker compose up --build
```

Поднимутся два контейнера: PostgreSQL и приложение.

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

Проверка, что всё работает:

```bash
curl http://localhost:8080/api/products
```

Остановить:

```bash
docker compose down
```

Остановить и удалить данные базы:

```bash
docker compose down -v
```

---

## Запуск локально

1. Поднять только базу:

```bash
docker compose up -d postgres
```

2. Запустить приложение:

```bash
./mvnw spring-boot:run
```

Приложение само применит миграции Flyway и создаст администратора.

### Демо-администратор

| Email | Пароль |
|---|---|
| `admin@orderhub.local` | `admin123` |

Пароль задаётся переменными `APP_ADMIN_EMAIL` и `APP_ADMIN_PASSWORD`.
Это учётка только для локального запуска — перед публикацией проекта поменяйте её.

---

## Переменные окружения

| Переменная | По умолчанию | Описание |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/order_management` | адрес базы |
| `DB_USER` | `orderhub` | пользователь базы |
| `DB_PASSWORD` | `orderhub123` | пароль базы |
| `JWT_SECRET` | dev-значение | ключ подписи, минимум 32 символа |
| `JWT_EXPIRATION` | `86400000` | время жизни токена в мс (24 часа) |
| `APP_ADMIN_EMAIL` | `admin@orderhub.local` | email админа |
| `APP_ADMIN_PASSWORD` | `admin123` | пароль админа |
| `APP_ADMIN_ENABLED` | `true` | создавать ли админа при старте |

Для локального запуска можно скопировать `.env.example` в `.env` и поменять значения.
Файл `.env` добавлен в `.gitignore`.

---

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

`DELETE` — мягкое удаление: товар перестаёт показываться в каталоге, но остаётся в старых заказах.

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

### Статусы заказа

```
CREATED → PAID → SHIPPED → DELIVERED
   ↓        ↓
CANCELLED CANCELLED
```

Отменить заказ можно только до отправки. При отмене остатки товаров возвращаются на склад.

---

## Пример запроса целиком

```bash
BASE=http://localhost:8080

# 1. Регистрация (сразу возвращает токен)
curl -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"ivan@example.com","password":"secret123"}'

# 2. Сохраняем токен в переменную
TOKEN=$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"ivan@example.com","password":"secret123"}' | jq -r .token)

# 3. Смотрим каталог (без токена)
curl "$BASE/api/products?size=5"

# 4. Добавляем товар в корзину
curl -X POST "$BASE/api/cart/items" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'

# 5. Смотрим корзину
curl "$BASE/api/cart" -H "Authorization: Bearer $TOKEN"

# 6. Оформляем заказ
curl -X POST "$BASE/api/orders" -H "Authorization: Bearer $TOKEN"

# 7. История заказов
curl "$BASE/api/orders" -H "Authorization: Bearer $TOKEN"
```

Админский пример:

```bash
ADMIN_TOKEN=$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@orderhub.local","password":"admin123"}' | jq -r .token)

# Создать товар
curl -X POST "$BASE/api/products" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Тестовый товар","description":"Описание","price":1234.50,"stockQuantity":3,"categoryId":1}'

# Сменить статус заказа
curl -X PATCH "$BASE/api/orders/1/status" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"PAID"}'
```

---

## Swagger и Bruno

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

В Swagger есть кнопка **Authorize**. Вставьте туда токен, чтобы вызывать защищённые эндпоинты.

Коллекция Bruno лежит в папке [`bruno/order-management`](bruno/order-management).
Откройте её в Bruno, выполните `Login`, токен сохранится в переменную `token`,
дальше можно вызывать запросы корзины и заказов.

---

## Тесты

```bash
./mvnw test
```

Тесты используют H2 в режиме PostgreSQL, поэтому Docker для них не нужен.

Покрыто:

- регистрация пользователя и проверка дубликата email;
- создание, поиск и «удаление» товара;
- добавление в корзину и проверка остатков;
- расчёт суммы заказа, уменьшение остатков и очистка корзины;
- запрет заказа при нехватке товара;
- возврат товаров на склад при отмене заказа;
- загрузка Spring-контекста.

---

## Структура проекта

Проект разделён на три классических слоя: **controller → service → repository**.

```
src/main/java/ru/orderhub/order_management
├── OrderManagementApplication.java
├── controller/            # REST-контроллеры: принимают запрос и отдают ответ
│   ├── AuthController.java
│   ├── ProductController.java
│   ├── CategoryController.java
│   ├── CartController.java
│   └── OrderController.java
├── service/               # бизнес-логика и транзакции
│   ├── AuthService.java
│   ├── ProductService.java
│   ├── CategoryService.java
│   ├── CartService.java
│   └── OrderService.java
├── repository/            # Spring Data JPA: запросы к базе
│   ├── UserRepository.java
│   ├── ProductRepository.java
│   ├── CategoryRepository.java
│   ├── CartRepository.java
│   ├── CartItemRepository.java
│   └── OrderRepository.java
├── entity/                # сущности БД
├── dto/                   # request/response объекты
├── exception/             # свои исключения и @RestControllerAdvice
├── security/              # JWT: генерация, фильтр, UserDetailsService
└── config/                # Security, Swagger, создание админа при старте
```

Как проходит запрос:

1. **Controller** принимает HTTP-запрос, проверяет `@Valid` и вызывает сервис.
   В контроллере нет работы с базой.
2. **Service** содержит бизнес-логику: проверки, расчёты, `@Transactional`.
3. **Repository** общается с базой через Spring Data JPA.
4. **Entity** — таблицы базы, **DTO** — то, что уходит клиенту и приходит от него.

Контроллеры не зависят от репозиториев напрямую, а репозитории не знают про HTTP.
Так проект проще тестировать и менять.

---

## Как работают транзакции при оформлении заказа

Метод `OrderService.createOrder` помечен `@Transactional`. Внутри одной транзакции:

1. проверяется, что корзина не пустая;
2. проверяется остаток каждого товара;
3. создаётся заказ и его позиции;
4. уменьшается `stockQuantity` у товаров;
5. корзина очищается.

Если на любом шаге выбросить исключение (например, товара не хватило), Spring откатит
всю транзакцию: заказ не сохранится, остатки не изменятся, корзина останется как была.
Это поведение проверено вручную и покрыто тестами.

---

## Миграции базы

Файлы лежат в `src/main/resources/db/migration`:

- `V1__init_schema.sql` — таблицы и индексы;
- `V2__seed_data.sql` — демо-категории и товары.

Hibernate настроен на `ddl-auto=validate`: он не меняет схему, а только проверяет,
что Entity совпадают с таблицами. Все изменения схемы делаются новой миграцией Flyway.

---

## Частые проблемы

**`Schema validation: missing table [...]` или Flyway просит baseline**

В базе остались таблицы от старой версии проекта, где Flyway ещё не использовался.
Для локальной разработки проще пересоздать базу:

```bash
docker compose down -v
docker compose up -d postgres
```

**Порт 5432 или 8080 занят**

Остановите старый контейнер или процесс, либо поменяйте порт в `docker-compose.yml` / `application.properties`.

**401 на защищённом эндпоинте**

Проверьте, что заголовок выглядит так: `Authorization: Bearer <token>` и токен не просрочен.

---

## Что можно добавить дальше

- Оплату и интеграцию с платёжным сервисом.
- Отзывы и рейтинг товаров.
- Обновление пароля и восстановление доступа.
- Интеграционные тесты через Testcontainers.
- CI на GitHub Actions.

---

## Безопасность

- Пароли хранятся только в виде BCrypt-хешей.
- Секреты читаются из переменных окружения, `.env` не коммитится.
- Значения по умолчанию (`orderhub123`, `admin123`, dev-секрет JWT) предназначены только
  для локального запуска. Перед публикацией проекта задайте свои.
