CREATE TABLE RELTARGETS AS
  SELECT *
  FROM   (SELECT TOKEN1LEMMA AS LEMMA,
                 TOKEN1TAG   AS TAG
          FROM   WINDOWCOUNT
          WHERE  TOKEN1LEMMA IN (SELECT RELATIONS.TOKEN1 AS LEMMA
                                 FROM   RELATIONS
                                 --WHERE  RELATIONS.CATEGORY = 'HYP'
                                        --AND RELATIONS.TAG LIKE 'N%'
                                 UNION
                                 SELECT RELATIONS.TOKEN2 AS LEMMA
                                 FROM   RELATIONS
                                 --WHERE  RELATIONS.CATEGORY = 'HYP'
                                        --AND RELATIONS.TAG LIKE 'N%'
                                        )
          UNION
          SELECT TOKEN2LEMMA AS LEMMA,
                 TOKEN2TAG   AS TAG
          FROM   WINDOWCOUNT
          WHERE  TOKEN2LEMMA IN (SELECT RELATIONS.TOKEN2 AS LEMMA
                                 FROM   RELATIONS
                                 --WHERE  RELATIONS.CATEGORY = 'HYP'
                                        --AND RELATIONS.TAG LIKE 'N%'
                                 UNION
                                 SELECT RELATIONS.TOKEN2 AS LEMMA
                                 FROM   RELATIONS
                                 --WHERE  RELATIONS.CATEGORY = 'HYP'
                                        --AND RELATIONS.TAG LIKE 'N%'
                                        ));