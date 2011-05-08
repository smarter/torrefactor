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
