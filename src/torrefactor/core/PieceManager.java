/* 
 *  This file is part of the Torrefactor project
 *  Copyright 2011 Guillaume Martres <smarter@ubuntu.com>
 *  Copyright 2011 Florian Vessaz <florian.vessaz@gmail.com> 
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *      1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *      2. Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */

package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.util.*;
import java.security.*;

/**
 * This class keeps track of the download blocks of datas in pieces,
 * check the SHA1 of pieces when they've finished downloading and
 * can suggests free blocks to download.
 *
 * Internally, it uses DataManager for disk I/O.
 */
public class PieceManager implements Serializable {
    private static Logger LOG = new Logger();
    //Map of the downloaded blocks
    public IntervalMap intervalMap;
    //map of the requested but not yet downloaded blocks
    private transient SawToothIntervalMap requestedMap;

    private DataManager dataManager;
    public byte[] bitfield;
    byte[] digestArray;


    // List of pieces for which we should send have messages.
    transient Object pieceToAnnounceLock = new Object();
    ArrayList<Integer> pieceToAnnounce = new ArrayList<Integer>();

    //Recommended by http://wiki.theory.org/BitTorrentSpecification#request:_.3Clen.3D0013.3E.3Cid.3D6.3E.3Cindex.3E.3Cbegin.3E.3Clength.3E
    static final int BLOCK_SIZE = (1 << 14); // in bytes

    static final int MAX_REQUESTS = 50;

    public PieceManager(List<Pair<File, Long>> files,
                        int pieceLength, byte[] _digestArray)
    throws FileNotFoundException, IOException {
        this.dataManager = new DataManager(files, pieceLength);
        this.intervalMap = new IntervalMap();
        this.requestedMap = new SawToothIntervalMap(MAX_REQUESTS);
        int fieldLength = (piecesNumber() - 1)/8 + 1;
        this.bitfield = new byte[fieldLength];
        Arrays.fill(this.bitfield, (byte) 0);
        this.digestArray = _digestArray;
    }

    /**
     * Try to find numBlocks free blocks available in the peerBitfield and
     * return a List of their DataBlockInfo(which size is between 0 and n)
     */
    public synchronized List<DataBlockInfo> getFreeBlocks(byte[] peerBitfield, int numBlocks)
    throws IOException {
         //TODO: should we return a smaller size if we already have part of
         //the block?
        List<DataBlockInfo> infoList = new ArrayList<DataBlockInfo>();
        if (numBlocks == 0) return infoList;

        for (int i = 0; i < 8*peerBitfield.length && infoList.size() < numBlocks; i++) {
            if (!ByteArrays.isBitSet(peerBitfield, i)) continue;
            long pieceBegin = i * this.dataManager.pieceLength();
            long pieceEnd = pieceBegin + this.dataManager.pieceLength() - 1;
            // Last block is smaller than the other
            pieceEnd = Math.min(pieceEnd, this.dataManager.totalSize() - 1);

            long offset = nextFreeByte(pieceBegin);
            while (infoList.size() < numBlocks) {
                if (offset >= this.dataManager.totalSize()) {
                    if (pieceBegin == 0) {
                        LOG.info("Everything has been requested at least once, forgetting about old requests");
                        this.requestedMap.clearFirstHalf();
                    }
                    return infoList;
                }
                if (offset > pieceEnd) {
                    // "- 1" because of the i++ in the for loop
                    i = (int) (offset / this.dataManager.pieceLength()) - 1;
                    break;
                }
                // Make sure the block size is not past the piece end or we might get dropped by the peer
                int blockSize = Math.min(BLOCK_SIZE, (int) (pieceEnd - offset) + 1);
                infoList.add(new DataBlockInfo(i, (int) (offset % this.dataManager.pieceLength()), blockSize));
                this.requestedMap.addInterval(offset, blockSize);
                LOG.debug(this, "Requested block at piece: " + i + " offset: " + (offset % this.dataManager.pieceLength())
                                         + " length: " + blockSize);

                offset += blockSize;
                offset = nextFreeByte(offset);
            }
        }
        return infoList;
    }

    /**
     * Return the offset of the next free byte not contained
     * in either the map of downloaded or the map of requested blocks
     */
    private long nextFreeByte(long offset) {
        long reqOffset = 0;
        do {
            reqOffset = this.requestedMap.nextFreePoint(offset);
            offset = this.intervalMap.nextFreePoint(reqOffset);
        } while (reqOffset != offset);
        return offset;
    }

    /**
     * Return the requested block if it's available, null otherwise
     */
    public byte[] getBlock(int piece, int offset, int length)
    throws IOException {
        long begin = piece*this.dataManager.pieceLength() + offset;
        if (!this.intervalMap.containsInterval(begin, length)) {
            return null;
        }
        return this.dataManager.getBlock(piece, offset, length);
    }

    /**
     * Write the array blockArray to piece "piece" with offset "offset" and add
     * it to the intervalMap if we haven't wrote it previously.
     * checkPiece() will be called and thus the bitfield might be updated.
     */
    public synchronized void putBlock(int piece, int offset, byte[] blockArray)
    throws IOException {
        long begin = piece * this.dataManager.pieceLength() + offset;
        if (this.intervalMap.containsInterval(begin, blockArray.length)) {
            LOG.warning(this, "Already got block at " + begin + " with length " + blockArray.length);
            return;
        }
        this.intervalMap.addInterval(begin, blockArray.length);
        this.dataManager.putBlock(piece, offset, blockArray);
        try {
            checkPiece(piece);
        } catch (NoSuchAlgorithmException e) {
            //Assume the piece is correct since we have no way of checking
            LOG.warning(this, "Piece + " + piece + " could not be checked and"
                                     + "may potentially be invalid");
            LOG.warning(this, e.toString());
        }
    }

    /**
     * If the piece is completely downloaded and valid, add it to the
     * bitfield and return true.
     * Otherwise, discard the blocks it's made of and return false
     */
    public boolean checkPiece(int piece)
    throws IOException, NoSuchAlgorithmException {
        long pieceBegin = piece * this.dataManager.pieceLength();
        int pieceLength = this.dataManager.pieceLength();

        // Last piece has different size
        if (piece == piecesNumber() - 1) {
            pieceLength = (int) (this.dataManager.totalSize() - pieceBegin);
        }

        if (!this.intervalMap.containsInterval(pieceBegin, pieceLength)) {
            return false;
        }

        byte[] expectedDigest = new byte[20];
        System.arraycopy(digestArray, 20*piece, expectedDigest, 0, 20);
        byte[] pieceArray = this.dataManager.getPiece(piece);
        byte[] digest = MessageDigest.getInstance("SHA1").digest(pieceArray);
        if (!Arrays.equals(digest, expectedDigest)) {
            this.intervalMap.removeIntervals(pieceBegin, pieceLength);
            LOG.error(this, "Invalid piece " + piece + " got: " + new String(digest) + " expected " + new String(expectedDigest));
            return false;
        }
        ByteArrays.setBit(bitfield, piece, 1);
        LOG.info(this, "Valid piece " + piece);

        synchronized (this.pieceToAnnounceLock) {
            this.pieceToAnnounce.add(piece);
        }
        
        return true;
    }

    /**
     * Returns true if we got all the pieces
     */
    public boolean isComplete() {
        return ByteArrays.isComplete(this.bitfield, piecesNumber());
    }

    /**
     * Returns the number of pieces of the torrent
     */
    public int piecesNumber() {
        return this.dataManager.piecesNumber();
    }

    /**
     * Returns the number of downloaded pieces of the torrent
     */
    public int piecesDownloaded() {
        return ByteArrays.done(this.bitfield);
    }

    /**
     * Returns the size of each piece of the torrent (except for the last one
     * which may be smaller).
     */
    public int pieceLength() {
        return this.dataManager.pieceLength();
    }

    /**
     * Returns and clear the list of newly downloaded pieces that haven't been
     * announced to peers via "have" message already.
     */
    public ArrayList<Integer> popToAnnounce () {
        ArrayList<Integer> list;
        synchronized (this.pieceToAnnounceLock) {
            list = this.pieceToAnnounce;
            this.pieceToAnnounce = new ArrayList<Integer>();
        }
        return list;
    }

    /**
     * This is used during the deserialization to recreate
     * the transient member variables.
     */
    private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.requestedMap = new SawToothIntervalMap(50);
        this.pieceToAnnounceLock = new Object();
    }
}
