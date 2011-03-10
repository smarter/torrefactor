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

    public PieceManager(DataManager _dataManager, byte[] _digestArray) {
        this.dataManager = _dataManager;
        this.blockMap = new TreeMap<Integer, Integer>();
        int fieldLength = (this.dataManager.pieceNumber() - 1)/8 + 1;
        this.bitfield = new byte[fieldLength];
        Arrays.fill(this.bitfield, (byte) 0);
        this.digestArray = _digestArray;
    }

    public boolean addBlock(int piece, int offset, int length) {
        int begin = piece*this.dataManager.pieceLength  + offset;
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
        if (block.getValue() >= begin) {
            // If our right side overlaps with a block B, extend our superior born to B's
            end = Math.max(end, block.getValue());
            // Discard all blocks contained in our block
            while (block != null && block.getKey() > begin) {
                this.blockMap.remove(block.getKey());
                block = this.blockMap.lowerEntry(block.getKey());
            }
            // If our left side overlaps with a block B, extend B's superior born to ours
            if (block != null && block.getValue() >= begin && block.getValue() <= end) {
                this.blockMap.put(block.getKey(), end);
                return true;
            }
        }
        // The block before our inferior born doesn't overlap with us
        this.blockMap.put(begin, end);
        return true;
    }

    //Remove blocks of data, blocks overlapping but not contained in it
    //will be shrunk
    public boolean removeBlocks(int piece, int offset, int length) {
        int begin = piece*this.dataManager.pieceLength + offset;
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


    // Return the requested block if it's available, null otherwise
    public DataBlock getBlock(int piece, int offset, int length)
    throws IOException {
        int begin = piece*this.dataManager.pieceLength + offset;
        int end = begin + length - 1;
        int beginKey = this.blockMap.floorKey(begin);
        int endKey = this.blockMap.floorKey(end);
        if (beginKey != endKey) {
            return null;
        } else {
            return this.dataManager.getBlock(piece, offset, length);
        }
    }

    public void putBlock(int piece, int offset, byte[] blockArray)
    throws IOException {
        DataBlock block = this.dataManager.getBlock(piece, offset, blockArray.length);
        block.put(blockArray);
    }

    // TODO: return a DataBlock ?
    public int getFreeBlock() {
        Map.Entry<Integer, Integer> firstEntry = this.blockMap.firstEntry();
        if (firstEntry == null) {
            return 0;
        } else {
            return firstEntry.getValue() + 1;
        }
    }


    // If the piece is valid, add it to the bitfield and return true
    // Otherwise, discard the blocks it's made of and return false
    public boolean checkPiece(int piece) throws IOException {
        byte[] expectedDigest = new byte[20];
        System.arraycopy(digestArray, 20*piece, expectedDigest, 0, 20);
        byte[] digest = this.dataManager.getPiece(piece).get();
        if (!Arrays.equals(digest, expectedDigest)) {
            removeBlocks(piece, 0, this.dataManager.pieceLength);
            return false;
        }
        int byteIndex = piece / 8;
        this.bitfield[byteIndex] |= 1 << 7 - (piece % 8);
        return true;
    }
}
