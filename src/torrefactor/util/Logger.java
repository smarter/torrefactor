package torrefactor.util;

import torrefactor.util.Log;

public class Logger {
    private static Log log = Log.getInstance ();
    private boolean enabled = true;
    private int localFlag = 0;
    // Should we set the object which is registered with the log messages here?
    // It may be convenient if we always use the same object with the same
    // instance of this class.
    
    public Logger () {}

    public int localFlag () {
        return localFlag;
    }

    public void enableLogging () {
        this.enabled = true;
    }

    public void disableLogging () {
        this.enabled = false;
    }

    public void error (Object object, String message) {
        if (this.enabled) {
            log.log (Log.ERROR | this.localFlag, object, message);
        }
    }

    public void error (Object object, Exception e) {
        if (this.enabled) {
            log.log (Log.ERROR | this.localFlag, object,
                     e.toString());
        }
    }

    public void warning (Object object, String message) {
        if (this.enabled) {
            log.log (Log.WARNING, object, message);
        }
    }

    public void info (Object object, String message) {
        if (this.enabled) {
            log.log (Log.INFO | this.localFlag, object, message);
        }
    }

    public void debug (Object object, String message) {
        if (this.enabled) {
            log.log (Log.DEBUG | this.localFlag, object, message);
        }
    }
}

