
package torrefactor.core;
import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class PeerManager extends Thread {
    public enum TrackerEvent {
        started, stopped, completed
    }

    private Torrent torrent;
    private Map<InetAddress, Peer> peerMap;
    private Map<InetAddress, Peer> activeMap;

    byte[] peerId = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 };
    int port = 6881;
    int interval;
    String trackerId;
    int seeders;
    int leechers;
    static final int MAX_PEERS = 25;
    int delay = 300000; //  milliseconds

    public PeerManager(Torrent _torrent) {
        this.torrent = _torrent;
        this.peerMap = new HashMap<InetAddress, Peer>();
        this.activeMap = new HashMap<InetAddress, Peer>();
    }

    public void run() {
        try {
            announceTracker(TrackerEvent.started);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        for (Map.Entry<InetAddress, Peer> peerEntry : activeMap.entrySet()) {
            this.torrent.downloaded += peerEntry.getValue().popDownloaded();
            this.torrent.uploaded += peerEntry.getValue().popUploaded();
            if (peerEntry.getValue().wasDisconnected()) {
                activeMap.remove(peerEntry.getKey());
            }
            // HACK: for testing only, real algorithm will not call addBlock until we actually get data
            int freeOffset = this.torrent.pieceManager.getFreeBlock();
            int freePieceIndex = freeOffset / this.torrent.dataManager.pieceLength;
            int freePieceOffset = freeOffset % this.torrent.dataManager.pieceLength;
            try {
                peerEntry.getValue().sendRequest(freePieceIndex, freePieceOffset, (1 << 14)); // should be made asynchronous
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            this.torrent.pieceManager.addBlock(freePieceIndex, freePieceOffset, (1 << 14));
        }
        int i = MAX_PEERS - activeMap.size();
        for (Map.Entry<InetAddress, Peer> peerEntry : peerMap.entrySet()) {
            peerEntry.getValue().start();
            activeMap.put(peerEntry.getKey(), peerEntry.getValue());
            i--;
            if (i == 0) break;
        }
        try {
            sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }
    }

    public void stopDownload() {
    }

    public void announceTracker(TrackerEvent event) throws ProtocolException, InvalidBencodeException,
                                                           IOException {
        String info_hash = urlEncode(torrent.infoHash);
        String peer_id = urlEncode(peerId);
        Object[] format = { info_hash, peer_id, port, Integer.toString(torrent.uploaded), Integer.toString(torrent.downloaded),
                            Integer.toString(torrent.left), event.toString() };
        String params = String.format("?info_hash=%s&peer_id=%s&port=%s"
                                      + "&uploaded=%s&downloaded=%s&left=%s&event=%s&compact=1",
                                      format);
        System.out.println("Request params: " + params);
        Map<String, Bencode> answerMap = null;
        for (List<String> tierList : this.torrent.announceList) {
            if (answerMap != null) break;
            for (int i = 0; i < tierList.size(); i++) {
                if (answerMap != null) break;
                try {
                    answerMap = httpAnnounce(tierList.get(i), params);
                    if (i != 0) {
                        String tracker = tierList.remove(i);
                        tierList.add(0, tracker);
                    }
                } catch (IOException e) {
                    answerMap = null;
                    e.printStackTrace();
                }
            }
        }
        if (answerMap == null) {
            throw new IOException("Couldn't connect to any tracker.");
        }

        if (answerMap.containsKey("failure reason")) {
            throw new ProtocolException(new String(answerMap.get("failure reason").toByteArray()));
        }
        if (answerMap.containsKey("warning message")) {
            System.err.println(new String(answerMap.get("warning message").toByteArray()));
        }
        this.interval = answerMap.get("interval").toInt();
        if (answerMap.containsKey("tracker id")) {
            this.trackerId = new String(answerMap.get("tracker id").toByteArray());
        }
        this.seeders = answerMap.get("complete").toInt();
        this.leechers = answerMap.get("incomplete").toInt();

        Map<InetAddress, Peer> oldMap = new HashMap<InetAddress, Peer>(peerMap);
        if (answerMap.get("peers").toObject() instanceof List) {
            List<Bencode> peers = answerMap.get("peers").toList();
            this.peerMap = new HashMap<InetAddress, Peer>();
            for (int i = 0; i < peers.size(); i++) {
                Map<String, Bencode> newMap = peers.get(i).toMap();
                InetAddress ip = InetAddress.getByAddress(newMap.get("ip").toByteArray());
                int port = newMap.get("port").toInt();
                updateMap(ip, port, oldMap);
            }
        } else if (answerMap.get("peers").toObject() instanceof byte[]) {
            byte[] peersArray = answerMap.get("peers").toByteArray();
            this.peerMap = new HashMap<InetAddress, Peer>();
            int i = 0;
            i = 0;
            while (i != peersArray.length) {
                byte[] ipArray = new byte[4];
                System.arraycopy(peersArray, i, ipArray, 0, 4);
                i += 4;
                InetAddress ip = InetAddress.getByAddress(ipArray);
                byte[] portArray = new byte[2];
                System.arraycopy(peersArray, i, portArray, 0, 2);
                i += 2;
                int port = (portArray[0] & 0xFF) << 8 | portArray[1] & 0xFF;
                updateMap(ip, port, oldMap);
            }
        } else {
            throw new ProtocolException("unrecognized peers format");
        }
    }

    private Map<String, Bencode> httpAnnounce(String urlString, String params)
    throws IOException, InvalidBencodeException {
        //PROJECT: we really should use URLConnection here, but for the project
        // we're required to use Socket
        URL url = new URL(urlString);
        System.out.println("Tracker " + url.getHost() + ':' + url.getPort() + '/' + url.getPath());
        Socket socket = new Socket(url.getHost(), url.getPort());
        BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
        BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());
        String getRequest = "GET " + url.getPath() + params + "HTTP/1.0\n\r\n\r\n";
        System.out.println(getRequest);
        output.write(getRequest.getBytes());
        output.flush();
        //Skip headers
        while (true) {
            int c = input.read();
            if (c == -1) throw new IOException("Unexpected end of stream");
            if (c == (int) '\r' && input.read() == (int) '\n') break;
        }
        Map<String, Bencode> answerMap = Bencode.decodeDict(input);
        input.close();
        output.close();
        socket.close();
        return answerMap;
    }

    private void updateMap(InetAddress ip, int port, Map<InetAddress, Peer> oldMap)
    throws IOException, UnknownHostException {
        if (oldMap.containsKey(ip)) {
            this.peerMap.put(ip, oldMap.get(ip));
        } else {
            this.peerMap.put(ip, new Peer(ip, port, this.torrent));
        }
    }

    private String urlEncode(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append("%");
            if ((array[i] & 0xF0) == 0) sb.append("0");
            sb.append(Integer.toHexString(array[i] & 0xFF));
        }
        return sb.toString();
    }
}
