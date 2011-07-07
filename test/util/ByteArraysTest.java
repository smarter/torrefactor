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

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.math.BigInteger;
import java.util.*;

import torrefactor.util.ByteArrays;


public class ByteArraysTest {

    public static void main (String[] strings) {
        org.junit.runner.JUnitCore.main("test.util.ByteArraysTest");
    }

//    @Test(expected = IllegalArgumentException.class)
//    public void testException(){
//    }

    @Test
    public void testBigIntegerIsPositive() {
        String[] positives = { "0", "2", "98370983749328749832749328749"};
        String[] negatives = { "-2", "-987432874628374629"};

        BigInteger b;
        boolean positive;

        for (int i=0; i<positives.length; i++) {
            b = new BigInteger(positives[i]);
            positive = ByteArrays.isPositiveBigInteger(b.toByteArray());
            assertTrue(positive);
        }

        for (int i=0; i<negatives.length; i++) {
            b = new BigInteger(negatives[i]);
            positive = ByteArrays.isPositiveBigInteger(b.toByteArray());
            assertFalse(positive);
        }
    }

    @Test
    public void testBigInteger() {
        String[] numbers = { "1", "-1", "10", "-10", "-123456789", "123456789"};
        int len = 2;

        BigInteger b,c;
        byte[] a;

        for (int i=0; i<numbers.length; i++) {
            b = new BigInteger(numbers[i]);
            a = ByteArrays.fromBigInteger(b, len);
            c = new BigInteger(a);
            assertTrue(a.length >= len);
            assertTrue(b.compareTo(c) == 0);
        }
    
    }
}
