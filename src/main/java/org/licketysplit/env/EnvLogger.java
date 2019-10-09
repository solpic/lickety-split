package org.licketysplit.env;

import java.io.OutputStream;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.*;

public class EnvLogger {
    String username;
    Logger logger;

    static Boolean hasReset = false;
    public static class StdoutConsoleHandler extends ConsoleHandler {
        protected void setOutputStream(OutputStream out) throws SecurityException {
            super.setOutputStream(System.out);
        }
    }
    public EnvLogger(String username) {
        this.username = username;


        synchronized (hasReset) {
            if(!hasReset) {
                LogManager.getLogManager().reset();
                hasReset = true;
                logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
                StdoutConsoleHandler handler = new StdoutConsoleHandler();
                handler.setFormatter(new SimpleFormatter() {
                    private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";

                    @Override
                    public synchronized String format(LogRecord lr) {
                        return String.format(format,
                                new Date(lr.getMillis()),
                                lr.getLevel().getLocalizedName(),
                                lr.getMessage()
                        );
                    }
                });

                logger.addHandler(handler);
            }else {
                logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
            }
        }

    }

    public void log(Level lvl, String msg) {

        logger.log(lvl, username+": "+msg);
    }


}
