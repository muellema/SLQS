  CREATE TEMP TABLE CSUMS AS
  SELECT EXPID,
         TOKEN,
         TAG,
         (SELECT SUM(TUPLECOUNT * TUPLECOUNT)
          FROM   WINDOWCOUNT
          WHERE  TOKEN1LEMMA = TOKEN
                 AND TOKEN1TAG = TAG
                 AND TOKEN2TAG IN (SELECT TAG
                                   FROM   TAGLISTS
                                   WHERE  EXP_ID = EXPID)) AS CC
  FROM   (SELECT EXPID,
                 TOKEN1 AS TOKEN,
                 TAG1   AS TAG
          FROM   EXPRELS
          UNION
          SELECT EXPID,
                 TOKEN2 AS TOKEN,
                 TAG2   AS TAG
          FROM   EXPRELS);