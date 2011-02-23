package torrefactor.core;
import torrefactor.core.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class PeerManager implements Runnable {
    public enum TrackerEvent {
        started, stopped, completed
    }

    private Torrent torrent;
    private Map<String, Peer> peerMap;

    private String peerId = "11111111111111111111";
    int port = 6881;
    int interval;
    String trackerId;
    int seeders;
    int leechers;

    public PeerManager(Torrent _torrent) {
        this.torrent = _torrent;
        announceTracker(TrackerEvent.started);
    }

    public void run() {
    }

    public void stop() {
    }

    public void announceTracker(TrackerEvent event) throws ProtocolException {
        String info_hash = URLEncoder.encode(torrent.infoHash, "UTF-8");
        String peer_id = URLEncoder.encode(peerId, "UTF-8");
        Object[] format = { info_hash, peer_id, Integer.toString(torrent.uploaded), Integer.toString(torrent.downloaded),
                            Integer.toString(torrent.left), event.toString() };
        String url = String.format(torrent.trackerURL
                                   + "/?info_hash=%s&peer_id=%s&port=%s"
                                   + "&uploaded=%s&downloaded=%s&left=%s&event=%s",
                                   format);
        URLConnection connection = new URL(url).openConnection();
        BufferedReader stream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        Map<String, Bencode> answerMap = Bencode.decode(stream);
        stream.close();
        if (answerMap.containsKey("failure reason")) {
            throw new ProtocolException(answerMap.get("failure reason").toString());
        }
        if (answerMap.containsKey("warning message")) {
            System.err.println(answerMap.get("warning message").toString());
        }
        this.interval = answerMap.get("interval").toInt();
        if (answerMap.containsKey("tracker id")) {
            this.trackerId = answerMap.get("tracker id").toString();
        }
        this.seeders = answerMap.get("complete").toInt();
        this.leechers = answerMap.get("incomplete").toInt();
        List<Bencode> peers = answerMap.get("peers").toList();
        Map<String, Peer> oldMap = new HashMap<String, Peer>(peerMap);
        this.peerMap = new HashMap<String, Peer>();
        for (int i = 0; i < peers.length(); i++) {
            Map<String, Bencode> newMap = peers.at(i).toMap();
            String id = newMap.get("peer id").toString();
            String ip = newMap.get("ip").toString();
            int port = newMap.get("port").toInt();
            if (oldMap.containsKey(id)) {
                this.peerMap.put(id, oldMap.get(id));
            }
            this.peerMap.put(id, new Peer(ip, port, torrent));
        }
    }
}
