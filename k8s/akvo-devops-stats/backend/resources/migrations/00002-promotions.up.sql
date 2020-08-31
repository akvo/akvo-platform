CREATE TABLE promotions (
  repository varchar(200) NOT NULL,
  sha varchar(40) NOT NULL,
  name varchar(40) NOT NULL,
  reason varchar(40),
  build_status varchar(200),
  finish_date TIMESTAMPTZ,
  constraint promotion_id unique (repository, sha, name));