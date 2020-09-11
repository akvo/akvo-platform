select resource.labels.project_id as tenant, httpRequest.status as status, count(*) as count, DATE(timestamp) as day
from `http_logs.appengine_googleapis_com_request_log_$date$`
where httpRequest.status >=500 OR httpRequest.status < 300
group by tenant,status,day