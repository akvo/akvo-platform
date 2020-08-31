CREATE TABLE commits (
  repository varchar(200) NOT NULL,
  sha varchar(40) NOT NULL,
  authored_date TIMESTAMPTZ NOT NULL,
  message_headline varchar(200),
  first_pr_sha varchar(40),
  first_pr_authored_date TIMESTAMPTZ,
  index SERIAL,
  constraint commit_id unique (repository, sha));