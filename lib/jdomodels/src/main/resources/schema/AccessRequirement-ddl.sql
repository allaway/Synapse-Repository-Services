CREATE TABLE IF NOT EXISTS `ACCESS_REQUIREMENT` (
  `ID` BIGINT NOT NULL,
  `ETAG` char(36) NOT NULL,
  `NAME` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `CURRENT_REV_NUM` BIGINT DEFAULT 0,
  `CREATED_BY` BIGINT NOT NULL,
  `CREATED_ON` BIGINT NOT NULL,
  `ACCESS_TYPE` ENUM('DOWNLOAD', 'PARTICIPATE') NOT NULL,
  `CONCRETE_TYPE` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `AR_NAME` (`NAME`),
  CONSTRAINT `ACCESS_REQUIREMENT_CREATED_BY_FK` FOREIGN KEY (`CREATED_BY`) REFERENCES `USER_GROUP` (`ID`) ON DELETE CASCADE
)
