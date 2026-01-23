FROM solr:9.1

USER root
RUN apt-get update && apt-get install -y python3 && rm -rf /var/lib/apt/lists/*
USER solr

ENV SOLR_SECURITY_MANAGER_ENABLED=false

COPY --chown=solr build/libs/*.jar /var/solr/data/lib/
COPY --chown=solr src/test/resources/part0.json ./
COPY --chown=solr src/test/resources/mock-proto-server ./
COPY --chown=solr src/test/resources/mock-json-server.py ./
