package torrefactor.core;

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
