package torrefactor.util;

import java.io.Serializable;

/**
 * This class calculate the average speed at which a value changed between
 * two call to the function getSpeed.
 */
public class SpeedMeter implements Serializable {
    long lastValue;
    long lastTime;

    /**
     * Create a new speed meter with the given value.
     */
    public SpeedMeter (long value) {
        this.lastValue = value;
        this.lastTime = System.currentTimeMillis();
    }

    /**
     * Return the speed at which the value changed since the last call.
     *
     * @param currentValue  the currentValue of the variable which speed is
     *                      calculated
     */
    public double getSpeed (long currentValue) {
        long currentTime = System.currentTimeMillis();
        double deltav = currentValue - lastValue;
        double deltat = (currentTime - lastTime);
        double speed = deltav * 1000 / deltat;

        lastValue = currentValue;
        lastTime = currentTime;

        return speed;
    }
}
