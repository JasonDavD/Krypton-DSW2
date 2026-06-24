-- V8: Category se desacopla a categorias-soap-service (SOAP). El monolito deja de ser dueño:
-- products.category_id pasa a ser un id SUELTO (sin FK, igual que en catalogo-service) y la
-- tabla categories se elimina del monolito. Las categorías ahora viven en el micro SOAP.
ALTER TABLE products DROP FOREIGN KEY fk_products_category;
DROP TABLE categories;
