CREATE TABLE IF NOT EXISTS `FILES` (
  `ID` BIGINT NOT NULL,
  `ETAG` char(36) NOT NULL,
  `PREVIEW_ID` BIGINT DEFAULT NULL,
  `CREATED_ON` TIMESTAMP NOT NULL,
  `CREATED_BY` BIGINT NOT NULL,
  `UPDATED_ON` TIMESTAMP NOT NULL,
  `METADATA_TYPE` ENUM('S3', 'EXTERNAL', 'GOOGLE_CLOUD', 'PROXY', 'EXTERNAL_OBJ_STORE') NOT NULL,
  `CONTENT_TYPE` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `CONTENT_SIZE` BIGINT DEFAULT NULL,
  `CONTENT_MD5` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `BUCKET_NAME` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `NAME` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `KEY` varchar(700) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `STORAGE_LOCATION_ID` BIGINT DEFAULT NULL,
  `ENDPOINT` varchar(512) DEFAULT NULL,
  `IS_PREVIEW` boolean NOT NULL DEFAULT FALSE,
  `STATUS` ENUM('AVAILABLE', 'UNLINKED', 'ARCHIVED', 'RESTORING') NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `KEY_KEY` (`KEY`),
  KEY `MD5_KEY` (`CONTENT_MD5`),
  KEY `STATUS_UPDATED_ON_BUCKET_NAME_KEY` (`STATUS`, `UPDATED_ON`, `BUCKET_NAME`),
  CONSTRAINT `FILE_PREVIEW_ID_FK` FOREIGN KEY (`PREVIEW_ID`) REFERENCES `FILES` (`ID`) ON DELETE SET NULL,
  CONSTRAINT `FILE_CREATED_BY_FK` FOREIGN KEY (`CREATED_BY`) REFERENCES `JDOUSERGROUP` (`ID`) ON DELETE CASCADE,
  CONSTRAINT `FILE_STORAGE_LOCATION_ID_FK` FOREIGN KEY (`STORAGE_LOCATION_ID`) REFERENCES `STORAGE_LOCATION` (`ID`)
)
