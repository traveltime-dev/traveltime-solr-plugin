package com.traveltime.plugin.solr;

import com.traveltime.plugin.solr.query.TraveltimeQueryParser;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;

public class TraveltimeQParserPlugin extends QParserPlugin {

   @Override
   public void init(NamedList args) {
      String appId = args.get("app_id").toString();
      String apiKey= args.get("api_key").toString();
      FetcherSingleton.INSTANCE.init(appId, apiKey);
   }

   @Override
   public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
      return new TraveltimeQueryParser(qstr, localParams, params, req, FetcherSingleton.INSTANCE.getFetcher());
   }

}
