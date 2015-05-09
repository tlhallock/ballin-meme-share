
CREATE TABLE IF NOT EXISTS  MACHINE (
   M_ID INTEGER PRIMARY KEY     AUTO_INCREMENT,
   MNAME          varchar(50)   NOT NULL,
   IP             varchar(50),
   PORT           INT,
   NPORTS         INT           NOT NULL DEFAULT  1,
   LAST_ACTIVE    LONG,
   SHARING        BOOLEAN       NOT NULL  DEFAULT TRUE,
   IDENT          varchar(50)   NOT NULL,
   IS_LOCAL       BOOLEAN       NOT NULL  DEFAULT 0,
   MESSAGES       BOOLEAN       NOT NULL  DEFAULT TRUE,

   UNIQUE (IP, PORT),
   UNIQUE (IDENT)
);

CREATE TABLE IF NOT EXISTS  PUBLIC_KEY (
   K_ID INTEGER PRIMARY KEY     AUTO_INCREMENT,
   KEYSTR         char(10)      NOT NULL,
   ADDED          DATETIME      NOT NULL,
   EXPIRES        DATETIME,
   MID            INT,

   FOREIGN KEY(MID) REFERENCES MACHINE(M_ID),
   UNIQUE(KEYSTR, MID)
);

CREATE TABLE IF NOT EXISTS  PELEM (
   P_ID           INTEGER       PRIMARY KEY   AUTO_INCREMENT,
   PARENT         INTEGER       NOT NULL,
   BROKEN         BOOLEAN       NOT NULL,
   PELEM          varchar(50)   NOT NULL,

   UNIQUE(PELEM, PARENT)
);

CREATE TABLE IF NOT EXISTS  ROOT (
   R_ID           INTEGER PRIMARY KEY   AUTO_INCREMENT,
   PELEM          INTEGER     NOT NULL,
   TAGS           varchar( 64),
   DESCR          varchar(256),
   MID            INT         NOT NULL,
   IS_LOCAL       BOOLEAN     NOT NULL,
   TSPACE         LONG,
   NFILES         LONG,
   RNAME          char(50)    NOT NULL,
   
   FOREIGN KEY(MID  ) REFERENCES MACHINE(M_ID),
   FOREIGN KEY(PELEM) REFERENCES   PELEM(P_ID),
   UNIQUE (PELEM, MID),
   UNIQUE (RNAME, MID)
);

-- Tells which remote this path exists in;
CREATE TABLE IF NOT EXISTS ROOT_CONTAINS (
   RID            INTEGER,
   PELEM          INTEGER,

   FOREIGN KEY(RID)   REFERENCES ROOT(R_ID),
   FOREIGN KEY(PELEM) REFERENCES PELEM(P_ID),
   PRIMARY KEY(RID, PELEM)
);

CREATE TABLE IF NOT EXISTS IGNORE_PATTERN (
   I_ID           INTEGER   PRIMARY KEY    AUTO_INCREMENT,
   RID            INT            NOT NULL,
   PATTERN        varchar(256)   NOT NULL,
   
   FOREIGN KEY(RID) REFERENCES ROOT(R_ID),
   UNIQUE(RID, PATTERN)
);

CREATE TABLE IF NOT EXISTS SFILE (
   F_ID           INTEGER       PRIMARY KEY     AUTO_INCREMENT,
   FSIZE          INT           NOT NULL,
   TAGS           varchar(64),
   CHKSUM         char(40),
   PELEM          INTEGER       NOT NULL,
   ROOT           INTEGER       NOT NULL,
   RSTATE         INTEGER       NOT NULL DEFAULT 0,
   MODIFIED       LONG,
   ERROR          INTEGER       DEFAULT 0,
   
   FOREIGN KEY(PELEM) REFERENCES PELEM(P_ID),
   FOREIGN KEY(ROOT)  REFERENCES  ROOT(R_ID),
   
   UNIQUE(PELEM, ROOT)
);

CREATE TABLE IF NOT EXISTS PENDING_DOWNLOAD (
   Q_ID           INTEGER PRIMARY KEY    AUTO_INCREMENT,
   FID            INTEGER NOT NULL,
   ADDED          LONG    NOT NULL,
   DSTATE         INTEGER,
   
   FOREIGN KEY(FID) REFERENCES SFILE(F_ID),
   UNIQUE(FID)
);

CREATE TABLE IF NOT EXISTS MESSAGES (
   M_ID           INTEGER PRIMARY KEY    AUTO_INCREMENT,
   MID            INTEGER NOT NULL,
   SENT           LONG    NOT NULL,
   MTYPE          INTEGER NOT NULL,
   
   FOREIGN KEY(MID) REFERENCES MACHINE(M_ID)
);


