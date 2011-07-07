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

import torrefactor.util.BDecode;
import torrefactor.util.BValue;

import java.io.*;
import java.util.*;

/**
 * A class to get the bencoded version of a BValue object.
 */
public class BEncode {
    /**
     * Write the bencoded long val to out.
     */
    private static void encode(long val, ByteArrayOutputStream out) {
        try {
            out.write('i');
            out.write(Long.toString(val).getBytes());
            out.write('e');
        } catch (IOException ignored) {
            //can't happen
        }
    }

    /**
     * Write the bencoded byte array string to out.
     */
    private static void encode(byte[] string, ByteArrayOutputStream out) {
        try {
            out.write(Integer.toString(string.length).getBytes());
            out.write(':');
            out.write(string);
        } catch (IOException ignored) {
            //can't happen
        }
    }

    /**
     * Write the bencoded list to out.
     */
    private static void encode(List<BValue> list, ByteArrayOutputStream out) {
        out.write('l');
        for (BValue elem : list) {
            encode(elem, out);
        }
        out.write('e');
    }

    /**
     * Write the bencoded map to out.
     */
    private static void encode(Map<String, BValue> map, ByteArrayOutputStream out)
    {
        out.write('d');
        for (Map.Entry<String, BValue> entry : map.entrySet()) {
            encode(entry.getKey().getBytes(), out);
            encode(entry.getValue(), out);
        }
        out.write('e');
    }

    /**
     * Write the BValue elem to out.
     */
    public static void encode(BValue elem, ByteArrayOutputStream out) {
        Object object = elem.toObject();
        if (object instanceof Long) {
            encode(elem.toLong(), out);
        } else if (object instanceof byte[]) {
            encode(elem.toByteArray(), out);
        } else if (object instanceof List) {
            encode(elem.toList(), out);
        } else if (object instanceof Map) {
            encode(elem.toMap(), out);
        }
    }

    /**
     * Returns a byte array containing the bencoded representation
     * of the BValue elem.
     */
    public static byte[] encode(BValue elem) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        encode(elem, out);
        return out.toByteArray();
    }
}
