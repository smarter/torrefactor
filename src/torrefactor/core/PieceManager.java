package torrefactor.core;

import torrefactor.core.*;

import java.io.*;
import java.util.*;

public class PieceManager {
    //Map every block beginning to its ending
    //TreeMap provides O(log(n)) lookup, insert, remove, previous and successor
    //HACK: public until we have something to test private methods with junit
    public TreeMap<Integer, Integer> blockMap;
    DataManager dataManager;
    public byte[] bitfield;
    byte[] digestArray;
    //Recommended by http://wiki.theory.org/BitTorrentSpecification#request:_.3Clen.3D0013.3E.3Cid.3D6.3E.3Cindex.3E.3Cbegin.3E.3Clength.3E
    static final int BLOCK_SIZE = (1 << 14);

    public PieceManager(String[] fileList, long[] fileSizes, int pieceLength, byte[] _digestArray)
    throws FileNotFoundException, IOException {
        this.dataManager = new DataManager(fileList, fileSizes, pieceLength);
        this.blockMap = new TreeMap<Integer, Integer>();
        int fieldLength = (this.dataManager.pieceNumber() - 1)/8 + 1;
        this.bitfield = new byte[fieldLength];
        Arrays.fill(this.bitfield, (byte) 0);
        this.digestArray = _digestArray;
    }

    //HACK: public until we have something to test private methods with junit
    public synchronized boolean addBlock(int piece, int offset, int length) {
        int begin = piece*this.dataManager.pieceLength()  + offset;
        int end =  begin + length - 1;
        Map.Entry<Integer, Integer> block = this.blockMap.floorEntry(end);
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

    // Return a DataBlock at least partially not downloaded and present
    // in peerBitfield or null if no such block could be found.
    public synchronized DataBlock getFreeBlock(byte[] peerBitfield)
    throws IOException {
         //TODO: should we return a smaller size if we already have part of
         //the block?
         //We should at least avoid going over piece boundary
        for (int i = 0; i < peerBitfield.length; i++) {
            if (peerBitfield[i] == 0) continue;
            int byteOffset = 7;
            while (byteOffset != 0) {
                int pieceIndex = 8*i + (7 - byteOffset);
                byteOffset--;
                if ((peerBitfield[i] >>> byteOffset) == 0) {
                    continue;
                }
                int pieceBegin = pieceIndex * this.dataManager.pieceLength();
                int pieceEnd = pieceBegin + this.dataManager.pieceLength() - 1;
                Map.Entry<Integer, Integer> block = this.blockMap.floorEntry(pieceBegin);
                if (block != null && block.getValue() >= pieceBegin) {
                    if (block.getValue() >= pieceEnd) {
                        continue;
                    } else {
                        System.out.println("XX " + this.blockMap);
                        int pieceOffset = (block.getValue() + 1) % this.dataManager.pieceLength();
                        System.out.println("XX Free, pieceIndex: " + pieceIndex + " pieceOffset " + pieceOffset);
                        addBlock(pieceIndex, pieceOffset, BLOCK_SIZE);
                        return this.dataManager.getBlock(pieceIndex, pieceOffset, BLOCK_SIZE);
                    }
                } else {
                    System.out.println("XX " + this.blockMap);
                    System.out.println("XX Free, pieceIndex: " + pieceIndex);
                    addBlock(pieceIndex, 0, BLOCK_SIZE);
                    return this.dataManager.getBlock(pieceIndex, 0, BLOCK_SIZE);
                }
            }
        }
        return null;
    }

    // Return the requested block if it's available, null otherwise
    public synchronized DataBlock getReadBlock(int piece, int offset, int length)
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
        DataBlock block = this.dataManager.getBlock(piece, offset, blockArray.length);
        block.put(blockArray);
    }

    // If the piece is valid, add it to the bitfield and return true
    // Otherwise, discard the blocks it's made of and return false
    public boolean checkPiece(int piece) throws IOException {
        byte[] expectedDigest = new byte[20];
        System.arraycopy(digestArray, 20*piece, expectedDigest, 0, 20);
        byte[] digest = this.dataManager.getPiece(piece).get();
        if (!Arrays.equals(digest, expectedDigest)) {
            removeBlocks(piece, 0, this.dataManager.pieceLength());
            return false;
        }
        int byteIndex = piece / 8;
        this.bitfield[byteIndex] |= 1 << 7 - (piece % 8);
        return true;
    }
}
