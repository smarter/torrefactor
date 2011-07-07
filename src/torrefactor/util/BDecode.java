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

package torrefactor.util;

import torrefactor.util.*;

import java.io.*;
import java.security.*;
import java.util.*;

/**
 * Decode a bencoded message from an InputStream.
 */
public class BDecode {
    /**
     * Returns the decoded message in InputStream.
     */
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

    /**
     * Decodes and returns a long from stream.
     */
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

    /**
     * Decodes and returns an array of byte from stream.
     */
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

    /**
     * Decodes and returns a list from stream.
     */
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

    /**
     * Decodes and returns a map("dictionary") from stream.
     */
    static public Map<String, BValue> decodeDict(InputStream stream)
    throws java.io.IOException, InvalidBDecodeException {
        try {
            return decodeDict(stream, null, null);
        } catch (java.security.NoSuchAlgorithmException e) {
            //can't happen, DigestInputStream constructor is never called
            return null;
        }
    }

    /**
     * Decodes and returns a map("dictionary") from stream, and store
     * the SHA1 hash corresponding to the value of the key hashTag in the
     * message to the byte array hashArray.
     * @param stream Stream which contains the message
     * @param hashTag Message key whose value should be hashed
     * @param hashArray 160 bits array where the hash should be stored
     */
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
