CREATE TABLE flips (
  repository varchar(200) NOT NULL,
  sha varchar(40) NOT NULL,
  name varchar(40) NOT NULL,
  finish_date TIMESTAMPTZ,
  constraint flip_id unique (repository, sha, name));