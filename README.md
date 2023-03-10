# traveltime-solr-plugin
Plugin for Solr that allows users to filter locations using the TravelTime API.

## Installation & configuration
This is a standard Solr plugin.
The plugin jar must be copied into the [solr lib directory](https://solr.apache.org/guide/8_4/libs.html#lib-directories)

To use the plugin you **must** add a `queryParser` with the class `com.traveltime.plugin.solr.TravelTimeQParserPlugin` or
`com.traveltime.plugin.solr.TimeFilterQParserPlugin`.
The `TravelTimeQParserPlugin` uses the [Travel Time Matrix Fast (Proto)](https://docs.traveltime.com/api/reference/travel-time-distance-matrix-proto) endpoint.
This is the recommended way to use our plugin due to its very low latency and high location volume per request.
The `TimeFilterQParserPlugin` uses the [Travel Time Matrix (Time Filter)](https://docs.traveltime.com/api/reference/travel-time-distance-matrix) endpoint.
This is more configurable and supports more countries.

These query parsers has two mandatory string configuration options:
- `app_id`: this is you API app id.
- `api_key`: this is the api key that corresponds to the app id.

The `TimeFilterQParserPlugin` has an optional integer field `location_limit` which represents the maximum amount of locations
that can be sent in a single request. Defaults to 2000, only increase this parameter if you API plan supports larger requests.
```xml
<queryParser name="traveltime" class="com.traveltime.plugin.solr.TravelTimeQParserPlugin">
  <str name="app_id">your_app_id_here</str>
  <str name="api_key">your_api_key_here</str>
</queryParser>
```

To display the travel times returned by the TravelTime API you must configure two more components: a `valueSourceParser`, one of:
```xml
<valueSourceParser name="traveltime" class="com.traveltime.plugin.solr.query.TravelTimeValueSourceParser" />
<valueSourceParser name="traveltime" class="com.traveltime.plugin.solr.query.timefilter.TimeFilterValueSourceParser" />
```
and a `cache`, one of:
```xml
<cache name="traveltime" class="com.traveltime.plugin.solr.cache.ExactRequestCache"/>
<cache name="traveltime" class="com.traveltime.plugin.solr.cache.ExactTimeFilterRequestCache"/>
<cache name="traveltime" class="com.traveltime.plugin.solr.cache.FuzzyRequestCache" secondary_size="50000"/>
<cache name="traveltime" class="com.traveltime.plugin.solr.cache.FuzzyTimeFilterRequestCache" secondary_size="50000"/>
```

## Querying data using proto time-filter/fast requests
The traveltime query can only be used as a filter query, and can only be used with fields that are indexed as `location`.
The query accepts the following (mandatory) configuration options:
- `origin`: the point from which travel time will be measured. The accepted formats are:
    - `"lat,lon"` string
    - geohash
- `field`: the document field that will be used as the destination in the TravelTime query.
- `limit`: the travel time limit in seconds. Must be non-negative.
- `mode`: Transportation mode used in the search. One of: `pt`, `walking+ferry`, `cycling+ferry`, `driving+ferry`.
- `country`: Country that the `origin` is in. Currently may only be one of: `uk`, `nl`, `at`, `be`, `de`, `fr`, `ie`, `lt`.

The configuration options may be passed as local query parameters: `?fq={!traveltime origin=51.53,-0.15 field=coords limit=7200 mode=pt country=uk}`, or as raw query parameters prefixed with `"traveltime_"`: `?fq={!traveltime}&traveltime_origin=51.53,-0.15&traveltime_field=coords&traveltime_limit=7200&traveltime_mode=pt&traveltime_country=uk}`.
If a parameter is specified in both ways, the local parameter takes precedence.

## Querying data using json time-filter requests
The query accepts the following configuration options:
- `field`: the document field that will be used as the destination in the TravelTime query.
- `travel_time`: the travel time limit in seconds. Must be non-negative.
- For arrival searches:
  - `arrival_time`: arrival time in ISO8601
  - `arrival_location`: string containing a JSON object with `lat` and `lng` keys describing the coordinates 
    of the arrival location
- For departure searches:
  - `departure_time`: departure time in ISO8601
  - `departure_location`: string containing a JSON object with `lat` and `lng` keys describing the coordinates
    of the departure location
- `transportation`: a string containing a JSON object describing the transportation mode as defined by the TravelTime API:
  https://docs.traveltime.com/api/reference/travel-time-distance-matrix#departure_searches-transportation
- (optional) `range`: : a string containing a JSON object describing the range search as defined by the TravelTime API:
  https://docs.traveltime.com/api/reference/travel-time-distance-matrix#departure_searches-range

An example query using `curl`:
```shell
curl
  --data-urlencode 'q=*:*'
  --data-urlencode 'traveltime_field=coords'
  --data-urlencode 'traveltime_travel_time=3000'
  --data-urlencode 'traveltime_arrival_location={"lat":51.536067,"lng":-0.153596}'
  --data-urlencode 'traveltime_arrival_time=2022-12-19T15:00:00Z'
  --data-urlencode 'transportation={"type":"public_transport"}'
  --data-urlencode fq="{!traveltime weight=1}"
  $URL
```

The configuration options may be passed as local query parameters, or as raw query parameters prefixed with `"traveltime_"`.
If a parameter is specified in both ways, the local parameter takes precedence.

## Displaying travel times

To display the travel times you must configure the `valueSourceParser` and `cache`.
When configured, the time can be accessed using the `fl` parameter: `?fl=time:traveltime()`.
The `valueSourceParser` accepts the same parameters as a query, but only in the raw query parameter form.
If no travel time is found in the cache it will be returned as `-1`.

## Request caches

Request caches can be used to enable the `valueSourceParser` and to reduce request latency for some workloads.
The plugin provides two request cache implementations: `ExactRequestCache` and `FuzzyRequestCache`.

`ExactRequestCache` uses all of the traveltime query parameters as a cache key.
Therefore, changing any of the parameters will result in a cache miss.
This is recommended if the cache is only needed to display the times in search results, or if the query parameters are mostly static.

`FuzzyRequestCache` uses only the `origin` and `mode` fields as cache keys.
This cache is useful for workloads where the set of possible `origin` parameters is limited since it will limit the amount of API calls needed to fetch data from TravelTime. The `secondary_size` controls the size of each per-origin traveltime cache. It should be set to at least the number of documents returned by each query.
