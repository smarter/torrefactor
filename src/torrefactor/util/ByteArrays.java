package torrefactor.util;

import java.net.InetAddress;

public class ByteArrays {
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

    public static long toLong(byte[] array) {
        long l = 0;
        l += ((int) array[0] & 0xFF) << 56;
        l += ((int) array[1] & 0xFF) << 48;
        l += ((int) array[2] & 0xFF) << 40;
        l += ((int) array[3] & 0xFF) << 32;
        l += ((int) array[3] & 0xFF) << 24;
        l += ((int) array[3] & 0xFF) << 16;
        l += ((int) array[3] & 0xFF) << 8;
        l += ((int) array[3] & 0xFF);
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
}
