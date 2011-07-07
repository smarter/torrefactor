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

import torrefactor.util.Logger;
import torrefactor.util.ByteArrays;


/**
 * Represents an unknown message. This message does not have a fixed id. The id
 * is set when constructing the message so it could reflect what has been
 * received on the wire.
 */
public class UnknownMessage extends Message {
    private static final Logger LOG = new Logger();
    private static  byte id = -2;
    final byte[] data;

    /**
     * Construct a new UnknownMessage with the given id and no data.
     *
     * @param    id        id of the message
     */
    public UnknownMessage (byte id) {
        this.id = id;
        this.data = null;
        LOG.warning("Unknown message with id " + id);
    }

    /**
     * Construct a new UnknownMessage with the given id and the given data.
     *
     * @param    id        id of the message
     * @param    data    the data contained in this message
     */
    public UnknownMessage (byte id, byte[] data) {
        this.id = id;
        this.data = data;
        LOG.warning("Unknown message with id " + id
                    + " and lenght " + data.length + ": "
                    + ByteArrays.toHexString(data));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte id () {
        return UnknownMessage.id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] toByteArray () {
        return ByteArrays.concat(new byte[][] {super.toByteArray(), this.data});
    }
}
