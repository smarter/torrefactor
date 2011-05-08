package torrefactor.util;

import torrefactor.util.*;

import java.net.InetAddress;
import java.math.BigInteger;

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
     * Returns true if all bits of bitfield are set to 1.
     */
    public static boolean isComplete(byte[] bitfield) {
        for (int i=0; i<bitfield.length; i++) {
            if (bitfield[i] != (byte) 0xFF) {
                return false;
            }
        }
        return true;
    }

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

    public static int toShortInt(byte[] array) {
        int i = 0;
        i += ((int) array[0] & 0xFF) << 8;
        i += ((int) array[1] & 0xFF);
        return i;
    }

    public static int toInt(byte[] array) {
        int i = 0;
        i += ((int) array[0] & 0xFF) << 24;
        i += ((int) array[1] & 0xFF) << 16;
        i += ((int) array[2] & 0xFF) << 8;
        i += ((int) array[3] & 0xFF);
        return i;
    }

    public static int toInt(byte[] array, int offset) {
        // FIXME: It may be better to do as in toInt(byte[]) but with offsets.
        //        But since I absolutly don't know what the compiler and the
        //        jvm will do of this code, it sounds like premature
        //        optimisation. Thus I'll leave it as it for now.
        byte[] i = new byte[4];
        System.arraycopy(array, offset, i, 0, 4);
        return toInt(i);
    }

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

    public static byte[] fromShortInt(int i) {
        return new byte[] { (byte)(i >>> 8),
                            (byte) i };
    }

    public static byte[] fromInt(int i) {
        return new byte[] { (byte)(i >>> 24),
                            (byte)(i >>> 16),
                            (byte)(i >>> 8),
                            (byte) i };
    }

    public static byte[] fromInts(int[] array) {
        byte[] b = new byte[array.length * 4];
        int offset = 0;
        for (int i=0; i<array.length; i++) {
            System.arraycopy(fromInt(array[i]), 0, b, offset, 4);
            offset += 4;
        }
        return b;
    }

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
