CREATE TEMP TABLE COSTEST AS
  SELECT EXPID,
         CAT,
         TOKEN1,
         TAG1,
         TOKEN2,
         TAG2,
         W.TUPLECOUNT  AS C1,
         W2.TUPLECOUNT AS C2
  FROM   WINDOWCOUNT AS W
         INNER JOIN WINDOWCOUNT AS W2
                 ON W.TOKEN2LEMMA = W2.TOKEN2LEMMA
                    AND W.TOKEN2TAG = W2.TOKEN2TAG
         INNER JOIN EXPRELS
                 ON W.TOKEN1LEMMA = TOKEN1
                    AND W.TOKEN1TAG = TAG1
                    AND W2.TOKEN1LEMMA = TOKEN2
                    AND W2.TOKEN1TAG = TAG2
  WHERE  W.TOKEN1TAG IN (SELECT TAG
                         FROM   TAGLISTS
                         WHERE  EXP_ID = EXPID)
         AND W2.TOKEN1TAG IN (SELECT TAG
                              FROM   TAGLISTS
                              WHERE  EXP_ID = EXPID)
         AND W.TOKEN2TAG IN (SELECT TAG
                             FROM   TAGLISTS
                             WHERE  EXP_ID = EXPID)
  ORDER  BY W.TOKEN1LEMMA;