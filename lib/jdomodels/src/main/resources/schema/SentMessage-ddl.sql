CREATE TABLE `SENT_MESSAGES` (
  `CHANGE_NUM` BIGINT DEFAULT NULL,
  `TIME_STAMP` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `OBJECT_ID` BIGINT NOT NULL,
  `OBJECT_VERSION` BIGINT NOT NULL,
  `OBJECT_TYPE` enum('JSON_SCHEMA_DEPENDANT','JSON_SCHEMA','ENTITY','ENTITY_CONTAINER','PRINCIPAL','ACTIVITY','EVALUATION','SUBMISSION','EVALUATION_SUBMISSIONS','FILE','MESSAGE','WIKI','FAVORITE','ACCESS_REQUIREMENT','ACCESS_APPROVAL','TEAM','TABLE','ACCESS_CONTROL_LIST','PROJECT_SETTING','VERIFICATION_SUBMISSION','CERTIFIED_USER_PASSING_RECORD','FORUM','THREAD','REPLY','ENTITY_VIEW','USER_PROFILE','DATA_ACCESS_REQUEST','DATA_ACCESS_SUBMISSION','DATA_ACCESS_SUBMISSION_STATUS','MEMBERSHIP_INVITATION', 'THREAD_VIEW') NOT NULL,
  PRIMARY KEY (`OBJECT_ID`,`OBJECT_VERSION`,`OBJECT_TYPE`),
  UNIQUE KEY `SENT_UNIQUE_CHANG_NUM` (`CHANGE_NUM`),
  CONSTRAINT `SENT_MESS_CH_NUM_FK` FOREIGN KEY (`OBJECT_ID`,`OBJECT_VERSION`,`OBJECT_TYPE`) REFERENCES `CHANGES` (`OBJECT_ID`,`OBJECT_VERSION`,`OBJECT_TYPE`) ON DELETE CASCADE)
