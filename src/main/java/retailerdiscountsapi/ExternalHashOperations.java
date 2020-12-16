package retailerdiscountsapi;

import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.hash.Hashing;

public class ExternalHashOperations {

	private static final ExternalHashOperations externalHashOperations = new ExternalHashOperations();

	private ExternalHashOperations() {

	}

	public static ExternalHashOperations getInstance() {
		return externalHashOperations;
	}

	private static final Logger logger = LogManager.getLogger(ExternalHashOperations.class);

	public String hashText(String text) {
		String hashValue = null;
		try {
			hashValue = Hashing.sha256().hashString(text, StandardCharsets.UTF_8).toString();
		} catch (Throwable e) {
			logger.error("could not hash text", e);
		}
		return hashValue;
	}

}
