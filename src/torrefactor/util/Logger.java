package torrefactor.util;

import torrefactor.util.Log;

/**
 * Handles log message from an instance of an object.
 */
public class Logger {
    private static Log log = Log.getInstance ();
    private boolean enabled = true;
    private int localFlag = 0;
    private Object headerObject;

    /**
     * Create a new logger that will log with the class name of the caller as
     * header.
     */
    public Logger () {
        this.headerObject = sun.reflect.Reflection.getCallerClass(2).getName();
    }

    /**
     * Create a new logger that will log with the specified header.
     */
    public Logger (Object header) {
        this.headerObject = header;
    }

    /**
     * Set the header to log message with.
     */
    public void setHeader (Object header) {
        this.headerObject = header;
    }

    /**
     * Returns the flags that will be used to log each message.
     */
    public int localFlag () {
        return localFlag;
    }

    /**
     * Enable logging with this instance.
     */
    public void enableLogging () {
        this.enabled = true;
    }

    /**
     * Disable logging with this instance.
     */
    public void disableLogging () {
        this.enabled = false;
    }

    /**
     * Log an error.
     */
    public void error (String message) {
        error (this.headerObject, message);
    }

    /**
     * Log an error with object as header.
     */
    public void error (Object object, String message) {
        if (this.enabled) {
            log.log (Log.ERROR | this.localFlag, object, message);
        }
    }

    /**
     * Log the specified object as an error.
     */
    public void error (Object obj) {
        if (this.enabled) {
            error (this.headerObject, obj);
        }
    }

    /**
     * Log the specified object as an error using the specified header.
     */
    public void error (Object header, Object obj) {
        if (this.enabled) {
            log.log (Log.ERROR | this.localFlag, header, String.valueOf(obj));
        }
    }

    /**
     * Log the specified warning message.
     */
    public void warning (String message) {
        warning (this.headerObject, message);
    }

    /**
     * Log the specified waring message with object as header.
     */
    public void warning (Object object, String message) {
        if (this.enabled) {
            log.log (Log.WARNING, object, message);
        }
    }

    /**
     * Log the specified object as warning.
     */
    public void warning (Object obj) {
        if (this.enabled) {
            warning (this.headerObject, obj);
        }
    }

    /**
     * Log the specified object with header as header.
     */
    public void warning (Object header, Object obj) {
        if (this.enabled) {
            log.log (Log.WARNING | this.localFlag, header, String.valueOf(obj));
        }
    }

    /**
     * Log the specified information message.
     */
    public void info (String message) {
        info (this.headerObject, message);
    }

    /**
     * Log the specified information message with object as header.
     */
    public void info (Object object, String message) {
        if (this.enabled) {
            log.log (Log.INFO | this.localFlag, object, message);
        }
    }

    /**
     * Log the specified object as information message.
     */
    public void info (Object obj) {
        if (this.enabled) {
            info (this.headerObject, obj);
        }
    }

    /**
     * Log the specified object as information message with header as header.
     */
    public void info (Object header, Object obj) {
        if (this.enabled) {
            log.log (Log.INFO | this.localFlag, header, String.valueOf(obj));
        }
    }

    /**
     * Log the specified message as debug.
     */
    public void debug (String message) {
        debug (this.headerObject, message);
    }

    /**
     * Log the specified message as debug with object as header.
     */
    public void debug (Object object, String message) {
        if (this.enabled) {
            log.log (Log.DEBUG | this.localFlag, object, message);
        }
    }

    /**
     * Log the specified object as debug.
     */
    public void debug (Object obj) {
        if (this.enabled) {
            debug (this.headerObject, obj);
        }
    }

    /**
     * Log the specified object as debug with header as header.
     */
    public void debug (Object header, Object obj) {
        if (this.enabled) {
            log.log (Log.DEBUG | this.localFlag, header, String.valueOf(obj));
        }
    }
}

