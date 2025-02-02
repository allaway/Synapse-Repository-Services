CREATE TABLE IF NOT EXISTS `ASYNCH_JOB_STATUS` (
  `JOB_ID` BIGINT NOT NULL,
  `ETAG` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `JOB_STATE` ENUM('PROCESSING','FAILED','COMPLETE') NOT NULL,
  `JOB_TYPE` ENUM('TABLE_UPDATE_TRANSACTION','UPLOAD_CSV_TO_TABLE_PREVIEW','DOWNLOAD_CSV_FROM_TABLE','QUERY','QUERY_NEXT_PAGE','BULK_FILE_DOWNLOAD','MIGRATION','DOI','ADD_FILES_TO_DOWNLOAD_LIST','STORAGE_REPORT','JSON_SCHEMA_CREATE','VIEW_COLUMN_MODEL_REQUEST','GET_VALIDATION_SCHEMA','QUERY_DOWNLOAD_LIST', 'ADD_TO_DOWNLOAD_LIST', 'DOWNLOAD_LIST_PACKAGE', 'DOWNLOAD_LIST_MANIFEST', 'FILE_HANDLE_ARCHIVAL_REQUEST', 'FILE_HANDLE_RESTORE_REQUEST') NOT NULL,
  `CANCELING` BIT(1) NOT NULL,
  `EXCEPTION` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `ERROR_MESSAGE` VARCHAR(3000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `ERROR_DETAILS` MEDIUMTEXT DEFAULT NULL,
  `PROGRESS_CURRENT` BIGINT DEFAULT NULL,
  `PROGRESS_TOTAL` BIGINT DEFAULT NULL,
  `PROGRESS_MESSAGE` VARCHAR(3000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `STARTED_ON` TIMESTAMP(3) NOT NULL,
  `STARTED_BY` BIGINT NOT NULL,
  `CHANGED_ON` TIMESTAMP(3) NOT NULL,
  `REQUEST_BODY` JSON NOT NULL,
  `RESPONSE_BODY` JSON,
  `RUNTIME_MS` BIGINT NOT NULL,
  `REQUEST_HASH` VARCHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  PRIMARY KEY (`JOB_ID`),
  KEY `ASYNCH_J_S_STARTED_BY_FK` (`STARTED_BY`),
  KEY `REQUEST_HASH` (`REQUEST_HASH`),
  CONSTRAINT `ASYNCH_J_S_STARTED_BY_FK` FOREIGN KEY (`STARTED_BY`) REFERENCES `USER_GROUP` (`ID`) ON DELETE CASCADE
)