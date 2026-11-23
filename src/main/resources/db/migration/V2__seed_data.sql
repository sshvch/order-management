-- Демо-данные, чтобы после запуска было что смотреть в Swagger.
-- Это не секреты: пароль администратора создаётся отдельно (см. AdminInitializer).

insert into categories (name, description) values
    ('Электроника', 'Телефоны, ноутбуки и аксессуары'),
    ('Книги', 'Книги и учебники'),
    ('Дом и кухня', 'Посуда и товары для дома');

insert into products (name, description, price, stock_quantity, active, category_id, created_at)
select 'Ноутбук', 'Ноутбук для учёбы и работы', 79990.00, 10, true, c.id, now()
from categories c where c.name = 'Электроника';

insert into products (name, description, price, stock_quantity, active, category_id, created_at)
select 'Смартфон', 'Смартфон с хорошей камерой', 49990.00, 25, true, c.id, now()
from categories c where c.name = 'Электроника';

insert into products (name, description, price, stock_quantity, active, category_id, created_at)
select 'Наушники', 'Беспроводные наушники', 8990.00, 40, true, c.id, now()
from categories c where c.name = 'Электроника';

insert into products (name, description, price, stock_quantity, active, category_id, created_at)
select 'Java. Полное руководство', 'Книга для изучения Java', 3200.00, 15, true, c.id, now()
from categories c where c.name = 'Книги';

insert into products (name, description, price, stock_quantity, active, category_id, created_at)
select 'Кофейник', 'Стеклянный кофейник на 1 литр', 1500.00, 30, true, c.id, now()
from categories c where c.name = 'Дом и кухня';
