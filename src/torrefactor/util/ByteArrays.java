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

import java.net.InetAddress;
import java.math.BigInteger;

/**
 * This class contains static helper functions to handle arrays of bytes
 */
public class ByteArrays {
    private static Logger LOG = new Logger();

    /**
     * "Compact IP-address/port info" format as specified in BEP 0005
     * Returns the 4 bytes IP address followed by the 2 bytes port.
     * Both are in network byte order.
     */
    public static byte[] compact(InetAddress ip, int port) {
        byte[] ret = new byte[6];
        System.arraycopy(ip.getAddress(), 0, ret, 0, 4);
        System.arraycopy(fromShortInt(port), 0, ret, 5, 2);
        return ret;
    }

    /**
     * "Compact node info" format as specified in BEP 0005
     * Returns the 20 bytes of the id followed by the 6 bytes
     * "Compact IP-address/port info".
     */
    public static byte[] compact(byte[] id, InetAddress ip, int port) {
        byte[] ret = new byte[26];
        System.arraycopy(id, 0, ret, 0, 20);
        byte[] ipPortInfo = compact(ip, port);
        System.arraycopy(ipPortInfo, 0, ret, 21, 6);
        return ret;
    }

    /**
     * Returns true if the first length bits are set to 1, false otherwise
     */
    public static boolean isComplete(byte[] bitfield, int length) {
        int i;
        for (i = 0; i<length/8; i++) {
            if (bitfield[i] != (byte) 0xFF) {
                return false;
            }
        }
        if (length % 8 == 0) {
            return true;
        }
        byte lastByte = (byte) (0xFF << 7 - (length % 8));
        return bitfield[i] == lastByte;
    }

    /**
     * Returns the percentage of bits set to 1.
     */
    public static double percentage(byte[] bitfield) {
        long max = bitfield.length * 8;
        long count = done(bitfield);

        return ((double) count) / ((double) max);
    }

    /**
     * Returns how much bits are set to 1.
     */
    public static int done(byte[] bitfield) {
        return new BigInteger(1, bitfield).bitCount();
    }

    /**
     * Returns an array of byte that is the concatenation
     * of all the arrays in arrays.
     * @param arrays Array of arrays of bytes to concatenate
     */
    public static byte[] concat(byte[][] arrays) {
        int size = 0;
        for (int i=0; i<arrays.length; i++) {
            size += arrays[i].length;
        }
        byte[] b = new byte[size];
        int offset = 0;
        for (int i=0; i<arrays.length; i++) {
            System.arraycopy(arrays[i], 0, b, offset, arrays[i].length);
            offset += arrays[i].length;
        }
        return b;
    }

    /**
     * Returns whether or not the byte with index "index" in the array
     * b is set.
     */
    public static boolean isBitSet(byte[] b, int index) {
        int bit = 7 - (index % 8);
        return ((b[index/8] >>> bit) & 1) == 1;
    }

    /**
     * Set the byte at position index in array b to 0 if value is 0
     * or to 1 if value is anything else.
     */
    public static void setBit(byte[] b, int index, int value) {
        int bit = 7 - (index % 8);
        if (value == 0) {
            b[index/8] &= ~(1 << bit);
        } else {
            b[index/8] |= (1 << bit);
        }
    }

    /**
     * Return the length in bits of the common prefix of
     * arrays a and b.
     */
    public static int commonPrefix(byte[] a, byte[] b) {
        byte xorByte = 0;
        int i;
        for (i = 0; i != 160/8 && xorByte == 0; i++) {
            xorByte = (byte) (a[i] ^ b[i]);
        }
        if (i == 160/8) {
            return 160;
        }
        int offset;
        for (offset = 7; (xorByte >>> offset) == 0; offset--) {
            ; // Do nothing
        }
        return 8*i + (7 - offset);
    }

    /**
     * Returns the int represented in the first two
     * elements of the array in Big Endian.
     */
    public static int toShortInt(byte[] array) {
        int i = 0;
        i += ((int) array[0] & 0xFF) << 8;
        i += ((int) array[1] & 0xFF);
        return i;
    }

    /**
     * Returns the int represented in the first four
     * elements of the array in Big Endian.
     */
    public static int toInt(byte[] array) {
        int i = 0;
        i += ((int) array[0] & 0xFF) << 24;
        i += ((int) array[1] & 0xFF) << 16;
        i += ((int) array[2] & 0xFF) << 8;
        i += ((int) array[3] & 0xFF);
        return i;
    }

    /**
     * Returns the int represented in the four elements
     * of the array at offset in Big Endian.
     */
    public static int toInt(byte[] array, int offset) {
        // FIXME: It may be better to do as in toInt(byte[]) but with offsets.
        //        But since I absolutly don't know what the compiler and the
        //        jvm will do of this code, it sounds like premature
        //        optimisation. Thus I'll leave it as it for now.
        byte[] i = new byte[4];
        System.arraycopy(array, offset, i, 0, 4);
        return toInt(i);
    }

    /**
     * Returns the long represented in the first eight
     * elements of the array in Big Endian.
     */
    public static long toLong(byte[] array) {
        long l = 0;
        l += ((int) array[0] & 0xFF) << 56;
        l += ((int) array[1] & 0xFF) << 48;
        l += ((int) array[2] & 0xFF) << 40;
        l += ((int) array[3] & 0xFF) << 32;
        l += ((int) array[4] & 0xFF) << 24;
        l += ((int) array[5] & 0xFF) << 16;
        l += ((int) array[6] & 0xFF) << 8;
        l += ((int) array[7] & 0xFF);
        return l;
    }


    /**
     * Returns the Big Endian representation of the 16 low bits
     * of i as an array of 2 bytes.
     */
    public static byte[] fromShortInt(int i) {
        return new byte[] { (byte)(i >>> 8),
                            (byte) i };
    }

    /**
     * Returns the Big Endian representation of the integer i as
     * an array of 4 bytes.
     */
    public static byte[] fromInt(int i) {
        return new byte[] { (byte)(i >>> 24),
                            (byte)(i >>> 16),
                            (byte)(i >>> 8),
                            (byte) i };
    }

    /**
     * Returns the concatenation of the Big Endian representations
     * of the integers in an array of length 4*array.length
     */
    public static byte[] fromInts(int[] array) {
        byte[] b = new byte[array.length * 4];
        int offset = 0;
        for (int i=0; i<array.length; i++) {
            System.arraycopy(fromInt(array[i]), 0, b, offset, 4);
            offset += 4;
        }
        return b;
    }

    /**
     * Returns the Big Endian representation of the long l as
     * an array of 8 bytes.
     */
    public static byte[] fromLong(long l) {
        return new byte[] { (byte)(l >>> 56),
                            (byte)(l >>> 48),
                            (byte)(l >>> 40),
                            (byte)(l >>> 32),
                            (byte)(l >>> 24),
                            (byte)(l >>> 16),
                            (byte)(l >>> 8),
                            (byte) l };
    }

    /**
     * Returns the byte array representation the BigInteger fitting the given
     * length. If the given length is to small, the returned array will be
     * bigger.
     */
    public static byte[] fromBigInteger(BigInteger bi, int arrayLength) {
        byte[] array;
        byte[] biArray = bi.toByteArray();
        if (biArray.length > arrayLength) {
            // The BigInteger is bigger than the requested length.
            return biArray;
        } else {
            array = new byte[arrayLength];
        }

        if (isPositiveBigInteger(biArray)) {
            // BigInteger is positive; pad with zeroes.
            System.arraycopy(biArray, 0,
                             array, array.length-biArray.length,
                             biArray.length);
        } else {
            // BigInteger is negative; pad with ones.
            for (int i=0; i<array.length-biArray.length; i++) {
                array[i] = (byte) 0xFF;
            }
            System.arraycopy(biArray, 0,
                             array, array.length-biArray.length,
                             biArray.length);
        }

        return array;
    }

    /**
     * Returns the byte array representation of the unsigned BigInteger padding
     * left with zeroes to fit the given length.  If the given length is to
     * small, the returned array will be bigger.  This function is not supposed
     * to work correctly with negative numbers.
     */
    public static byte[] fromUnsignedBigInteger(BigInteger bi, int arrayLength)
    {
        int minlen = (bi.bitLength() + 7) / 8;
        byte[] array;
        if (minlen > arrayLength) {
            array = new byte[minlen];
        } else {
            array = new byte[arrayLength];
        }

        byte[] biArray = bi.toByteArray();
        int biaOffset;
        if (biArray[0] == 0) {
            biaOffset = 1; //Remove extra byte for positive sign
        } else {
            LOG.debug(ByteArrays.class,
                        "Non-zero byte at offset 0 for BigInteger "
                              + bi.toString() + " with representation "
                              + toHexString(biArray));
            biaOffset = 0;
        }
        int cpLength = biArray.length - biaOffset;
        int arrayOffset = array.length - cpLength;
        System.arraycopy(biArray, biaOffset, array, arrayOffset, cpLength);
        
        return array;
    }

    /**
     * Returns the unsigned BigInteger from the value of the given byte array.
     */
    public static BigInteger toUnsignedBigInteger(byte[] array) {
        byte[] realArray = new byte[array.length];
        //System.arraycopy(array, 0, realArray, 1, array.length);
        //BigInteger bi = new BigInteger(realArray);
        BigInteger bi = new BigInteger(array);
        return bi;
    }

    /**
     * Returns true if the BigInteger that would be constructed from the byte
     * array is positive.
     */
    public static boolean isPositiveBigInteger(byte[] array) {
        if ((array[0] & (1 << 31)) == 0) {
            return true;
        }
        return false;
    }

    /**
     * Returns the hexadecimal string representation of the given byte array.
     */
    public static String toHexString(byte[] array) {
        StringBuilder sb = new StringBuilder(); //FIXME: predict length
        for (int i=0; i<array.length; i++) {
            int v = (int) array[i] & 0xFF;
            if (v > 15) {
                sb.append(Integer.toHexString(v));
            } else if (v > 0) {
                sb.append(0);
                sb.append(Integer.toHexString(v));
            } else {
                sb.append(0);
                sb.append(0);
            }
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Returns the binary string representation of the given byte array.
     */
    public static String toBinaryString(byte[] array) {
        StringBuilder sb = new StringBuilder(); //FIXME: predict length
        for (int i=0; i<array.length; i++) {
            sb.append(Integer.toBinaryString((int) array[i] & 0xFF));
            sb.append(" ");
        }
        return sb.toString();
    }

}
