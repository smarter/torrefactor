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

import torrefactor.util.ByteArrays;

/**
 * Represents a have message.
 *  id          1 byte
 *  pieceIndex  4 byte
 */
public class HaveMessage extends Message {
    final static byte id = 4;
    final int index;

    /**
     * Create a new HaveMessage for the given index.
     *
     * @param index the piece index
     */
    public HaveMessage (int index) {
        this.index = index;
    }

    /**
     * Create a new HaveMessage for the given byte array representation.
     *
     * @param msg the byte array representation from which to build the
     * message.
     */
    public HaveMessage (byte[] msg) {
        this.index = ByteArrays.toInt(msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte id () {
        return HaveMessage.id;
    }

    // Java does not override static method thus we cannot use @inheritDoc
    /**
     * {@link torrefactor.core.Message#isValid(byte[])}
     */
    public static boolean isValid (byte[] msg) {
        return msg.length == 4;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] toByteArray () {
        byte[] t = super.toByteArray();
        byte[] i = ByteArrays.fromInt(index);

        return ByteArrays.concat(new byte[][] {t, i});
    }
}
