-- :name insert-commit! :!
INSERT INTO commits (repository, sha, authored_date, message_headline, first_pr_sha, first_pr_authored_date)
VALUES (:repository, :sha, :authored-date, :message-headline, :first-pr-sha, :first-pr-authored-date)

-- :name get-commits-for-repo :? :*
SELECT * FROM commits where repository=:repository order by index DESC

-- :name last-commit-for-repo :? :1
SELECT * FROM commits where repository=:repository order by index DESC LIMIT 1