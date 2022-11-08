CREATE TABLE IF NOT EXISTS `NODE` (
  `ID` BIGINT NOT NULL,
  `CREATED_BY` BIGINT NOT NULL,
  `CREATED_ON` BIGINT NOT NULL,
  `CURRENT_REV_NUM` BIGINT NOT NULL,
  `MAX_REV_NUM` BIGINT NOT NULL,
  `ETAG` char(36) NOT NULL,
  `NAME` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `NODE_TYPE` ENUM('project', 'folder', 'link','file','table','entityview','dockerrepo','submissionview', 'dataset', 'datasetcollection', 'materializedview') NOT NULL,
  `PARENT_ID` BIGINT DEFAULT NULL,
  `ALIAS` varchar(64) CHARACTER SET latin1 COLLATE latin1_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `NODE_UNIQUE_CHILD_NAME` (`PARENT_ID`,`NAME`),
  INDEX `NODE_TYPE` (`NODE_TYPE`),
  INDEX `NODE_PARENT_TYPE` (`PARENT_ID`,`NODE_TYPE`),
  INDEX `NODE_NAME_INDEX` (`NAME` ASC),
  UNIQUE KEY `NODE_UNIQUE_ALIAS` (`ALIAS`),
  CONSTRAINT `NODE_PARENT_FK` FOREIGN KEY (`PARENT_ID`) REFERENCES `NODE` (`ID`) ON DELETE CASCADE,
  CONSTRAINT `NODE_CREATED_BY_FK` FOREIGN KEY (`CREATED_BY`) REFERENCES `USER_GROUP` (`ID`) ON DELETE CASCADE
)
