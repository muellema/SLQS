SELECT TOK1
       || TAG
       || TOK2 AS CONCAT
FROM   (SELECT COS_SIM.TOKEN1                    AS TOK1,
               COS_SIM.TAG2                      AS TAG,
               COS_SIM.TOKEN2                    AS TOK2,
               COSINE_SIM,
               ( E1.E / E2.E ) AS SLQS
        FROM   RELATIONS,
               COS_SIM,
               EVALUES AS E1,
               EVALUES AS E2
        WHERE  RELATIONS.TAG = E1.TAG
               AND RELATIONS.TAG = E2.TAG
               AND RELATIONS.TOKEN1 = COS_SIM.TOKEN1
               AND RELATIONS.TOKEN2 = COS_SIM.TOKEN2
               AND E1.LIM = E2.LIM
               AND E1.LIM = ?
               AND E1.EXPID = E2.EXPID
               AND E1.EXPID = ?
               AND E1.E NOT NULL
               AND COS_SIM.EXPID = E1.EXPID
               AND E1.LEMMA = COS_SIM.TOKEN1
               AND E1.TAG = TAG1
               AND E2.LEMMA = COS_SIM.TOKEN2
               AND E2.TAG = TAG2
               AND E2.E NOT NULL
               AND COS_SIM.CAT = RELATIONS.CATEGORY
               AND RELATIONS.SCORE >= ?
               ?
        ORDER  BY ? ?);
