CREATE TABLE APORDER
  (
     CAT TEXT,
     NUM INTEGER
  );

INSERT INTO APORDER
            (CAT,
             NUM)
VALUES      ('HYP',
             3);

INSERT INTO APORDER
            (CAT,
             NUM)
VALUES      ('ANT',
             2);

INSERT INTO APORDER
            (CAT,
             NUM)
VALUES      ('SYN',
             1);

CREATE INDEX APO
  ON APORDER(CAT, NUM);