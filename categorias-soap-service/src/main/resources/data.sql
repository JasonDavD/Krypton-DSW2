-- Seed: copia de las categorías actuales del monolito (mismos ids, porque products.category_id
-- las referencia). Idempotente: si ya existen, refresca nombre/descripción.
INSERT INTO categories (id, name, description) VALUES
  (1, 'Laptops',     'Notebooks y ultrabooks'),
  (2, 'Audio',       'Audífonos, parlantes y micrófonos'),
  (3, 'Componentes', 'GPU, CPU, RAM y almacenamiento'),
  (4, 'Periféricos', 'Teclados, mouse y accesorios'),
  (5, 'Monitores',   'Monitores y pantallas')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);
