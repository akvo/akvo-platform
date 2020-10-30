FROM openjdk:11.0.9-jre-slim

RUN set -ex; apt-get update && \
    mkdir -p /usr/share/man/man7/ && \
    apt-get -qq install -y --no-install-recommends --no-install-suggests \
    postgresql-client && \
    rm -rf /var/cache/apt/* /usr/share/man/*

WORKDIR /app
COPY prod/start-prod-env.sh /app/start-prod-env.sh
COPY target/akvo-devops-stats.jar /app/akvo-devops-stats.jar

CMD ["/app/start-prod-env.sh"]
