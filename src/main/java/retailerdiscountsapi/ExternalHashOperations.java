package retailerdiscountsapi;

import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.hash.Hashing;

/**
 * This class is used to perform hashing operations.
 * 
 * @author gizemabali
 *
 */
public class ExternalHashOperations {

	/**
	 * Singleton utility class instance.
	 */
	private static final ExternalHashOperations externalHashOperations = new ExternalHashOperations();

	/**
	 * Default constructor made private to prevent outside access. Use singleton method getter {@link #getInstance()}
	 * instead.
	 *
	 * @see #getInstance()
	 */
	private ExternalHashOperations() {
	}

	/**
	 * Getter for the singleton instance.
	 */
	public static ExternalHashOperations getInstance() {
		return externalHashOperations;
	}

	/**
	 * Logger instance.
	 */
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
