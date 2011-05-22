package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Handle an UDP Bittorent tracker as described in
 * http://bittorrent.org/beps/bep_0015.html
 */
public class UdpTracker extends Tracker {
    private static Logger LOG = new Logger();
    private static Config CONF = Config.getConfig();
    private long connection_id;
    private byte[] info_hash;
    private byte[] peer_id;
    private int numWant = 100;
    private long extensions = 0;
    private String host;
    private int port;
    private DatagramSocket socket;
    private final static int UDP_SOCKET_TIMEOUT = 15000;
    private final static long UDP_PROTOCOL_MAGICK = 0x41727101980L;
    private final static int UDP_ACTION_CONNECTION = 0;
    private final static int UDP_ACTION_ANNOUNCE = 1;
    private final static int UDP_ACTION_ERROR = 3;

    /**
     * Create a new tracker for the given uri and use a uniqKey
     *
     * @param _uri        the uri of the tracker
     * @param uniqKey    our own uniqKey
     */
    public UdpTracker (String _uri, int uniqKey)
    throws UnsupportedOperationException, IllegalArgumentException,
           UnknownHostException {
        this (_uri);
        this.uniqKey = uniqKey;
    }

    /**
     * Create a new tracker for the given uri
     *
     * @param _uri    the uri of the tracker
     */
    public UdpTracker (String _uri)
    throws UnsupportedOperationException, IllegalArgumentException,
           UnknownHostException {
        this.uri = _uri;
        URI uriObject = null;
        try {
            uriObject = new URI(_uri);
        } catch (URISyntaxException e ) {
            e.printStackTrace();
            throw new UnsupportedOperationException ("Cannot parse uri \"" 
                                                     + uri + "\"");
        }

        //Test whether we support this uri
        if ( ! uriObject.getScheme().equals("udp")) {
            throw new UnsupportedOperationException ("UdpTracker does not "
                    + "support the uri \"" + _uri + "\"");
        }

        this.host = uriObject.getHost();
        this.port = uriObject.getPort();
        LOG.debug(this, "Uri : " + this.uri);
        LOG.debug(this, "Host: " + this.host);
        LOG.debug(this, "Port: " + this.port);
    }

    /**
     * {@inheritDoc}
     */
    public ArrayList<Pair<byte[], Integer>>
    announce (Torrent torrent, Event event) {
        ArrayList<Pair<byte[], Integer>> peers = null;
        try {
            peers = udpAnnounce(torrent, event);
        } catch (Exception e ) {
            LOG.debug(this, "Got exception while announcing to \""
                               + this.uri + "\"");
            e.printStackTrace();
        }
        if (peers != null) LOG.debug(this, "Got " + peers.size()
                                           + " peers.");
        updateActive();
        return peers;
    }

    /**
     * Send a packet to the tracker and return the response of the specified
     * length.
     */
    private DatagramPacket
    sendReceive (DatagramPacket packet, int recvlen, byte[] transactionId)
    throws IOException, SocketException {
        // Send packet, wait UDP_SOCKET_TIMEOUT seconds for answer
        // When no response retry three times and then abandon

        this.socket.setSoTimeout(UDP_SOCKET_TIMEOUT);

        for (int i=0; i<3; i++) {
                this.socket.send(packet);

            // Connect reply
            byte[] buffer = new byte[recvlen];
            LOG.debug(this, "Len: " + buffer.length);
            packet = new DatagramPacket(buffer, buffer.length);
            try {
                this.socket.receive(packet);
            }
            catch (SocketTimeoutException e) {
                LOG.debug(this, e.getMessage());
            }
            if ( ! udpCheckTransactionId(packet, transactionId)) {
                // Got a packet with different transactionId
                LOG.debug(this, "TransactionId do not match.");
                i--;
                continue;
            }
            break;
        }
        return packet;
    }


    /**
     * Makes the actual udp announce to the tracker, returns a peer list or
     * null
     */
    public ArrayList<Pair<byte[],Integer>>
    udpAnnounce (Torrent torrent, Event event)
    throws IOException, SocketException {
        DatagramPacket packet;
        byte[] transactionId;
        byte[] buffer;
        byte[] connectionId;
        Random random = new Random();

        // Get socket
        this.socket = DatagramSockets.getDatagramSocket(this.port);
        if (this.socket == null) return null;
        int lport = CONF.getPropertyInt("ListenPort");

        // Connect
        buffer = new byte[16];
        System.arraycopy(ByteArrays.fromLong(UDP_PROTOCOL_MAGICK), 0,
                                  buffer, 0, 8);
        System.arraycopy(ByteArrays.fromInt(UDP_ACTION_CONNECTION), 0,
                         buffer, 8, 4);
        transactionId = ByteArrays.fromInt(random.nextInt());
        System.arraycopy(transactionId, 0, buffer, 12, 4);
        InetAddress address = InetAddress.getByName(this.host);
        packet = new DatagramPacket(buffer, buffer.length, address, this.port);

        packet = sendReceive(packet, 16, transactionId);

        // Parse reply
        byte[] action = new byte[4];
        System.arraycopy(packet.getData(), 4, action, 0, 4);
        if (action[3] == (byte) UDP_ACTION_ERROR) {
            // error
            LOG.error(this, udpGetErrorMessage(packet));
        }
        // Get connection_id
        connectionId = new byte[8];
        System.arraycopy(packet.getData(), 8, connectionId, 0, 8);


        // Announce
        buffer = new byte[100];
        System.arraycopy(connectionId, 0, buffer, 0, 8);
        System.arraycopy(ByteArrays.fromInt(UDP_ACTION_ANNOUNCE), 0,
                         buffer, 8, 4);
        transactionId = ByteArrays.fromInt(random.nextInt());
        System.arraycopy(transactionId, 0, buffer, 12, 4);
        System.arraycopy(torrent.infoHash, 0, buffer, 16, 20);
        System.arraycopy(torrent.peerManager.peerId(), 0, buffer, 36, 20);
        System.arraycopy(ByteArrays.fromLong(torrent.downloaded()), 0,
                         buffer, 56, 8);
        System.arraycopy(ByteArrays.fromLong(torrent.left()), 0,
                         buffer, 64, 8);
        System.arraycopy(ByteArrays.fromLong(torrent.uploaded()), 0,
                         buffer, 72, 8);
        System.arraycopy(ByteArrays.fromInt(event.ordinal()), 0,
                         buffer, 80, 4);
        System.arraycopy(address.getAddress(), 0, buffer, 84, 4);
        //byte[] key = ByteArrays.fromInt(random.nextInt());
        byte[] key = ByteArrays.fromInt(this.uniqKey);
        System.arraycopy(key, 0, buffer, 88, 4);
        System.arraycopy(ByteArrays.fromInt(this.numWant), 0, buffer, 92, 4);
        System.arraycopy(ByteArrays.fromInt(lport), 2, buffer, 96, 2);
        System.arraycopy(ByteArrays.fromLong(extensions), 0, buffer, 98, 2);
        packet = new DatagramPacket(buffer, buffer.length, address, this.port);

        packet = sendReceive(packet, 20+6*this.numWant,
                                   transactionId);

        action = new byte[4];
        System.arraycopy(packet.getData(), 0, action, 0, 4);
        if (action[3] == (byte) UDP_ACTION_ERROR) {
            // error
            LOG.error(this, udpGetErrorMessage(packet));
        }
        byte[] b = new byte[4];
        System.arraycopy(buffer, 8, b, 0, 4);
        int interval = ByteArrays.toInt(b);
        System.arraycopy(buffer, 12, b, 0, 4);
        int leechers = ByteArrays.toInt(b);
        System.arraycopy(buffer, 16, b, 0, 4);
        int seeders = ByteArrays.toInt(b);

        // Parse peer-port segments
        ArrayList<Pair<byte[], Integer>> peerList =
            new ArrayList<Pair<byte[], Integer>>();
        int i = 20;
        while (i < packet.getLength()) {
            byte[] peerAddress = new byte[4];
            System.arraycopy(packet.getData(), i, peerAddress, 0, 4);
            byte[] portArray = new byte[2];
            System.arraycopy(packet.getData(), i+4, portArray, 0, 2);
            int peerPort = ByteArrays.toShortInt(portArray);
            Pair<byte[], Integer> peer =
                new Pair<byte[], Integer>(peerAddress, peerPort);
            peerList.add(peer);
            i += 6;
        }

        return peerList;
    }

    /**
     * Returns true if the packet matches the transaction id.
     */
    private static boolean udpCheckTransactionId (DatagramPacket packet,
                                                  byte[] id) {
        return arraycmp(packet.getData(), 4, id, 0, 4);
    }

    /**
     * Returns the error string sent by the tracker in the packet.
     */
    private static String udpGetErrorMessage (DatagramPacket packet) {
        byte[] data = packet.getData();
        byte[] array = new byte[data.length];
        System.arraycopy(data, 8, array, 0, data.length);
        return new String(array);
    }

    /**
     * Returns true if array1 starting at offset1 equals array2 starting at
     * offset2 for the given size; 'size' data must be available at the given
     * offsets, no check is done to verify this.
     */
    private static boolean arraycmp(byte[] array1, int offset1,
                                    byte[] array2, int offset2, int size) {
        for (int i=0; i<size; i++) {
            if ( ! (array1[i+offset1] == array2[i+offset2])) {
                return false;
            }
        }
        return true;
    }
}
