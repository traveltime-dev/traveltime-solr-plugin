FROM solr:7.7.3

USER root
RUN apt-get update && apt-get install -y python3 && rm -rf /var/lib/apt/lists/*
USER solr

COPY build/libs/*.jar server/solr/lib/
COPY src/test/resources/part0.json ./
COPY src/test/resources/mock-proto-server ./
COPY src/test/resources/mock-json-server.py ./
