package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class UdpTracker extends Tracker {
    private static Log LOG = Log.getInstance();
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

    public UdpTracker (String _uri, int uniqKey)
    throws UnsupportedOperationException, IllegalArgumentException,
           UnknownHostException {
        this (_uri);
        this.uniqKey = uniqKey;
    }

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
        LOG.log(Log.DEBUG, this, "Uri : " + this.uri);
        LOG.log(Log.DEBUG, this, "Host: " + this.host);
        LOG.log(Log.DEBUG, this, "Port: " + this.port);
    }

    public ArrayList<Pair<byte[], Integer>> announce (Torrent torrent, Event event) {
        ArrayList<Pair<byte[], Integer>> peers = null;
        try {
            peers = udpAnnounce(torrent, event);
        } catch (Exception e ) {
            LOG.log(Log.WARNING, this, "Got exception while announcing to \""
                               + this.uri + "\"");
            e.printStackTrace();
        }
        if (peers != null) LOG.log(Log.DEBUG, this, "Got " + peers.size()
                                              + " peers."); //DEBUG
        return peers;
    }

    private DatagramPacket sendReceive (DatagramPacket packet, int recvlen,
                                        byte[] transactionId)
    throws IOException, SocketException {
        // Send packet, wait UDP_SOCKET_TIMEOUT seconds for answer
        // When no response retry three times and then abandon

        this.socket.setSoTimeout(UDP_SOCKET_TIMEOUT);

        for (int i=0; i<3; i++) {
                this.socket.send(packet);

            // Connect reply
            byte[] buffer = new byte[recvlen];
            LOG.log(Log.DEBUG, this, "Len: " + buffer.length);
            packet = new DatagramPacket(buffer, buffer.length);
            try {
                this.socket.receive(packet);
            }
            catch (SocketTimeoutException e) {
                LOG.log(Log.DEBUG, this, e.getMessage()); //DEBUG
            }
            if ( ! udpCheckTransactionId(packet, transactionId)) {
                // Got a packet with different transactionId
                LOG.log(Log.DEBUG, this, "TransactionId do not match."); //DEBUG
                i--;
                continue;
            }
            break;
        }
        return packet;
    }


  public ArrayList<Pair<byte[],Integer>> udpAnnounce (Torrent torrent, Event event)
  throws IOException, SocketException {
        DatagramPacket packet;
        byte[] transactionId;
        byte[] buffer;
        byte[] connectionId;
        Random random = new Random();

        // Get socket
        this.socket = DatagramSockets.getDatagramSocket(this.port);
        if (this.socket == null) return null;
        int lport = this.socket.getPort();

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
            LOG.log(Log.ERROR, this, udpGetErrorMessage(packet));
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
        System.arraycopy(torrent.peerManager.peerId, 0, buffer, 36, 20);
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
            LOG.log(Log.ERROR, this, udpGetErrorMessage(packet));
        }
        byte[] b = new byte[4];
        System.arraycopy(buffer, 8, b, 0, 4);
        int interval = ByteArrays.toInt(b);
        System.arraycopy(buffer, 12, b, 0, 4);
        int leechers = ByteArrays.toInt(b);
        System.arraycopy(buffer, 16, b, 0, 4);
        int seeders = ByteArrays.toInt(b);

        // Parse peer-port segments
        ArrayList<Pair<byte[], Integer>> peerList = new ArrayList<Pair<byte[], Integer>>();
        int i = 20;
        while (i <= packet.getLength()) {
            byte[] peerAddress = new byte[4];
            System.arraycopy(packet.getData(), i, peerAddress, 0, 4);
            byte[] portArray = new byte[2];
            System.arraycopy(packet.getData(), i+4, portArray, 0, 2);
            int peerPort = ByteArrays.toShortInt(portArray);
            Pair<byte[], Integer> peer = new Pair<byte[], Integer>(peerAddress, peerPort);
            peerList.add(peer);
            i += 6;
        }

        return peerList;
    }

    private static boolean udpCheckTransactionId (DatagramPacket packet,
                                                  byte[] id) {
        return arraycmp(packet.getData(), 4, id, 0, 4);
    }

    private static String udpGetErrorMessage (DatagramPacket packet) {
        byte[] data = packet.getData();
        byte[] array = new byte[data.length];
        System.arraycopy(data, 8, array, 0, data.length);
        return new String(array);
    }

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
