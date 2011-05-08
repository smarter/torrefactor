package torrefactor.util;

import torrefactor.util.Log;

public class Logger {
    private static Log log = Log.getInstance ();
    private boolean enabled = true;
    private int localFlag = 0;
    private Object headerObject;
    
    public Logger () {
        this.headerObject = sun.reflect.Reflection.getCallerClass(2).getName();
    }

    public Logger (Object header) {
        this.headerObject = header;
    }

    public void setHeader (Object header) {
        this.headerObject = header;
    }

    public int localFlag () {
        return localFlag;
    }

    public void enableLogging () {
        this.enabled = true;
    }

    public void disableLogging () {
        this.enabled = false;
    }

    public void error (String message) {
        error (this.headerObject, message);
    }

    public void error (Object object, String message) {
        if (this.enabled) {
            log.log (Log.ERROR | this.localFlag, object, message);
        }
    }

    public void error (Exception e) {
        error (this.headerObject, e);
    }

    public void error (Object object, Exception e) {
        if (this.enabled) {
            log.log (Log.ERROR | this.localFlag, object,
                     e.toString());
        }
    }

    public void warning (String message) {
        warning (this.headerObject, message);
    }

    public void warning (Object object, String message) {
        if (this.enabled) {
            log.log (Log.WARNING, object, message);
        }
    }

    public void info (String message) {
        info (this.headerObject, message);
    }

    public void info (Object object, String message) {
        if (this.enabled) {
            log.log (Log.INFO | this.localFlag, object, message);
        }
    }

    public void debug (String message) {
        debug (this.headerObject, message);
    }

    public void debug (Object object, String message) {
        if (this.enabled) {
            log.log (Log.DEBUG | this.localFlag, object, message);
        }
    }
}

