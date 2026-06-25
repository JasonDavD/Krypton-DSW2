-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: localhost    Database: catalogo
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `catalogo`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `catalogo` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `catalogo`;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'1','catalogo schema','SQL','V1__catalogo_schema.sql',539045603,'krypton','2026-06-22 16:51:12',598,1),(2,'2','seed products','SQL','V2__seed_products.sql',1462519518,'krypton','2026-06-22 16:51:12',6,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_image`
--

DROP TABLE IF EXISTS `product_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `path` varchar(500) NOT NULL,
  `display_order` smallint NOT NULL DEFAULT '0',
  `is_cover` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `cover_key` bigint GENERATED ALWAYS AS (if((`is_cover` = 1),`product_id`,NULL)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_product_image_cover` (`cover_key`),
  KEY `idx_product_image_product` (`product_id`),
  CONSTRAINT `fk_product_image_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_image`
--

LOCK TABLES `product_image` WRITE;
/*!40000 ALTER TABLE `product_image` DISABLE KEYS */;
INSERT INTO `product_image` (`id`, `product_id`, `path`, `display_order`, `is_cover`, `created_at`) VALUES (3,2,'ebf7c825-10c6-4a3e-a718-fa33285c3be0.png',0,0,'2026-06-25 00:10:11.859805'),(4,2,'933198cf-f87b-4737-a4e1-ee99e8b2f470.png',1,1,'2026-06-25 00:10:11.862084'),(5,2,'c4c193c4-07c2-483e-ab9f-908f8b41e862.png',2,0,'2026-06-25 00:10:11.863960'),(6,2,'d97a8e29-9660-48e0-af1a-1260f93a6bae.png',3,0,'2026-06-25 00:10:11.865950'),(7,2,'ff0e4339-50f9-470d-afc5-9515fee0f009.png',4,0,'2026-06-25 00:10:11.868511');
/*!40000 ALTER TABLE `product_image` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sku` varchar(60) NOT NULL,
  `name` varchar(150) NOT NULL,
  `description` varchar(2000) DEFAULT NULL,
  `price` decimal(12,2) NOT NULL,
  `stock` int NOT NULL DEFAULT '0',
  `image_url` varchar(500) DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `category_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `sku` (`sku`),
  KEY `idx_products_category` (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `products`
--

LOCK TABLES `products` WRITE;
/*!40000 ALTER TABLE `products` DISABLE KEYS */;
INSERT INTO `products` VALUES (1,'KR-LAP-001','Laptop Krypton Vortex 15','Intel Core i7, 16GB RAM, RTX 4060, 15.6\" 144Hz',4299.00,0,'http://localhost:8094/api/uploads/images/c05fc7f1-fb08-43ce-95ec-5b7d6c8b5edf.jpg',1,1),(2,'KR-LAP-002','Laptop Krypton Air 14','Intel Core i5, 8GB RAM, SSD 512GB, 14\" liviana',2799.00,16,'http://localhost:8094/api/uploads/images/933198cf-f87b-4737-a4e1-ee99e8b2f470.png',1,1),(3,'KR-AUD-001','Audífonos Krypton Pulse X','Inalámbricos, cancelación de ruido, 30h batería',349.90,33,NULL,1,2),(4,'KR-AUD-002','Parlante Krypton Boom','Bluetooth, resistente al agua IPX7, 20W',199.90,28,NULL,1,2),(5,'KR-CMP-001','Tarjeta de video Krypton RTX 4070','GDDR6X 12GB, ray tracing, DLSS 3',2599.00,8,NULL,1,3),(6,'KR-CMP-002','Memoria RAM Krypton Fury 32GB','DDR5 6000MHz, kit 2x16GB, RGB',559.00,40,NULL,1,3),(7,'KR-CMP-003','SSD Krypton NVMe 1TB','Gen4, 7000MB/s de lectura',389.00,50,NULL,1,3),(8,'KR-PER-001','Teclado Mecanico Krypton TKL','Switches rojos, hot-swap, retroiluminado',259.00,30,NULL,1,4),(9,'KR-PER-002','Mouse Krypton Glide Pro','Inalambrico, 26000 DPI, 60g',179.00,45,NULL,1,4),(10,'KR-MON-001','Monitor Krypton View 27','QHD 165Hz, IPS, 1ms',1099.00,15,NULL,1,5),(12,'1231234126456456454564','Audio Prueba','dsadasdsadas',12354.00,12,'http://localhost:8094/api/uploads/images/cb6b33ab-0b79-4cba-a6e7-a5fe61d8c589.jpg',1,2);
/*!40000 ALTER TABLE `products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'catalogo'
--

--
-- Dumping routines for database 'catalogo'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-25  0:12:10
