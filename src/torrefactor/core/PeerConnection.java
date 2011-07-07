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
import torrefactor.util.*;

import java.net.*;
import java.io.*;
import java.util.Arrays;
import java.security.SecureRandom;


/**
 * This class represents a connection with a peer.
 * It handles the IO streams and provides methods to send and receive messages.
 */
public class PeerConnection {
    private final Logger LOG = new Logger();
    private static final Config CONF = Config.getConfig();

    static final byte BITTORRENT_HEADER[] = "BitTorrent protocol".getBytes();
    static final int SEND_BUFFER_SIZE = (1 << 18); // in bytes
    static final int RECEIVE_BUFFER_SIZE = (1 << 18); // in bytes
    static final int CONNECT_TIMEOUT =  5000; // in ms
    static final int SO_TIMEOUT =  2*60*1000; // in ms
    static final int MAX_MESSAGE_LENGTH = 1 << 21;

    private PeerConnectionListener listener;
    private Socket socket;
    private InetAddress address;
    private int port;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    /**
     * Create a new PeerConnection to address at port.
     *
     * @param address    the InetAddress of the peer
     * @param port       the port where the peer is listening
     * @param listener   the PeerConnectionListener for this connection
     */
    public PeerConnection (InetAddress address, int port,
            PeerConnectionListener listener) {
        this.listener = listener;
        this.socket = new Socket();
        setupSocket();
        this.address = address;
        this.port = port;
        LOG.setHeader("PeerConnection" + address + ':' + port);
    }

    /**
     * Create a new PeerConnection from socket.
     *
     * @param socket    the socket connected to the peer
     * @param listener  the PeerConnectionListener for this connection
     */
    public PeerConnection (Socket socket, PeerConnectionListener listener) {
        this.listener = listener;
        this.socket = socket;
        this.address = this.socket.getInetAddress();
        this.port = this.socket.getPort();

        if (this.socket.isConnected()) {
            try {
                setupStreams();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Set various parameters of the socket.
     */
    private void setupSocket () {
        try {
            this.socket.setSendBufferSize(SEND_BUFFER_SIZE);
        } catch (SocketException e) {
            LOG.warning("Couldn't set socket's send buffer size to "
                        + SEND_BUFFER_SIZE + ": " + e.getMessage());
        }
        try {
            this.socket.setReceiveBufferSize(RECEIVE_BUFFER_SIZE);
        } catch (SocketException e) {
            LOG.warning("Couldn't set socket's receive buffer size to "
                        + RECEIVE_BUFFER_SIZE + ": " + e.getMessage());
        }
        try {
            this.socket.setSoTimeout(SO_TIMEOUT);
        } catch (SocketException e) {
            LOG.warning("Couldn't set socket's SO_TIMOUT to " + SO_TIMEOUT
                        + ": " + e.getMessage());
        }
    }

    /**
     * Establish the connection.
     *
     * @throws IOException    when an IOException is thrown by connect()
     */
    public void connect ()
    throws IOException {
        LOG.debug("Connecting…");
        this.socket.connect(
            new InetSocketAddress(this.address, this.port),
            CONNECT_TIMEOUT);
        LOG.debug("Connected.");

        setupStreams();
    }

    /**
     * Setup streams
     *
     * @throws IOException  when getInputStream() or getOutputStream() throws
     *                      it
     */
    private void setupStreams ()
    throws IOException {
        // FIXME: Why do we need Buffered streams?
        inputStream = new DataInputStream(
                          new BufferedInputStream(
                              this.socket.getInputStream()));
        outputStream = new DataOutputStream(
                           new BufferedOutputStream(
                               this.socket.getOutputStream()));
    }

    /**
     * Close the connection.
     *
     * @throws IOException when an IOException is thrown by close()
     */
    public void close ()
    throws IOException {
        if (this.inputStream != null) {
            this.inputStream.close();
        }
        if (this.outputStream != null) {
            this.outputStream.close();
        }
        this.socket.close();
    }

    /**
     * Returns wether the connection is established or not.
     *
     * @return true if the connection is established
     */
    public boolean isConnected () {
        return this.socket.isConnected();
    }

    /**
     * Sends the given message to the peer.
     *
     * @throws IOException    if an IOException was thrown by write() or flush()
     */
    public void send (Message msg) 
    throws IOException {
        byte[] a = msg.toByteArray();
        byte[] len = ByteArrays.fromInt(a.length);
        this.outputStream.write(len);
        this.outputStream.write(a);
        this.outputStream.flush();
        
        if (a.length > 12) {
        LOG.debug("Sent message: " + msg
                  + ' ' + "with len: "
                  + ByteArrays.toHexString(len) + " (" + a.length + ")");
        } else {
        LOG.debug("Sent message: " + msg
                  + ' ' + ByteArrays.toHexString(a) + "with len: "
                  + ByteArrays.toHexString(len) + " (" + a.length + ")");
        }
    }

    /**
     * Receive the next message and dispatch events to the
     * PeerConnectionListener.
     * This method does not block and returns null if no message was available.
     *
     * @return    the received Message or null if no message was available
     * @throws IOException when an IOException is thrown by available() or
     *                       blockingReceive()
     */
    public Message receive () 
    throws IOException {
        if (this.inputStream.available() == 0) return null;

        return blockingReceive();
    }

    /**
     * Reads the next message and dispatch events to the
     * PeerConnectionListener.
     * This method block until a message is received.
     *
     * @return the received Message
     * @throws IOException when in IOException occured while reading on the
     *                       socket.
     */
    public Message blockingReceive ()
    throws IOException {

        byte[] i = new byte[4];
        Message msg = null;
        
        this.inputStream.read(i);
        int len = ByteArrays.toInt(i);
        LOG.debug("Length: " + len);

        if (len == 0) {
            msg = new KeepAliveMessage();
            this.listener.onKeepAliveMessage();
            return msg;
        } else if (len > MAX_MESSAGE_LENGTH) {
            LOG.error("Got message with length=" + len
                      + " (len>MAX_MESSAGE_LENGTH) dropping connection.");
            close();
        }

        byte id = this.inputStream.readByte();
        len--;
        LOG.debug("Id: " + id);

        byte[] msgArray = null;
        if (len > 0) {
            msgArray = new byte[len];
            inputStream.readFully(msgArray);
        }
        LOG.debug("Message read with id: " + id);

        switch (id) {

            case ChokeMessage.id:
                msg = new ChokeMessage();
                this.listener.onChokeMessage((ChokeMessage) msg);
                break;
        
            case UnchokeMessage.id:
                msg = new UnchokeMessage();
                this.listener.onUnchokeMessage((UnchokeMessage) msg);
                break;

            case InterestedMessage.id:
                msg = new InterestedMessage();
                this.listener.onInterestedMessage((InterestedMessage) msg);
                break;

            case NotInterestedMessage.id:
                msg = new NotInterestedMessage();
                this.listener.onNotInterestedMessage(
                        (NotInterestedMessage) msg);
                break;

            case HaveMessage.id:
                if (HaveMessage.isValid(msgArray)) {
                    msg = new HaveMessage(msgArray);
                    this.listener.onHaveMessage((HaveMessage) msg);
                }
                break;

            case BitfieldMessage.id:
                if (BitfieldMessage.isValid(msgArray)) {
                    msg = new BitfieldMessage(msgArray);
                    this.listener.onBitfieldMessage((BitfieldMessage) msg);
                }
                break;

            case RequestMessage.id:
                if (RequestMessage.isValid(msgArray)) {
                    msg = new RequestMessage(msgArray);
                    this.listener.onRequestMessage((RequestMessage) msg);
                }
                break;

            case PieceMessage.id:
                if (PieceMessage.isValid(msgArray)) {
                    msg = new PieceMessage(msgArray);
                    this.listener.onPieceMessage((PieceMessage) msg);
                }
                break;

            case CancelMessage.id:
                if (CancelMessage.isValid(msgArray)) {
                    msg = new CancelMessage(msgArray);
                    this.listener.onCancelMessage((CancelMessage) msg);
                }
                break;

            case PortMessage.id:
                if (PortMessage.isValid(msgArray)) {
                    msg = new PortMessage(msgArray);
                    this.listener.onPortMessage((PortMessage) msg);
                }
                break;

            default:
                if (msgArray == null) {
                    msg = new UnknownMessage(id);
                } else {
                    msg = new UnknownMessage(id, msgArray);
                }
                this.listener.onUnknownMessage((UnknownMessage) msg);
                break;
        }

        if (msg == null) {
            LOG.warning("Non-valid message of known type:");
            msg = new UnknownMessage(id, msgArray);
            this.listener.onUnknownMessage((UnknownMessage) msg);
        }

        LOG.debug("Message is " + msg);
        return msg;
    }

    /**
     * This method handles the handshake with the peer. 
     * 
     * @param ownInfoHash    our info hash
     * @param ownReserved    our reserved bytes
     * @param ownPeerId        our peer id
     * @return true if the handshake was successful.
     * @throws IOException when it occured while reading or writing on the
     *                     socket
     */
    public boolean handshake (byte[] ownInfoHash, byte[] ownReserved,
            byte[] ownPeerId)
    throws IOException {

        if (ownInfoHash != null) {
            sendHandshake(ownInfoHash, ownReserved, ownPeerId);
            if (!receiveHandshake(ownReserved.length)) return false;
        }

        else {
            if (!receiveHandshake(ownReserved.length)) return false;
            sendHandshake(this.listener.ownInfoHash(), ownReserved, ownPeerId);
        }

        return true;
    }
    
    private void sendHandshake (byte[] ownInfoHash, byte[] ownReserved,
            byte[] ownPeerId) 
    throws IOException {
        outputStream.writeByte(BITTORRENT_HEADER.length);
        outputStream.write(BITTORRENT_HEADER);

        outputStream.write(ownReserved);
        outputStream.write(ownInfoHash);
        outputStream.write(ownPeerId);
        outputStream.flush();
    }

    /**
     * Receive the handshake.
     *
     * @return Triplet resereved bytes, peer id, info hash
     */
    private boolean receiveHandshake (int reservedLength)
    throws IOException {
        int inLength = inputStream.read();
        if (inLength == -1) {
            return false;
        }

        byte[] inHeader = new byte[inLength];
        inputStream.readFully(inHeader);
        if (!Arrays.equals(BITTORRENT_HEADER, inHeader)) {
            LOG.debug("Unsupported protocol header: "
                            + new String(inHeader));
            return false;
        }

        byte[] inReserved = new byte[reservedLength];
        inputStream.readFully(inReserved);

        byte[] inInfoHash = new byte[20];
        inputStream.readFully(inInfoHash);

        byte[] inPeerId = new byte[20];
        inputStream.readFully(inPeerId);

        if (!this.listener.onHandshake(inPeerId, inReserved, inInfoHash)) {
            return false;
        }

        return true;
    }
}
