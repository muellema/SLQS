CREATE TABLE TEMPACC AS 
SELECT Cast(T.SCORE AS INTEGER) AS SCORE,
       Cast(T.LIM AS   INTEGER) AS LIM,
       Cast(T.LIMTYPE AS   TEXT) AS LIMTYPE,
       Cast(
              (
              SELECT Count()
              FROM   TEMPSLQS
              WHERE  SLQS > 0.0
              AND    TEMPSLQS.SCORE >= T.SCORE
              AND    TEMPSLQS.LIM = T.LIM
              AND    TEMPSLQS.LIMTYPE = T.LIMTYPE) / ( 1.0 *
            (
                   SELECT Count()
                   FROM   TEMPSLQS
                   WHERE  TEMPSLQS.SCORE >= T.SCORE
                   AND    TEMPSLQS.LIM = T.LIM
                   AND    TEMPSLQS.LIMTYPE = T.LIMTYPE) ) AS REAL) AS ACC
FROM   (
              SELECT *
              FROM   (
                                     SELECT DISTINCT SCORE
                                     FROM            RELATIONS
                                     WHERE           CATEGORY = 'HYP'
                                     AND             TAG LIKE 'N%') AS T
              JOIN
                     (
                                     SELECT DISTINCT EVALUES.LIM
                                     FROM            EVALUES )
              JOIN
                     (
                                     SELECT DISTINCT EVALUES.LIMTYPE
                                     FROM            EVALUES)) AS T;