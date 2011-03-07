package torrefactor.core;

import torrefactor.core.DataBlock;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

// TODO
//  - set buffers limit so we cannot write past the chunk end.

public class DataManager {
    private int pieceSize;
    private String[] filePaths;
    private long[] fileSizes;
    private RandomAccessFile[] raFiles;
    private FileChannel[] fileChannels;
    private int totalSize;

    public DataManager (String[] _filePaths, long[] _fileSizes,
                               int _pieceSize)
    throws java.io.FileNotFoundException, java.io.IOException {
        this.filePaths = _filePaths;
        this.fileSizes = _fileSizes;
        this.pieceSize = _pieceSize;
        this.raFiles = new RandomAccessFile[this.filePaths.length];
        this.fileChannels = new FileChannel[this.filePaths.length];
        
        // Calculate total size, open each file, allocate disk space if
        // necessary and get FileChannel.
        this.totalSize = 0;
        for (int i=0; i<this.fileSizes.length; i++) {
            this.totalSize += this.fileSizes[i];
            this.raFiles[i] = new RandomAccessFile(filePaths[i], "rw");
            if (this.raFiles[i].length() != this.fileSizes[i]) {
                this.raFiles[i].setLength(this.fileSizes[i]);
            }
            this.fileChannels[i] = this.raFiles[i].getChannel();
            System.out.println("Got channel for " + this.filePaths[i]); //DELETEME
        }

    }

    public DataBlock getBlock(int pieceNumber, int offset, int length)
    throws java.io.IOException {
        long startOffset = (long) pieceNumber * (long) this.pieceSize + offset;
        if (startOffset + length > this.totalSize) {
            throw new IllegalArgumentException ("Block at offset " + offset
                    + " with length " + length + "is outside of torrent's"
                    + " data.");
        }
        
        // Calculate the number of buffers necessary to map this chunk.
        int numBuffers = 0;
        int remainingLength = length;
        long localOffset = startOffset;
        System.out.println("Length of chunk: " + remainingLength); //DELETEME
        for (int i=0; i < this.fileChannels.length; i++) {
            if (localOffset < this.fileSizes[i]) {
                numBuffers +=1;
                System.out.println("File: " + this.filePaths[i] + " with size: " + this.fileChannels[i].size());    //DELETEME
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

        // Map the chunk
        System.out.println("NumBuffers: " + numBuffers);    //DELETEME
        MappedByteBuffer[] buffers = new MappedByteBuffer[numBuffers];
        remainingLength = length;
        localOffset = startOffset;
        for (int i=0; i < this.fileChannels.length; i++) {
            if (localOffset < fileSizes[i]) {
                long remaining =  this.fileChannels[i].size() - localOffset;
                if (remaining >= remainingLength) {
                    // End of chunk is in this channel.
                    buffers[i] = fileChannels[i].map(
                                                FileChannel.MapMode.READ_WRITE,
                                                localOffset,
                                                remainingLength);
                    break;
                }
                // End of chunk is past this channel's end.
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

        // return the chunk.
        return new DataBlock(buffers, length);
    }

    public DataBlock getPiece(int number)
    throws java.io.IOException {
        // Does the piece exist?
        int start = number * this.pieceSize;
        if (start > this.totalSize) {
            throw new IllegalArgumentException(
                    "Piece number " + number + " starts past torrent's data.");
        }
        
        // Get the length
        int length = this.pieceSize;
        if (start + length > this.totalSize) {
            length = totalSize - start;
        }

        return getBlock(number, 0, length);
    }

//    public byte[] get (int number, int offset, int length) {
//        long startOffset = number * this.pieceSize + offset;
//        int remainingLength = length;
//        byte[] chunk = byte[length];
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


