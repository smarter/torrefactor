/* 
 *  This file is part of the Torrefactor project
 *  Copyright 2011 Guillaume Martres <smarter@ubuntu.com>
 *  Copyright 2011 Florian Vessaz <florian.vessaz@gmail.com> 
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *      1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *      2. Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */

package torrefactor.util;

import torrefactor.util.Logger;

import java.util.*;
import java.io.*;

/**
 * This class stores the configuration of Torrefactor.
 */
public class Config extends Properties {
    private static final Logger LOG = new Logger ();
    private static Config lastInstance;
    private String filePath;

    /**
     * Returns the last constructed instance of this class.
     */
    public static Config getConfig () {
        if (Config.lastInstance == null) throw new NullPointerException();
        return Config.lastInstance;
    }

    /**
     * Sets the properties to their default values in the given Properties
     * object.
     */
    private static void setDefaults (Properties p) {
        p.setProperty("ListenPort", "7979");
        p.setProperty("DHT", "true");

        p.setProperty("Ui.Swing.BasePath", System.getProperty("user.home"));
    }

    /**
     * Validates the properties and set the invalid properties to their default
     * values. Return false if some properties were set to their default.
     */
    public boolean validate () {
        boolean r = true;
        Properties defaults = new Properties();
        setDefaults(defaults);

        if (! validateInt("ListenPort", defaults)) r = false;
        if (! validateBoolean("DHT", defaults)) r = false;

        if (! validateDirectory("Ui.Swing.BasePath", defaults)) r = false;

        return r;
    }

    /**
     * Sets the properties to their default value.
     */
    public void setDefaults () {
        setDefaults(this);
    }

    public Config () {
        super();
        this.filePath = new File(
                new File(
                    new File(System.getProperty("user.home")),
                    ".torrefactor"),
                "config"
                ).getAbsolutePath();
        setDefaults();
        try {
            load();
        } catch (IOException e) {
            LOG.error("IOException while loading config from " + this.filePath);
            e.printStackTrace();
            LOG.error("Using default configuration");

            if (e instanceof FileNotFoundException) {
                try {
                    store();
                } catch (IOException f) {
                    LOG.error("IOException while trying to save default config"
                              + " to " + this.filePath);
                    f.printStackTrace();
               }
            }
        }
        Config.lastInstance = this;
    }

    public Config (String filePath) {
        super();
        this.filePath = filePath;
        setDefaults();
        try {
            load();
        } catch (IOException e) {
            LOG.error("IOException while loading config from " + this.filePath);
            e.printStackTrace();
            LOG.error("Using default configuration");
        }
        Config.lastInstance = this;
    }


    public Config (Properties defaults) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Saves the non-default properties.
     */
    public void store ()
    throws IOException, FileNotFoundException {
        File file = new File(this.filePath);
        if (! file.exists()) {
            if (! file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
        }

        OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file));
        this.store(writer, "Torrefactor config file");
        writer.close();
        LOG.info("Config saved");
    }

    /**
     * Loads and validate properties. If a property is invalid, its default is
     * used instead and the config file is rewritten so it is valid.
     */
    public void load () throws IOException, FileNotFoundException {
        File file = new File(this.filePath);
        InputStreamReader reader = new InputStreamReader(
                new FileInputStream(file));
        this.load(reader);
        reader.close();

        if (! validate()) {
            LOG.info("Some properties where invalid, saving config with valid "
                     + "values.");
            store();
        }

        LOG.info("Config loaded");
    }

    /**
     * Returns true if the property accessed by key is a valid boolean.
     * Otherwise set the property to the default value in defaults and return
     * false. The value of the properties in defaults are not validated.
     */
    private boolean validateBoolean (String key, Properties defaults) {
        String value = getProperty(key).toLowerCase();
        if (value.equals("false")) return true;
        if (value.equals("true")) return true;
        if (value.equals("on")) return true;
        if (value.equals("off")) return true;

        String dvalue = defaults.getProperty(key);
        LOG.warning("Uknown boolean value \"" + value +"\" for property \""
                    + key + "\" Using default value \"" + dvalue + "\"");
        setProperty(key, dvalue);
        return false;
    }

    /**
     * Returns the boolean represented by string or false.
     */
    private static boolean toBoolean (String string) {
        string = string.toLowerCase();
        if (string.equals("true")) return true;
        if (string.equals("on")) return true;
        //if (string.equals("false")) return false;
        //if (string.equals("off")) return false;
        return false;
    }

    /**
     * Returns the string representing the boolean.
     */
    private static String toString (boolean b) {
        if (b) return "true";
        return "false";
    }

    /**
     * Returns the property as boolean.
     */
    public boolean getPropertyBoolean (String key) {
        String value = getProperty(key);
        return toBoolean(value);
    }

    /**
     * Set the property with a boolean.
     */
    public void setPropertyBoolean (String key, boolean b) {
        setProperty(key, toString(b));
    }

    /**
     * Returns true if the property accessed by key is a valid int.
     * Otherwise set the property to the default value in defaults and return
     * false. The value of the properties in defaults are not validated.
     */
    private boolean validateInt (String key, Properties defaults) {
        String value = getProperty(key);
        try {
            Integer.valueOf(value);
            return true;
        } catch (NumberFormatException e) {
            String dvalue = defaults.getProperty(key);
            LOG.warning("Uknown int value \"" + value +"\" for property \""
                        + key + "\" Using default value \"" + dvalue + "\"");
            setProperty(key, dvalue);
            return false;
        }
    }

    /**
     * Returns the int represented by string or 0.
     */
    private static int toInt (String string) {
        int i;
        try {
            i = Integer.valueOf(string);
            return i;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Returns the string representing the int.
     */
    private static String toString (int i) {
        return Integer.toString(i);
    }

    /**
     * Returns the property as int.
     */
    public int getPropertyInt(String key) {
        String value = getProperty(key);
        return toInt(value);
    }

    /**
     * Sets the property with a int.
     */
    public void setProperty(String key, int i) {
        setProperty(key, toString(i));
    }

    /**
     * Returns true if the property accessed by key is a valid directory.
     * Otherwise set the property to the default value in defaults and return
     * false. The value of the properties in defaults are not validated.
     */
    private boolean validateDirectory (String key, Properties defaults) {
        String value = getProperty(key);
        File file = new File(value);
        if (! file.isDirectory()) {
            String dvalue = defaults.getProperty(key);
            LOG.warning("Property " + key + "=\"" + value + "\" is not a "
                        + "directory. Using default value \"" + dvalue
                        + "\" instead.");
            setProperty(key, dvalue);
            return false;
        }
        return true;
    }
}

