-- V1: esquema del microservicio de Catálogo — MySQL 8 / InnoDB
-- Solo las tablas que pertenecen a este servicio: products y product_image.
--
-- ADAPTACIÓN MICROSERVICIOS: la tabla `categories` vive en OTRO servicio
-- (reservado para SOAP). Por eso NO se crea la FK fk_products_category.
-- `category_id` queda como columna plana (sin FK), conservando el índice.

CREATE TABLE products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku         VARCHAR(60)   NOT NULL UNIQUE,
    name        VARCHAR(150)  NOT NULL,
    description VARCHAR(2000),
    price       DECIMAL(12,2) NOT NULL,
    stock       INT           NOT NULL DEFAULT 0,   -- valor cacheado del stock actual
    image_url   VARCHAR(500),
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    category_id BIGINT        NOT NULL              -- sin FK: la categoría vive en otro servicio
);

CREATE TABLE product_image (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id    BIGINT       NOT NULL,
    path          VARCHAR(500) NOT NULL,
    display_order SMALLINT     NOT NULL DEFAULT 0,
    is_cover      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    -- Columna generada: = product_id solo cuando is_cover; NULL en caso contrario.
    -- MySQL permite múltiples NULL en un índice UNIQUE → garantiza máximo UNA
    -- portada por producto.
    cover_key     BIGINT AS (IF(is_cover = 1, product_id, NULL)) STORED,
    CONSTRAINT fk_product_image_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT uq_product_image_cover   UNIQUE (cover_key)
);

CREATE INDEX idx_products_category     ON products (category_id);
CREATE INDEX idx_product_image_product ON product_image (product_id);
