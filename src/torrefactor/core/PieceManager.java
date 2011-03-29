package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.Pair;

import java.io.*;
import java.util.*;
import java.security.*;

public class PieceManager implements Serializable {
    //Map every block beginning to its ending
    //TreeMap provides O(log(n)) lookup, insert, remove, previous and successor
    //HACK: public until we have something to test private methods with junit
    public TreeMap<Integer, Integer> blockMap;

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
        this.blockMap = new TreeMap<Integer, Integer>();
        this.pieceLockMap = new HashMap<Integer, Long>();
        int fieldLength = (this.dataManager.pieceNumber() - 1)/8 + 1;
        this.bitfield = new byte[fieldLength];
        Arrays.fill(this.bitfield, (byte) 0);
        this.digestArray = _digestArray;
    }

    //HACK: public until we have something to test private methods with junit
    public synchronized boolean addBlock(int piece, int offset, int length) {
        int begin = piece*this.dataManager.pieceLength()  + offset;
        int end =  begin + length - 1;
        Map.Entry<Integer, Integer> block = this.blockMap.floorEntry(end + 1);
        if (block == null) {
            // No block beginning before our superior born, so no overlap possible
            this.blockMap.put(begin, end);
            return true;
        }
        if (block.getKey() <= begin && block.getValue() >= end) {
            //We're included in a block, nothing to add
            return false;
        }
        if (block.getValue() + 1 >= begin) {
            // If our right side overlaps with a block B, extend our superior born to B's
            end = Math.max(end, block.getValue());
            // Discard all blocks contained in our block
            while (block != null && block.getKey() > begin) {
                this.blockMap.remove(block.getKey());
                block = this.blockMap.lowerEntry(block.getKey());
            }
            // If our left side overlaps with or is adjacent to a block B, extend B's superior born to ours
            if (block != null && block.getValue() + 1 >= begin && block.getValue() <= end) {
                this.blockMap.put(block.getKey(), end);
                return true;
            }
        }
        // The block before our left side doesn't overlap with us or is adjacent to us
        this.blockMap.put(begin, end);
        return true;
    }

    //Remove blocks of data, blocks overlapping but not contained in it
    //will be shrunk
    //HACK: public until we have something to test private methods with junit
    public synchronized boolean removeBlocks(int piece, int offset, int length) {
        int begin = piece*this.dataManager.pieceLength() + offset;
        int end = begin + length - 1;
        Map.Entry<Integer, Integer> block = this.blockMap.floorEntry(end);
        if (block == null || block.getValue() < begin) {
            return false;
        }
        if (block.getValue() > end) {
            this.blockMap.put(end+1, block.getValue());
        }
        while (block != null && block.getKey() >= begin) {
            this.blockMap.remove(block.getKey());
            block = this.blockMap.lowerEntry(block.getKey());
        }
        if (block != null) {
            this.blockMap.put(block.getKey(), begin - 1);
        }
        return true;
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
                int pieceOffset = 0;
                Map.Entry<Integer, Integer> block = this.blockMap.floorEntry(pieceBegin);
                if (block != null) {
                    if (block.getValue() > pieceEnd) {
                        continue;
                    }
                    if (block.getValue() > pieceBegin) {
                        pieceOffset = (block.getValue() + 1) % this.dataManager.pieceLength();
                    }
                }
                Map.Entry<Integer, Integer> higherBlock = null;
                while (infoList.size() < numBlocks && pieceOffset < pieceEnd) {
                    System.out.println("** Requested block at piece " + pieceIndex + " offset" + pieceOffset);
                    infoList.add(new DataBlockInfo(pieceIndex, pieceOffset, BLOCK_SIZE));
                    pieceOffset += BLOCK_SIZE;
                    // If the offset is now inside an existing block, skip to its end
                    higherBlock = this.blockMap.floorEntry(pieceOffset);
                    if (higherBlock != null && (block == null || higherBlock.getKey() > block.getKey())
                        && pieceOffset >= higherBlock.getKey()) {
                        pieceOffset = higherBlock.getValue() + 1;
                    }
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
    public synchronized byte[] getBlock(int piece, int offset, int length)
    throws IOException {
        int begin = piece*this.dataManager.pieceLength() + offset;
        int end = begin + length - 1;
        Integer beginKey = this.blockMap.floorKey(begin);
        if (beginKey == null) {
            return null;
        }
        Integer endKey = this.blockMap.ceilingKey(end);
        if (endKey == null) {
            return null;
        }
        if (!beginKey.equals(endKey)) {
            return null;
        }
        return this.dataManager.getBlock(piece, offset, length);
    }

    public synchronized void putBlock(int piece, int offset, byte[] blockArray)
    throws IOException {
        boolean newBlock = addBlock(piece, offset, blockArray.length);
        if (!newBlock) {
            System.out.println("!!! Already got " + piece*this.dataManager.pieceLength() + offset);
            return;
        }
        this.dataManager.putBlock(piece, offset, blockArray);
        System.out.println("XX " + this.blockMap);
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
        Map.Entry<Integer, Integer> pieceEntry = this.blockMap.floorEntry(piece*this.dataManager.pieceLength());
        int pieceEnd = (piece + 1) * this.dataManager.pieceLength() - 1;
        if (pieceEntry == null || pieceEntry.getValue() < pieceEnd
            || (pieceEntry.getValue() - pieceEntry.getKey() + 1) < this.dataManager.pieceLength()) {
            return false;
        }
        byte[] expectedDigest = new byte[20];
        System.arraycopy(digestArray, 20*piece, expectedDigest, 0, 20);
        byte[] pieceArray = this.dataManager.getPiece(piece);
        byte[] digest = MessageDigest.getInstance("SHA1").digest(pieceArray);
        if (!Arrays.equals(digest, expectedDigest)) {
            removeBlocks(piece, 0, this.dataManager.pieceLength());
            System.out.println("######## " + pieceEntry.getKey() + " " + pieceEntry.getValue());
            System.out.println("~~ Invalid piece " + piece + " got: " + new String(digest) + " expected " + new String(expectedDigest));
            return false;
        }
        int byteIndex = piece / 8;
        this.bitfield[byteIndex] |= 1 << 7 - (piece % 8);
        System.out.println("~~ Valid piece " + piece);
        return true;
    }

    private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.pieceLockMap = new HashMap<Integer, Long>();
    }
}
