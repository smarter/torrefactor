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

import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * A class containing static helper methods to create DatagramSocket
 */
public class DatagramSockets {
    private static Logger LOG = new Logger();
    /**
     * See http://www.iana.org/assignments/port-numbers for port range
     * definitions.
     */
    public final static int IANA_RESERVED_END = 1024;
    public final static int IANA_PRIVATE_START = 49152;
    public final static int IANA_MAX_PORT = 65535;

    /**
     * Returns a DatagramSocket bound to the first boundable port of the IANA
     * private port range or null if no port could be bounded.
     */
    public static DatagramSocket getDatagramSocket () { return
        getDatagramSocket (IANA_PRIVATE_START);
    }

    /**
     * Returns a DatagramSocket bound to the specified port or the next
     * boundable port or null if no port could be bounded. Does not bind to a
     * port in the reserved range.
     */
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
