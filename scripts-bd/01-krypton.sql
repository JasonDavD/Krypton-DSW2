-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: localhost    Database: krypton
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
-- Current Database: `krypton`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `krypton` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `krypton`;

--
-- Table structure for table `cart`
--

DROP TABLE IF EXISTS `cart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  CONSTRAINT `fk_cart_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cart`
--

LOCK TABLES `cart` WRITE;
/*!40000 ALTER TABLE `cart` DISABLE KEYS */;
INSERT INTO `cart` VALUES (1,1,'2026-06-22 23:05:42.102068','2026-06-24 16:17:39.867784'),(2,2,'2026-06-22 23:07:25.335017','2026-06-23 17:53:57.070450'),(3,3,'2026-06-23 17:48:48.293633','2026-06-23 17:49:43.154262'),(5,9,'2026-06-24 05:25:00.045967','2026-06-24 17:45:09.348903'),(7,11,'2026-06-24 06:41:26.725145','2026-06-24 06:41:26.751358'),(8,12,'2026-06-24 06:42:06.935158','2026-06-24 06:42:06.943230');
/*!40000 ALTER TABLE `cart` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cart_item`
--

DROP TABLE IF EXISTS `cart_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cart_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `quantity` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_cart_item_cart_product` (`cart_id`,`product_id`),
  KEY `fk_cart_item_product` (`product_id`),
  KEY `idx_cart_item_cart` (`cart_id`),
  CONSTRAINT `fk_cart_item_cart` FOREIGN KEY (`cart_id`) REFERENCES `cart` (`id`),
  CONSTRAINT `fk_cart_item_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cart_item`
--

LOCK TABLES `cart_item` WRITE;
/*!40000 ALTER TABLE `cart_item` DISABLE KEYS */;
INSERT INTO `cart_item` VALUES (9,7,3,1),(10,8,1,1);
/*!40000 ALTER TABLE `cart_item` ENABLE KEYS */;
UNLOCK TABLES;

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
INSERT INTO `flyway_schema_history` VALUES (1,'1','create initial schema','SQL','V1__create_initial_schema.sql',635421880,'krypton','2026-06-22 16:51:13',3144,1),(2,'2','add user active','SQL','V2__add_user_active.sql',260658605,'krypton','2026-06-22 16:51:13',287,1),(3,'3','seed admin','SQL','V3__seed_admin.sql',1220289334,'krypton','2026-06-22 16:51:13',5,1),(4,'4','add cart item unique','SQL','V4__add_cart_item_unique.sql',128653874,'krypton','2026-06-22 16:51:14',84,1),(5,'5','add product image','SQL','V5__add_product_image.sql',-100061072,'krypton','2026-06-22 16:51:14',401,1),(6,'6','seed demo products','SQL','V6__seed_demo_products.sql',219678314,'krypton','2026-06-22 16:51:14',8,1),(7,'7','add order billing','SQL','V7__add_order_billing.sql',748282793,'krypton','2026-06-22 16:51:14',130,1),(8,'9','decouple category to soap','SQL','V9__decouple_category_to_soap.sql',-692750997,'krypton','2026-06-24 19:53:57',1799,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_items`
--

DROP TABLE IF EXISTS `order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `quantity` int NOT NULL,
  `unit_price` decimal(12,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_order_items_product` (`product_id`),
  KEY `idx_order_items_order` (`order_id`),
  CONSTRAINT `fk_order_items_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_order_items_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_items`
--

LOCK TABLES `order_items` WRITE;
/*!40000 ALTER TABLE `order_items` DISABLE KEYS */;
INSERT INTO `order_items` VALUES (1,1,1,1,4299.00),(2,2,1,1,4299.00),(3,3,2,1,2799.00),(4,4,1,1,4299.00),(5,5,3,1,349.90);
/*!40000 ALTER TABLE `order_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `order_date` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `status` varchar(20) NOT NULL,
  `document_type` varchar(10) NOT NULL DEFAULT 'BOLETA',
  `customer_name` varchar(150) NOT NULL DEFAULT '',
  `customer_doc` varchar(11) NOT NULL DEFAULT '',
  `subtotal` decimal(12,2) NOT NULL DEFAULT '0.00',
  `shipping_cost` decimal(12,2) NOT NULL DEFAULT '0.00',
  `igv` decimal(12,2) NOT NULL DEFAULT '0.00',
  `total` decimal(12,2) NOT NULL,
  `payment_method` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_orders_user` (`user_id`),
  CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
INSERT INTO `orders` VALUES (1,2,'2026-06-23 17:18:50.156873','PENDIENTE','BOLETA','Prueba','45567899',4299.00,0.00,655.78,4299.00,NULL),(2,3,'2026-06-23 17:48:48.457921','CONFIRMADA','BOLETA','Juan Test','12345678',4299.00,0.00,655.78,4299.00,'DEBIT_CARD'),(3,3,'2026-06-23 17:49:43.124089','CONFIRMADA','BOLETA','Juan Test','12345678',2799.00,0.00,426.97,2799.00,'CREDIT_CARD'),(4,2,'2026-06-23 17:53:57.049701','ENTREGADO','BOLETA','Jose','12455689',4299.00,0.00,655.78,4299.00,'DEBIT_CARD'),(5,9,'2026-06-24 05:25:11.499132','CONFIRMADA','BOLETA','Joel Curi','12345645',349.90,0.00,53.37,349.90,NULL);
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_image`
--

LOCK TABLES `product_image` WRITE;
/*!40000 ALTER TABLE `product_image` DISABLE KEYS */;
INSERT INTO `product_image` (`id`, `product_id`, `path`, `display_order`, `is_cover`, `created_at`) VALUES (1,11,'43400d3b-3e97-4028-ad92-5c6c8389e639.webp',0,1,'2026-06-23 18:35:39.923914'),(2,11,'8a778fac-a1ba-4523-b6df-fa7d39518d87.webp',1,0,'2026-06-23 18:35:42.982855'),(3,11,'e5a77a93-a052-4a05-9119-e732f060ee9d.webp',2,0,'2026-06-23 18:35:45.308580'),(4,1,'c05fc7f1-fb08-43ce-95ec-5b7d6c8b5edf.jpg',0,1,'2026-06-24 22:03:44.334857'),(5,1,'59686e3f-f195-4bf8-803f-7a42cd47983c.png',1,0,'2026-06-24 22:06:42.444529'),(6,1,'83e1f1c4-4a3c-48e6-b1df-a534ba3045e0.png',2,0,'2026-06-24 22:06:42.687673'),(7,1,'c05c0373-ffbf-4002-ae69-d6662c7873d2.png',3,0,'2026-06-24 22:06:42.852316'),(8,1,'b7cbf13f-f6af-49bb-8cfa-21582ef53c4d.png',4,0,'2026-06-24 22:15:39.123645'),(9,2,'ebf7c825-10c6-4a3e-a718-fa33285c3be0.png',0,0,'2026-06-24 23:04:11.956326'),(10,2,'933198cf-f87b-4737-a4e1-ee99e8b2f470.png',1,1,'2026-06-24 23:25:51.193899'),(11,12,'cb6b33ab-0b79-4cba-a6e7-a5fe61d8c589.jpg',0,1,'2026-06-24 23:27:57.835254'),(12,12,'ec1a85bb-79df-4371-b038-65fef8d8c486.jpg',1,0,'2026-06-24 23:27:58.310080'),(13,2,'c4c193c4-07c2-483e-ab9f-908f8b41e862.png',2,0,'2026-06-25 00:01:06.415512'),(14,2,'d97a8e29-9660-48e0-af1a-1260f93a6bae.png',3,0,'2026-06-25 00:06:18.905125'),(15,2,'ff0e4339-50f9-470d-afc5-9515fee0f009.png',4,0,'2026-06-25 00:10:11.826035');
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
INSERT INTO `products` VALUES (1,'KR-LAP-001','Laptop Krypton Vortex 15','Intel Core i7, 16GB RAM, RTX 4060, 15.6\" 144Hz',4299.00,0,'http://localhost:8094/api/uploads/images/c05fc7f1-fb08-43ce-95ec-5b7d6c8b5edf.jpg',1,1),(2,'KR-LAP-002','Laptop Krypton Air 14','Intel Core i5, 8GB RAM, SSD 512GB, 14\" liviana',2799.00,16,'http://localhost:8094/api/uploads/images/933198cf-f87b-4737-a4e1-ee99e8b2f470.png',1,1),(3,'KR-AUD-001','Audífonos Krypton Pulse X','Inalámbricos, cancelación de ruido, 30h batería',349.90,33,NULL,1,2),(4,'KR-AUD-002','Parlante Krypton Boom','Bluetooth, resistente al agua IPX7, 20W',199.90,28,NULL,1,2),(5,'KR-CMP-001','Tarjeta de video Krypton RTX 4070','GDDR6X 12GB, ray tracing, DLSS 3',2599.00,8,NULL,1,3),(6,'KR-CMP-002','Memoria RAM Krypton Fury 32GB','DDR5 6000MHz, kit 2x16GB, RGB',559.00,40,NULL,1,3),(7,'KR-CMP-003','SSD Krypton NVMe 1TB','Gen4, 7000MB/s de lectura',389.00,50,NULL,1,3),(8,'KR-PER-001','Teclado Mecánico Krypton TKL','Switches rojos, hot-swap, retroiluminado',259.00,30,NULL,1,4),(9,'KR-PER-002','Mouse Krypton Glide Pro','Inalámbrico, 26000 DPI, 60g',179.00,45,NULL,1,4),(10,'KR-MON-001','Monitor Krypton View 27','QHD 165Hz, IPS, 1ms',1099.00,15,NULL,1,5),(11,'1001650726','Laptop Acer Aspire Lite AL16-31P-36HW Intel Core i3-N305 8GB 512GB SSD 16 WQXGA','Características destacadas:\n\nVendido y despachado por: INFOTECSHOP\nTérminos y condiciones del seller: Ver términos y condiciones\nCapacidad del disco duro: 512GB SSD\nMemoria RAM: 8GB\nProcesador: Intel Core i3-N305',2099.00,5,'http://localhost:8080/api/uploads/images/43400d3b-3e97-4028-ad92-5c6c8389e639.webp',1,1),(12,'1231234126456456454564','Audio Prueba','dsadasdsadas',12354.00,12,'http://localhost:8094/api/uploads/images/cb6b33ab-0b79-4cba-a6e7-a5fe61d8c589.jpg',1,2);
/*!40000 ALTER TABLE `products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stock_movement`
--

DROP TABLE IF EXISTS `stock_movement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_movement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `type` varchar(20) NOT NULL,
  `quantity` int NOT NULL,
  `reason` varchar(120) DEFAULT NULL,
  `reference` varchar(120) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_stock_movement_user` (`created_by`),
  KEY `idx_stock_movement_product` (`product_id`),
  CONSTRAINT `fk_stock_movement_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `fk_stock_movement_user` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_movement`
--

LOCK TABLES `stock_movement` WRITE;
/*!40000 ALTER TABLE `stock_movement` DISABLE KEYS */;
INSERT INTO `stock_movement` VALUES (1,1,'SALIDA',1,'Venta orden #1','ORDER-1','2026-06-23 17:18:50.188709',NULL),(2,1,'SALIDA',1,'Venta orden #2','ORDER-2','2026-06-23 17:48:48.467517',NULL),(3,2,'SALIDA',1,'Venta orden #3','ORDER-3','2026-06-23 17:49:43.138071',NULL),(4,1,'SALIDA',1,'Venta orden #4','ORDER-4','2026-06-23 17:53:57.054922',NULL),(5,3,'SALIDA',1,'Venta orden #5','ORDER-5','2026-06-24 05:25:11.799243',NULL),(6,1,'SALIDA',1,'Venta orden #8','ORDER-8','2026-06-24 15:44:55.075323',NULL),(7,1,'SALIDA',1,'Venta orden #9','ORDER-9','2026-06-24 15:45:38.226685',NULL),(8,1,'SALIDA',1,'Venta orden #10','ORDER-10','2026-06-24 15:50:43.890320',NULL),(9,1,'SALIDA',1,'Venta orden #11','ORDER-11','2026-06-24 16:04:00.801365',NULL),(10,3,'SALIDA',1,'Venta orden #12','ORDER-12','2026-06-24 16:15:53.577917',NULL),(11,1,'SALIDA',1,'Venta orden #13','ORDER-13','2026-06-24 16:17:39.796080',NULL),(12,1,'SALIDA',1,'Venta orden #14','ORDER-14','2026-06-24 16:22:02.085940',NULL),(13,1,'SALIDA',1,'Venta orden #17','ORDER-17','2026-06-24 17:45:08.731393',NULL),(14,1,'SALIDA',1,'Venta orden #18','ORDER-18','2026-06-24 20:19:34.581445',NULL),(15,1,'ENTRADA',1,'Cancelación orden #18','ORDER-18','2026-06-24 20:19:38.165871',NULL),(16,1,'SALIDA',1,'Venta orden #19','ORDER-19','2026-06-24 20:20:32.934875',NULL),(17,1,'ENTRADA',1,'Cancelación orden #19','ORDER-19','2026-06-24 20:20:37.336307',NULL),(18,1,'SALIDA',1,'Venta orden #20','ORDER-20','2026-06-24 20:21:30.872605',NULL),(19,1,'ENTRADA',1,'Cancelación orden #20','ORDER-20','2026-06-24 20:21:34.989598',NULL),(20,1,'SALIDA',1,'Venta orden #21','ORDER-21','2026-06-24 20:33:30.718356',NULL),(21,1,'SALIDA',1,'Venta orden #22','ORDER-22','2026-06-24 22:21:20.994010',NULL),(22,2,'SALIDA',1,'Venta orden #23','ORDER-23','2026-06-24 22:24:03.692465',NULL),(23,2,'SALIDA',1,'Venta orden #24','ORDER-24','2026-06-24 22:39:43.358140',NULL),(24,2,'SALIDA',1,'Venta orden #25','ORDER-25','2026-06-24 22:40:38.022428',NULL);
/*!40000 ALTER TABLE `stock_movement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(120) NOT NULL,
  `email` varchar(160) NOT NULL,
  `password` varchar(120) NOT NULL,
  `role` varchar(20) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'Admin Krypton','admin@krypton.pe','$2a$10$N0.6BPMeDJxcK3BQW/cDnOXSjq6hj9rHwkZd7rEliqr0g.dTnPdBy','ADMIN','2026-06-22 16:51:13.945097',1),(2,'Prueba','prueba@prueba.com','$2a$10$H/QrGeel9N/XM1PGxObSMuBlk3ByleY.xjGi3ZqhU6uPB7t3Gith6','CLIENTE','2026-06-22 23:07:20.481951',1),(3,'Juan Test','cliente.test@krypton.pe','$2a$10$FEkqG47IJiHijKeckiGWxuTwWfrJiEhLzFbfzrlYo8/Qok8Bei3I2','CLIENTE','2026-06-23 17:48:47.793687',1),(4,'E2E Cliente','e2e-17261-11700@krypton.pe','$2a$10$KRNzLUX29pivY7gubmiEGuX03DX5GwTome2dfllrMQnsojpMsO0/2','CLIENTE','2026-06-24 04:45:38.600485',1),(5,'E2E Cliente','e2e-31077-11025@krypton.pe','$2a$10$oLORskTbkGY1EMg/zXEli.CojR5FQMWjykp5cN1WdZuO.79fMIII.','CLIENTE','2026-06-24 04:46:44.596681',1),(6,'E2E Cliente','e2e-10496-19031@krypton.pe','$2a$10$F1ugfdjyR.aV7vI1psMbJuChb2ZgXJC5XQ467rGyb/OCYWShw2xLm','CLIENTE','2026-06-24 04:47:35.117772',1),(7,'E2E Cliente','e2e-17060-13251@krypton.pe','$2a$10$y4d0uazwgH3nMezvdQ/wcOQv7qC0orBzHp10Ec/CjUHRGbG39rQUa','CLIENTE','2026-06-24 04:48:35.626844',1),(8,'E2E Cliente','e2e-9519-32460@krypton.pe','$2a$10$tPMGDtZlOFVtNH.9s/Z8OeoDkMSbUzWi5qLP59GA0nRv61/46MbUy','CLIENTE','2026-06-24 05:18:16.631161',1),(9,'Joel Curi','curi@curi.com','$2a$10$xpKwzPpBEBCUYUwaktZyxu4/nzXuGjCJ25v623YSHUpX3QhNCe4O6','CLIENTE','2026-06-24 05:24:35.948926',1),(10,'Cart Test','cart-28560@krypton.pe','$2a$10$.n7hChrQCcWzTDGbA7rWReDJjeappGYFmvGWp8mgSCuz5tUomPlDO','CLIENTE','2026-06-24 06:40:09.783499',1),(11,'T','c3-29178@krypton.pe','$2a$10$eWHg9fMrSCjpU9rAw4l.FeEjJwZiTuthqX7bO4Mm1IuvPMT7m3GK2','CLIENTE','2026-06-24 06:41:26.424997',1),(12,'T','c1-7729@krypton.pe','$2a$10$Kc3GkQACnW.thPpmuF30kOw1eHdb16uhrIHKlDSAHFf8kR16WXiOm','CLIENTE','2026-06-24 06:42:06.640722',1),(13,'Stock Test','stocktest_3108@krypton.test','$2a$10$XQVXBEkNrqeVOrZLWxgT4uBauYB6.ofabv3c75DCoBQ7fxcvtet46','CLIENTE','2026-06-24 15:44:51.718492',1),(14,'SS','ss_13998@krypton.test','$2a$10$cs2lZSNuNk6/Pj/6b9Ojau0OMD.ayPhgrWjPpkge251zFVfJYBEj2','CLIENTE','2026-06-24 15:45:37.892098',1),(15,'Cmp','cmp_3789@krypton.test','$2a$10$DXDcE1OO4qZHJBSNennyC.LhX8fNdwrtfjTbl4P62fX9/HFKMoVd6','CLIENTE','2026-06-24 16:02:48.669610',1),(16,'Diag','diag_32352@krypton.test','$2a$10$E.QAaxgFw5.qJ62lw2RuZew7ptg6wyCv7KRS3ZDU2j8AYFaWoyIw.','CLIENTE','2026-06-24 16:03:34.136288',1),(17,'Cmp','cmp2_32496@krypton.test','$2a$10$05mdsMGlV2er.DspSn/cVOV8LvtPEt/NzfQARxVgFPR5aRynnARC.','CLIENTE','2026-06-24 16:04:00.142055',1),(18,'Res','res_26088@krypton.test','$2a$10$nXXmI7rFk0Q9/Ewh3LNcuO17BXVW/B4kpitdBOWcvgKrKYiLBNUIi','CLIENTE','2026-06-24 16:22:00.998416',1),(19,'BD','bd_6251@krypton.test','$2a$10$jWR8dfA48k8Q.zAg1IEBgOm8kuWanJHOWW.dSGUi/tW//InAXI5Ti','CLIENTE','2026-06-24 16:25:47.221286',1),(20,'Rev','rev_2523@krypton.test','$2a$10$TcLikmqud0aamqQPNaHxm.aslqgNVIvNrHrwV0c7wS8DryfV5/NBi','CLIENTE','2026-06-24 20:17:59.721310',1),(21,'Rev','rev2_19003@krypton.test','$2a$10$j7ROCj8GTtAqGopakwFDWeFf7oc5nsQx8DaZS1Q8wuFP/wrjGolpu','CLIENTE','2026-06-24 20:19:33.489529',1),(22,'R','rev3_29572@krypton.test','$2a$10$1gHDlgoK0.nRPgHduF0LX.bfaM9zQm6UfQbc4gKhMQnU1Pp0p4usm','CLIENTE','2026-06-24 20:20:32.226838',1),(23,'R','rev4_29018@krypton.test','$2a$10$og78XVOhBt7szJpaj9FmvOApTfRWEwFyhM/sKPdVCESzyDxsufHUy','CLIENTE','2026-06-24 20:21:30.494169',1),(24,'Ensayo','ensayo_2640@krypton.test','$2a$10$GueEo4a69ZKk8FoDT3kf4uo4/I5ztmuhsNfz6FtOdk9zVjnBUFtS.','CLIENTE','2026-06-24 20:33:28.842404',1),(25,'Rep','rep_21017@krypton.test','$2a$10$ll.bqSTE4WKjV75bkqmf/eeRac3H47qZfYCguHnTol97o8ZNKAuTe','CLIENTE','2026-06-24 22:21:18.873169',1),(26,'P','pend_18843@krypton.test','$2a$10$f87GADhZC2th2t5hoh0tUudzM3Q3dbt97rq3Qm5TzsmX4yGxBKS8G','CLIENTE','2026-06-24 22:24:02.935538',1),(27,'E','ent_24974@krypton.test','$2a$10$bcnzAvlqDA/IFZSmdUVhw.5clrlgg5lHGR0uQXPqDJpIo6/ir0lpi','CLIENTE','2026-06-24 22:25:47.345146',1),(28,'E2E','e2e_9530@krypton.test','$2a$10$./0lKEQ8z2VS.vuAThzjXuQuQ.PnBqKRLayILtId51t1jtb9KIBbe','CLIENTE','2026-06-24 22:39:41.891202',1),(29,'OK','ok_16604@krypton.test','$2a$10$naNJy9waWU/y.QZui5jhh.Y8RZAHyAdD09i5DPCKrV/l6q2yPVZxe','CLIENTE','2026-06-24 22:40:37.211804',1);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'krypton'
--

--
-- Dumping routines for database 'krypton'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-25  0:12:09
