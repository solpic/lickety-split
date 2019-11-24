package org.licketysplit.env;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
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
    boolean enabled = true;
    public void disable() {
        enabled = false;
    }
    public void enable() {
        enabled = true;
    }

    public void setLogFile(File f) throws Exception{
        FileHandler fh = new FileHandler(f.getPath());
        fh.setFormatter(getFormatter());
        logger.addHandler(fh);
    }

    Formatter getFormatter() {
        return new SimpleFormatter() {
            private static final String format = "[%1$tF %1$tT.%1$tL] %3$s %n";

            @Override
            public synchronized String format(LogRecord lr) {
                return String.format(format,
                        new Date(lr.getMillis()),
                        lr.getLevel().getLocalizedName(),
                        lr.getMessage()
                );
            }
        };
    }

    public EnvLogger(String username) {
        this.username = username;


        synchronized (hasReset) {
            if(!hasReset) {
                LogManager.getLogManager().reset();
                hasReset = true;
            }
            logger = Logger.getLogger(username);
            StdoutConsoleHandler handler = new StdoutConsoleHandler();
            handler.setFormatter(getFormatter());

            logger.addHandler(handler);
            /*}else {
                logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
            }*/
        }

    }

    public void log(Level lvl, String msg, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();

        log(lvl,
                String.format("%s\n%s", msg, stackTrace));
    }

    public void log(String msg) {
        log(Level.INFO, msg);
    }

    public void trigger() {

    }

    public void log(Level lvl, String msg) {
        if(enabled) {
            String logLine = username + ": " + msg;
            logger.log(lvl, logLine);
            Debugger.global().parseTrigger(logLine);
        }
    }

    public void log(Level lvl, String msg, Throwable thrown) {
        if(enabled) {
            logger.log(lvl, username + ": " + msg, thrown);
        }
    }


}
