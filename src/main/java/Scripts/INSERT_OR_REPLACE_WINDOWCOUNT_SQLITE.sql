INSERT
OR     REPLACE
INTO   WINDOWCOUNT
       (
              TOKEN1LEMMA,
              TOKEN1TAG,
              TOKEN2LEMMA,
              TOKEN2TAG,
              TUPLECOUNT
       )
       VALUES
       (
              ?,
              ?,
              ?,
              ?,
              COALESCE(
                         (
                         SELECT TUPLECOUNT + ?
                         FROM   WINDOWCOUNT
                         WHERE  TOKEN1LEMMA = ?
                         AND    TOKEN1TAG = ?
                         AND    TOKEN2LEMMA = ?
                         AND    TOKEN2TAG = ?), ?)
       );