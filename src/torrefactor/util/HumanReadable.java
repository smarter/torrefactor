package torrefactor.util;


public class HumanReadable {

    /**
     * Return a human readable representation of a long.
     * It's clearly not the best way to do it but we are not coding in C so we
     * forget about optimization since we are going to waste CPU cycles and
     * memory anyway.
     */
     public static String fromLong (long l) {
        double t;

        if (l < 1000) {
            return (l + "B");
        }

        t = l / 1024.0d;
        if (t < 1000) {
            return (String.format("%.1f", t) + "KB");
        }


        t = t / 1024.0d;
        if ( t < 1000 ) {
            return (String.format("%.1f", t) + "MB");
        }

        t = t / 1024.0d;
        return (String.format("%.1f", t) + "GB");
    }

}
