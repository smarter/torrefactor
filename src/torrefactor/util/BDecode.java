package torrefactor.util;

import torrefactor.util.*;

import java.io.*;
import java.security.*;
import java.util.*;

public class BDecode {
    private static BValue decode(InputStream stream)
    throws java.io.IOException, InvalidBDecodeException {
        PushbackInputStream pbstream = new PushbackInputStream(stream);
        int c = pbstream.read();
        pbstream.unread(c);

        if (c == -1) throw new InvalidBDecodeException(
                                                   "Unexpected end of stream");

        if (Character.isDigit((char) c)) {
            return new BValue(decodeByteArray(pbstream));
        }
        switch ((char) c) {
        case 'i':
            return new BValue(decodeLong(pbstream));
        case 'l':
            return new BValue(decodeList(pbstream));
        case 'd':
            return new BValue(decodeDict(pbstream));
        default:
            throw new InvalidBDecodeException(
                                     "Not a BDecode string (should start with "
                                   + "'i', 'l', 'd' or a digit)");
        }
    }

    static public long decodeLong(InputStream stream)
    throws java.io.IOException, InvalidBDecodeException {
        StringBuilder sb = new StringBuilder();

        int c = stream.read();
        if (c != (int) 'i') throw new InvalidBDecodeException(
                                                            "Not an int/long");
        while ((c = stream.read()) != (int) 'e') {
            if (c == -1) throw new InvalidBDecodeException(
                              "Reached end of stream while parsing int/long.");
            sb.append((char) c);
        }

        long longValue;
        try {
            longValue = Long.parseLong(sb.toString());
            return longValue;
        } catch (NumberFormatException exception) {
            throw new InvalidBDecodeException('"' + sb.toString()
                                           + "\" is not a bencoded int/long.");
        }

    }

    static public byte[] decodeByteArray(InputStream stream)
    throws java.io.IOException, InvalidBDecodeException {
        StringBuilder sb = new StringBuilder();

        int c;
        while ((c = stream.read()) != (int) ':') {
            if (c == -1) throw new InvalidBDecodeException(
                    "Reached end of stream while parsing string length.");
            sb.append((char) c);
        }

        int length;
        try {
            length = Integer.parseInt(sb.toString());
        } catch (NumberFormatException exception) {
            throw new InvalidBDecodeException('"' + sb.toString()
                    + "\" is not a bencoded string: the length is not valid.");
        }

        byte[] byteArray = new byte[length];
        for (int i = 0; i < length; i++) {
            c = stream.read();
            if (c == -1) throw new InvalidBDecodeException(
                    "Reached end of stream while parsing string.");
            if (c >= 256) throw new InvalidBDecodeException("!");
            byteArray[i] = (byte) c;
        }

        return byteArray;
    }


    static public ArrayList<BValue> decodeList(InputStream stream)
    throws java.io.IOException, InvalidBDecodeException {
        ArrayList<BValue> list = new ArrayList<BValue>();
        PushbackInputStream pbstream = new PushbackInputStream(stream);

        int c = stream.read();
        if (c != (int) 'l') throw new InvalidBDecodeException("Not a list");

        while ((c = stream.read()) != (int) 'e') {
            pbstream.unread(c);
            list.add(decode(pbstream));
        }
        return list;
    }

    static public Map<String, BValue> decodeDict(InputStream stream)
    throws java.io.IOException, InvalidBDecodeException {
        try {
            return decodeDict(stream, null, null);
        } catch (java.security.NoSuchAlgorithmException e) {
            //can't happen, DigestInputStream constructor is never called
            return null;
        }
    }

    static public Map<String, BValue>
    decodeDict(InputStream stream, String hashTag, byte[] hashArray)
    throws java.io.IOException, java.security.NoSuchAlgorithmException,
           InvalidBDecodeException {
        DigestInputStream dstream = null;

        PushbackInputStream pbstream;
        if (hashTag != null) {
            dstream = (DigestInputStream) stream;
            dstream.on(false);
            pbstream = new PushbackInputStream(dstream);
        } else {
            pbstream = new PushbackInputStream(stream);
        }

        HashMap<String, BValue> map = new HashMap<String, BValue>();

        int c = pbstream.read();
        if (c != (int) 'd') throw new InvalidBDecodeException("Not a dictionary");

        boolean foundTag = false;
        while ((c = pbstream.read()) != (int) 'e') {
            if (!Character.isDigit((char) c)) {
                throw new InvalidBDecodeException("Didn't got end of "
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
