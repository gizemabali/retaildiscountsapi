package retailerdiscountsapi;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ControllerTest {

	private static final String PRODUCT_INDEX = "product";

	private static final String USERINFO_INDEX = "userinfo";

	private RestHighLevelClient testClient = null;

	private ElasticClientOperations operations;

	private ClientUtils clientUtils;

	@ClassRule
	public final static EnvironmentVariables environmentVariables = new EnvironmentVariables();

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
		Mockito.doCallRealMethod().when(operations).getTypeRelatedProducts(Mockito.anyString(), Mockito.anyString());
		Mockito.doCallRealMethod().when(operations).createUser(Mockito.any(JsonObject.class), Mockito.anyString());

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

	@Autowired
	private MockMvc mockMvc;
	
	@Test
	public void getCategory() throws Exception {
		// set up
		String initialQuestionsStr = "[{\"productName\":\"Blue Shoes\",\"type\":\"shoes\",\"price\":200},{\"productName\":\"Blue Dress\",\"type\":\"garment\",\"price\":400},{\"productName\":\"Red Carpet\",\"type\":\"home\",\"price\":100},{\"productName\":\"Red Sofa\",\"type\":\"home\",\"price\":150},{\"productName\":\"Bananas\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Mango\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Apple\",\"type\":\"groceries\",\"price\":15}]";
		JsonArray initialQuestions = JsonParser.parseString(initialQuestionsStr).getAsJsonArray();
		indexDocuments(initialQuestions);
		clientUtils.refresh(PRODUCT_INDEX);

		// execute
		MvcResult response = mockMvc.perform(get("/products/shoes", 42L).contentType("application/json"))
				.andExpect(status().isOk()).andReturn();
		MockHttpServletResponse resp = response.getResponse();
		String responseStr = resp.getContentAsString();

		// assert
		String expectedProducts = "[{\"productName\":\"Blue Shoes\",\"type\":\"shoes\",\"price\":200}]";
		assertEquals("expectedProducts has one product!", JsonParser.parseString(expectedProducts).getAsJsonArray(),
				JsonParser.parseString(responseStr).getAsJsonArray());
		assertEquals("must be ok reponse!", 200, response.getResponse().getStatus());
	}
	
	@Test
	public void getCategory_failure() throws Exception {
		// set up
		ClientConfiguration clientConfiguration = ClientConfiguration.builder().connectedTo("invalidhost:9200").build();
		testClient = RestClients.create(clientConfiguration).rest();
		operations.setClient(testClient);
		String initialQuestionsStr = "[{\"productName\":\"Blue Shoes\",\"type\":\"shoes\",\"price\":200},{\"productName\":\"Blue Dress\",\"type\":\"garment\",\"price\":400},{\"productName\":\"Red Carpet\",\"type\":\"home\",\"price\":100},{\"productName\":\"Red Sofa\",\"type\":\"home\",\"price\":150},{\"productName\":\"Bananas\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Mango\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Apple\",\"type\":\"groceries\",\"price\":15}]";
		JsonArray initialQuestions = JsonParser.parseString(initialQuestionsStr).getAsJsonArray();
		indexDocuments(initialQuestions);
		clientUtils.refresh(PRODUCT_INDEX);

		// execute
		MvcResult response = mockMvc.perform(get("/products/shoes", 42L).contentType("application/json"))
				.andReturn();
		MockHttpServletResponse resp = response.getResponse();
		String responseStr = resp.getContentAsString();

		// assert
		String expectedResponse = "{\"response\":\"unexpected error occur!\"}";
		assertEquals("response is an unexpected error!", JsonParser.parseString(expectedResponse).getAsJsonObject(),
				JsonParser.parseString(responseStr).getAsJsonObject());
		assertEquals("must be ok reponse!", 500, response.getResponse().getStatus());
	}
	
	@Test
	public void createUser() throws Exception {
		// set up
		String initialQuestionsStr = "[{\"productName\":\"Blue Shoes\",\"type\":\"shoes\",\"price\":200},{\"productName\":\"Blue Dress\",\"type\":\"garment\",\"price\":400},{\"productName\":\"Red Carpet\",\"type\":\"home\",\"price\":100},{\"productName\":\"Red Sofa\",\"type\":\"home\",\"price\":150},{\"productName\":\"Bananas\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Mango\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Apple\",\"type\":\"groceries\",\"price\":15}]";
		JsonArray initialQuestions = JsonParser.parseString(initialQuestionsStr).getAsJsonArray();
		indexDocuments(initialQuestions);
		clientUtils.refresh(PRODUCT_INDEX);

		// execute
		String userInfo = "{\"username\":\"example@mail.com\",\"password\":\"123456\",\"employee\":true,\"affiliate\":true,\"customer\":true}";
		MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post("/user").content(userInfo)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andReturn();
		MockHttpServletResponse resp = response.getResponse();
		String responseStr = resp.getContentAsString();

		// assert
		String expectedResponse = "{\"status\":\"success\"}";
		assertEquals("expectedProducts has one product!", JsonParser.parseString(expectedResponse).getAsJsonObject(),
				JsonParser.parseString(responseStr).getAsJsonObject());
		assertEquals("must be ok reponse!", 200, response.getResponse().getStatus());
	}
	
	@Test
	public void createUser_failure() throws Exception {
		// set up
		ClientConfiguration clientConfiguration = ClientConfiguration.builder().connectedTo("invalidhost:9200").build();
		testClient = RestClients.create(clientConfiguration).rest();
		operations.setClient(testClient);
		String initialQuestionsStr = "[{\"productName\":\"Blue Shoes\",\"type\":\"shoes\",\"price\":200},{\"productName\":\"Blue Dress\",\"type\":\"garment\",\"price\":400},{\"productName\":\"Red Carpet\",\"type\":\"home\",\"price\":100},{\"productName\":\"Red Sofa\",\"type\":\"home\",\"price\":150},{\"productName\":\"Bananas\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Mango\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Apple\",\"type\":\"groceries\",\"price\":15}]";
		JsonArray initialQuestions = JsonParser.parseString(initialQuestionsStr).getAsJsonArray();
		indexDocuments(initialQuestions);
		clientUtils.refresh(PRODUCT_INDEX);

		// execute
		String userInfo = "{\"username\":\"example@mail.com\",\"password\":\"123456\",\"employee\":true,\"affiliate\":true,\"customer\":true}";
		MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post("/user").content(userInfo)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andReturn();
		MockHttpServletResponse resp = response.getResponse();
		String responseStr = resp.getContentAsString();

		// assert
		String expectedResponse = "{\"response\":\"unexpected error occur!\"}";
		assertEquals("expectedProducts has one product!", JsonParser.parseString(expectedResponse).getAsJsonObject(),
				JsonParser.parseString(responseStr).getAsJsonObject());
		assertEquals("must be ok reponse!", 500, response.getResponse().getStatus());
	}
	
	
	@Test
	public void calculateBasket_employee() throws Exception {
		// set up
		String initialQuestionsStr = "[{\"productName\":\"Blue Shoes\",\"type\":\"shoes\",\"price\":200},{\"productName\":\"Blue Dress\",\"type\":\"garment\",\"price\":400},{\"productName\":\"Red Carpet\",\"type\":\"home\",\"price\":100},{\"productName\":\"Red Sofa\",\"type\":\"home\",\"price\":150},{\"productName\":\"Bananas\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Mango\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Apple\",\"type\":\"groceries\",\"price\":15}]";
		JsonArray initialQuestions = JsonParser.parseString(initialQuestionsStr).getAsJsonArray();
		indexDocuments(initialQuestions);
		clientUtils.refresh(PRODUCT_INDEX);

		// execute
		String userInfo = "{\"userDetails\":{\"username\":\"example@mail.com\",\"employee\":true,\"affiliate\":true,\"customer\":true,\"accountCreationDate\":\"2020-12-16 00:12:47\"},\"basketDetails\":[{\"productName\":\"Red Carpet\",\"amount\":1},{\"productName\":\"Blue Dress\",\"amount\":2},{\"productName\":\"Bananas\",\"amount\":1}]}";
		MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post("/calculatebasket").content(userInfo)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andReturn();
		MockHttpServletResponse resp = response.getResponse();
		String responseStr = resp.getContentAsString();

		// assert
		String expectedResponse = "{\"totalPrice\":615}";
		assertEquals("price must be $615!", JsonParser.parseString(expectedResponse).getAsJsonObject(),
				JsonParser.parseString(responseStr).getAsJsonObject());
		assertEquals("must be ok reponse!", 200, response.getResponse().getStatus());
	}
	
	@Test
	public void calculateBasket_affiliate() throws Exception {
		// set up
		String initialQuestionsStr = "[{\"productName\":\"Blue Shoes\",\"type\":\"shoes\",\"price\":200},{\"productName\":\"Blue Dress\",\"type\":\"garment\",\"price\":400},{\"productName\":\"Red Carpet\",\"type\":\"home\",\"price\":100},{\"productName\":\"Red Sofa\",\"type\":\"home\",\"price\":150},{\"productName\":\"Bananas\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Mango\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Apple\",\"type\":\"groceries\",\"price\":15}]";
		JsonArray initialQuestions = JsonParser.parseString(initialQuestionsStr).getAsJsonArray();
		indexDocuments(initialQuestions);
		clientUtils.refresh(PRODUCT_INDEX);

		// execute
		String userInfo = "{\"userDetails\":{\"username\":\"example@mail.com\",\"employee\":false,\"affiliate\":true,\"customer\":true,\"accountCreationDate\":\"2020-12-16 00:12:47\"},\"basketDetails\":[{\"productName\":\"Red Carpet\",\"amount\":1},{\"productName\":\"Blue Dress\",\"amount\":2},{\"productName\":\"Bananas\",\"amount\":1}]}";
		MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post("/calculatebasket").content(userInfo)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andReturn();
		MockHttpServletResponse resp = response.getResponse();
		String responseStr = resp.getContentAsString();

		// assert
		String expectedResponse = "{\"totalPrice\":785}";
		assertEquals("price must be $615!", JsonParser.parseString(expectedResponse).getAsJsonObject(),
				JsonParser.parseString(responseStr).getAsJsonObject());
		assertEquals("must be ok reponse!", 200, response.getResponse().getStatus());
	}
	
	@Test
	public void calculateBasket_failure() throws Exception {
		// set up
		ClientConfiguration clientConfiguration = ClientConfiguration.builder().connectedTo("invalidhost:9200").build();
		testClient = RestClients.create(clientConfiguration).rest();
		operations.setClient(testClient);
		String initialQuestionsStr = "[{\"productName\":\"Blue Shoes\",\"type\":\"shoes\",\"price\":200},{\"productName\":\"Blue Dress\",\"type\":\"garment\",\"price\":400},{\"productName\":\"Red Carpet\",\"type\":\"home\",\"price\":100},{\"productName\":\"Red Sofa\",\"type\":\"home\",\"price\":150},{\"productName\":\"Bananas\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Mango\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Apple\",\"type\":\"groceries\",\"price\":15}]";
		JsonArray initialQuestions = JsonParser.parseString(initialQuestionsStr).getAsJsonArray();
		indexDocuments(initialQuestions);
		clientUtils.refresh(PRODUCT_INDEX);

		// execute
		String userInfo = "{\"userDetails\":{\"username\":\"example@mail.com\",\"employee\":false,\"affiliate\":true,\"customer\":true,\"accountCreationDate\":\"2020-12-16 00:12:47\"},\"basketDetails\":[{\"productName\":\"Red Carpet\",\"amount\":1},{\"productName\":\"Blue Dress\",\"amount\":2},{\"productName\":\"Bananas\",\"amount\":1}]}";
		MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post("/calculatebasket").content(userInfo)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andReturn();
		MockHttpServletResponse resp = response.getResponse();
		String responseStr = resp.getContentAsString();

		// assert
		String expectedResponse = "{\"response\":\"unexpected error occur!\"}";
		assertEquals("response is an unexpected error!", JsonParser.parseString(expectedResponse).getAsJsonObject(),
				JsonParser.parseString(responseStr).getAsJsonObject());
		assertEquals("must be ok reponse!", 500, response.getResponse().getStatus());
	}
	
	
	@Test
	public void calculateBasket_customerOver2Years() throws Exception {
		// set up
		String initialQuestionsStr = "[{\"productName\":\"Blue Shoes\",\"type\":\"shoes\",\"price\":200},{\"productName\":\"Blue Dress\",\"type\":\"garment\",\"price\":400},{\"productName\":\"Red Carpet\",\"type\":\"home\",\"price\":100},{\"productName\":\"Red Sofa\",\"type\":\"home\",\"price\":150},{\"productName\":\"Bananas\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Mango\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Apple\",\"type\":\"groceries\",\"price\":15}]";
		JsonArray initialQuestions = JsonParser.parseString(initialQuestionsStr).getAsJsonArray();
		indexDocuments(initialQuestions);
		clientUtils.refresh(PRODUCT_INDEX);

		// execute
		String userInfo = "{\"userDetails\":{\"username\":\"example@mail.com\",\"employee\":false,\"affiliate\":false,\"customer\":true,\"accountCreationDate\":\"2016-12-16 00:12:47\"},\"basketDetails\":[{\"productName\":\"Red Carpet\",\"amount\":1},{\"productName\":\"Blue Dress\",\"amount\":2},{\"productName\":\"Bananas\",\"amount\":1}]}";
		MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post("/calculatebasket").content(userInfo)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andReturn();
		MockHttpServletResponse resp = response.getResponse();
		String responseStr = resp.getContentAsString();

		// assert
		String expectedResponse = "{\"totalPrice\":830}";
		assertEquals("price must be $615!", JsonParser.parseString(expectedResponse).getAsJsonObject(),
				JsonParser.parseString(responseStr).getAsJsonObject());
		assertEquals("must be ok reponse!", 200, response.getResponse().getStatus());
	}
	
	
	@Test
	public void calculateBasket_customerNotOver2Years() throws Exception {
		// set up
		String initialQuestionsStr = "[{\"productName\":\"Blue Shoes\",\"type\":\"shoes\",\"price\":200},{\"productName\":\"Blue Dress\",\"type\":\"garment\",\"price\":400},{\"productName\":\"Red Carpet\",\"type\":\"home\",\"price\":100},{\"productName\":\"Red Sofa\",\"type\":\"home\",\"price\":150},{\"productName\":\"Bananas\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Mango\",\"type\":\"groceries\",\"price\":15},{\"productName\":\"Apple\",\"type\":\"groceries\",\"price\":15}]";
		JsonArray initialQuestions = JsonParser.parseString(initialQuestionsStr).getAsJsonArray();
		indexDocuments(initialQuestions);
		clientUtils.refresh(PRODUCT_INDEX);

		// execute
		String userInfo = "{\"userDetails\":{\"username\":\"example@mail.com\",\"employee\":false,\"affiliate\":false,\"customer\":true,\"accountCreationDate\":\"2020-12-16 00:12:47\"},\"basketDetails\":[{\"productName\":\"Red Carpet\",\"amount\":1},{\"productName\":\"Blue Dress\",\"amount\":2},{\"productName\":\"Bananas\",\"amount\":1}]}";
		MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post("/calculatebasket").content(userInfo)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andReturn();
		MockHttpServletResponse resp = response.getResponse();
		String responseStr = resp.getContentAsString();

		// assert
		String expectedResponse = "{\"totalPrice\":870}";
		assertEquals("price must be $615!", JsonParser.parseString(expectedResponse).getAsJsonObject(),
				JsonParser.parseString(responseStr).getAsJsonObject());
		assertEquals("must be ok reponse!", 200, response.getResponse().getStatus());
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
