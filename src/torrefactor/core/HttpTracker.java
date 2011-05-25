package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * This class handles both standard and compact responses from
 * HTTP trackers.
 *
 * References:
 * - Standard format:
     http://wiki.theory.org/BitTorrentSpecification#Tracker_HTTP.2FHTTPS_Protocol
 * - Compact format: http://bittorrent.org/beps/bep_0023.html
 */
public class HttpTracker extends Tracker {
    private static Logger LOG = new Logger();
    private static Config CONF = Config.getConfig();
    private String host;
    private int port;
    private String path;
    private String trackerId; //TODO: what's this thing?
    private static final int CONNECT_TIMEOUT = 5*1000; //in milliseconds

    /**
     * Create a new tracker for the given uri and use a uniqKey
     *
     * @param _uri        the uri of the tracker
     * @param uniqKey    our own uniqKey
     */
    public HttpTracker (String _uri, int uniqKey)
    throws UnsupportedOperationException, IllegalArgumentException,
           UnknownHostException, URISyntaxException {
        this (_uri);
        this.uniqKey = uniqKey;
    }

    /**
     * Create a new tracker for the given uri
     *
     * @param _uri    the uri of the tracker
     */
    public HttpTracker (String _uri)
    throws UnsupportedOperationException, IllegalArgumentException,
           UnknownHostException, URISyntaxException {
        this.uri = _uri;
        URI uriObject = new URI(_uri);

        //Test whether we support this uri
        if ( ! uriObject.getScheme().equals("http")) {
            throw new UnsupportedOperationException ("HttpTracker does not "
                    + "support the uri \"" + _uri + "\"");
        }

        this.host = uriObject.getHost();
        this.port = uriObject.getPort();
        if (this.port == -1) {
            this.port = 80;
        }
        this.path = "";
        if (uriObject.getRawPath() != null) {
            this.path += uriObject.getRawPath();
        }
        if (uriObject.getRawQuery() != null) {
            this.path += "?" + uriObject.getRawQuery();
        }

        //DEBUG
        LOG.debug(this, "host: " + this.host);
        LOG.debug(this, "port: " + this.port);
        LOG.debug(this, "path: " + this.path);
    }

    /**
     * {@inheritDoc}
     */
    public ArrayList<Pair<byte[], Integer>>
    announce(Torrent torrent, Event event)
    throws ProtocolException, InvalidBDecodeException, IOException {
        // Construct request
        String infoHash = urlEncode(torrent.infoHash);
        String peerId = urlEncode(torrent.peerManager.peerId());
        StringBuilder sb = new StringBuilder(200);
        sb.append("?info_hash=");
        sb.append(infoHash);
        sb.append("&port=");
        sb.append(CONF.getPropertyInt("ListenPort"));
        sb.append("&numwant=200");
        sb.append("&peer_id=");
        sb.append(peerId);
        sb.append("&key=");
        sb.append(this.uniqKey);
        sb.append("&uploaded=");
        sb.append(torrent.uploaded());
        sb.append("&downloaded=");
        sb.append(torrent.downloaded());
        sb.append("&left=");
        sb.append(torrent.left());
        sb.append("&compact=1");
        if (event != Event.none) {
            sb.append("&event=");
            sb.append(event.toString());
        }
        String params = sb.toString();
        LOG.debug(this, "Request params: " + params);

        // Announce
        Map<String, BValue> answerMap = null;
        try {
            answerMap = httpAnnounce(this.path, params);
        } catch (IOException e) {
            this.statusMessage = e.getMessage();
            throw e;
        } catch (InvalidBDecodeException e) {
            this.statusMessage = "Didn't understood response: "
                                 + e.getMessage();
            throw e;
        }

        if (answerMap == null) {
            LOG.error(this, "answesMap is null!!!");
            return null;
        }

        // Parse response
        if (answerMap.containsKey("failure reason")) {
            throw new ProtocolException(
                    new String(answerMap.get("failure reason").toByteArray()));
        }
        if (answerMap.containsKey("warning message")) {
            this.statusMessage = new String(
                                answerMap.get("warning message").toByteArray());
        }
        this.interval = answerMap.get("interval").toInt();
        if (answerMap.containsKey("tracker id")) {
            this.trackerId = new String(
                                     answerMap.get("tracker id").toByteArray());
        }
        this.seeders = answerMap.get("complete").toInt();
        this.leechers = answerMap.get("incomplete").toInt();

        // Parse peers
        ArrayList<Pair<byte[], Integer>> peersList = new ArrayList<Pair<byte[], Integer>>();

        if (answerMap.get("peers").toObject() instanceof List) { // Classic response
            List<BValue> peers = answerMap.get("peers").toList();
            for (int i = 0; i < peers.size(); i++) {
                Map<String, BValue> peerMap = peers.get(i).toMap();
                byte[] ip = peerMap.get("ip").toByteArray();
                int port = peerMap.get("port").toInt();
                Pair<byte[], Integer> peer = new Pair<byte[], Integer>(ip, port);
                peersList.add(peer);
            }
        } else if (answerMap.get("peers").toObject() instanceof byte[]) { // Compact response
            byte[] peersArray = answerMap.get("peers").toByteArray();
            int i = 0;
            while (i < peersArray.length) {
                byte[] ip = new byte[4];
                System.arraycopy(peersArray, i, ip, 0, 4);
                i += 4;
                byte[] portArray = new byte[2];
                System.arraycopy(peersArray, i, portArray, 0, 2);
                i += 2;
                int port = (portArray[0] & 0xFF) << 8 | portArray[1] & 0xFF;
                Pair<byte[], Integer> peer = new Pair<byte[], Integer>(ip, port);
                peersList.add(peer);
            }
        } else {
            this.statusMessage = "Host replied but wasn't understood.";
            throw new ProtocolException("unrecognized peers format");
        }

        this.statusMessage = "Number of peers received: " + peersList.size();

        updateActive();
        return peersList;
    }

    /**
     * Makes the actual http announce to the tracker, returns the answer map.
     */
    private Map<String, BValue> httpAnnounce(String path, String params)
    throws IOException, InvalidBDecodeException {
        //PROJECT: we really should use URLConnection here, but for the project
        // we're required to use Socket
        // Aka: «I deny you to use the thing you should use and you're stupid
        //       enough to do so.»

        // Setup socket
        InetAddress address = InetAddress.getByName(this.host);
        Socket socket = null;
        try {
            SocketAddress socketAddress = new InetSocketAddress(address, this.port);
            socket = new Socket();
            socket.connect(socketAddress, CONNECT_TIMEOUT);
        } catch (ConnectException e) {
            LOG.warning(this, e.getMessage());
            return null;
        }
        BufferedInputStream input = new BufferedInputStream(
                                                socket.getInputStream());
        BufferedOutputStream output = new BufferedOutputStream(
                                                  socket.getOutputStream());

        // Send request
        LOG.debug(this, "path: " + this.path); //DEBUG
        LOG.debug(this, "params: " + params); //DEBUG
        String getRequest = "GET " + this.path + params + " HTTP/1.0\r\n"
                            + "Host: " + this.host + "\r\n"
                            + "User-Agent: Torrefactor/0.1\r\n\r\n";
        LOG.debug(this, getRequest);
        output.write(getRequest.getBytes());
        output.flush();

        // Parse headers
        while (true) {
            int c = input.read();
            if (c == -1) throw new IOException("Unexpected end of stream");
            if (c == (int) ' ') break;
        }

        // Check http status code
        char[] scode = new char[3];
        for (int i=0; i<3; i++) {
            int c = input.read();
            if (c == -1) throw new IOException("Unexpected end of steam");
            scode[i] = (char) c;
        }

        int code = Integer.parseInt(new String(scode));
        if (code >=300) {
            throw new IOException("Got http status code " + code);
        }

        // Skip until end of header
        while (true) {
            int c = input.read();
            if (c == -1) throw new IOException("Unexpected end of stream");
            if (c == (int) '\r'
                && input.read() == (int) '\n'
                && input.read() == (int) '\r'
                && input.read() == (int) '\n') break;
        }

        // Decode response, clean and return
        Map<String, BValue> answerMap = BDecode.decodeDict(input);
        input.close();
        output.close();
        socket.close();
        return answerMap;
    }

    /**
     * Encode a byte array into url format.
     */
    private static String urlEncode(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            //Range of characters supported by trackers
            if (('0' <= (char) array[i] && (char) array[i] <= '9')
                || ('A' <= (char) array[i] && (char) array[i] <= 'Z')
                || ('a' <= (char) array[i] && (char) array[i] <= 'z')
                || (char) array[i] == '.' || (char) array[i] == '-'
                || (char) array[i] == '_' || (char) array[i] == '~') {
                sb.append((char) array[i]);
                continue;
            }
            sb.append("%");
            if ((array[i] & 0xF0) == 0) sb.append("0");
            sb.append(Integer.toHexString(array[i] & 0xFF));
        }
        return sb.toString();
    }

}
