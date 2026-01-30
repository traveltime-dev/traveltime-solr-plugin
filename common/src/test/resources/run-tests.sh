#!/usr/bin/env bash

set -ex

trap "docker logs $IMAGE_NAME; docker stop $IMAGE_NAME; exit 1" EXIT

docker run -d --rm --name $IMAGE_NAME $IMAGE_NAME solr-foreground -a "-Xss4M"
docker exec -u 0 -d $IMAGE_NAME /opt/traveltime/mock-proto-server --port 80
docker exec -u 0 -d $IMAGE_NAME python3 /opt/traveltime/mock-json-server.py 81

while ! grep -q "Server Started Server" <(docker logs $IMAGE_NAME) && ! grep -q "Registered new searcher" <(docker logs $IMAGE_NAME); do
  sleep 1
done

URL='http://localhost:8983/solr/london/select'
DATA_ARGS="\
  --data-urlencode q=*:*\
  --data-urlencode traveltime_field=coords\
  --data-urlencode traveltime_limit=50\
  --data-urlencode traveltime_origin=51.509865,-0.118092\
  --data-urlencode country=uk\
  --data-urlencode mode=pt\
  --data-urlencode wt=json\
"

docker exec $IMAGE_NAME \
  curl -s --fail $DATA_ARGS --data-urlencode fq="{!traveltime weight=1}" --data-urlencode "fl=id" $URL \
  | jq '.response.numFound' | xargs test 224 -eq

docker exec $IMAGE_NAME \
  curl -s --fail $DATA_ARGS --data-urlencode fq="{!traveltime weight=1}" --data-urlencode "fl=id${SCORE_FIELD}" $URL \
  | jq '.response.docs[0].id' | xargs test "n3079325660" ==

docker exec $IMAGE_NAME \
  curl -s --fail $DATA_ARGS --data-urlencode fq="{!traveltime_f weight=1}" --data-urlencode "fl=time:traveltime_f(),id" $URL \
  | jq '.response.numFound' | xargs test 224 -eq

docker exec $IMAGE_NAME \
  curl -s --fail $DATA_ARGS --data-urlencode fq="{!traveltime_f weight=1}" --data-urlencode "fl=time:traveltime_f(),id${SCORE_FIELD}" $URL \
  | jq '.response.docs[0].id' | xargs test "n3079325660" ==

docker exec $IMAGE_NAME \
  curl -s --fail $DATA_ARGS --data-urlencode fq="{!traveltime_e weight=1}" --data-urlencode "fl=time:traveltime_e(),id" $URL \
  | jq '.response.numFound' | xargs test 224 -eq

docker exec $IMAGE_NAME \
  curl -s --fail $DATA_ARGS --data-urlencode fq="{!traveltime_e weight=1}" --data-urlencode "fl=time:traveltime_e(),id${SCORE_FIELD}" $URL \
  | jq '.response.docs[0].id' | xargs test "n3079325660" ==

docker exec $IMAGE_NAME \
  curl -s --fail $DATA_ARGS --data-urlencode fq="{!traveltime_e weight=1}" --data-urlencode "traveltime_distances=true" --data-urlencode "fl=time:traveltime_e(),dist:distance_e(),id${SCORE_FIELD}" $URL \
  | jq '.response.docs[0].time' | xargs test "4" ==

docker exec $IMAGE_NAME \
  curl -s --fail $DATA_ARGS --data-urlencode fq="{!traveltime_e weight=1}" --data-urlencode "traveltime_distances=true" --data-urlencode "fl=time:traveltime_e(),dist:distance_e(),id${SCORE_FIELD}" $URL \
  | jq '.response.docs[0].dist' | xargs test "57" ==

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
  --data-urlencode wt=json\
"

docker exec $IMAGE_NAME \
  curl -s --fail $DUAL_ARGS --data-urlencode "q=*:*" --data-urlencode fq="{!traveltime_driving weight=1}" --data-urlencode fq="{!traveltime_walking weight=1}" --data-urlencode "fl=driving:traveltime_driving(),walking:traveltime_walking(),id" $URL \
  | jq '.response.numFound' | xargs test 221 -eq

docker exec $IMAGE_NAME \
  curl -s --fail $DUAL_ARGS --data-urlencode q="({!traveltime_driving}^0.5 OR {!traveltime_walking}^0.5)" --data-urlencode fq="{!traveltime_driving}" --data-urlencode fq="{!traveltime_walking}" --data-urlencode "fl=id,score" $URL \
  | jq '.response.docs[0].score' | xargs test 0.9259259 ==

# TimeFilter tests (JSON API)
TF_DEPARTURE_ARGS="\
  --data-urlencode q=*:*\
  --data-urlencode timefilter_field=coords\
  --data-urlencode timefilter_travel_time=3000\
  --data-urlencode timefilter_departure_location={\"lat\":51.509865,\"lng\":-0.118092}\
  --data-urlencode timefilter_departure_time=2022-12-19T15:00:00Z\
  --data-urlencode timefilter_transportation={\"type\":\"public_transport\"}\
  --data-urlencode wt=json\
"

# Test TimeFilter basic departure search
docker exec $IMAGE_NAME \
  curl -s --fail $TF_DEPARTURE_ARGS --data-urlencode fq="{!timefilter weight=1}" --data-urlencode "fl=id" $URL \
  | jq '.response.numFound' | xargs test 0 -lt

# Test TimeFilter with exact cache and value source
docker exec $IMAGE_NAME \
  curl -s --fail $TF_DEPARTURE_ARGS --data-urlencode fq="{!timefilter_e weight=1}" --data-urlencode "fl=time:timefilter_e(),id" $URL \
  | jq '.response.numFound' | xargs test 0 -lt

# Test TimeFilter with fuzzy cache
docker exec $IMAGE_NAME \
  curl -s --fail $TF_DEPARTURE_ARGS --data-urlencode fq="{!timefilter_f weight=1}" --data-urlencode "fl=time:timefilter_f(),id" $URL \
  | jq '.response.numFound' | xargs test 0 -lt

# Test TimeFilter with distances
docker exec $IMAGE_NAME \
  curl -s --fail $TF_DEPARTURE_ARGS --data-urlencode fq="{!timefilter_e weight=1}" --data-urlencode "timefilter_distances=true" --data-urlencode "fl=time:timefilter_e(),dist:timefilter_distance_e(),id" $URL \
  | jq '.response.docs[0].time' | xargs test -1 -ne

docker exec $IMAGE_NAME \
  curl -s --fail $TF_DEPARTURE_ARGS --data-urlencode fq="{!timefilter_e weight=1}" --data-urlencode "timefilter_distances=true" --data-urlencode "fl=time:timefilter_e(),dist:timefilter_distance_e(),id" $URL \
  | jq '.response.docs[0].dist' | xargs test -1 -ne

# Test TimeFilter arrival search
TF_ARRIVAL_ARGS="\
  --data-urlencode q=*:*\
  --data-urlencode timefilter_field=coords\
  --data-urlencode timefilter_travel_time=3000\
  --data-urlencode timefilter_arrival_location={\"lat\":51.509865,\"lng\":-0.118092}\
  --data-urlencode timefilter_arrival_time=2022-12-19T15:00:00Z\
  --data-urlencode timefilter_transportation={\"type\":\"public_transport\"}\
  --data-urlencode wt=json\
"

docker exec $IMAGE_NAME \
  curl -s --fail $TF_ARRIVAL_ARGS --data-urlencode fq="{!timefilter_e weight=1}" --data-urlencode "fl=id" $URL \
  | jq '.response.numFound' | xargs test 0 -lt

docker stop $IMAGE_NAME
trap EXIT
