
CREATE TABLE IF NOT EXISTS  MACHINE (
   M_ID INTEGER PRIMARY KEY     AUTO_INCREMENT,
   MNAME          varchar(50)   NOT NULL,
   IP             varchar(50),
   PORT           INT,
   NPORTS         INT           NOT NULL DEFAULT  1,
   LAST_ACTIVE    LONG,
   SHARING        INTEGER       NOT NULL  DEFAULT TRUE,
   SHARES_WITH_US INTEGER,
   IDENT          varchar(50)   NOT NULL,
   IS_LOCAL       BOOLEAN       NOT NULL  DEFAULT 0,
   MESSAGES       BOOLEAN       NOT NULL  DEFAULT TRUE,
   ACCEPT_PEERS   BOOLEAN                 DEFAULT FALSE,
);

CREATE UNIQUE      INDEX IF NOT EXISTS MU ON MACHINE(IP, PORT);
CREATE UNIQUE HASH INDEX IF NOT EXISTS MI ON MACHINE(IDENT);











CREATE TABLE IF NOT EXISTS  PUBLIC_KEY (
   K_ID INTEGER PRIMARY KEY     AUTO_INCREMENT,
   KEYSTR         char(280)     NOT NULL,
   ADDED          LONG          NOT NULL,
   EXPIRES        LONG,
   MID            INT,

   FOREIGN KEY(MID) REFERENCES MACHINE(M_ID) ON DELETE CASCADE,
);
CREATE UNIQUE      INDEX IF NOT EXISTS KU ON PUBLIC_KEY(KEYSTR, MID);










CREATE TABLE IF NOT EXISTS  PELEM (
   P_ID           LONG          PRIMARY KEY   AUTO_INCREMENT,
   PARENT         LONG          NOT NULL,
   BROKEN         BOOLEAN       NOT NULL,
   PELEM          varchar(20)   NOT NULL,
);
CREATE        HASH INDEX IF NOT EXISTS PU ON PELEM(PELEM);
CREATE UNIQUE      INDEX IF NOT EXISTS PF ON PELEM(PELEM, PARENT);












CREATE TABLE IF NOT EXISTS  ROOT (
   R_ID           INTEGER PRIMARY KEY   AUTO_INCREMENT,
   PELEM          LONG        NOT NULL,
   TAGS           varchar( 64),
   DESCR          varchar(256),
   MID            INT         NOT NULL,
   IS_LOCAL       BOOLEAN     NOT NULL,
   TSPACE         LONG,
   NFILES         LONG,
   RNAME          char(50)    NOT NULL,
   SHARING        INTEGER     NOT NULL,
   
   FOREIGN KEY(MID  ) REFERENCES MACHINE(M_ID)  ON DELETE CASCADE,
   FOREIGN KEY(PELEM) REFERENCES   PELEM(P_ID),
);

CREATE UNIQUE      INDEX IF NOT EXISTS RUP ON ROOT(PELEM, MID);
CREATE UNIQUE      INDEX IF NOT EXISTS RUN ON ROOT(RNAME, MID);










-- Tells which remote this path exists in;
CREATE TABLE IF NOT EXISTS ROOT_CONTAINS (
   RID            INTEGER,
   PELEM          LONG,

   FOREIGN KEY(RID)   REFERENCES ROOT(R_ID)   ON DELETE CASCADE,
   FOREIGN KEY(PELEM) REFERENCES PELEM(P_ID)  ON DELETE CASCADE,
   PRIMARY KEY(RID, PELEM),
);
CREATE             INDEX IF NOT EXISTS RCF ON ROOT_CONTAINS(PELEM);












CREATE TABLE IF NOT EXISTS IGNORE_PATTERN (
   I_ID           INTEGER         PRIMARY KEY    AUTO_INCREMENT,
   RID            INT             NOT NULL,
   PATTERN        varchar(1024)   NOT NULL,
   
   FOREIGN KEY(RID) REFERENCES ROOT(R_ID)     ON DELETE CASCADE,
);

CREATE UNIQUE      INDEX IF NOT EXISTS IU ON IGNORE_PATTERN(RID, PATTERN);











CREATE TABLE IF NOT EXISTS SFILE (
   F_ID           INTEGER       PRIMARY KEY     AUTO_INCREMENT,
   FSIZE          LONG          NOT NULL,
   TAGS           varchar(64),
   CHKSUM         char(40),
   PELEM          LONG          NOT NULL,
   ROOT           INTEGER       NOT NULL,
   RSTATE         INTEGER       NOT NULL DEFAULT 0,
   MODIFIED       LONG,
   ERROR          INTEGER       DEFAULT 0,
   
   FOREIGN KEY(PELEM) REFERENCES PELEM(P_ID),
   FOREIGN KEY(ROOT)  REFERENCES  ROOT(R_ID)  ON DELETE CASCADE,
);
CREATE UNIQUE      INDEX IF NOT EXISTS FU ON SFILE(PELEM, ROOT);













CREATE TABLE IF NOT EXISTS DOWNLOAD (
   Q_ID           INTEGER PRIMARY KEY    AUTO_INCREMENT,
   FID            INTEGER NOT NULL,
   ADDED          LONG    NOT NULL,
   DSTATE         INTEGER,
   PRIORITY       INTEGER,
   DEST_FILE      VARCHAR,
   
   FOREIGN KEY(FID) REFERENCES SFILE(F_ID)  ON DELETE CASCADE,
);
CREATE UNIQUE      INDEX IF NOT EXISTS DU ON DOWNLOAD(FID);













CREATE TABLE IF NOT EXISTS MESSAGE (
   M_ID           INTEGER       PRIMARY KEY    AUTO_INCREMENT,
   MID            INTEGER       NOT NULL,
   SENT           LONG          NOT NULL,
   MTYPE          INTEGER       NOT NULL,
   MESSAGE        varchar(1024) NOT NULL,
   
   FOREIGN KEY(MID) REFERENCES MACHINE(M_ID)  ON DELETE CASCADE
);












CREATE TABLE IF NOT EXISTS SHARE_ROOT (
   RID             INTEGER NOT NULL,
   MID             INTEGER NOT NULL,
   IS_SHARING      INTEGER NOT NULL,

   FOREIGN KEY(MID) REFERENCES MACHINE(M_ID)  ON DELETE CASCADE,
   FOREIGN KEY(RID) REFERENCES ROOT   (R_ID)  ON DELETE CASCADE,
   PRIMARY KEY(RID, MID)
);












CREATE TABLE IF NOT EXISTS CHUNK (
   C_ID            INTEGER     PRIMARY KEY AUTO_INCREMENT,
   DID             INTEGER     NOT NULL,
   BEGIN_OFFSET    LONG        NOT NULL,
   END_OFFSET      LONG        NOT NULL,
   CHECKSUM        varchar(40) NOT NULL,
   IS_DOWNLOADED   BOOLEAN     NOT NULL,

   FOREIGN KEY(DID) REFERENCES DOWNLOAD(Q_ID)  ON DELETE CASCADE,
);
CREATE UNIQUE      INDEX IF NOT EXISTS CU ON CHUNK(DID, BEGIN_OFFSET, END_OFFSET);















MERGE INTO PELEM KEY(P_ID) VALUES (0, 0, FALSE, '                    ');
ALTER TABLE PELEM ADD FOREIGN KEY (PARENT) REFERENCES PELEM(P_ID);

