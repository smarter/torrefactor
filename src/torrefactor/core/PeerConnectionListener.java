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

import torrefactor.core.messages.*;

public interface PeerConnectionListener {

    /**
     * Executed when a ChokeMessage is received.
     *
     * @param msg the ChokeMessage received
     */
    public void onChokeMessage (ChokeMessage msg);

    /**
     * Executed when a UnchokeMessage is received.
     *
     * @param msg the UnchokeMessage received
     */
    public void onUnchokeMessage (UnchokeMessage msg);

    /**
     * Executed when a InterestedMessage is received.
     *
     * @param msg the InterestedMessage received
     */
    public void onInterestedMessage (InterestedMessage msg);

    /**
     * Executed when a NotInterestedMessage is received.
     *
     * @param msg the NotInterestedMessage received
     */
    public void onNotInterestedMessage (NotInterestedMessage msg);

    /**
     * Executed when a HaveMessage is received.
     *
     * @param msg the HaveMessage received
     */
    public void onHaveMessage (HaveMessage msg);

    /**
     * Executed when a BitfieldMessage is received.
     *
     * @param msg the BitfieldMessage received
     */
    public void onBitfieldMessage (BitfieldMessage msg);

    /**
     * Executed when a RequestMessage is received.
     *
     * @param msg the RequestMessage received
     */
    public void onRequestMessage (RequestMessage msg);

    /**
     * Executed when a PieceMessage is received.
     *
     * @param msg the PieceMessage received
     */
    public void onPieceMessage (PieceMessage msg);

    /**
     * Executed when a CancelMessage is received.
     *
     * @param msg the CancelMessage received
     */
    public void onCancelMessage (CancelMessage msg);

    /**
     * Executed when a PortMessage is received.
     *
     * @param msg the PortMessage received
     */
    public void onPortMessage (PortMessage msg);

    /**
     * Executed when a UnknownMessage is received.
     *
     * @param msg the UnknownMessage received
     */
    public void onUnknownMessage (UnknownMessage msg);

    /**
     * Executed when a KeepAliveMessage is received.
     */
    public void onKeepAliveMessage ();

    /**
     * Executed when the Handshak is done.
     *
     * @return false if we must abort the handshake.
     */
    public boolean onHandshake(byte[] inPeerId, byte[] inReserved,
            byte[] inInfoHash);

    /**
     * Return our own info hash.
     */
    public byte[] ownInfoHash();
    
}
