FROM alpine:3.12.1

RUN apk add --no-cache curl==7.69.1-r1 jq==1.6-r1 bash==5.0.17-r0

COPY pull-reminders.sh .

CMD ["/bin/bash", "./pull-reminders.sh"]
