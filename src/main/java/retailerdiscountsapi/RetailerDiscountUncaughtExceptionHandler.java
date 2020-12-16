package retailerdiscountsapi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * This class is used to control uncaught exceptions.
 * 
 * @author gizemabali
 *
 */
@ControllerAdvice
public class RetailerDiscountUncaughtExceptionHandler {

	private static final Logger logger = LogManager.getLogger(RetailerDiscountUncaughtExceptionHandler.class);

	@ExceptionHandler(value = Exception.class)
	public void defaultExceptionHandler(Exception e) {
		logger.fatal("some uncaught exception occur!", e);
	}

}
