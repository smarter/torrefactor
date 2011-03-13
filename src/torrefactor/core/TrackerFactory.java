package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class TrackerFactory {

    public static Tracker getTracker (String uri) {
        try {
            if (uri.substring(0, 6).equals("udp://")) {
                return new UdpTracker (uri);
            } else if (uri.substring(0, 7).equals("http://")) {
                return new HttpTracker (uri);
            }/* else if (uri.substring(0, 8).equals("https://")) {
                return new HttpTracker (uri);
            }*/
            throw new UnsupportedOperationException (
                    "Don't know how to handle uri \"" + uri + "\"");
        } catch (Exception e) {
            return null;
        }
    }
}
