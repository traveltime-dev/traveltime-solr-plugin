# traveltime-solr-plugin
Plugin for Solr that allows users to filter locations using the Traveltime API.

## Installation & configuration
This is a standard Solr plugin.
The plugin jar must be copied into the [solr lib directory](https://solr.apache.org/guide/8_4/libs.html#lib-directories)

To use the plugin you **must** add a `queryParser` with the class `com.traveltime.plugin.solr.TraveltimeQParserPlugin`.
This query parser has two mandatory string configuration options:
- `app_id`: this is you API app id.
- `api_key`: this is the api key that corresponds to the app id.

```xml
<queryParser name="traveltime" class="com.traveltime.plugin.solr.TraveltimeQParserPlugin">
  <str name="app_id">your_app_id_here</str>
  <str name="api_key">your_api_key_here</str>
</queryParser>
```

To display the travel times returned by the TravelTime API you must configure two more components: a `valueSourceParser`:
```xml
<valueSourceParser name="traveltime" class="com.traveltime.plugin.solr.query.TravelTimeValueSourceParser" />
```
and a `cache`:
```xml
<cache name="traveltime" class="com.traveltime.plugin.solr.cache.ExactRequestCache"/>
```
or
```xml
<cache name="traveltime" class="com.traveltime.plugin.solr.cache.FuzzyRequestCache" secondary_size="50000"/>
```

## Querying data
The traveltime query can only be used as a filter query, and can only be used with fields that are indexed as `location`.
The query accepts the following (mandatory) configuration options:
- `origin`: the point from which travel time will be measured. The accepted formats are:
    - `"lat,lon"` string
    - geohash
- `field`: the document field that will be used as the destination in the Traveltime query.
- `limit`: the travel time limit in seconds. Must be non-negative.
- `mode`: Transportation mode used in the search. One of: `pt`, `walking+ferry`, `cycling+ferry`, `driving+ferry`.
- `country`: Country that the `origin` is in. Currently may only be one of: `uk`, `nl`, `at`, `be`, `de`, `fr`, `ie`, `lt`.

The configuration options may be passed as local query parameters: `?fq={!traveltime origin=51.53,-0.15 field=coords limit=7200 mode=pt country=uk}`, or as raw query parameters prefixed with `"traveltime_"`: `?fq={!traveltime}&traveltime_origin=51.53,-0.15&traveltime_field=coords&traveltime_limit=7200&traveltime_mode=pt&traveltime_country=uk}`.
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
This cache is useful for workloads where the set of possible `origin` parameters is limited since it will limit the amount of API calls needed to fetch data from Traveltime. The `secondary_size` controls the size of each per-origin traveltime cache. It should be set to at least the number of documents returned by each query.
