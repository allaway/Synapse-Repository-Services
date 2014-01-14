CREATE TABLE `STORAGE_QUOTA` (
	`OWNER_ID` BIGINT(20) NOT NULL,
	`ETAG` CHAR(36) NOT NULL,
	`QUOTA_IN_MB` INT(20) NOT NULL,
	PRIMARY KEY (`OWNER_ID`),
	CONSTRAINT `STORAGE_QUOTA_OWNER_FK` FOREIGN KEY (`OWNER_ID`) REFERENCES `PRINCIPAL` (`ID`) ON DELETE CASCADE
)
