package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.Pair;

import java.io.*;
import java.util.*;
import java.security.*;

public class PieceManager implements Serializable {
    public IntervalMap intervalMap;

    //Map a piece number to the time in milliseconds when a block from this
    //piece was requested. Used to prevent getFreeBlocks from returning blocks
    //already being requested by a peer
    private transient Map<Integer, Long> pieceLockMap;

    DataManager dataManager;
    public byte[] bitfield;
    byte[] digestArray;

    //Recommended by http://wiki.theory.org/BitTorrentSpecification#request:_.3Clen.3D0013.3E.3Cid.3D6.3E.3Cindex.3E.3Cbegin.3E.3Clength.3E
    static final int BLOCK_SIZE = (1 << 14); // in bytes

    static final int PIECE_LOCK_TIMEOUT = 30000; // in milliseconds

    public PieceManager(List<Pair<File, Long>> files,
                        int pieceLength, byte[] _digestArray)
    throws FileNotFoundException, IOException {
        this.dataManager = new DataManager(files, pieceLength);
        this.intervalMap = new IntervalMap();
        this.pieceLockMap = new HashMap<Integer, Long>();
        int fieldLength = (this.dataManager.pieceNumber() - 1)/8 + 1;
        this.bitfield = new byte[fieldLength];
        Arrays.fill(this.bitfield, (byte) 0);
        this.digestArray = _digestArray;
    }

    // Try to find numBlocks free blocks available in the peerBitfield and
    // return a List of their DataBlockInfo(which size is between 0 and n)
    public synchronized List<DataBlockInfo> getFreeBlocks(byte[] peerBitfield, int numBlocks)
    throws IOException {
         //TODO: should we return a smaller size if we already have part of
         //the block?
         //We should at least avoid going over piece boundary
        List<DataBlockInfo> infoList = new ArrayList<DataBlockInfo>();
        if (numBlocks == 0) return infoList;

        for (int i = 0; i < peerBitfield.length && infoList.size() < numBlocks; i++) {
            if (peerBitfield[i] == 0) continue;

            for (int byteOffset = 7; byteOffset != 0 && infoList.size() < numBlocks; byteOffset--) {
                if ((peerBitfield[i] >>> byteOffset) == 0) continue;

                int pieceIndex = 8*i + (7 - byteOffset);
                if (isLocked(pieceIndex)) continue;

                int pieceBegin = pieceIndex * this.dataManager.pieceLength();
                int pieceEnd = pieceBegin + this.dataManager.pieceLength() - 1;
                int offset = this.intervalMap.nextFreePoint(pieceBegin);
                if (offset > pieceEnd) {
                    continue;
                }
                Map.Entry<Integer, Integer> higherBlock = null;
                while (infoList.size() < numBlocks && offset < pieceEnd) {
                    System.out.println("** Requested block at piece " + pieceIndex + " offset" + (offset % this.dataManager.pieceLength()));
                    // Make sure the block size is not past the piece end or we might get dropped by the peer
                    int blockSize = Math.min(BLOCK_SIZE, pieceEnd - offset + 1);
                    infoList.add(new DataBlockInfo(pieceIndex, (offset % this.dataManager.pieceLength()), blockSize));

                    offset += blockSize;
                    offset = this.intervalMap.nextFreePoint(offset);
                }
                this.pieceLockMap.put(pieceIndex, System.currentTimeMillis());
            }
        }
        return infoList;
    }

    private synchronized boolean isLocked(int pieceIndex) {
        if (!this.pieceLockMap.containsKey(pieceIndex)) {
            return false;
        }
        if (System.currentTimeMillis() - this.pieceLockMap.get(pieceIndex) > PIECE_LOCK_TIMEOUT) {
            System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXxXX UNLOCK");
            this.pieceLockMap.remove(pieceIndex);
            return false;
        }
        return true;
    }

    // Return the requested block if it's available, null otherwise
    public byte[] getBlock(int piece, int offset, int length)
    throws IOException {
        int begin = piece*this.dataManager.pieceLength() + offset;
        if (!this.intervalMap.containsInterval(begin, length)) {
            return null;
        }
        return this.dataManager.getBlock(piece, offset, length);
    }

    public synchronized void putBlock(int piece, int offset, byte[] blockArray)
    throws IOException {
        int begin = piece * this.dataManager.pieceLength() + offset;
        if (this.intervalMap.containsInterval(begin, blockArray.length)) {
            System.out.println("!!! Already got " + piece*this.dataManager.pieceLength() + offset);
            return;
        }
        this.intervalMap.addInterval(begin, blockArray.length);
        this.dataManager.putBlock(piece, offset, blockArray);
        System.out.println(this.intervalMap);
        try {
            checkPiece(piece);
        } catch (NoSuchAlgorithmException e) {
            //Assume the piece is correct since we have no way of checking
            System.err.println("Warning: piece + " + piece + " could not be checked and"
                               + "may potentially be invalid");
            System.err.println(e.toString());
        }
    }

    // If the piece is completely downloaded and valid, add it to the
    // bitfield and return true.
    // Otherwise, discard the blocks it's made of and return false
    public boolean checkPiece(int piece)
    throws IOException, NoSuchAlgorithmException {
        if (!this.intervalMap.containsInterval(piece*this.dataManager.pieceLength(), this.dataManager.pieceLength())) {
            return false;
        }

        byte[] expectedDigest = new byte[20];
        System.arraycopy(digestArray, 20*piece, expectedDigest, 0, 20);
        byte[] pieceArray = this.dataManager.getPiece(piece);
        byte[] digest = MessageDigest.getInstance("SHA1").digest(pieceArray);
        if (!Arrays.equals(digest, expectedDigest)) {
            this.intervalMap.removeIntervals(piece * this.dataManager.pieceLength(), this.dataManager.pieceLength());
            System.out.println("~~ Invalid piece " + piece + " got: " + new String(digest) + " expected " + new String(expectedDigest));
            return false;
        }
        int byteIndex = piece / 8;
        this.bitfield[byteIndex] |= 1 << (7 - (piece % 8));
        System.out.println("~~ Valid piece " + piece);
        //TODO: send "have" message to peers
        return true;
    }

    private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.pieceLockMap = new HashMap<Integer, Long>();
    }
}
