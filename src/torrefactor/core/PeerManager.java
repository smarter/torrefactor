package torrefactor.core;
import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class PeerManager implements Runnable {
    public enum TrackerEvent {
        started, stopped, completed
    }

    private Torrent torrent;
    private Map<String, Peer> peerMap;
    private Map<String, Peer> activeMap;

    String peerId = "11111111111111111111";
    int port = 6881;
    int interval;
    String trackerId;
    int seeders;
    int leechers;
    final int MAX_PEERS = 25;

    public PeerManager(Torrent _torrent) throws ProtocolException, InvalidBencodeException,
                                                IOException {
        this.torrent = _torrent;
        announceTracker(TrackerEvent.started);
    }

    public void run() {
        for (Map.Entry<String, Peer> peerEntry : activeMap.entrySet()) {
            this.torrent.downloaded += peerEntry.getValue().popDownloaded();
            this.torrent.uploaded += peerEntry.getValue().popUploaded();
            if (peerEntry.getValue().wasDisconnected()) {
                activeMap.remove(peerEntry.getKey());
            }
        }
        for (int i = 0; i < MAX_PEERS - activeMap.size(); i++) {
            //Entry<String, Peer> peerEntry = selectPeer();
            //peerEntry.getValue().start();
            //activeMap.add(peerEntry);
        }
    }

    public void stopDownload() {
    }

    public void announceTracker(TrackerEvent event) throws ProtocolException, InvalidBencodeException,
                                                           IOException {
        String info_hash = URLEncoder.encode(torrent.infoHash, "UTF-8");
        String peer_id = URLEncoder.encode(peerId, "UTF-8");
        Object[] format = { info_hash, peer_id, this.port, Integer.toString(torrent.uploaded), Integer.toString(torrent.downloaded),
                            Integer.toString(torrent.left), event.toString() };
        String url = String.format(torrent.trackerURL
                                   + "/?info_hash=%s&peer_id=%s&port=%s"
                                   + "&uploaded=%s&downloaded=%s&left=%s&event=%s",
                                   format);
        URLConnection connection = new URL(url).openConnection();
        BufferedInputStream stream = new BufferedInputStream(connection.getInputStream());
        Map<String, Bencode> answerMap = Bencode.decodeDict(stream);
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
        for (int i = 0; i < peers.size(); i++) {
            Map<String, Bencode> newMap = peers.get(i).toMap();
            String id = newMap.get("peer id").toString();
            String ip = newMap.get("ip").toString();
            int port = newMap.get("port").toInt();
            if (oldMap.containsKey(id)) {
                this.peerMap.put(id, oldMap.get(id));
            } else {
                this.peerMap.put(id, new Peer(ip, port, torrent));
            }
        }
    }
}
