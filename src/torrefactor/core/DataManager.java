package torrefactor.core;

import torrefactor.core.DataBlock;
import torrefactor.util.Pair;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

// TODO
//  - set buffers limit so we cannot write past the block end.
//  - Are we allowed to use package access for attributes ?

public class DataManager implements Serializable {
    private File[]files;
    private long[] fileSizes;
    private transient RandomAccessFile[] raFiles;
    private transient FileChannel[] fileChannels;
    private long totalSize;
    private int pieceNumber;
    private int pieceLength;

    public DataManager (List<Pair<File, Long>> _files ,int _pieceLength)
    throws java.io.FileNotFoundException, java.io.IOException {
        setFilesAndSizes(_files);
        init(_pieceLength);
    }

    public DataManager (File[] _files, long[] _sizes, int _pieceLength)
    throws java.io.FileNotFoundException, java.io.IOException {
        setFilesAndSizes(_files, _sizes);
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
            this.raFiles[i] = new RandomAccessFile(this.files[i], "rw");
            if (this.raFiles[i].length() != this.fileSizes[i]) {
                this.raFiles[i].setLength(this.fileSizes[i]);
            }
            this.fileChannels[i] = this.raFiles[i].getChannel();
            System.out.println("Got channel for " + this.files[i]); //DELETEME
        }
        this.pieceNumber = (int) ( (this.totalSize - 1) / this.pieceLength) + 1;
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

    private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        init(this.pieceLength);
    }

    public int pieceNumber() {
        return this.pieceNumber;
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
        if (startOffset + length > this.totalSize) {
            throw new IllegalArgumentException ("Block at offset " + offset
                    + " with length " + length + "is outside of torrent's"
                    + " data.");
        }

        // Calculate the number of buffers necessary to map this block.
        int numBuffers = 0;
        int remainingLength = length;
        long localOffset = startOffset;
        System.out.println("Length of block: " + remainingLength); //DELETEME
        for (int i=0; i < this.fileChannels.length; i++) {
            if (localOffset < this.fileSizes[i]) {
                numBuffers +=1;
                System.out.println("File: " + files[i] + " with size: " + this.fileChannels[i].size());    //DELETEME
                System.out.println("localOffest: " + localOffset);
                remainingLength -= this.fileSizes[i] - localOffset;
                System.out.println("Remaining length: " + remainingLength); //DELETEME
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
        System.out.println("NumBuffers: " + numBuffers);    //DELETEME
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


