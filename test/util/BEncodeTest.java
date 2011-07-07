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

package test.util;

import java.io.*;
import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;

import torrefactor.util.*;

public class BEncodeTest {
    public static void main (String[] args) {
        org.junit.runner.JUnitCore.main("test.util.BEncodeTest");
    }

    @Test public void DecodeEncodeDecode()
    throws IOException, InvalidBDecodeException {
        InputStream origStream = new FileInputStream("data/Ocarina_of_Time__Complete_Collection_-_Zelda_Reorchestrated.5241636.TPB.torrent");
        Map<String, BValue> decoded1 = BDecode.decodeDict(origStream);
        //System.out.println("XX");
        byte[] encoded = BEncode.encode(new BValue(decoded1));
        InputStream encStream = new ByteArrayInputStream(encoded);
        Map<String, BValue> decoded2 = BDecode.decodeDict(encStream);
        //System.out.println(decoded1.toString());
        //System.out.println(decoded2.toString());
        /*for (Map.Entry<String, BValue> dec1 : decoded1.entrySet()) {
            BValue dec2Val = decoded2.get(dec1.getKey());
            if (!dec2Val.equals(dec1.getValue())) {
                System.out.println("XX");
                System.out.println(dec2Val.toString());
                System.out.println(dec1.getValue().toString());
            }
        }*/
        assertTrue(decoded1.equals(decoded2));
    }
}
