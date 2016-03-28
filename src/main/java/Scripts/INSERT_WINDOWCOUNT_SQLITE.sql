INSERT INTO WINDOWCOUNT
            (TOKEN1TAG,
             TOKEN1LEMMA,
             TOKEN2TAG,
             TOKEN2LEMMA,
             TUPLECOUNT)
SELECT TOKEN1TAG,
       TOKEN1LEMMA,
       TOKEN2TAG,
       TOKEN2LEMMA,
       Sum (C) AS C
FROM   (SELECT TOKEN1TAG,
               TOKEN1LEMMA,
               TOKEN2TAG,
               TOKEN2LEMMA,
               Sum(TUPLECOUNT) AS C
        FROM   WINDOWS
        --WHERE  TOKEN1TAG LIKE 'N%'
        GROUP  BY TOKEN1TAG,
                  TOKEN1LEMMA,
                  TOKEN2TAG,
                  TOKEN2LEMMA
        UNION ALL
        SELECT TOKEN2TAG,
               TOKEN2LEMMA,
               TOKEN1TAG,
               TOKEN1LEMMA,
               Sum(TUPLECOUNT) AS C
        FROM   WINDOWS
        --WHERE  TOKEN2TAG LIKE 'N%'
        GROUP  BY TOKEN1TAG,
                  TOKEN1LEMMA,
                  TOKEN2TAG,
                  TOKEN2LEMMA)
GROUP  BY TOKEN1TAG,
          TOKEN1LEMMA,
          TOKEN2TAG,
          TOKEN2LEMMA
ORDER  BY C DESC; 
