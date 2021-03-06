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

    /**
     * Constructs a DataBlock object corresponding to a block in a piece of the torrent.
     * @param _buffers      List of MappedByteBuffer mapped in order to each
     *                      file in the torrent
     * @param _length       Length of the block
     * @param _pieceIndex   Index of the piece this block is in
     * @param _offset       Offset of this block within the piece.
     */
    public DataBlock (MappedByteBuffer[] _buffers, int _length, int _pieceIndex, int _offset) {
        this.buffers = _buffers;
        this.length = _length;
        this.pieceIndex = _pieceIndex;
        this.offset = _offset;
    }

    /**
     * Returns the length of this block
     */
    public int length() {
        return this.length;
    }

    /**
     * Returns the index of the piece this block is in
     */
    public int pieceIndex() {
        return this.pieceIndex;
    }

    /**
     * Return the offset of this block within the piece
     */
    public int offset() {
        return this.offset;
    }

    /**
     * Return an array of length length() containing the whole
     * block.
     */
    public byte[] get () {
        return get(0, this.length);
    }

    /**
     * Return an array of length "length" containing a part of this
     * block, starting at offset "offset" in bytes.
     */
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

    /**
     * Write an array starting at the beginning of this block.
     */
    public void put(byte[] block) {
        put(0, block.length, block);
    }

    /**
     * Write an array starting at a particular offset within this
     * block.
     */
    public void put(int offset, byte[] block) {
        put(offset, block.length, block);
    }


    /**
     * Write the length first bytes from array "block" starting at at
     * a particular offset to this block.
     */
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
