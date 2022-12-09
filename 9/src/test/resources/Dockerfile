FROM solr:9.1


COPY --chown=solr build/libs/*.jar /var/solr/data/lib/
COPY --chown=solr src/test/resources/part0.json ./
COPY --chown=solr src/test/resources/mock-proto-server ./
