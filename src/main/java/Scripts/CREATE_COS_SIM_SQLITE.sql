CREATE TABLE COS_SIM AS
  SELECT EXPID,
         TOKEN1,
         TAG1,
         TOKEN2,
         TAG2,
         CAT,
         Sum(C1 * C2) / ( 1.0 * Sqrt ((SELECT CC
                                       FROM   CSUMS
                                       WHERE  CSUMS.EXPID = COSTEST.EXPID
                                              AND TOKEN = TOKEN1
                                              AND TAG = TAG1)) * Sqrt (
                          (SELECT CC
                           FROM   CSUMS
                           WHERE  CSUMS.EXPID = COSTEST.EXPID
                                  AND TOKEN = TOKEN2
                                  AND TAG = TAG2)) ) AS COSINE_SIM
  FROM   COSTEST
  GROUP  BY EXPID,
            TOKEN1,
            TAG1,
            TOKEN2,
            TAG2,
            CAT
  ORDER  BY EXPID,
            TOKEN1,
            TAG1,
            TOKEN2,
            TAG2;
