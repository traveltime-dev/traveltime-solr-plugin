ARG SOLR_VERSION

FROM solr:${SOLR_VERSION}

ARG VERSION_DIR
ARG SOLR_LIB_PATH

USER root
RUN apt-get update && apt-get install -y python3 && rm -rf /var/lib/apt/lists/*
USER solr

ENV SOLR_SECURITY_MANAGER_ENABLED=false

COPY --chown=solr ${VERSION_DIR}/build/libs/*.jar ${SOLR_LIB_PATH}/
COPY --chown=solr common/src/test/resources/part0.json ./
COPY --chown=solr common/src/test/resources/mock-proto-server ./
COPY --chown=solr common/src/test/resources/mock-json-server.py ./
