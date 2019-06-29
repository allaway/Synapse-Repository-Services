CREATE TABLE `SENT_MESSAGES` (
  `CHANGE_NUM` bigint(20) DEFAULT NULL,
  `TIME_STAMP` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `OBJECT_ID` bigint(20) NOT NULL,
  `OBJECT_VERSION` bigint(20) NOT NULL,
  `OBJECT_TYPE` enum('ENTITY','ENTITY_CONTAINER','PRINCIPAL','ACTIVITY','EVALUATION','SUBMISSION','EVALUATION_SUBMISSIONS','FILE','MESSAGE','WIKI','FAVORITE','ACCESS_REQUIREMENT','ACCESS_APPROVAL','TEAM','TABLE','ACCESS_CONTROL_LIST','PROJECT_SETTING','VERIFICATION_SUBMISSION','CERTIFIED_USER_PASSING_RECORD','FORUM','THREAD','REPLY','ENTITY_VIEW','USER_PROFILE','DATA_ACCESS_REQUEST','DATA_ACCESS_SUBMISSION','DATA_ACCESS_SUBMISSION_STATUS','MEMBERSHIP_INVITATION') NOT NULL,
  PRIMARY KEY (`OBJECT_ID`,`OBJECT_VERSION`,`OBJECT_TYPE`),
  UNIQUE KEY `SENT_UNIQUE_CHANG_NUM` (`CHANGE_NUM`),
  CONSTRAINT `SENT_MESS_CH_NUM_FK` FOREIGN KEY (`OBJECT_ID`,`OBJECT_VERSION`,`OBJECT_TYPE`) REFERENCES `changes` (`OBJECT_ID`,`OBJECT_VERSION`,`OBJECT_TYPE`) ON DELETE CASCADE)
