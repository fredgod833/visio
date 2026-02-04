CREATE DATABASE `YOUR_CAR_YOUR_WAY`;

CREATE TABLE `AGENCY` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(63) DEFAULT NULL,
  `address` text,
  `email` varchar(250) NOT NULL,
  `phoneNo` varchar(15) DEFAULT NULL,
  `latitude` decimal(8,6) NOT NULL,
  `longitude` decimal(9,6) NOT NULL,
  `createdAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updateAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `CAR` (
  `id` int NOT NULL AUTO_INCREMENT,
  `categoryId` int NOT NULL,
  `ownerAgencyId` int NOT NULL,
  `registrationNo` varchar(12) NOT NULL,
  `name` varchar(31) DEFAULT NULL,
  `description` text,
  `createdAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updateAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `registrationNo` (`registrationNo`),
  KEY `categoryId` (`categoryId`),
  KEY `ownerAgencyId` (`ownerAgencyId`),
  CONSTRAINT `CAR_ibfk_1` FOREIGN KEY (`categoryId`) REFERENCES `CAR_CATEGORY` (`id`),
  CONSTRAINT `CAR_ibfk_2` FOREIGN KEY (`ownerAgencyId`) REFERENCES `AGENCY` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `CAR_BOOKING` (
  `id` int NOT NULL AUTO_INCREMENT,
  `status` int NOT NULL,
  `customerId` int NOT NULL,
  `categoryId` int NOT NULL,
  `carId` int DEFAULT NULL,
  `pickupAgencyId` int NOT NULL,
  `returnAgencyId` int NOT NULL,
  `pickUpDate` date NOT NULL,
  `returnDate` date NOT NULL,
  `price` decimal(10,2) DEFAULT NULL,
  `createdAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updateAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `customerId` (`customerId`),
  KEY `pickupAgencyId` (`pickupAgencyId`),
  KEY `returnAgencyId` (`returnAgencyId`),
  KEY `categoryId` (`categoryId`),
  KEY `carId` (`carId`),
  CONSTRAINT `CAR_BOOKING_ibfk_1` FOREIGN KEY (`customerId`) REFERENCES `CUSTOMER` (`id`),
  CONSTRAINT `CAR_BOOKING_ibfk_2` FOREIGN KEY (`pickupAgencyId`) REFERENCES `AGENCY` (`id`),
  CONSTRAINT `CAR_BOOKING_ibfk_3` FOREIGN KEY (`returnAgencyId`) REFERENCES `AGENCY` (`id`),
  CONSTRAINT `CAR_BOOKING_ibfk_4` FOREIGN KEY (`categoryId`) REFERENCES `CAR_CATEGORY` (`id`),
  CONSTRAINT `CAR_BOOKING_ibfk_5` FOREIGN KEY (`carId`) REFERENCES `CAR` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `CAR_CATEGORY` (
  `id` int NOT NULL AUTO_INCREMENT,
  `acrissCode` varchar(4) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `acrissCode` (`acrissCode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `CUSTOMER` (
  `id` int NOT NULL AUTO_INCREMENT,
  `userId` int DEFAULT NULL,
  `birthdate` date DEFAULT NULL,
  `address` text,
  `phoneNo` varchar(15) DEFAULT NULL,
  `createdAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updateAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `userId` (`userId`),
  CONSTRAINT `CUSTOMER_ibfk_1` FOREIGN KEY (`userId`) REFERENCES `USER` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `MANAGER` (
  `id` int NOT NULL AUTO_INCREMENT,
  `userId` int DEFAULT NULL,
  `agencyId` int DEFAULT NULL,
  `createdAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updateAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `userId` (`userId`),
  CONSTRAINT `MANAGER_ibfk_1` FOREIGN KEY (`userId`) REFERENCES `USER` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `MESSAGE` (
  `id` int NOT NULL AUTO_INCREMENT,
  `senderId` int DEFAULT NULL,
  `customerId` int DEFAULT NULL,
  `agencyId` int DEFAULT NULL,
  `object` varchar(255) DEFAULT NULL,
  `content` text,
  `date` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `customerId` (`customerId`),
  KEY `agencyId` (`agencyId`),
  KEY `senderId` (`senderId`),
  CONSTRAINT `MESSAGE_ibfk_1` FOREIGN KEY (`customerId`) REFERENCES `CUSTOMER` (`id`),
  CONSTRAINT `MESSAGE_ibfk_2` FOREIGN KEY (`agencyId`) REFERENCES `AGENCY` (`id`),
  CONSTRAINT `MESSAGE_ibfk_3` FOREIGN KEY (`senderId`) REFERENCES `USER` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `USER` (
  `id` int NOT NULL AUTO_INCREMENT,
  `email` varchar(250) NOT NULL,
  `password` varchar(255) NOT NULL,
  `status` varchar(50) NOT NULL DEFAULT 'OFFLINE',
  `firstName` varchar(31) DEFAULT NULL,
  `lastName` varchar(31) DEFAULT NULL,
  `type` varchar(8) DEFAULT 'CUSTOMER',
  `createdAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updateAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `password` (`password`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
