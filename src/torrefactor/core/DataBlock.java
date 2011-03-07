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
                    System.out.println("arrayOffset: " + arrayOffset);  //DELETEME
                    buffers[i].get(block, arrayOffset, length);
                    break;
                }
                System.out.println("GetBlock " + arrayOffset + " " + remaining);
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

    public void put (int offset, byte block) {
        for (int i=0; i < this.buffers.length; i++) {
            if (offset < this.buffers[i].limit()) {
                this.buffers[i].put(block);
            }
            offset -= this.buffers[i].limit();
        }
        throw new IllegalArgumentException(
                "Offset " + offset + "is past block's end.");
    }

    public void put (int offset, byte[] block) {
        put(block, offset, block.length);
    }

    public void put (byte[] block) {
        put(block, 0, block.length);
    }

    public void put (byte[] block, int offset, int length) {
        int arrayOffset = 0;
        int localOffset = offset;
        for (int i=0; i<buffers.length; i++) {
            if (localOffset < buffers[i].limit()) {
                buffers[i].position(localOffset);
                do {
                    if (arrayOffset >= length) {
                        break;
                    }
                    buffers[i].put(block[arrayOffset]);
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
