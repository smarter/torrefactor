package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class HttpTracker extends Tracker {
    private String host;
    private int port;
    private String path;
    private String trackerId; //TODO: what's this thing?

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
        this.path = "";
        if (uriObject.getRawPath() != null) {
            this.path += uriObject.getRawPath();
        }
        if (uriObject.getRawQuery() != null) {
            this.path += "?" + uriObject.getRawQuery();
        }

        //DEBUG
        System.out.println("host: " + this.host);
        System.out.println("port: " + this.port);
        System.out.println("path: " + this.path);
    }

    public ArrayList<ArrayList> announce(Torrent torrent, Event event)
    throws ProtocolException, InvalidBencodeException, IOException {
        // Construct request
        String infoHash = urlEncode(torrent.infoHash);
        String peerId = urlEncode(torrent.peerManager.peerId);
        Object[] format = { infoHash, peerId, this.port,
                            Integer.toString(torrent.uploaded),
                            Integer.toString(torrent.downloaded),
                            Integer.toString(torrent.left),
                            event.toString() };
        String params = String.format("?info_hash=%s&peer_id=%s&port=%s"
                                      + "&uploaded=%s&downloaded=%s"
                                      + "&left=%s&event=%s&compact=1",
                                      format);
        System.out.println("Request params: " + params);

        // Announce
        Map<String, Bencode> answerMap = null;
        try {
            answerMap = httpAnnounce(this.path, params);
        } catch (IOException e) {
            this.statusMessage = e.getMessage();
            throw e;
        } catch (InvalidBencodeException e) {
            this.statusMessage = "Didn't understood response: "
                                 + e.getMessage();
            throw e;
        }

        if (answerMap == null) {
            throw new IOException("Couldn't connect to any tracker.");
        }

        // Parse response
        if (answerMap.containsKey("failure reason")) {
            throw new ProtocolException(new String(answerMap.get("failure reason").toByteArray()));
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
        ArrayList<ArrayList> peersList = new ArrayList();
        // FIXME: explain why we do this if-else
        if (answerMap.get("peers").toObject() instanceof List) {
            List<Bencode> peers = answerMap.get("peers").toList();
            for (int i = 0; i < peers.size(); i++) {
                Map<String, Bencode> peerMap = peers.get(i).toMap();
                byte[] ip = peerMap.get("ip").toByteArray();
                int port = peerMap.get("port").toInt();
                ArrayList peer = new ArrayList(2);
                peer.add(ip);
                peer.add(new Integer(port));
                peersList.add(peer);
            }
        } else if (answerMap.get("peers").toObject() instanceof byte[]) {
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
                ArrayList peer = new ArrayList(2);
                peer.add(ip);
                peer.add(new Integer(port));
                peersList.add(peer);
            }
        } else {
            this.statusMessage = "Host replied but wasn't understood.";
            throw new ProtocolException("unrecognized peers format");
        }

        this.statusMessage = "Number of peers received: " + peersList.size();

        return peersList;
    }

    private Map<String, Bencode> httpAnnounce(String path, String params)
    throws IOException, InvalidBencodeException {
        //PROJECT: we really should use URLConnection here, but for the project
        // we're required to use Socket
        // Aka: «I deny you to use the thing you should use and you're stupid
        //       enough to do so.»

        // Setup socket
        InetAddress address = InetAddress.getByName(this.host);
        Socket socket = new Socket(address, this.port);
        BufferedInputStream input = new BufferedInputStream(
                                                socket.getInputStream());
        BufferedOutputStream output = new BufferedOutputStream(
                                                  socket.getOutputStream());
        
        // Send request
        System.out.println("path: " + this.path); //DEBUG
        System.out.println("params: " + params); //DEBUG
        String getRequest = "GET " + this.path + params + " HTTP/1.0\n\r\n\r\n";
        System.out.println(getRequest);
        output.write(getRequest.getBytes());
        output.flush();

        // Parse headers
        // TODO: Check http status code
        while (true) {
            int c = input.read();
            if (c == -1) throw new IOException("Unexpected end of stream");
            if (c == (int) ' ') break;
        }

        //Check http status code
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

        while (true) {
            int c = input.read();
            if (c == -1) throw new IOException("Unexpected end of stream");
            if (c == (int) '\r'
                && input.read() == (int) '\n'
                && input.read() == (int) '\r'
                && input.read() == (int) '\n') break;
        }

//        System.out.println("----------------------------");
//        while (true) {
//            int c = input.read();
//            System.out.print((char)c);
//            if (c == -1) System.exit(123);
//        }

        Map<String, Bencode> answerMap = Bencode.decodeDict(input);
        input.close();
        output.close();
        socket.close();
        return answerMap;
    }

    private static String urlEncode(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append("%");
            if ((array[i] & 0xF0) == 0) sb.append("0");
            sb.append(Integer.toHexString(array[i] & 0xFF));
        }
        return sb.toString();
    }

}
