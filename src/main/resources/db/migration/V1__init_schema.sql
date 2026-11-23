-- Начальная схема базы данных.
-- Таблицы соответствуют Entity-классам в пакетах user, product, cart, order.

create table users (
    id         bigserial primary key,
    email      varchar(255) not null unique,
    password   varchar(255) not null,
    role       varchar(20)  not null,
    created_at timestamp    not null
);

create table categories (
    id          bigserial primary key,
    name        varchar(100) not null unique,
    description varchar(500)
);

create table products (
    id             bigserial primary key,
    name           varchar(150)   not null,
    description    varchar(1000),
    price          numeric(10, 2) not null,
    stock_quantity integer        not null,
    active         boolean        not null default true,
    category_id    bigint references categories (id),
    created_at     timestamp      not null
);

create table carts (
    id         bigserial primary key,
    user_id    bigint    not null unique references users (id) on delete cascade,
    created_at timestamp not null
);

create table cart_items (
    id         bigserial primary key,
    cart_id    bigint  not null references carts (id) on delete cascade,
    product_id bigint  not null references products (id),
    quantity   integer not null,
    constraint uq_cart_item unique (cart_id, product_id)
);

create table orders (
    id           bigserial primary key,
    user_id      bigint         not null references users (id),
    status       varchar(20)    not null,
    total_amount numeric(12, 2) not null,
    created_at   timestamp      not null
);

create table order_items (
    id           bigserial primary key,
    order_id     bigint         not null references orders (id) on delete cascade,
    product_id   bigint         not null references products (id),
    product_name varchar(150)   not null,
    unit_price   numeric(10, 2) not null,
    quantity     integer        not null
);

create index idx_products_name on products (name);
create index idx_orders_user on orders (user_id);
