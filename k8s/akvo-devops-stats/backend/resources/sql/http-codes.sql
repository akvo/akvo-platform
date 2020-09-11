-- :name insert-http-code! :!
INSERT INTO http_codes (service, tenant, day, year_week, year_month, http_code, times)
VALUES (:service, :tenant, :day, :year-week, :year-month, :http-code, :times)

-- :name last-day-with-stats :? :1
SELECT day from http_codes order by day DESC limit 1

-- :name stats-per-week :? :*
SELECT service,year_week,year_month,sum(times),
            CASE WHEN http_code<500 THEN 'ok'
            ELSE 'error' END as simple_status
FROM http_codes
GROUP BY service,year_week,year_month,simple_status

-- :name stats-per-day-since :? :*
SELECT service,day, sum(times),
            CASE WHEN http_code<500 THEN 'ok'
            ELSE 'error' END as simple_status
FROM http_codes
WHERE day >= :day
GROUP BY service, day, simple_status