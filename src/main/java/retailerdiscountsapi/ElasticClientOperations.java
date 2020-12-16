package retailerdiscountsapi;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * This is a component class which is used to perform elastic queries over given elastic indices.
 * 
 * @author gizemabali
 *
 */
@Component
public class ElasticClientOperations {

	private static final DateOperations dateOperations = DateOperations.getInstance();

	private static final ExternalHashOperations hashOperations = ExternalHashOperations.getInstance();

	private static final Logger logger = LogManager.getLogger(ElasticClientOperations.class);

	/**
	 * This is a rest client of elastic to perform elastic requests.
	 */
	@Autowired
	static RestHighLevelClient client;

	/**
	 * Singleton ElasticClientOperations instance.
	 */
	private final static ElasticClientOperations operations = new ElasticClientOperations();

	private ElasticClientOperations() {
	};

	/**
	 * set client
	 * 
	 * @param restClient
	 */
	@Autowired
	public void setClient(RestHighLevelClient restClient) {
		client = restClient;
	}

	/**
	 * This method is used to close elastic client gracefully
	 * 
	 * @return
	 */
	public boolean closeClient() {
		boolean isClosed = false;
		try {
			client.close();
			isClosed = true;
		} catch (Exception e) {
			logger.error(Constants.COULD_NOT_CLOSE_THE_CLIENT, e);
		}
		return isClosed;
	}

	/**
	 * @return singleton ElasticClientOperations instance.
	 */
	public static ElasticClientOperations getInstance() {
		return operations;
	}

	/**
	 * This method is used to calculate total price of the products in the basket. It searchs in the index and according
	 * to the user information, discount operation is performed.
	 * 
	 * <ul>
	 * <li>If the user is an employee of the store, he gets a 30% discount</li>
	 * <li>If the user is an affiliate of the store, he gets a 10% discount</li>
	 * <li>If the user has been a customer for over 2 years, he gets a 5% discount.</li>
	 * <li>The percentage based discounts do not apply on groceries.</li>
	 * <li>A user can get only one of the percentage based discounts on a bill.</li>
	 * <li>For every $100 on the bill, there would be a $ 5 discount (e.g. for $ 990, you get $ 45 as a discount).</li>
	 * </ul>
	 * 
	 * @param basketAndUserDetailsObj it is the object that has basket product information and user information
	 * @param index                   it is the name of the index that will be searched
	 * @return a Response Entity object to the user.
	 * @throws Exception
	 */
	public ResponseEntity<String> calculateBasket(JsonObject basketAndUserDetailsObj, String index) throws Exception {
		JsonObject userDetailsObj = basketAndUserDetailsObj.get(Constants.USER_DETAILS).getAsJsonObject();
		JsonArray basketDetailsList = basketAndUserDetailsObj.get(Constants.BASKET_DETAILS).getAsJsonArray();

		HashMap<String, Integer> productAmounts = new HashMap<String, Integer>();
		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
		for (JsonElement basketElement : basketDetailsList) {
			JsonObject basketObject = basketElement.getAsJsonObject();
			boolQuery.should(QueryBuilders.termQuery(Constants.PRODUCT_NAME,
					basketObject.get(Constants.PRODUCT_NAME).getAsString()));
			productAmounts.put(basketObject.get(Constants.PRODUCT_NAME).getAsString(),
					basketObject.get(Constants.AMOUNT).getAsInt());
		}
		HashMap<String, Long> groceriesProducts = new HashMap<String, Long>();
		HashMap<String, Long> otherProducts = new HashMap<String, Long>();
		getProductDetails(index, boolQuery, groceriesProducts, otherProducts);

		long totalGroceriesProductsPrice = (long) 0;
		long totalOtherProductsPrice = (long) 0;
		for (Entry<String, Integer> productEntry : productAmounts.entrySet()) {
			if (groceriesProducts.containsKey(productEntry.getKey())) {
				totalGroceriesProductsPrice += productEntry.getValue() * groceriesProducts.get(productEntry.getKey());
			} else {
				totalOtherProductsPrice += productEntry.getValue() * otherProducts.get(productEntry.getKey());
			}
		}
		try {
			totalOtherProductsPrice = calculateDiscountedPriceOfProducts(userDetailsObj, totalOtherProductsPrice);
			long totalProductPrice = (long) 0;
			totalProductPrice += totalGroceriesProductsPrice;
			totalProductPrice += totalOtherProductsPrice;

			long discount = (totalProductPrice - (totalProductPrice % 100)) / 100;
			totalProductPrice = totalProductPrice - (discount * 5);
			JsonObject responseObj = new JsonObject();
			responseObj.addProperty(Constants.TOTAL_PRICE, totalProductPrice);
			return ResponseEntity.status(200).body(responseObj.toString());
		} catch (Exception e) {
			logger.error(Constants.UNEXPECTED_ERROR_OCCUR, e);
			JsonObject responseObj = new JsonObject();
			responseObj.addProperty(Constants.ERROR, Constants.UNEXPECTED_ERROR_OCCUR);
			return ResponseEntity.status(500).body(responseObj.toString());
		}
	}

	/**
	 * This method is used to calculate discounted price of the products except from grocery prodcuts.
	 * 
	 * @param userDetailsObj          it is the user information object.
	 * @param totalOtherProductsPrice it is the total price of the products except from groceries products
	 * @return the discounted price
	 * @throws ParseException
	 */
	public long calculateDiscountedPriceOfProducts(JsonObject userDetailsObj, long totalOtherProductsPrice)
			throws ParseException {
		if (userDetailsObj.get(Constants.EMPLOYEE).getAsBoolean()) {
			totalOtherProductsPrice = (totalOtherProductsPrice * 70) / 100;
		} else if (userDetailsObj.get(Constants.AFFILIATE).getAsBoolean()) {
			totalOtherProductsPrice = (totalOtherProductsPrice * 90) / 100;
		} else if (userDetailsObj.get(Constants.CUSTOMER).getAsBoolean()) {
			String accountCreationDate = userDetailsObj.get(Constants.ACCOUNT_CREATION_DATE).getAsString();
			long calenderTodayLong = getCalendar().getTimeInMillis();
			SimpleDateFormat sdf = new SimpleDateFormat(Constants.YYYY_MM_DD_HH_MM_SS);
			sdf.setTimeZone(TimeZone.getTimeZone(Constants.GMT_3));
			Date date;
			date = sdf.parse(accountCreationDate);
			Calendar consentDateCalendar = getCalendar();
			consentDateCalendar.setTime(date);
			long consentDateCalendarLong = consentDateCalendar.getTimeInMillis();
			if (calenderTodayLong - consentDateCalendarLong > 63113904000L) {
				totalOtherProductsPrice = (totalOtherProductsPrice * 95) / 100;
			}
		}
		return totalOtherProductsPrice;
	}

	/**
	 * This method is used to get product details from the given index using the given bool querty, then it fills the
	 * given groceriesProducts and otherProducts maps.
	 * 
	 * @param index             it is the name of the index.
	 * @param boolQuery         it is the bool query that will be used to search documents.
	 * @param groceriesProducts it is the map object that will be filled with only groceries products
	 * @param otherProducts     it is the map object that will be filled with only products except from groceries
	 *                          products
	 * @throws Exception
	 */
	public void getProductDetails(String index, BoolQueryBuilder boolQuery, HashMap<String, Long> groceriesProducts,
			HashMap<String, Long> otherProducts) throws Exception {
		int size = 100;
		int from = 0;
		boolean getNextChunk = true;
		while (getNextChunk) {
			SearchRequest searchRequest = new SearchRequest(index);
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
			searchSourceBuilder.query(boolQuery).from(from).size(size);
			searchRequest.source(searchSourceBuilder);
			SearchResponse searchResponse = null;
			try {
				searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
				SearchHit[] hits = searchResponse.getHits().getHits();
				for (SearchHit hit : hits) {
					JsonObject hitObj = JsonParser.parseString(hit.getSourceAsString()).getAsJsonObject();
					String type = hitObj.get(Constants.TYPE).getAsString();
					if (type.equals(Constants.GROCERIES)) {
						groceriesProducts.put(hitObj.get(Constants.PRODUCT_NAME).getAsString(),
								hitObj.get(Constants.PRICE).getAsLong());
					} else {
						otherProducts.put(hitObj.get(Constants.PRODUCT_NAME).getAsString(),
								hitObj.get(Constants.PRICE).getAsLong());
					}
				}
				if (hits.length < size) {
					getNextChunk = false;
				}
			} catch (Exception e) {
				logger.error(Constants.UNEXPECTED_ERROR_OCCUR, e);
				throw e;
			}
			from += size;
		}
	}

	/**
	 * This method is used to create Calendar object of the current date.
	 * 
	 * @return a Calendar object
	 */
	public Calendar getCalendar() {
		return Calendar.getInstance(TimeZone.getTimeZone(Constants.GMT_3));
	}

	/**
	 * This method is used to get product list of the given type from the given index.
	 * 
	 * @param type  it is the type of the product.
	 * @param index is is the name of the index.
	 * @return a Object that has product details.
	 * @throws Exception
	 */
	public Object getTypeRelatedProducts(String type, String index) throws Exception {
		logger.debug(String.format("getting type related documents type: \"%s\", index: \"%s\"", type, index));
		JsonArray productDetails = new JsonArray();
		try {
			int size = 100;
			int from = 0;
			boolean getNextChunk = true;
			while (getNextChunk) {
				SearchSourceBuilder builder = new SearchSourceBuilder()
						.query(QueryBuilders.termQuery(Constants.TYPE, type)).from(from).size(size);
				SearchRequest searchRequest = new SearchRequest().indices(index).source(builder);
				SearchResponse response = null;
				try {
					response = client.search(searchRequest, RequestOptions.DEFAULT);
					SearchHit[] hits = response.getHits().getHits();
					for (SearchHit hit : hits) {
						JsonObject hitObj = JsonParser.parseString(hit.getSourceAsString()).getAsJsonObject();

						productDetails.add(hitObj);
					}
					if (hits.length < size) {
						getNextChunk = false;
					}
				} catch (Exception e) {
					logger.error(Constants.COULD_NOT_SEARCH_IN_ELASTIC, e);
					throw e;
				}
				from += size;
			}

		} catch (Exception e) {
			logger.error(Constants.UNEXPECTED_ERROR_OCCUR, e);
			throw e;
		}
		logger.info(String.format("got question details for category %s! questionDetails %s", type,
				productDetails.toString()));
		return productDetails;
	}

	/**
	 * This methos is used to index user information to the given index. Password is hashed and then saved to the index.
	 * 
	 * @param userDetailsObj it is the user information object.
	 * @param index          it is the name of the index.
	 * @return a Response Entity object
	 * @throws Exception
	 */
	public ResponseEntity<String> createUser(JsonObject userDetailsObj, String index) throws Exception {
		userDetailsObj.addProperty(Constants.ACCOUNT_CREATION_DATE, getDateOperations().getCurrentDate());
		String password = userDetailsObj.get(Constants.PASSWORD).getAsString();
		userDetailsObj.addProperty(Constants.PASSWORD, hashOperations.hashText(password));
		String docId = indexDocument(index, userDetailsObj, null);
		JsonObject responseObj = new JsonObject();
		int statusCode = 200;
		if (docId != null) {
			responseObj.addProperty(Constants.STATUS, Constants.SUCCESS);
		} else {
			responseObj.addProperty(Constants.STATUS, Constants.FAILURE);
			statusCode = 500;
		}
		return ResponseEntity.status(statusCode).body(responseObj.toString());
	}

	public DateOperations getDateOperations() {
		return dateOperations;
	}

	/**
	 * This method is used to index then given answerObj documents to the given index using the given id.
	 * 
	 * @param indexName   it is the name of the index that document will be indexed.
	 * @param documentObj it is the document object that will be indexed to the given index.
	 * @param id          it is the id of the document that will be used to index document into the given index.
	 * @return a doc id from the elastic search
	 * @throws Exception
	 */
	public String indexDocument(String index, JsonObject documentObj, String id) throws Exception {
		IndexRequest insertRequest = new IndexRequest(index);
		if (id != null) {
			insertRequest.id(id);
		}
		insertRequest.source(documentObj.toString(), XContentType.JSON);
		return client.index(insertRequest, RequestOptions.DEFAULT).getId();
	}

}
