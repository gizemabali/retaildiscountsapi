package retailerdiscountsapi;

import javax.annotation.PreDestroy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class RetailStoreDiscountCalculator extends SpringBootServletInitializer {

	/**
	 * Start point
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		SpringApplication.run(RetailStoreDiscountCalculator.class, args);
	}

	/**
	 * This method is used to close inner clients gracefully.
	 */
	@PreDestroy
	public void onExit() {
		ElasticClientOperations.getInstance().closeClient();
	}
	
}
