package log;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;

public class UtilLogger {

	public static boolean SetupLogger() {
		boolean r = false;

		try {

			 Logger rl = LogManager.getRootLogger();
			 
			
			ConsoleAppender console = ConsoleAppender.createAppender(
					PatternLayout.createDefaultLayout(), null, "SYSTEM_OUT",
					"console", null, null);
			final LoggerContext ctx = (LoggerContext) new org.apache.logging.log4j.core.LoggerContext(
					"console");
			final Configuration config = ((org.apache.logging.log4j.core.LoggerContext) ctx)
					.getConfiguration();
			console.start();
			config.addAppender(console);
			AppenderRef ref = AppenderRef.createAppenderRef("console", null,
					null);
			AppenderRef[] refs = new AppenderRef[] { ref };
			LoggerConfig loggerConfig = LoggerConfig.createLogger("false",
					Level.ALL, "org.apache.logging.log4j", "true", refs, null,
					config, null);
			loggerConfig.addAppender(console, null, null);
			config.addLogger("org.apache.logging.log4j", loggerConfig);
			((org.apache.logging.log4j.core.LoggerContext) ctx).updateLoggers();
			ExtendedLogger logger = (ExtendedLogger) ctx.getLogger("console");
			r = true;
		} catch (Exception e) {
			e.printStackTrace();
			r = false;
		}

		return r;
	}
}
