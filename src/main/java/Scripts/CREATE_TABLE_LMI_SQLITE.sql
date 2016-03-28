CREATE TEMP TABLE LMI AS
  SELECT Cast(EXPERIMENTS.ID AS INTEGER) AS EXPID,
         Cast(WINDOWCOUNT.ID AS INTEGER) AS WID,
         Cast(WINDOWCOUNT.TUPLECOUNT * Log (WINDOWCOUNT.TUPLECOUNT *
                                            (
                                                      1.0 *
                                            (SELECT C
                                                   FROM   GCOUNT
                                                   WHERE
                                                 WINDOWCOUNT.TOKEN2LEMMA =
                                                 GCOUNT.LEMMA
                                                 AND WINDOWCOUNT.TOKEN2TAG =
                                                     GCOUNT.TAG
                                                 AND GCOUNT.EXPID =
                                                     EXPERIMENTS.ID) *
                                                                (
                                                      SELECT
                                                                               C
                                                      FROM
                                                      GCOUNT
                                                      WHERE
              WINDOWCOUNT.TOKEN1LEMMA = GCOUNT.LEMMA
              AND WINDOWCOUNT.TOKEN1TAG = GCOUNT.TAG
              AND GCOUNT.EXPID = EXPERIMENTS.ID) / (
                        SELECT
              N
                        FROM
              GLOBALTEMP
                        WHERE
              GLOBALTEMP.EXPID = EXPERIMENTS.ID) ))
              AS REAL
         )                               AS LMI
  FROM   WINDOWCOUNT,
         EXPERIMENTS
  WHERE  WINDOWCOUNT.TOKEN1LEMMA IN (SELECT RELTARGETS.LEMMA
                                     FROM   RELTARGETS
                                     WHERE  RELTARGETS.TAG IN
                                            (SELECT TAG
                                             FROM   TAGLISTS
                                             WHERE  EXP_ID = EXPERIMENTS.ID))
  ORDER  BY WINDOWCOUNT.TOKEN1LEMMA;
