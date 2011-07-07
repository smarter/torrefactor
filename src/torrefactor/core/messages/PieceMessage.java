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

package torrefactor.core.messages;

import torrefactor.util.ByteArrays;


/**
 * Represents a piece message.
 *  id      1 byte
 *  index   4 byte
 *  offset  4 byte
 *  block   x byte
 */
public class PieceMessage extends Message {
    public final static byte id = 7;
    public final int index;
    public final int offset;
    public final byte[] block;

    /**
     * Create a new PieceMessage.
     *
     * @param index        the index of the piece
     * @param offset    the offset within the piece
     * @param block        the data block
     */
    public PieceMessage (int index, int offset, byte[] block) {
        this.index = index;
        this.offset = offset;
        this.block = block;
    }

    /**
     * Create a new PieceMessage from the given byte array representation.
     *
     * @param msg    the byte array representation from which to build this
     *                message.
     */
    public PieceMessage (byte[] msg) {
        this.index = ByteArrays.toInt(msg);
        this.offset = ByteArrays.toInt(msg, 4);
        int len = msg.length - 8;
        this.block = new byte[len];
        System.arraycopy(msg, 8, this.block, 0, len);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte id () {
        return PieceMessage.id;
    }

    // Java does not override static method thus we cannot use @inheritDoc
    /**
     * {@link torrefactor.core.messages.Message#isValid(byte[])}
     */
    public static boolean isValid (byte[] msg) {
        return msg.length >= 9;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] toByteArray () {
        byte[] t = super.toByteArray();
        byte[] i = ByteArrays.fromInts(new int[] {index, offset});
        byte[] a = ByteArrays.concat(new byte[][] {t, i, block});

        return a;
    }
}
