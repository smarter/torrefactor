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

import torrefactor.core.DataBlock;
import torrefactor.util.*;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

// TODO
//  - set buffers limit so we cannot write past the block end.
//  - Are we allowed to use package access for attributes ?

/**
 * The DataManager is responsible for creating files and for
 * reading and writing blocks of data to them.
 *
 * Internally, this is implemented using MappedByteBuffer to
 * let the Operating System handle the disk I/O.
 */
public class DataManager implements Serializable {
    private static Logger LOG = new Logger();
    private File[]files;
    private long[] fileSizes;
    private transient RandomAccessFile[] raFiles;
    private transient FileChannel[] fileChannels;
    private long totalSize;
    private int piecesNumber;
    private int pieceLength;

    public DataManager (List<Pair<File, Long>> _files ,int _pieceLength)
    throws java.io.FileNotFoundException, java.io.IOException {
        setFilesAndSizes(_files);
        init(_pieceLength);
    }

    private void init (int pieceLength)
    throws java.io.FileNotFoundException, java.io.IOException {
        this.pieceLength = pieceLength;
        this.raFiles = new RandomAccessFile[this.files.length];
        this.fileChannels = new FileChannel[this.files.length];

        // Calculate total size, open each file, allocate disk space if
        // necessary and get FileChannel.
        this.totalSize = 0;
        for (int i=0; i<this.fileSizes.length; i++) {
            this.totalSize += this.fileSizes[i];
            File parent = this.files[i].getParentFile();
            parent.mkdirs();
            this.raFiles[i] = new RandomAccessFile(this.files[i], "rw");
            if (this.raFiles[i].length() != this.fileSizes[i]) {
                this.raFiles[i].setLength(this.fileSizes[i]);
            }
            this.fileChannels[i] = this.raFiles[i].getChannel();
            LOG.debug(this, "Got channel for " + this.files[i]);
        }
        this.piecesNumber = (int) ( (this.totalSize - 1) / this.pieceLength) + 1;
    }

    private void setFilesAndSizes (List<Pair<File, Long>> fpairs) {
        this.files = new File[fpairs.size()];
        this.fileSizes = new long[fpairs.size()];
        for (int i=0; i<fpairs.size(); i++) {
            this.files[i] = fpairs.get(i).first();
            this.fileSizes[i] = fpairs.get(i).second();
        }
    }

    private void setFilesAndSizes (File[] files, long[] sizes) {
        assert (files.length == sizes.length);
        this.files = files;
        this.fileSizes = sizes;
    }

    /**
     * This is used during the deserialization to recreate
     * the transient member variables.
     */
    private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        init(this.pieceLength);
    }

    public int piecesNumber() {
        return this.piecesNumber;
    }

    public int pieceLength() {
        return this.pieceLength;
    }

    public long totalSize() {
        return this.totalSize;
    }

    public void putBlock(int pieceNumber, int offset, byte[] data)
    throws IOException {
        getDataBlock(pieceNumber, offset, data.length).put(data);
    }

    public byte[] getBlock(int pieceNumber, int offset, int length)
    throws IOException {
        return getDataBlock(pieceNumber, offset, length).get();
    }


    //TODO: cache the most recent DataBlocks?
    private DataBlock getDataBlock(int pieceNumber, int offset, int length)
    throws java.io.IOException {
        long startOffset = (long) pieceNumber * (long) this.pieceLength + offset;
        if (startOffset + length - 1 > this.totalSize) {
            throw new IllegalArgumentException
                ("Block at offset " + startOffset
                 + " with length " + length + " is outside of torrent's"
                 + " data which has length " + this.totalSize);
        }

        // Calculate the number of buffers necessary to map this block.
        int numBuffers = 0;
        int remainingLength = length;
        long localOffset = startOffset;
        LOG.debug(this, "Length of block: " + remainingLength);
        for (int i=0; i < this.fileChannels.length; i++) {
            if (localOffset < this.fileSizes[i]) {
                numBuffers +=1;
                //LOG.debug(this, "File: " + files[i] + " with size: "
                //                         + this.fileChannels[i].size());
                //LOG.debug(this, "localOffest: " + localOffset);
                remainingLength -= this.fileSizes[i] - localOffset;
                //LOG.debug(this, "Remaining length: " + remainingLength);
                if (remainingLength <= 0) {
                    break;
                }
            }
            localOffset -= this.fileSizes[i];
            if (localOffset < 0) {
                localOffset = 0;
            }
        }

        // Map the block
        LOG.debug(this, "NumBuffers: " + numBuffers);
        MappedByteBuffer[] buffers = new MappedByteBuffer[numBuffers];
        remainingLength = length;
        localOffset = startOffset;
        for (int i=0; i < this.fileChannels.length; i++) {
            if (localOffset < fileSizes[i]) {
                long remaining =  this.fileChannels[i].size() - localOffset;
                if (remaining >= remainingLength) {
                    // End of block is in this channel.
                    buffers[i] = fileChannels[i].map(
                                                FileChannel.MapMode.READ_WRITE,
                                                localOffset,
                                                remainingLength);
                    break;
                }
                // End of block is past this channel's end.
                buffers[i] = fileChannels[i].map(FileChannel.MapMode.READ_WRITE,
                                                 localOffset,
                                                 remaining);
                remainingLength -= remaining;
            }
            localOffset -= fileChannels[i].size();
            if (localOffset < 0) {
                localOffset = 0;
            }
        }

        // return the block.
        return new DataBlock(buffers, length, pieceNumber, offset);
    }

    public byte[] getPiece(int number)
    throws java.io.IOException {
        // Does the piece exist?
        long start = number * this.pieceLength;
        if (start > this.totalSize) {
            throw new IllegalArgumentException(
                    "Piece number " + number + " starts past torrent's data.");
        }

        // Get the length
        int length = this.pieceLength;
        if (start + length > this.totalSize) {
            length = (int) (totalSize - start);
        }

        return getBlock(number, 0, length);
    }

//    public byte[] get (int number, int offset, int length) {
//        long startOffset = number * this.pieceLength + offset;
//        int remainingLength = length;
//        byte[] block = byte[length];
//
//        for (int i=0; i<this.fileChannels; i++) {
//            if (startOffset < this.fileChannels[i].size()) {
//                // java api doesn't provide a method to read
//                // a particular position in the channel into a 
//                // particular position in a byte array.
//                // Thus we should read into a byte array matching the
//                // size to read and then copy those arrays into the
//                // final one.
//                //
//                // It's so stupid that I won't do it. We use memory map
//                // instead
//            }
//        }
//    }

}


