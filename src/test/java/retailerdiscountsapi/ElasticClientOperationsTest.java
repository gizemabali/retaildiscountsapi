package retailerdiscountsapi;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.http.ResponseEntity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class ElasticClientOperationsTest {

	private RestHighLevelClient testClient = null;

	private ElasticClientOperations operations;

	private ClientUtils clientUtils;

	private static final String PRODUCT_INDEX = "product";

	private static final String USERINFO_INDEX = "userinfo";

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		ClientConfiguration clientConfiguration = ClientConfiguration.builder().connectedTo("localhost:9200").build();
		testClient = RestClients.create(clientConfiguration).rest();
		operations = Mockito.mock(ElasticClientOperations.class);
		Mockito.doCallRealMethod().when(operations).calculateBasket(Mockito.any(JsonObject.class), Mockito.anyString());
		Mockito.doCallRealMethod().when(operations).setClient(Mockito.any(RestHighLevelClient.class));
		Mockito.doCallRealMethod().when(operations).calculateDiscountedPriceOfProducts(Mockito.any(JsonObject.class),
				Mockito.anyLong());
		Mockito.doCallRealMethod().when(operations).getProductDetails(Mockito.anyString(),
				Mockito.any(BoolQueryBuilder.class), Mockito.any(HashMap.class), Mockito.any(HashMap.class));

		Mockito.doCallRealMethod().when(operations).indexDocument(Mockito.anyString(), Mockito.any(),
				Mockito.anyString());
		Mockito.doCallRealMethod().when(operations).getCalendar();
		Mockito.doCallRealMethod().when(operations).getDateOperations();
		Mockito.doCallRealMethod().when(operations).getTypeRelatedProducts(Mockito.anyString(), Mockito.anyString());
		Mockito.doCallRealMethod().when(operations).createUser(Mockito.any(JsonObject.class), Mockito.anyString());
		DateOperations mockDateOperations = Mockito.mock(DateOperations.class);
		Mockito.doCallRealMethod().when(mockDateOperations).getCalendar();
		Mockito.doCallRealMethod().when(mockDateOperations).getCurrentDate();
		Calendar calender = Calendar.getInstance();
		calender.set(2020, 3, 3, 9, 9, 9);
		Mockito.when(mockDateOperations.getCalendar()).thenReturn(calender);
		Mockito.when(operations.getDateOperations()).thenReturn(mockDateOperations);
		operations.setClient(testClient);
		clientUtils = new ClientUtils(testClient);

		if (clientUtils.indexAvailable(PRODUCT_INDEX)) {
			clientUtils.deleteIndex(PRODUCT_INDEX);
		}
		if (clientUtils.indexAvailable(USERINFO_INDEX)) {
			clientUtils.deleteIndex(USERINFO_INDEX);
		}
		String productIndexTemplate = "{\"index_patterns\":[\"product*\"],\"settings\":{\"number_of_shards\":1},\"mappings\":{\"_source\":{\"enabled\":true},\"properties\":{\"productName\":{\"type\":\"keyword\"},\"type\":{\"type\":\"keyword\"},\"price\":{\"type\":\"long\"}}}}";
		clientUtils.createTemplate("template-product", productIndexTemplate);
		Thread.sleep(200);
		clientUtils.createIndex(PRODUCT_INDEX, null, 1);

		String userinfoIndexTemplate = "{\"index_patterns\":[\"userinfo*\"],\"settings\":{\"number_of_shards\":1},\"mappings\":{\"_source\":{\"enabled\":true},\"properties\":{\"username\":{\"type\":\"keyword\"},\"password\":{\"type\":\"keyword\"},\"accountCreationDate\":{\"type\":\"date\",\"format\":\"yyyy-MM-dd HH:mm:ss\"},\"employee\":{\"type\":\"boolean\"},\"affiliate\":{\"type\":\"boolean\"},\"customer\":{\"type\":\"boolean\"}}}}";
		clientUtils.createTemplate("template-userinfo", userinfoIndexTemplate);
		Thread.sleep(200);
		clientUtils.createIndex(USERINFO_INDEX, null, 1);
	}

	@Test
	public void calculateDiscountOfProducts() throws JsonSyntaxException, ParseException {
		// set up
		String userDetailsObj = "{\"username\":\"example@mail.com\",\"employee\":true,\"affiliate\":true,\"customer\":true,\"accountCreationDate\":\"2020-12-16 00:12:47\"}";
		// execute
		long finalPrice = operations
				.calculateDiscountedPriceOfProducts(JsonParser.parseString(userDetailsObj).getAsJsonObject(), 440);
		// assert
		assertEquals(308, finalPrice);
	}

	@Test
	public void getCalendar() throws JsonSyntaxException, ParseException {
		// assert
		assertTrue(operations.getCalendar() != null);
	}

	@Test
	public void getProductDetails() throws Exception {
		// set up
		String initialQuestionsStr = "[{\"productName\":\"Blue Shoes\",\"type\":\"shoes\",\"price\":200},{\"productName\":\"Blue Dress\",\"type\":\"garment\",\"price\":400},{\"productName\":\"Red Carpet\",\"type\":\"home\",\"price\":100},{\"productName\":\"Red Sofa\",\"type\":\"home\",\"price\":150},{\"productName\":\"Bananas\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Mango\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Apple\",\"type\":\"groceries\",\"price\":15}]";
		JsonArray initialQuestions = JsonParser.parseString(initialQuestionsStr).getAsJsonArray();
		indexDocuments(initialQuestions);
		clientUtils.refresh(PRODUCT_INDEX);
		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
		boolQuery.should(QueryBuilders.termQuery("productName", "Red Carpet"));
		HashMap<String, Long> groceriesProducts = new HashMap<String, Long>();
		HashMap<String, Long> otherProducts = new HashMap<String, Long>();

		// execute
		operations.getProductDetails(PRODUCT_INDEX, boolQuery, groceriesProducts, otherProducts);

		// assert
		assertEquals(0, groceriesProducts.size());
		assertEquals(1, otherProducts.size());
		assertEquals(100, otherProducts.get("Red Carpet"));
	}
	
	@Test
	public void calculateBasket() throws Exception {
		// set up
		String initialQuestionsStr = "[{\"productName\":\"Blue Shoes\",\"type\":\"shoes\",\"price\":200},{\"productName\":\"Blue Dress\",\"type\":\"garment\",\"price\":400},{\"productName\":\"Red Carpet\",\"type\":\"home\",\"price\":100},{\"productName\":\"Red Sofa\",\"type\":\"home\",\"price\":150},{\"productName\":\"Bananas\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Mango\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Apple\",\"type\":\"groceries\",\"price\":15}]";
		JsonArray initialQuestions = JsonParser.parseString(initialQuestionsStr).getAsJsonArray();
		indexDocuments(initialQuestions);
		clientUtils.refresh(PRODUCT_INDEX);
		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
		boolQuery.should(QueryBuilders.termQuery("productName", "Red Carpet"));
		String userInfo = "{\"userDetails\":{\"username\":\"example@mail.com\",\"employee\":true,\"affiliate\":true,\"customer\":true,\"accountCreationDate\":\"2020-12-16 00:12:47\"},\"basketDetails\":[{\"productName\":\"Red Carpet\",\"amount\":1},{\"productName\":\"Blue Dress\",\"amount\":2},{\"productName\":\"Bananas\",\"amount\":1}]}";
		// execute
		ResponseEntity<String> entity = operations.calculateBasket(JsonParser.parseString(userInfo).getAsJsonObject(), PRODUCT_INDEX);

		String expectedEntityBody = "{\"totalPrice\":615}";
		// assert
		assertEquals(JsonParser.parseString(expectedEntityBody).getAsJsonObject(), JsonParser.parseString(entity.getBody()).getAsJsonObject());
		assertEquals(200, entity.getStatusCodeValue());
	}

	@Test
	public void createUser() throws Exception {
		// set up
		String initialQuestionsStr = "[{\"productName\":\"Blue Shoes\",\"type\":\"shoes\",\"price\":200},{\"productName\":\"Blue Dress\",\"type\":\"garment\",\"price\":400},{\"productName\":\"Red Carpet\",\"type\":\"home\",\"price\":100},{\"productName\":\"Red Sofa\",\"type\":\"home\",\"price\":150},{\"productName\":\"Bananas\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Mango\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Apple\",\"type\":\"groceries\",\"price\":15}]";
		JsonArray initialQuestions = JsonParser.parseString(initialQuestionsStr).getAsJsonArray();
		indexDocuments(initialQuestions);
		clientUtils.refresh(PRODUCT_INDEX);
		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
		boolQuery.should(QueryBuilders.termQuery("productName", "Red Carpet"));
		String userInfo = "{\"username\":\"example@mail.com\",\"password\":\"123456\",\"employee\":true,\"affiliate\":true,\"customer\":true}";

		// execute
		operations.createUser(JsonParser.parseString(userInfo).getAsJsonObject(), USERINFO_INDEX);

		clientUtils.refresh(USERINFO_INDEX);
		JsonArray documents = clientUtils.getAllDocuments(USERINFO_INDEX);
		// assert
		String expectedDocuments = "[{\"username\":\"example@mail.com\",\"password\":\"8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92\",\"employee\":true,\"affiliate\":true,\"customer\":true,\"accountCreationDate\":\"2020-04-03 09:09:09\"}]";
		assertEquals(JsonParser.parseString(expectedDocuments).getAsJsonArray(), documents);
	}

	@Test
	public void indexDocument() throws Exception {
		// set up
		String documentStr = "{\"productName\":\"Blue Shoes\",\"type\":\"shoes\",\"price\":200}";

		// execute
		operations.indexDocument(PRODUCT_INDEX, JsonParser.parseString(documentStr).getAsJsonObject(), "id");
		clientUtils.refresh(PRODUCT_INDEX);
		String document = clientUtils.getById(PRODUCT_INDEX, "id");
		String expectedDocument = "{\"productName\":\"Blue Shoes\",\"type\":\"shoes\",\"price\":200}";

		// assert
		assertEquals(JsonParser.parseString(expectedDocument).getAsJsonObject(),
				JsonParser.parseString(document).getAsJsonObject());
	}

	private void indexDocuments(JsonArray initialQuestions) {
		for (int i = 0; i < initialQuestions.size(); i++) {
			try {
				operations.indexDocument(PRODUCT_INDEX, initialQuestions.get(i).getAsJsonObject(), String.valueOf(i));
			} catch (Exception e) {
				System.err.println("could not index data " + e);
			}
		}
	}

}
