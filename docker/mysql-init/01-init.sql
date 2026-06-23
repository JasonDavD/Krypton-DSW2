-- Se ejecuta UNA vez, al inicializar el volumen de MySQL (como root).
-- Crea las dos bases (monolito + catalogo) y un usuario con permisos sobre ambas.
CREATE DATABASE IF NOT EXISTS krypton  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS catalogo CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'krypton'@'%' IDENTIFIED BY 'krypton';
GRANT ALL PRIVILEGES ON krypton.*  TO 'krypton'@'%';
GRANT ALL PRIVILEGES ON catalogo.* TO 'krypton'@'%';
FLUSH PRIVILEGES;
