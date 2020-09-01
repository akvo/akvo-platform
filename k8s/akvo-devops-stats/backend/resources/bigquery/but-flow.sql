WITH t1 AS
(select
  IF (NET.REG_DOMAIN(httpRequest.requestUrl) = 'akvoapp.org', 'rsr.akvo.org',
  IF (NET.REG_DOMAIN(httpRequest.requestUrl) = 'akvolumen.org', 'lumen',
  NET.HOST(httpRequest.requestUrl))) as service,
  DATE(timestamp) as day,
  httpRequest.status as status,
  httpRequest.requestMethod
from akvoflow_http_logs.requests
where httpRequest.status != 503
and _PARTITIONTIME >= TIMESTAMP "2020-04-01"
and _PARTITIONTIME < TIMESTAMP "2020-04-02"
and httpRequest.requestUrl not like '%/healthz'
and httpRequest.userAgent not like 'GoogleStackdriv%'
and httpRequest.userAgent not like 'Prometh%'
and httpRequest.userAgent not like '%bot%')

select service, day, status, count(*) as count from t1
where status < 300 or status >=500
group by service,status, day
order by service,day