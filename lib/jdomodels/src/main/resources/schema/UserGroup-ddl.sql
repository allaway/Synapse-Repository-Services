CREATE TABLE IF NOT EXISTS `USER_GROUP` (
  `ID` BIGINT NOT NULL,
  `CREATION_DATE` datetime DEFAULT NULL,
  `ISINDIVIDUAL` bit(1) DEFAULT NULL,
  `ETAG` char(36) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`)
)
