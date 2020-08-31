-- :name upsert-flip! :!
INSERT INTO flips (repository, sha, finish_date, name)
VALUES (:repository, :sha, :finish-date, :name)
ON CONFLICT ON CONSTRAINT flip_id
DO
  UPDATE
    SET finish_date = :finish-date
RETURNING *

-- :name get-flips-for-repo :? :*
SELECT * FROM flips where repository=:repository ORDER BY name DESC

-- :name get-last-flip-for-repo :? :1
SELECT * FROM flips where repository=:repository ORDER BY name DESC LIMIT 1