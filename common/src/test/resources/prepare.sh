#!/bin/bash
set -ex

if command -v init-var-solr > /dev/null 2>&1 ; then
  init-var-solr
fi

# Create the test core
logs=$(mktemp -p /tmp solr_logs.XXXXXX)
solr-precreate london > $logs &
PRECREATE_PID=$!

sleep 1

# Wait until the text "Server Started Server" appears in the solr log file
while ! grep -q "Server Started Server" <(cat $logs) && ! grep -q "Registered new searcher" <(cat $logs); do
  sleep 1
done

# Create the lib directory for the test core and copy the plugin
LIB_PATH="$SOLR_CORE_PATH/london/lib/"
mkdir -p $LIB_PATH
cp /opt/traveltime/*.jar $LIB_PATH

# Reload the core to pick up the new jar
curl --fail -s -o /dev/null "http://localhost:8983/solr/admin/cores?action=RELOAD&core=london"

sleep 1


function solr_configure() {
  #local variable endpoint, second argument or config by default
  local endpoint=${2:-config}

  curl --fail -s -o /dev/null -X POST -H 'Content-type:application/json' -d "$1" "http://localhost:8983/solr/london/$endpoint"
}

function solr_post() {
  bash -c "${POST_COMMAND} -c london $1"
}

# Coords field
solr_configure '{"add-field": {"name":"coords", "type":"location", "stored":true}}' schema

# Caches
solr_configure '{"add-cache": {"name": "traveltime_fuzzy", "class": "com.traveltime.plugin.solr.cache.FuzzyRequestCache", "secondary_size": "150000"}}'
solr_configure '{"add-cache": {"name": "traveltime_exact", "class": "com.traveltime.plugin.solr.cache.ExactRequestCache"}}'
solr_configure '{"add-cache": {"name": "timefilter_fuzzy", "class": "com.traveltime.plugin.solr.cache.FuzzyTimeFilterRequestCache", "secondary_size": "150000"}}'
solr_configure '{"add-cache": {"name": "timefilter_exact", "class": "com.traveltime.plugin.solr.cache.ExactTimeFilterRequestCache"}}'

# Proto queryparsers
solr_configure '{"add-queryparser": {"name": "traveltime", "class": "com.traveltime.plugin.solr.TravelTimeQParserPlugin", "api_uri": "http://localhost/", "app_id": "id", "api_key": "key"}}'
solr_configure '{"add-queryparser": {"name": "traveltime_e", "class": "com.traveltime.plugin.solr.TravelTimeQParserPlugin", "api_uri": "http://localhost/", "app_id": "id", "api_key": "key", "cache": "traveltime_exact"}}'
solr_configure '{"add-queryparser": {"name": "traveltime_f", "class": "com.traveltime.plugin.solr.TravelTimeQParserPlugin", "api_uri": "http://localhost/", "app_id": "id", "api_key": "key", "cache": "traveltime_fuzzy"}}'
solr_configure '{"add-valuesourceparser": {"name": "traveltime_e", "class": "com.traveltime.plugin.solr.query.TravelTimeValueSourceParser", "cache": "traveltime_exact"}}'
solr_configure '{"add-valuesourceparser": {"name": "distance_e", "class": "com.traveltime.plugin.solr.query.DistanceValueSourceParser", "cache": "traveltime_exact"}}'
solr_configure '{"add-valuesourceparser": {"name": "traveltime_f", "class": "com.traveltime.plugin.solr.query.TravelTimeValueSourceParser", "cache": "traveltime_fuzzy"}}'

# Dual queryparsers
solr_configure '{"add-queryparser": {"name": "traveltime_driving", "prefix": "driving_", "class": "com.traveltime.plugin.solr.TravelTimeQParserPlugin", "api_uri": "http://localhost/", "app_id": "id", "api_key": "key", "cache": "traveltime_exact"}}'
solr_configure '{"add-queryparser": {"name": "traveltime_walking", "prefix": "walking_",  "class": "com.traveltime.plugin.solr.TravelTimeQParserPlugin", "api_uri": "http://localhost/", "app_id": "id", "api_key": "key", "cache": "traveltime_exact"}}'
solr_configure '{"add-valuesourceparser": {"name": "traveltime_driving", "prefix": "driving_", "class": "com.traveltime.plugin.solr.query.TravelTimeValueSourceParser", "cache": "traveltime_exact"}}'
solr_configure '{"add-valuesourceparser": {"name": "traveltime_walking", "prefix": "walking_","class": "com.traveltime.plugin.solr.query.TravelTimeValueSourceParser", "cache": "traveltime_exact"}}'

# No-filter queryparser
solr_configure '{"add-queryparser": {"name": "traveltime_nofilter", "class": "com.traveltime.plugin.solr.TravelTimeQParserPlugin", "api_uri": "http://localhost/", "app_id": "id", "api_key": "key", "filtering_disabled": true}}'

# JSON queryparsers
solr_configure '{"add-queryparser": {"name": "timefilter", "class": "com.traveltime.plugin.solr.TimeFilterQParserPlugin", "api_uri": "http://localhost:81/v4/", "app_id": "id", "api_key": "key", "prefix": "timefilter_"}}'
solr_configure '{"add-queryparser": {"name": "timefilter_e", "class": "com.traveltime.plugin.solr.TimeFilterQParserPlugin", "api_uri": "http://localhost:81/v4/", "app_id": "id", "api_key": "key", "cache": "timefilter_exact", "prefix": "timefilter_"}}'
solr_configure '{"add-queryparser": {"name": "timefilter_f", "class": "com.traveltime.plugin.solr.TimeFilterQParserPlugin", "api_uri": "http://localhost:81/v4/", "app_id": "id", "api_key": "key", "cache": "timefilter_fuzzy", "prefix": "timefilter_"}}'
solr_configure '{"add-valuesourceparser": {"name": "timefilter_e", "class": "com.traveltime.plugin.solr.query.timefilter.TimeFilterValueSourceParser", "cache": "timefilter_exact", "prefix": "timefilter_"}}'
solr_configure '{"add-valuesourceparser": {"name": "timefilter_distance_e", "class": "com.traveltime.plugin.solr.query.timefilter.DistanceValueSourceParser", "cache": "timefilter_exact", "prefix": "timefilter_"}}'
solr_configure '{"add-valuesourceparser": {"name": "timefilter_f", "class": "com.traveltime.plugin.solr.query.timefilter.TimeFilterValueSourceParser", "cache": "timefilter_fuzzy", "prefix": "timefilter_"}}'

solr_post /opt/traveltime/part0.json

sleep 1

kill $PRECREATE_PID || true

