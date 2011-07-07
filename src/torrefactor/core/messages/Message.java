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
 * This is the parent class of all messages. The variable id MUST be overriden
 * by classes extending this class.
 */
public abstract class Message {

    // Should be abstract but java doesn't allow abstract variables. Thus we
    // default to something stupid so we may have a better clue if it goes
    // wrong.
    //public final static byte id = -42;

    /**
     * Workaround to emulate overiding class variable: this methods must return
     * the class variable id.
     */
    abstract byte id();
    
    /**
     * Check if the given byte array can be interpreted as a valid Message.
     *
     * @param    msg        the byte array on which the check is made
     * @return    true if msg can be interpreted as a valid message for this
     *            class
     */
    public static boolean isValid (byte[] msg) {
        return msg.length == 0;
    }

    /**
     * Returns the byte array representation of this message as it should be
     * sent on the wire.
     *
     * @return the byte array representation of this message
     */
    public byte[] toByteArray () {
        return new byte[] {id()};
    }
}
