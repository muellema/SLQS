CREATE TABLE GLOBALTEMP AS
  SELECT Cast(EXPERIMENTS.ID AS INTEGER)
         AS
                                    EXPID,
         Cast ((SELECT Sum(W.TUPLECOUNT)
                FROM   WINDOWCOUNT AS W
                WHERE  W.TOKEN1TAG IN (SELECT TAG
                                       FROM   TAGLISTS
                                       WHERE  EXP_ID = EXPERIMENTS.ID)
                       AND W.TOKEN2TAG IN (SELECT TAG
                                           FROM   TAGLISTS
                                           WHERE  EXP_ID = EXPERIMENTS.ID)) AS
               INTEGER)AS
         N
  FROM   EXPERIMENTS; 
