-- :name insert-promotion! :!
INSERT INTO promotions (repository, sha, name, reason)
VALUES (:repository, :sha, :name, :reason)

-- :name update-promotion-with-build-status! :!
UPDATE promotions
 SET
    finish_date = :finish-date,
    build_status = :build-status
WHERE
    repository = :repository AND
    sha = :sha AND
    name = :name

-- :name get-promotions-for-repository :? :*
SELECT * FROM promotions where repository = :repository ORDER BY name DESC

-- :name get-last-promotion-for-repository :? :1
SELECT * FROM promotions where repository = :repository ORDER BY name DESC LIMIT 1