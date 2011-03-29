package torrefactor.util;
import torrefactor.util.InvalidBencodeException;

import java.io.InputStream;
import java.io.PushbackInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class Bencode {
    private Object value = null;

    public Bencode(int number) {
        // This is not an error we store int as long
        this.value = Long.valueOf(number);
    }

    public Bencode(long number) {
        this.value = Long.valueOf(number);
    }

    public Bencode(byte[] byteArray) {
        this.value = byteArray;
    }

    public Bencode(List<Bencode> list) {
        this.value = list;
    }

    public Bencode(HashMap<String, Bencode> map) {
        this.value = map;
    }

    public Object toObject() {
        return this.value;
    }

    public String toString() {
        if (this.value instanceof byte[]) {
            return new String( (byte[]) this.value);
        } else if (this.value instanceof Integer) {
            return ( (Integer) this.value).toString();
        } else if (this.value instanceof Long) {
            return ( (Long) this.value).toString();
        } else if (this.value instanceof List) {
            return ( (List) this.value).toString();
        } else {
            return this.value.toString();
        }
    }

    public int toInt() {
        return (int) ((Long) this.value).longValue();
    }

    public long toLong() {
        return ((Long) this.value).longValue();
    }

    public byte[] toByteArray() {
        return (byte[]) this.value;
    }

    @SuppressWarnings(value = "unchecked")
    public List<Bencode> toList() {
        return (List<Bencode>) this.value;
    }

    @SuppressWarnings(value = "unchecked")
    public HashMap<String, Bencode> toMap() {
        return (HashMap<String, Bencode>) this.value;
    }

    private static Bencode decode(InputStream stream)
    throws java.io.IOException, InvalidBencodeException {
        PushbackInputStream pbstream = new PushbackInputStream(stream);
        int c = pbstream.read();
        pbstream.unread(c);

        if (c == -1) throw new InvalidBencodeException(
                                                   "Unexpected end of stream");

        if (Character.isDigit((char) c)) {
            return new Bencode(decodeByteArray(pbstream));
        }
        switch ((char) c) {
        case 'i':
            return new Bencode(decodeLong(pbstream));
        case 'l':
            return new Bencode(decodeList(pbstream));
        case 'd':
            return new Bencode(decodeDict(pbstream));
        default:
            throw new InvalidBencodeException(
                                     "Not a Bencode string (should start with "
                                   + "'i', 'l', 'd' or a digit)");
        }
    }

    static public long decodeLong(InputStream stream)
    throws java.io.IOException, InvalidBencodeException {
        StringBuilder sb = new StringBuilder();

        int c = stream.read();
        if (c != (int) 'i') throw new InvalidBencodeException(
                                                            "Not an int/long");
        while ((c = stream.read()) != (int) 'e') {
            if (c == -1) throw new InvalidBencodeException(
                              "Reached end of stream while parsing int/long.");
            sb.append((char) c);
        }

        long longValue;
        try {
            longValue = Long.parseLong(sb.toString());
            return longValue;
        } catch (NumberFormatException exception) {
            throw new InvalidBencodeException('"' + sb.toString()
                                           + "\" is not a bencoded int/long.");
        }

    }

    static public byte[] decodeByteArray(InputStream stream)
    throws java.io.IOException, InvalidBencodeException {
        StringBuilder sb = new StringBuilder();

        int c;
        while ((c = stream.read()) != (int) ':') {
            if (c == -1) throw new InvalidBencodeException(
                    "Reached end of stream while parsing string length.");
            sb.append((char) c);
        }

        int length;
        try {
            length = Integer.parseInt(sb.toString());
        } catch (NumberFormatException exception) {
            throw new InvalidBencodeException('"' + sb.toString()
                    + "\" is not a bencoded string: the length is not valid.");
        }

        byte[] byteArray = new byte[length];
        for (int i = 0; i < length; i++) {
            c = stream.read();
            if (c == -1) throw new InvalidBencodeException(
                    "Reached end of stream while parsing string.");
            if (c >= 256) throw new InvalidBencodeException("!");
            byteArray[i] = (byte) c;
        }

        return byteArray;
    }


    static public ArrayList<Bencode> decodeList(InputStream stream)
    throws java.io.IOException, InvalidBencodeException {
        ArrayList<Bencode> list = new ArrayList<Bencode>();
        PushbackInputStream pbstream = new PushbackInputStream(stream);

        int c = stream.read();
        if (c != (int) 'l') throw new InvalidBencodeException("Not a list");

        while ((c = stream.read()) != (int) 'e') {
            pbstream.unread(c);
            list.add(decode(pbstream));
        }
        return list;
    }

    static public HashMap<String, Bencode> decodeDict(InputStream stream)
    throws java.io.IOException, InvalidBencodeException {
        try {
            return decodeDict(stream, null, null);
        } catch (java.security.NoSuchAlgorithmException e) {
            //can't happen, DigestInputStream constructor is never called
            return null;
        }
    }

    static public HashMap<String, Bencode>
    decodeDict(InputStream stream, String hashTag, byte[] hashArray)
    throws java.io.IOException, java.security.NoSuchAlgorithmException,
           InvalidBencodeException {
        DigestInputStream dstream = null;

        PushbackInputStream pbstream;
        if (hashTag != null) {
            dstream = (DigestInputStream) stream;
            dstream.on(false);
            pbstream = new PushbackInputStream(dstream);
        } else {
            pbstream = new PushbackInputStream(stream);
        }

        HashMap<String, Bencode> map = new HashMap<String, Bencode>();

        int c = pbstream.read();
        if (c != (int) 'd') throw new InvalidBencodeException("Not a dictionary");

        boolean foundTag = false;
        while ((c = pbstream.read()) != (int) 'e') {
            if (!Character.isDigit((char) c)) {
                throw new InvalidBencodeException("Didn't got end of "
                        + "dictionary or bencoded string as dictionary key.");
            }
            pbstream.unread(c);
            String key;
            key = new String(decodeByteArray(pbstream));
            if (key.equals(hashTag)) {
                foundTag = true;
                dstream.on(true);
            }
            map.put(key, decode(pbstream));
            if (dstream != null && foundTag) {
                dstream.on(false);
                byte[] sha1Hash = dstream.getMessageDigest().digest();
                for (int i = 0; i < hashArray.length; i++) {
                    hashArray[i] = sha1Hash[i];
                }
                dstream = null;
            }
        }

        return map;
    }
}
