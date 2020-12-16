package retailerdiscountsapi.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import retailerdiscountsapi.ElasticClientOperations;

/**
 * This controller is used to perform api operations.
 * 
 * @author gizemabali
 *
 */
@RestController
public class RetailerDiscountsController {

	private static final Logger logger = LogManager.getLogger(RetailerDiscountsController.class);

	/**
	 * This is a rest client of elastic to perform elastic requests.
	 */
	@Autowired
	RestHighLevelClient client;

	/**
	 * This api is used to get products of certain types.
	 * 
	 * @param type it is the parameter that indicates product type.
	 * @return a ResponseEntity object to the client.
	 */
	@CrossOrigin
	@RequestMapping(value = "products/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getTypeRelatedProducts(@PathVariable String type) {
		try {
			return ResponseEntity
					.ok(ElasticClientOperations.getInstance().getTypeRelatedProducts(type, "product").toString());
		} catch (Exception e) {
			logger.error("getProducts", e);
			return sendErrorResponse();
		}
	}

	/**
	 * This api is used to calculate total prices of the products in the basket according to the given user detail
	 * information inside of the request body.
	 * 
	 * @param basketAndUserDetails it is the request body that has the information of the products in the basket and the
	 *                             user information of the current user that has the basket.
	 * @return a ResponseEntity object to the client.
	 */
	@CrossOrigin
	@PostMapping("calculatebasket")
	public ResponseEntity<String> calculateBasket(@RequestBody String basketAndUserDetails) {
		try {
			JsonObject basketAndUserDetailsObj = JsonParser.parseString(basketAndUserDetails).getAsJsonObject();
			return ElasticClientOperations.getInstance().calculateBasket(basketAndUserDetailsObj, "product");
		} catch (Exception e) {
			logger.error("calculateBasketError", e);
			return sendErrorResponse();
		}
	}

	/**
	 * This api is used to create user information in the elasticsearch.
	 * 
	 * @param userDetails it is the user detail object that has user information. 
	 * @return a ResponseEntity object to the client.
	 */
	@CrossOrigin
	@PostMapping("user")
	public ResponseEntity<String> createUser(@RequestBody String userDetails) {
		try {
			JsonObject userDetailsObj = JsonParser.parseString(userDetails).getAsJsonObject();
			return ElasticClientOperations.getInstance().createUser(userDetailsObj, "userinfo");
		} catch (Exception e) {
			logger.error("calculateBasketError", e);
			return sendErrorResponse();
		}
	}

	/**
	 * This method creates and unexpected error occur response to the user when an expected error occur!
	 * <ul>
	 * <li><code>{"response":{@value Constants.ErrorMessages.UNEXPECTED_ERROR_OCCUR}}</code></li>
	 * </ul>
	 * 
	 * @return a ResponseEntity object
	 */
	private ResponseEntity<String> sendErrorResponse() {
		JsonObject responseObj = new JsonObject();
		responseObj.addProperty("response", "unexpected error occur!");
		return ResponseEntity.status(500).body(responseObj.toString());
	}

}
