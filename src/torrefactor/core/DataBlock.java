package torrefactor.core;

import torrefactor.util.*;

import java.nio.*;

/**
 * An abstraction to read and writes blocks of datas from files via
 * MappedByteBuffer[].
 */
public class DataBlock {
    private static Logger LOG = new Logger();
    private MappedByteBuffer[] buffers;
    private int length;
    private int pieceIndex;
    private int offset;

    public DataBlock (MappedByteBuffer[] _buffers, int _length, int _pieceIndex, int _offset) {
        this.buffers = _buffers;
        this.length = _length;
        this.pieceIndex = _pieceIndex;
        this.offset = _offset;
    }

    public int length() {
        return this.length;
    }

    public int pieceIndex() {
        return this.pieceIndex;
    }

    // Return the offset within the piece
    public int offset() {
        return this.offset;
    }

    public byte get (int offset) {
        for (int i=0; i < this.buffers.length; i++) {
            if (offset < this.buffers[i].limit()){
                return this.buffers[i].get(offset);
            }
            offset -= this.buffers[i].limit();
        }
        throw new IllegalArgumentException(
                "Offset " + offset + " is past block's end.");
    }

    public byte[] get () {
        return get(0, this.length);
    }

    public byte[] get (int offset, int length) {
        byte[] block = new byte[length];
        int arrayOffset = 0;
        int localOffset = offset;
        for (int i=0; i<this.buffers.length; i++) {
            if (localOffset < buffers[i].limit()) {
                buffers[i].position(localOffset);
                int remaining = buffers[i].remaining();
                if (length <= remaining) {
                    LOG.debug(this, "arrayOffset: " + arrayOffset);
                    buffers[i].get(block, arrayOffset, length);
                    break;
                }
                LOG.debug(this, "GetBlock " + arrayOffset + " " + remaining);
                buffers[i].get(block, arrayOffset, remaining);
                length -= remaining;
                arrayOffset += remaining;
            }

            localOffset -= buffers[i].limit();
            if (localOffset < 0) {
                localOffset = 0;
            }
        }

        return block;
    }

    public void put(byte[] block) {
        put(0, block.length, block);
    }

    public void put(int offset, byte[] block) {
        put(offset, block.length, block);
    }

    public void put(int offset, int length, byte[] block) {
        int arrayOffset = 0;
        int localOffset = offset;

        for (int i=0; i<buffers.length; i++) {
            int bufferRemaining = buffers[i].limit() - localOffset;
            if (localOffset < buffers[i].limit()) {
                int cpLength;
                cpLength = Math.min(length, bufferRemaining) ;
                buffers[i].put(block, arrayOffset, cpLength);
                arrayOffset += cpLength;
            }

            localOffset -= buffers[i].limit();
            if (localOffset < 0) {
                localOffset = 0;
            }
            LOG.debug(this, "%% Writing to " + offset + " length: " + length
                            + " at piece: " + this.pieceIndex);

        }
    }
}
