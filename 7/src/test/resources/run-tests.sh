#!/usr/bin/env bash

set -ex

trap "docker stop $IMAGE_NAME; exit 1" EXIT

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

docker run -d --rm --name $IMAGE_NAME $IMAGE_NAME solr-fg -a "-Xss4M"
docker exec -d $IMAGE_NAME ./mock-proto-server --port 80
docker exec $IMAGE_NAME bash -c 'sleep 5; solr create_core -c london'
docker exec $IMAGE_NAME curl -s -o /dev/null -X POST -H 'Content-type:application/json' -d '{"add-field": {"name":"coords", "type":"location", "stored":true}}' http://localhost:8983/solr/london/schema
docker exec $IMAGE_NAME curl -s -o /dev/null -X POST -H 'Content-type:application/json' -d '{"add-cache": {"name": "traveltime_fuzzy", "class": "com.traveltime.plugin.solr.cache.FuzzyRequestCache", "secondary_size": "150000"}}'  http://localhost:8983/solr/london/config
docker exec $IMAGE_NAME curl -s -o /dev/null -X POST -H 'Content-type:application/json' -d '{"add-cache": {"name": "traveltime_exact", "class": "com.traveltime.plugin.solr.cache.ExactRequestCache"}}'  http://localhost:8983/solr/london/config
docker exec $IMAGE_NAME curl -s -o /dev/null -X POST -H 'Content-type:application/json' -d '{"add-queryparser": {"name": "traveltime", "class": "com.traveltime.plugin.solr.TraveltimeQParserPlugin", "api_uri": "http://localhost/", "app_id": "id", "api_key": "key"}}' http://localhost:8983/solr/london/config
docker exec $IMAGE_NAME curl -s -o /dev/null -X POST -H 'Content-type:application/json' -d '{"add-queryparser": {"name": "traveltime_e", "class": "com.traveltime.plugin.solr.TraveltimeQParserPlugin", "api_uri": "http://localhost/", "app_id": "id", "api_key": "key", "cache": "traveltime_exact"}}' http://localhost:8983/solr/london/config
docker exec $IMAGE_NAME curl -s -o /dev/null -X POST -H 'Content-type:application/json' -d '{"add-queryparser": {"name": "traveltime_f", "class": "com.traveltime.plugin.solr.TraveltimeQParserPlugin", "api_uri": "http://localhost/", "app_id": "id", "api_key": "key", "cache": "traveltime_fuzzy"}}' http://localhost:8983/solr/london/config
docker exec $IMAGE_NAME curl -s -o /dev/null -X POST -H 'Content-type:application/json' -d '{"add-valuesourceparser": {"name": "traveltime_e", "class": "com.traveltime.plugin.solr.query.TraveltimeValueSourceParser", "cache": "traveltime_exact"}}' http://localhost:8983/solr/london/config
docker exec $IMAGE_NAME curl -s -o /dev/null -X POST -H 'Content-type:application/json' -d '{"add-valuesourceparser": {"name": "traveltime_f", "class": "com.traveltime.plugin.solr.query.TraveltimeValueSourceParser", "cache": "traveltime_fuzzy"}}' http://localhost:8983/solr/london/config

docker exec $IMAGE_NAME curl -s -o /dev/null -X POST -H 'Content-type:application/json' -d '{"add-queryparser": {"name": "traveltime_driving", "prefix": "driving_", "class": "com.traveltime.plugin.solr.TraveltimeQParserPlugin", "api_uri": "http://localhost/", "app_id": "id", "api_key": "key", "cache": "traveltime_exact"}}' http://localhost:8983/solr/london/config
docker exec $IMAGE_NAME curl -s -o /dev/null -X POST -H 'Content-type:application/json' -d '{"add-queryparser": {"name": "traveltime_walking", "prefix": "walking_",  "class": "com.traveltime.plugin.solr.TraveltimeQParserPlugin", "api_uri": "http://localhost/", "app_id": "id", "api_key": "key", "cache": "traveltime_exact"}}' http://localhost:8983/solr/london/config
docker exec $IMAGE_NAME curl -s -o /dev/null -X POST -H 'Content-type:application/json' -d '{"add-valuesourceparser": {"name": "traveltime_driving", "prefix": "driving_", "class": "com.traveltime.plugin.solr.query.TraveltimeValueSourceParser", "cache": "traveltime_exact"}}' http://localhost:8983/solr/london/config
docker exec $IMAGE_NAME curl -s -o /dev/null -X POST -H 'Content-type:application/json' -d '{"add-valuesourceparser": {"name": "traveltime_walking", "prefix": "walking_","class": "com.traveltime.plugin.solr.query.TraveltimeValueSourceParser", "cache": "traveltime_exact"}}' http://localhost:8983/solr/london/config

docker exec $IMAGE_NAME curl -s -o /dev/null -X POST -H 'Content-type:application/json' -d '{"add-queryparser": {"name": "traveltime_nofilter", "class": "com.traveltime.plugin.solr.TraveltimeQParserPlugin", "api_uri": "http://localhost/", "app_id": "id", "api_key": "key", "filtering_disabled": true}}' http://localhost:8983/solr/london/config

docker exec $IMAGE_NAME post -c london part0.json

URL='http://localhost:8983/solr/london/select'
DATA_ARGS="\
  --data-urlencode q=*:*\
  --data-urlencode traveltime_field=coords\
  --data-urlencode traveltime_limit=50\
  --data-urlencode traveltime_origin=51.509865,-0.118092\
  --data-urlencode country=uk\
  --data-urlencode mode=pt\
"

docker exec $IMAGE_NAME \
  curl -s --fail $DATA_ARGS --data-urlencode fq="{!traveltime weight=1}" --data-urlencode "fl=id" $URL \
  | jq '.response.numFound' | xargs test 224 -eq

docker exec $IMAGE_NAME \
  curl -s --fail $DATA_ARGS --data-urlencode fq="{!traveltime weight=1}" --data-urlencode "fl=id" $URL \
  | jq '.response.docs[0].id' | xargs test "n3079325660" ==

docker exec $IMAGE_NAME \
  curl -s --fail $DATA_ARGS --data-urlencode fq="{!traveltime_f weight=1}" --data-urlencode "fl=time:traveltime_f(),id" $URL \
  | jq '.response.numFound' | xargs test 224 -eq

docker exec $IMAGE_NAME \
  curl -s --fail $DATA_ARGS --data-urlencode fq="{!traveltime_f weight=1}" --data-urlencode "fl=time:traveltime_f(),id" $URL \
  | jq '.response.docs[0].id' | xargs test "n3079325660" ==

docker exec $IMAGE_NAME \
  curl -s --fail $DATA_ARGS --data-urlencode fq="{!traveltime_e weight=1}" --data-urlencode "fl=time:traveltime_e(),id" $URL \
  | jq '.response.numFound' | xargs test 224 -eq

docker exec $IMAGE_NAME \
  curl -s --fail $DATA_ARGS --data-urlencode fq="{!traveltime_e weight=1}" --data-urlencode "fl=time:traveltime_e(),id" $URL \
  | jq '.response.docs[0].id' | xargs test "n3079325660" ==

docker exec $IMAGE_NAME \
  curl -s --fail $DATA_ARGS --data-urlencode fq="{!traveltime_nofilter weight=1}" --data-urlencode "fl=id" $URL \
  | jq '.response.numFound' | xargs test 103821 -eq

DUAL_ARGS="\
  --data-urlencode walking_field=coords\
  --data-urlencode walking_limit=50\
  --data-urlencode walking_origin=51.509865,-0.118092\
  --data-urlencode walking_country=uk\
  --data-urlencode walking_mode=walking\
  --data-urlencode driving_field=coords\
  --data-urlencode driving_limit=50\
  --data-urlencode driving_origin=51.5098,-0.1180\
  --data-urlencode driving_country=uk\
  --data-urlencode driving_mode=driving\
"

docker exec $IMAGE_NAME \
  curl -s --fail $DUAL_ARGS --data-urlencode "q=*:*" --data-urlencode fq="{!traveltime_driving weight=1}" --data-urlencode fq="{!traveltime_walking weight=1}" --data-urlencode "fl=driving:traveltime_driving(),walking:traveltime_walking(),id" $URL \
  | jq '.response.numFound' | xargs test 221 -eq

docker exec $IMAGE_NAME \
  curl -s --fail $DUAL_ARGS --data-urlencode q="({!traveltime_driving}^0.5 OR {!traveltime_walking}^0.5)" --data-urlencode fq="{!traveltime_driving}" --data-urlencode fq="{!traveltime_walking}" --data-urlencode "fl=id,score" $URL \
  | jq '.response.docs[0].score' | xargs test 0.9259259 ==

docker stop $IMAGE_NAME
trap EXIT
