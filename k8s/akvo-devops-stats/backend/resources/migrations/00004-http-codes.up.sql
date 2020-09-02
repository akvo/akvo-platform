CREATE TABLE http_codes (
  service varchar(200) NOT NULL,
  tenant varchar(200),
  day varchar(14) NOT NULL,
  year_week varchar(14) NOT NULL,
  year_month varchar(14) NOT NULL,
  http_code bigint NOT NULL,
  times bigint NOT NULL,
  constraint http_codes_id unique (service, tenant, day, http_code)
  );

CREATE UNIQUE INDEX http_codes_null ON http_codes (service, (tenant IS NULL), day, http_code) WHERE tenant IS NULL;
