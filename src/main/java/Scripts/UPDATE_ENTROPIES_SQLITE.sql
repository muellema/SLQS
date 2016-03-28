UPDATE GCOUNT
SET    ENTROPY = (SELECT -Sum(PROB * Log(PROB))
                  FROM   (SELECT 1.0 * TUPLECOUNT / ( C ) AS PROB
                          FROM   WINDOWCOUNT
                          WHERE  WINDOWCOUNT.TOKEN1LEMMA = LEMMA
                                 AND WINDOWCOUNT.TOKEN1TAG = TAG
                                 AND WINDOWCOUNT.TOKEN2TAG IN
                                     (SELECT TAG
                                      FROM   TAGLISTS
                                      WHERE  EXP_ID = EXPID)));