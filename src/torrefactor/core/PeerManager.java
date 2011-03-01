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
    final int MAX_PEERS = 25;
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
        String url = String.format(torrent.trackerURL
                                   + "?info_hash=%s&peer_id=%s&port=%s"
                                   + "&uploaded=%s&downloaded=%s&left=%s&event=%s&compact=1",
                                   format);
        System.out.println("Tracker GET: " + url);
        URLConnection connection = new URL(url).openConnection();
        BufferedInputStream stream = new BufferedInputStream(connection.getInputStream());
        Map<String, Bencode> answerMap = Bencode.decodeDict(stream);
        stream.close();
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
                for (int j = 0; j < 4; j++, i++) {
                    ipArray[j] = peersArray[i];
                }
                InetAddress ip = InetAddress.getByAddress(ipArray);
                byte[] portArray = new byte[2];
                for (int j = 0; j < 2; j++, i++) {
                    portArray[j] = peersArray[i];
                }
                int port = (portArray[0] & 0xFF) << 8 | portArray[1] & 0xFF;
                updateMap(ip, port, oldMap);
            }
        } else {
            throw new ProtocolException("unrecognized peers format");
        }
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
