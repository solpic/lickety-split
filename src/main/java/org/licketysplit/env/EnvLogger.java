package org.licketysplit.env;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.*;

/**
 * A logging class. Gives a centralized way to log important info for a peer,
 * rather than calling System.out.println everywhere.
 * Usually, it will log to STDOUT as well as a log file.
 *
 * Also importantly, it can trigger callbacks on the Debugger class,
 * either in the current process or over the network. This will enable us
 * to debug peers on the local computer even if they are running on an EC2
 * instance.
 *
 * It is worth noting that many of the functions here are convenience functions
 * that simply wrap other functions with default parameters.
 */
public class EnvLogger {
    /**
     * The Username.
     */
    String username;
    /**
     * The Logger.
     */
    Logger logger;

    /**
     * Keep track of whether we reset the global logger.
     */
    static Boolean hasReset = false;

    /**
     * Class to make the logger print to STDOUT not STDERR.
     */
    public static class StdoutConsoleHandler extends ConsoleHandler {
        protected void setOutputStream(OutputStream out) throws SecurityException {
            super.setOutputStream(System.out);
        }
    }

    /**
     * Enable or disable the logger.
     */
    boolean enabled = true;

    /**
     * Disable.
     */
    public void disable() {
        enabled = false;
    }

    /**
     * Enable.
     */
    public void enable() {
        enabled = true;
    }

    /**
     * Sets log file.
     *
     * @param f the log file
     * @throws Exception the exception
     */
    public void setLogFile(File f) throws Exception{
        FileHandler fh = new FileHandler(f.getPath());
        fh.setFormatter(getFormatter());
        logger.addHandler(fh);
    }

    /**
     * Log a message and an exception stack trace.
     *
     * @param s the message
     * @param e the exception
     */
    public void log(String s, Exception e) {
        log(Level.INFO, s, e);
    }

    /**
     * Creates an instance of the default formatter, logging the time of the message.
     *
     * @return the formatter
     */
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


    /**
     * Resets the global logger, so that we can use our STDOUT logger
     * rather than the STDERR logger.
     */
    public static void resetLogmanager() {
        synchronized (hasReset) {
            if (!hasReset) {
                LogManager.getLogManager().reset();
                hasReset = true;
            }
        }
    }

    /**
     * Initializes the logger with the peers username and resets global logger
     * if needed.
     *
     * @param username the username
     */
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
        }

    }

    /**
     * Log a message and an exception stack trace.
     *
     * @param lvl the log level
     * @param msg the message
     * @param e   the exception
     */
    public void log(Level lvl, String msg, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();

        log(lvl,
                String.format("%s\n%s", msg, stackTrace));
    }

    /**
     * Log a message.
     *
     * @param msg the msg
     */
    public void log(String msg) {
        log(Level.INFO, msg);
    }

    /**
     * Serializes a trigger and writes it to STDOUT, as well as calling the global trigger handler.
     * This is purely a debugging function, and if the application is running normally nothing
     * important will happen.
     *
     * However, during debugging it can do one of two things. If the debug/test is running
     * in a threaded simulation, the global trigger will be initialized and some callback function
     * will be called.
     *
     * If the debug/test is running in a networked simulation over EC2, the trigger will be written to STDOUT,
     * parsed by the test runner, and the global trigger handler will be called on the machine running the test.
     *
     * These triggers are mostly used to collect state information from various peers, such as download progress.
     *
     * @param key  the name of the trigger
     * @param args arguments to the trigger function
     * @throws Exception the exception
     */
    public void trigger(String key, Object ...args) throws Exception {
        String msg = Debugger.global().serializeTrigger(key, args);
        Object[] argsArray = new Object[args.length+1];
        for (int i = 0; i < args.length; i++) {
            argsArray[i+1] = args[i];
        }
        argsArray[0] = username;
        Debugger.global().triggerWithArray(key, argsArray);
        log(Level.INFO, msg);
    }

    /**
     * Log a message.
     *
     * @param lvl the log level
     * @param msg the message
     */
    public void log(Level lvl, String msg) {
        if(enabled) {
            String logLine = username + ": " + msg;
            logger.log(lvl, logLine);
        }
    }

    /**
     * Log a message and an exception stack trace.
     *
     * @param lvl    the log level
     * @param msg    the message
     * @param thrown the exception
     */
    public void log(Level lvl, String msg, Throwable thrown) {
        if(enabled) {
            logger.log(lvl, username + ": " + msg, thrown);
        }
    }


}
