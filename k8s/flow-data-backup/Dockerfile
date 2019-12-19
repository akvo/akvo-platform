FROM google/cloud-sdk:258.0.0

RUN set -ex; apt-get update && \
    apt-get -qq install -y --no-install-recommends --no-install-suggests \
    jq python-openssl python3-openssl python3-pip && \
    rm -rf /var/lib/apt/lists/*

RUN curl -sL https://sentry.io/get-cli/ | bash
RUN pip3 install google-cloud-datastore

WORKDIR /usr/src/app

COPY src /usr/src/app
CMD ["bash", "./backup_all.sh"]
