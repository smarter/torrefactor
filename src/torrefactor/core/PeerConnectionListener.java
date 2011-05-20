package torrefactor.core;

public interface PeerConnectionListener {

        public void onChokeMessage (ChokeMessage msg);
        public void onUnchokeMessage (UnchokeMessage msg);
        public void onInterestedMessage (InterestedMessage msg);
        public void onNotInterestedMessage (NotInterestedMessage msg);
        public void onHaveMessage (HaveMessage msg);
        public void onBitfieldMessage (BitfieldMessage msg);
        public void onRequestMessage (RequestMessage msg);
        public void onPieceMessage (PieceMessage msg);
        public void onCancelMessage (CancelMessage msg);
        public void onPortMessage (PortMessage msg);
        public void onUnknownMessage (UnknownMessage msg);

        public void onConnectionClosed ();
        public void onKeepAliveMessage ();
        public void onHandshake(byte[] inPeerId, byte[] inReserved);
    
}
