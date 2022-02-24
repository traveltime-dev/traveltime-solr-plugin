#!/usr/bin/env bash

set -x

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

docker exec -d solr ./mock-proto-server 80
docker exec -it solr solr create_core -c london
curl -X POST -H 'Content-type:application/json' -d '{ "add-field": { "name":"coords", "type":"location", "stored":true } }' http://localhost:8983/solr/london/schema
curl -X POST -H 'Content-type:application/json' -d '{"add-cache": {"name": "traveltime_fuzzy", "class": "com.traveltime.plugin.solr.cache.FuzzyRequestCache"}}'  http://localhost:8983/solr/london/config
curl -X POST -H 'Content-type:application/json' -d '{"add-cache": {"name": "traveltime_exact", "class": "com.traveltime.plugin.solr.cache.ExactRequestCache"}}'  http://localhost:8983/solr/london/config
curl -X POST -H 'Content-type:application/json' -d '{"add-queryparser": {"name": "traveltime_e", "class": "com.traveltime.plugin.solr.TraveltimeQParserPlugin", "api_uri": "http://localhost/", "app_id": "id", "api_key": "key", "cache": "traveltime_exact"}}' http://localhost:8983/solr/london/config
curl -X POST -H 'Content-type:application/json' -d '{"add-queryparser": {"name": "traveltime_f", "class": "com.traveltime.plugin.solr.TraveltimeQParserPlugin", "api_uri": "http://localhost/", "app_id": "id", "api_key": "key", "cache": "traveltime_fuzzy"}}' http://localhost:8983/solr/london/config
curl -X POST -H 'Content-type:application/json' -d '{"add-valuesourceparser": {"name": "traveltime_e", "class": "com.traveltime.plugin.solr.query.TraveltimeValueSourceParser", "cache": "traveltime_exact"}}' http://localhost:8983/solr/london/config
curl -X POST -H 'Content-type:application/json' -d '{"add-valuesourceparser": {"name": "traveltime_f", "class": "com.traveltime.plugin.solr.query.TraveltimeValueSourceParser", "cache": "traveltime_fuzzy"}}' http://localhost:8983/solr/london/config

curl -X POST -d @$SCRIPT_DIR/sample_data.json http://localhost:8983/solr/london/update/json/docs?commit=true
