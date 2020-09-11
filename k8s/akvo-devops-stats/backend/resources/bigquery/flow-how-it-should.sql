--This should work and be the most efficient way but some whatever reason _TABLE_SUFFIX is being ignored
--// from date: 20200603
--// to date: 20200609
select resource.labels.project_id as tenant, httpRequest.status as status, count(*) as count, DATE(timestamp) as day
from `http_logs.appengine_googleapis_com_request_log_*`
where httpRequest.status >=500 OR httpRequest.status < 300
AND _TABLE_SUFFIX BETWEEN "$from-date$" and "$to-date$"
group by tenant,status,day

