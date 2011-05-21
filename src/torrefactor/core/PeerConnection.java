package torrefactor.core;

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
    private static final Logger LOG = new Logger();
    private static final Config CONF = Config.getConfig();

    static final int SEND_BUFFER_SIZE = (1 << 18); // in bytes
    static final int RECEIVE_BUFFER_SIZE = (1 << 18); // in bytes
    static final int CONNECT_TIMEOUT =  5000; // in ms
    static final int SO_TIMEOUT =  2*60*1000; // in ms
    static final boolean USE_STUPID_ENCRYPTION = 
        CONF.getPropertyBoolean("Peer.UseStupidEncryption");
    static final boolean FORCE_STUPID_ENCRYPTION =
        CONF.getPropertyBoolean("Peer.ForceStupidEncryption");
    static final int RSA_KEY_BITLENGTH =
        CONF.getPropertyInt("Peer.RsaKeyBitlength");
    static final int XOR_KEY_LENGTH = CONF.getPropertyInt("Peer.XorLength");

    private PeerConnectionListener listener;
    private Socket socket;
    private InetAddress address;
    private int port;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    /**
     * Creates a new PeerConnection to address at port.
	 *
	 * @param address	the InetAddress of the peer
	 * @param port		the port where the peer is listening
     */
    public PeerConnection (InetAddress address, int port,
            PeerConnectionListener listener) {
        this.listener = listener;
        this.socket = new Socket();
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
        this.address = address;
        this.port = port;
        LOG.setHeader("PeerConnection" + address + ':' + port);
    }

    /**
     * Establish the connection.
	 *
	 * @throws IOException	when an IOException is thrown by connect()
     */
    public void connect ()
    throws IOException {
        LOG.debug("Connecting…");
        this.socket.connect(
            new InetSocketAddress(this.address, this.port),
            CONNECT_TIMEOUT);
        LOG.debug("Connected.");

        
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
        this.inputStream.close();
        this.outputStream.close();
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
	 * @throws IOException	if an IOException was thrown by write() or flush()
     */
    public void send (Message msg) 
    throws IOException {
        byte[] a = msg.toByteArray();
        byte[] len = ByteArrays.fromInt(a.length);
        this.outputStream.write(len);
        this.outputStream.write(a);
        this.outputStream.flush();
        LOG.debug("Sent message: " + msg
                  + ' ' + ByteArrays.toHexString(msg.toByteArray()));
    }

	/**
	 * Receive the next message and dispatch events to the
	 * PeerConnectionListener.
	 * This method does not block and returns null if no message was available.
	 *
	 * @return	the received Message or null if no message was available
	 * @throws IOException when an IOException is thrown by available() or
	 *					   blockingReceive()
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
	 *					   socket.
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

            case SendRsaMessage.id:
                if (SendRsaMessage.isValid(msgArray)) {
                    msg = new SendRsaMessage(msgArray);
                }
                break;

            case SendSymmetricMessage.id:
                if (SendSymmetricMessage.isValid(msgArray)) {
                    msg = new SendSymmetricMessage(msgArray);
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

        return msg;
    }

    /**
     * This method handles the handshake with the peer. 
     * 
	 * @param ownInfoHash	our info hash
	 * @param ownReserved	our reserved bytes
	 * @param ownPeerId		our peer id
	 * @return true if the handshake was successful.
	 * @throws IOException when it occured while reading or writing on the
	 *                     socket
     */
    public boolean handshake (byte[] ownInfoHash, byte[] ownReserved,
        byte[] ownPeerId)
    throws IOException {

        // Send handshake
        outputStream.writeByte(19);
        byte header[] = "BitTorrent protocol".getBytes();
        outputStream.write(header);

        outputStream.write(ownReserved);
        outputStream.write(ownInfoHash);
        outputStream.write(ownPeerId);
        outputStream.flush();

        // Receive handshake
        int inLength = inputStream.read();
        if (inLength == -1) {
            return false;
        }

        byte[] inHeader = new byte[inLength];
        inputStream.readFully(inHeader);
        if (!Arrays.equals(header, inHeader)) {
            LOG.debug("Unsupported protocol header: "
                            + new String(inHeader));
            return false;
        }

        byte[] inReserved = new byte[ownReserved.length];
        inputStream.readFully(inReserved);

        byte[] inInfoHash = new byte[20];
        inputStream.readFully(inInfoHash);
        if (!Arrays.equals(ownInfoHash, inInfoHash)) {
            LOG.warning("Wrong info_hash");
            return false;
        }

        byte[] inPeerId = new byte[20];
        inputStream.readFully(inPeerId);

        if (USE_STUPID_ENCRYPTION) {
            boolean activated = stupidEncryptionSetup(inReserved);
            if ((! activated) && FORCE_STUPID_ENCRYPTION) {
                return false;
            }
        }

        this.listener.onHandshake(inPeerId, inReserved);
        return true;
    }

    /**
     * Setup the StupidEncryption streams.
     * 
	 * @return true if the encryption streams where successfully created.
     */ 
    private boolean stupidEncryptionSetup (byte[] reserved) {
        if ((reserved[7] & (1 << 4)) == 0) {
            // Peer does not support StupidEncryption
            return false;
        }

        Rsa rsa = new Rsa (RSA_KEY_BITLENGTH);
        Pair<DataInputStream, DataOutputStream> oldStreams = null;

        SecureRandom srandom = new SecureRandom();
        byte[] outKey = new byte[XOR_KEY_LENGTH];
        srandom.nextBytes(outKey);

        try {
            stupidEncryptionSendRSAKey(rsa);
            SendRsaMessage rmsg = stupidEncryptionReceiveRSAKey(rsa);
            if (rmsg != null) {
                oldStreams = stupidEncryptionEnableRSAStreams(
                        rsa, rmsg.chunkLength);
                stupidEncryptionSendSymmetricKey(outKey);
                byte[] inKey = stupidEncryptionReceiveSymmetricKey();
                if (inKey != null) {
                    LOG.debug("Recvd XOR is " + ByteArrays.toHexString(inKey));
                    stupidEncryptionDisableRSAStreams(oldStreams);
                    stupidEncryptionEnableSymmetricStreams(inKey, outKey);
                } else {
                    // It's stupid to throws those two exceptions but it
                    // simplifies the code a lot because we're forced
                    // to handle the exceptions comming from the io api.
                    throw new Exception("Didn't receive SendSymmetric");
                }
            } else {
                throw new Exception("Didn't receive SendRsa");
            }
        } catch (Exception e) {
            LOG.error(e);
            e.printStackTrace();
            if (oldStreams != null) {
                try {
                    stupidEncryptionDisableRSAStreams(oldStreams);
                } catch (Exception f) {
                    LOG.error("Got exception while restoring old streams.");
                    f.printStackTrace();
                }
            }
            LOG.error("Failed to enable StupidEncryption aborting connection");
			try {
				close();
			} catch (Exception f) {
				f.printStackTrace();
			}
            return false;
        }

        LOG.info("Stupid encryption enabled.");
        return true;
    }

	/**
	 * Send the public rsa key
	 *
	 * @param rsa	The rsa object from which to get the public key
	 * @throws IOException when send(msg) throws it
	 */
    private void stupidEncryptionSendRSAKey(Rsa rsa)
    throws IOException {
        byte[] key = rsa.getPublicKey();
        byte[] modulo = rsa.getModulo();
        int chunkLength = modulo.length;

        Message msg = new SendRsaMessage(key, modulo, chunkLength);
        send(msg);
        LOG.debug("RSA key sent.");
    }

	/**
	 * Receive the rsa key of the peer.
	 *
	 * @param rsa	The Rsa object which will have its decrypt key set
	 * @return null when something wrong happened and the connection should be
	 *		   closed.
	 * @throws IOException when blockingReceive() throws it
	 */
	private SendRsaMessage stupidEncryptionReceiveRSAKey (Rsa rsa)
    throws IOException {
        Message msg = blockingReceive();
        if (msg.id() != SendRsaMessage.id) {
            LOG.warning("Received message with id " + msg.id() + " while"
                        + "expecting " + SendRsaMessage.id + " (SendRsa)");
            return null;
        }

        SendRsaMessage rmsg = (SendRsaMessage) msg;

        if (! ByteArrays.isPositiveBigInteger(rmsg.key)) {
            LOG.warning("Received non positive RSA key: \n"
                        + ByteArrays.toHexString(rmsg.key));
        }
        if (! ByteArrays.isPositiveBigInteger(rmsg.modulo)) {
            LOG.warning("Received non positive modulo: \n"
                        + ByteArrays.toHexString(rmsg.key));
        }

        rsa.setEncryptKey(rmsg.key, rmsg.modulo);
        LOG.debug("RSA key received.");

        return rmsg;
    }

	/**
	 * Send the xor key
	 *
	 * @param key	the key
	 * @throws IOException when send(msg) throws it
	 */
    private void stupidEncryptionSendSymmetricKey (byte[] key)
    throws IOException {
        Message msg = new SendSymmetricMessage(key);
        send(msg);
        LOG.debug("XOR key sent with length " + key.length);
    }

	/**
	 * Receive the xor key of the peer.
	 *
	 * @return the xor key
	 * @throws IOException when blockingReceive() throws it
	 */
    private byte[] stupidEncryptionReceiveSymmetricKey ()
    throws IOException {
        Message msg = blockingReceive();
        if (msg.id() != SendSymmetricMessage.id) {
            LOG.warning("Received message with id " + msg.id() + " while"
                        + " expecting " + SendSymmetricMessage.id
                        + " (SendSymmetric)");
            return null;
        }

        SendSymmetricMessage smsg = (SendSymmetricMessage) msg;

        return smsg.key;
    }

	/**
	 * Enable the Rsa stream for the StupidEncryption
	 *
	 * @param rsa		the Rsa object to be used by the streams
	 * @param chunkSize	the chunk size to be used by the output stream
	 * @return a pair containing the old DataInputStream and DataOutputStream
	 */
    private Pair <DataInputStream, DataOutputStream> 
    stupidEncryptionEnableRSAStreams (Rsa rsa, int chunkSize) {
        Pair<DataInputStream, DataOutputStream> oldStreams;
        oldStreams = new Pair<DataInputStream, DataOutputStream>
                         (this.inputStream, this.outputStream);

        this.inputStream = new DataInputStream(
                                new RsaInputStream(
                                    this.inputStream, rsa));
        this.outputStream = new DataOutputStream(
                                new RsaOutputStream(
                                    this.outputStream,
                                    rsa,
                                    chunkSize));

        LOG.debug("Now using Rsa streams.");
        return oldStreams;
    }

	/**
	 * Enable the symmetric streams
	 *
	 * @param in	the input stream's xor key
	 * @param out	the output stream's xor key
	 */
    private void stupidEncryptionEnableSymmetricStreams(byte[] in, byte[] out) {
        this.inputStream = new DataInputStream(
                              new XorInputStream(
                                  this.inputStream,
                                  in,
                                  in.length));
        this.outputStream = new DataOutputStream(
                              new XorOutputStream(
                                  this.outputStream,
                                  out,
                                  out.length));
        LOG.debug("Now using XOR encryption.");
    }

	/**
	 * Disable the rsa streams.
	 *
	 * @param oldStreams the pair of DataInputStream, DataOutputStream to use
	 *                   to replace the rsa streams (as returned by
	 *                   stupidEncryptionEnableRSAStreams)
	 */
	private void stupidEncryptionDisableRSAStreams 
        (Pair<DataInputStream, DataOutputStream> oldStreams) {
        this.inputStream = oldStreams.first();
        this.outputStream = oldStreams.second();
        LOG.debug("Rsa streams are disabled");
    }
}
