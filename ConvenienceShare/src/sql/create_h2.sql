
CREATE TABLE IF NOT EXISTS  MACHINE (
   M_ID INTEGER PRIMARY KEY     AUTO_INCREMENT,
   NAME           TEXT          NOT NULL,
   IP             char(50),
   PORT           INT,
   NPORTS         INT           NOT NULL DEFAULT  1,
   LASTACTIVE     DATETIME,
   SHARING        INT           NOT NULL  DEFAULT 0,
   IDENT          char(50)      NOT NULL,
   LOCAL          INT           NOT NULL  DEFAULT 0,
   UNIQUE (IP, PORT),
   UNIQUE (IDENT)
);

CREATE TABLE IF NOT EXISTS  KEY (
   K_ID INTEGER PRIMARY KEY     AUTO_INCREMENT,
   KEY            char(10)      NOT NULL,
   ADDED          DATETIME      NOT NULL,
   MID            INT,
   FOREIGN KEY(MID) REFERENCES MACHINE(M_ID),
   UNIQUE(KEY, MID)
);

CREATE TABLE IF NOT EXISTS  ROOT (
   R_ID           INTEGER PRIMARY KEY   AUTO_INCREMENT,
   PATH           char(256)   NOT NULL,
   TAGS           char(50),
   DESC           char(100),
   MID            INT         NOT NULL,
   LOCAL          INT         NOT NULL,
   SPACE          LONG,
   NFILES         LONG,
   
   FOREIGN KEY(MID) REFERENCES MACHINE(M_ID),
   UNIQUE (PATH, MID)
);

CREATE TABLE IF NOT EXISTS IGNOREPATTERN (
   I_ID INTEGER   PRIMARY KEY    AUTO_INCREMENT,
   RID            INT            NOT NULL,
   PATTERN        char(256)      NOT NULL,
   
   FOREIGN KEY(RID) REFERENCES ROOT(R_ID),
   UNIQUE(RID, PATTERN)
);

CREATE TABLE IF NOT EXISTS  PATH (
   P_ID INTEGER   PRIMARY KEY   AUTO_INCREMENT,
   PATH           char(256)     NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS FILE (
   F_ID INTEGER PRIMARY KEY     AUTO_INCREMENT,
   NAME           char(256)     NOT NULL,
   SIZE           INT           NOT NULL,
   CHKSUM         CHAR(50),
   PATH           INT NOT NULL,
   ROOT           INT NOT NULL,
   STATE          INT NOT NULL DEFAULT 0,
   MODIFIED       DATETIME,
   ERROR          INT DEFAULT 0,
   
   FOREIGN KEY(PATH) REFERENCES PATH(P_ID),
   FOREIGN KEY(ROOT) REFERENCES ROOT(R_ID),
   
   UNIQUE(NAME, PATH, ROOT)
);

CREATE TABLE IF NOT EXISTS PENDING (
   Q_ID INTEGER PRIMARY KEY    AUTO_INCREMENT,
   FID            INT NOT NULL,
   
   FOREIGN KEY(FID) REFERENCES FILE(F_ID),
   UNIQUE(FID)
);
