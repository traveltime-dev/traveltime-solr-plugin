ARG SOLR_VERSION
FROM solr:${SOLR_VERSION}

USER root
# Copy and run script to fix apt repositories for old Debian versions (Jessie, Stretch)
COPY --chmod=755 common/src/test/resources/fix-old-debian-repos.sh /opt/traveltime/fix-old-debian-repos.sh
RUN /opt/traveltime/fix-old-debian-repos.sh
RUN apt-get update && apt-get install -y python3 && rm -rf /var/lib/apt/lists/*
USER solr

COPY --chown=solr common/src/test/resources/part0.json /opt/traveltime/
COPY --chown=solr common/src/test/resources/mock-proto-server /opt/traveltime/
COPY --chown=solr common/src/test/resources/mock-json-server.py /opt/traveltime/

ARG VERSION_DIR
ARG SOLR_CORE_PATH
ARG POST_COMMAND

ENV POST_COMMAND="${POST_COMMAND}"
ENV SOLR_CORE_PATH="${SOLR_CORE_PATH}"
ENV SOLR_SECURITY_MANAGER_ENABLED=false

COPY --chown=solr ${VERSION_DIR}/build/libs/*.jar /opt/traveltime/
COPY --chown=solr --chmod=+x common/src/test/resources/prepare.sh /opt/traveltime/

RUN /opt/traveltime/prepare.sh

