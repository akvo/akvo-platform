-- :name insert-http-code! :!
INSERT INTO http_codes (service, tenant, day, year_week, year_month, http_code, times)
VALUES (:service, :tenant, :day, :year-week, :year-month, :http-code, :times)

-- :name last-day-with-stats :? :1
SELECT day from http_codes order by day DESC limit 1