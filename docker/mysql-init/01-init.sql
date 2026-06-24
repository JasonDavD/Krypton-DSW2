-- Se ejecuta UNA vez, al inicializar el volumen de MySQL (como root).
-- Crea las bases de los servicios que comparten este MySQL y un usuario con permisos:
--   krypton    -> monolito        catalogo  -> catalogo-service
--   categorias -> categorias-soap-service (SOAP)
CREATE DATABASE IF NOT EXISTS krypton    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS catalogo   CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS categorias CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'krypton'@'%' IDENTIFIED BY 'krypton';
GRANT ALL PRIVILEGES ON krypton.*    TO 'krypton'@'%';
GRANT ALL PRIVILEGES ON catalogo.*   TO 'krypton'@'%';
GRANT ALL PRIVILEGES ON categorias.* TO 'krypton'@'%';
FLUSH PRIVILEGES;
