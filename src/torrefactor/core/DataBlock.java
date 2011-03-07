package torrefactor.core;

import java.nio.*;


public class DataBlock {
    private MappedByteBuffer[] buffers;
    private int length;

    public DataBlock (MappedByteBuffer[] _buffers, int _length) {
        this.buffers = _buffers;
        this.length = _length;
    }

    public byte get (int offset) {
        for (int i=0; i < this.buffers.length; i++) {
            if (offset < this.buffers[i].limit()){
                return this.buffers[i].get(offset);
            }
            offset -= this.buffers[i].limit();
        }
        throw new IllegalArgumentException(
                "Offset " + offset + " is past chunk's end.");
    }

    public byte[] get (int offset, int length) {
        byte[] chunk = new byte[length];
        int arrayOffset = 0;
        int localOffset = offset;
        for (int i=0; i<this.buffers.length; i++) {
            if (localOffset < buffers[i].limit()) {
                buffers[i].position(localOffset);
                int remaining = buffers[i].remaining();
                if (length <= remaining) {
                    System.out.println("arrayOffset: " + arrayOffset);  //DELETEME
                    buffers[i].get(chunk, arrayOffset, length);
                    break;
                }
                System.out.println("GetChunk " + arrayOffset + " " + remaining);
                buffers[i].get(chunk, arrayOffset, remaining);
                length -= remaining;
                arrayOffset += remaining;
            }

            localOffset -= buffers[i].limit();
            if (localOffset < 0) {
                localOffset = 0;
            }
        }

        return chunk;
    }

    public void put (int offset, byte chunk) {
        for (int i=0; i < this.buffers.length; i++) {
            if (offset < this.buffers[i].limit()) {
                this.buffers[i].put(chunk);
            }
            offset -= this.buffers[i].limit();
        }
        throw new IllegalArgumentException(
                "Offset " + offset + "is past chunk's end.");
    }

    public void put (int offset, byte[] chunk) {
        put(chunk, offset, chunk.length);
    }

    public void put (byte[] chunk, int offset, int length) {
        int arrayOffset = 0;
        int localOffset = offset;
        for (int i=0; i<buffers.length; i++) {
            if (localOffset < buffers[i].limit()) {
                buffers[i].position(localOffset);
                do {
                    if (arrayOffset >= length) {
                        break;
                    }
                    buffers[i].put(chunk[arrayOffset]);
                    arrayOffset ++;
                } while (buffers[i].remaining()>0);
            }

            localOffset -= buffers[i].limit();
            if (localOffset < 0) {
                localOffset = 0;
            }
        }
    }
}
