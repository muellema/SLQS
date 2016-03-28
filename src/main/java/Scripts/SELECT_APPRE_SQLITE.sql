SELECT *
FROM   (SELECT COSINE_SIM,
               DIFF,
               PRERES.CATEGORY
        FROM   PRERES,
               COS_SIM
        WHERE  PRERES.EXPID = ?
               AND PRERES.LIM = ?
               AND PRERES.SCORE >= ?
               AND PRERES.SC = 10
               AND PRERES.TOKEN1 = COS_SIM.TOKEN1
               AND PRERES.TOKEN2 = COS_SIM.TOKEN2
               AND PRERES.LEMMA1TAG = COS_SIM.TAG1
               AND PRERES.LEMMA2TAG = COS_SIM.TAG2
               AND PRERES.EXPID = COS_SIM.EXPID)
ORDER  BY ( 1 - COSINE_SIM ) * ( 1 - DIFF ) DESC; 
