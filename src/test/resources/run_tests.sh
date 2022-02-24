#!/usr/bin/env bash

set -x

QUERY_URI='http://localhost:8983/solr/london/select?country=uk&fl=time%3Atraveltime_f()%2Cid&fq=%7B!traveltime_f%20weight%3D1%7D&mode=pt&q=*%3A*&traveltime_field=coords&traveltime_limit=800&traveltime_origin=51.53%2C-0.15'

curl -s $QUERY_URI | jq '.response.numFound' | xargs test 5 -eq
curl -s $QUERY_URI | jq '.response.docs[0].id' | xargs test n101849 =
