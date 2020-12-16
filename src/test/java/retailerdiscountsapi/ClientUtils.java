package retailerdiscountsapi;

import java.io.IOException;

import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ClientUtils {

	private RestHighLevelClient testClient = null;

	public ClientUtils(RestHighLevelClient testClient) {
		this.testClient = testClient;
	}

	public boolean indexAvailable(String index) {
		boolean indexExists = false;
		GetIndexRequest indexRequest = new GetIndexRequest(index);
		try {
			indexExists = testClient.indices().exists(indexRequest, RequestOptions.DEFAULT);
		} catch (Throwable e) {
			System.err.println(String.format("could not check if the index is available! index: %s", index));
		}
		return indexExists;
	}

	public boolean deleteIndex(String index) {
		boolean isAcknowledged = false;
		DeleteIndexRequest request = new DeleteIndexRequest(index);
		AcknowledgedResponse deleteIndexResponse = null;
		try {
			deleteIndexResponse = testClient.indices().delete(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			System.err.println(String.format("could not delete index! index: %s", index));
		}
		isAcknowledged = deleteIndexResponse.isAcknowledged();
		return isAcknowledged;
	}

	public boolean createTemplate(String templateName, String templateSource) {
		PutIndexTemplateRequest request = new PutIndexTemplateRequest(templateName);
		request.source(templateSource, XContentType.JSON);
		AcknowledgedResponse response = null;
		try {
			response = testClient.indices().putTemplate(request, RequestOptions.DEFAULT);
		} catch (Exception e) {
			System.err.println(String.format("could not create template! template: %s", templateName));
		}
		return response.isAcknowledged();
	}

	public boolean createIndex(String index, String mapping, int shardCount) {
		CreateIndexRequest createIndexReq = new CreateIndexRequest(index);
		// add index mapping to request
		if (mapping != null) {
			createIndexReq.settings(Settings.builder().put("index.number_of_shards", shardCount));
			createIndexReq.mapping(mapping, XContentType.JSON);
		}
		// create index
		CreateIndexResponse createIndexResp = null;
		try {
			createIndexResp = testClient.indices().create(createIndexReq, RequestOptions.DEFAULT);
		} catch (IOException e) {
			System.err.println(String.format("could not create index! index: %s", index));
		}
		return createIndexResp.isAcknowledged();
	}

	public void refresh(String index) {
		RefreshRequest refreshRequest = new RefreshRequest(index);
		try {
			testClient.indices().refresh(refreshRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			System.err.println(String.format("could not refresh index! index: %s", index));
		}
	}

	public String getById(String indexName, String id) throws Exception {
		String doc = null;
		GetResponse response = testClient.get(new GetRequest().index(indexName).id(id), RequestOptions.DEFAULT);
		if (response.isExists() && !response.isSourceEmpty()) {
			doc = response.getSourceAsString();
		}
		return doc;
	}

	public String getAll(String indexName, String id) throws Exception {
		String doc = null;
		GetResponse response = testClient.get(new GetRequest().index(indexName).id(id), RequestOptions.DEFAULT);
		if (response.isExists() && !response.isSourceEmpty()) {
			doc = response.getSourceAsString();
		}
		return doc;
	}

	public JsonArray getAllDocuments(String index) throws Exception {
		final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
		SearchRequest searchRequest = new SearchRequest(index);
		searchRequest.scroll(scroll);
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(QueryBuilders.matchAllQuery());
		searchRequest.source(searchSourceBuilder);

		SearchResponse searchResponse = null;
		searchResponse = testClient.search(searchRequest, RequestOptions.DEFAULT);
		String scrollId = searchResponse.getScrollId();
		SearchHit[] searchHits = searchResponse.getHits().getHits();

		JsonArray indexDocuments = new JsonArray();
		// add search hits objects to indexDocuments array
		indexDocuments.addAll(getSearchHits(searchHits));

		while (searchHits != null && searchHits.length > 0) {
			if (scroll != null) {
				SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
				scrollRequest.scroll(scroll);
				searchResponse = testClient.scroll(scrollRequest, RequestOptions.DEFAULT);
				scrollId = searchResponse.getScrollId();
				searchHits = searchResponse.getHits().getHits();
				indexDocuments.addAll(getSearchHits(searchHits));
			} else {
				break;
			}
		}
		if (scrollId != null) {
			ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
			clearScrollRequest.addScrollId(scrollId);
			testClient.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
		}
		return indexDocuments;
	}

	private JsonArray getSearchHits(SearchHit[] searchHits) {
		JsonArray documents = new JsonArray();
		if (searchHits != null) {
			for (SearchHit hit : searchHits) {
				JsonElement hitEl = new Gson().fromJson(hit.getSourceAsString(), JsonElement.class);
				JsonObject hitObj = hitEl.getAsJsonObject();
				documents.add(hitObj);
			}
		}
		return documents;
	}
}