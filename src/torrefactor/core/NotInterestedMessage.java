package torrefactor.core;

public class NotInterestedMessage extends Message {
    final static byte id = 3;

    public byte id () {
        return NotInterestedMessage.id;
    }
}
