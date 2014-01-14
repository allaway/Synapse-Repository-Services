CREATE TABLE JDOEVALUATION (
    ID bigint(20) NOT NULL,
    ETAG char(36) NOT NULL,
    NAME varchar(256)CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
    DESCRIPTION mediumblob DEFAULT NULL,
    OWNER_ID bigint(20) NOT NULL,
    CREATED_ON bigint(20) NOT NULL,
    CONTENT_SOURCE bigint(20) NOT NULL,
    STATUS int NOT NULL,
    SUBMISSION_INSTRUCTIONS_MESSAGE mediumblob,
    SUBMISSION_RECEIPT_MESSAGE mediumblob,
    PRIMARY KEY (ID),
    UNIQUE KEY  JDOEVALUATION_U1 (NAME),
    FOREIGN KEY (OWNER_ID) REFERENCES PRINCIPAL (ID),
    FOREIGN KEY (CONTENT_SOURCE) REFERENCES JDONODE (ID) ON DELETE CASCADE
);