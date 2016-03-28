CREATE TABLE TEMPSLQS AS
  SELECT RELATIONS.TOKEN1,
         EV1.TAG                   AS TAG1,
         RELATIONS.TOKEN2,
         EV2.TAG                   AS TAG2,
         ( 1 - ( EV1.E / EV2.E ) ) AS SLQS,
         SCORE, EV1.LIM AS LIM, EV1.LIMTYPE AS LIMTYPE
  FROM   EVALUES AS EV1,
         EVALUES AS EV2,
         RELATIONS
  WHERE  RELATIONS.CATEGORY = 'HYP'
         --AND RELATIONS.TAG LIKE 'N%'
         AND RELATIONS.TOKEN1 = EV1.LEMMA
         AND RELATIONS.TOKEN2 = EV2.LEMMA
         AND EV1.LIM = EV2.LIM
         AND EV1.LIMTYPE = EV2.LIMTYPE;