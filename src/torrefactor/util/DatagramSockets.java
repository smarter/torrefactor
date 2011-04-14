package torrefactor.util;

import java.net.DatagramSocket;
import java.net.SocketException;

public class DatagramSockets {
    private static Logger LOG = new Logger();
    /* See http://www.iana.org/assignments/port-numbers for port range
     * definitions. */
    public final static int IANA_RESERVED_END = 1024;
    public final static int IANA_PRIVATE_START = 49152;
    public final static int IANA_MAX_PORT = 65535;

    /* Returns a DatagramSocket bound to the first boundable port of the IANA
     * private port range or null if no port could be bounded. */
    public static DatagramSocket getDatagramSocket () { return
        getDatagramSocket (IANA_PRIVATE_START);
    }

    /* Returns a DatagramSocket bound to the specified port or the next
     * boundable port or null if no port could be bounded. Does not bind to a
     * port in the reserved range. */
    public static DatagramSocket getDatagramSocket (int port) {
        if (port < IANA_RESERVED_END) {
            port = IANA_PRIVATE_START;
        }
        DatagramSocket socket = null;
        while (socket == null) {
            try {
                socket = new DatagramSocket(port);
            }
            catch (SocketException e) {
                LOG.debug(DatagramSockets.class, e.getMessage());
                if (port < IANA_PRIVATE_START) {
                    port = IANA_PRIVATE_START;
                } else if (port < IANA_MAX_PORT) {
                    port ++;
                } else {
                   LOG.warning(DatagramSockets.class,  
                               "No port available in private port range.");
                    return null;
                }
            }
        }
        return socket;
    }
}
