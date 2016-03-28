CREATE TABLE GCOUNT AS
  SELECT TOKEN1LEMMA                   AS LEMMA,
         TOKEN1TAG                     AS TAG,
         Cast(Sum (TUPLECOUNT) AS INT) AS C,
         EXPERIMENTS.ID                AS EXPID,
         CAST(NULL AS REAL) AS ENTROPY
  FROM   WINDOWCOUNT,
         EXPERIMENTS
  WHERE  TOKEN2TAG IN (SELECT TAG
                       FROM   TAGLISTS
                       WHERE  EXP_ID = EXPERIMENTS.ID)
  GROUP  BY TOKEN1LEMMA,
            TOKEN1TAG,
            EXPID
  ORDER  BY TOKEN1LEMMA;