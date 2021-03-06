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


public class BDecodeTest {

    public static void main (String[] args) {
        org.junit.runner.JUnitCore.main("test.util.BDecodeTest");
    }

    @Test public void decodeValidInt()
    throws java.io.IOException, InvalidBDecodeException {
        String string = "i12345e";
        InputStream stream = new ByteArrayInputStream(string.getBytes());
        int expected = 12345;

        long result = BDecode.decodeLong(stream);
        assertTrue((int) result == expected);
    }

    @Test public void decodeValidString()
    throws java.io.IOException, InvalidBDecodeException {
        String string = "6:Foobar";
        InputStream stream = new ByteArrayInputStream(string.getBytes());
        String expected = "Foobar";

        String result = new String(BDecode.decodeByteArray(stream));
        assertTrue(result.equals(expected));
    }

    @Test public void decodeValidList()
    throws java.io.IOException, InvalidBDecodeException {
        String string = "l6:coffeei42ee";
        InputStream stream = new ByteArrayInputStream(string.getBytes());
        ArrayList<BValue> result = BDecode.decodeList(stream);

        assertTrue((new String(result.get(0).toByteArray())).equals("coffee"));
        assertTrue(result.get(1).toInt() ==  42);
    }

    @Test public void decodeValidDictionary()
    throws java.io.IOException, InvalidBDecodeException {
        String string = "d3:Foo3:Bar11:coffee cupsi849ee";
        InputStream stream = new ByteArrayInputStream(string.getBytes());

        Map<String, BValue> result = BDecode.decodeDict(stream);

        assertTrue(new String(result.get("Foo").toByteArray()).equals("Bar"));
        assertTrue(result.get("coffee cups").toInt() == 849);
    }
}
