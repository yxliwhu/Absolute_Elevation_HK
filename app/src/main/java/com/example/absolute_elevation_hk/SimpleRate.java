package com.example.absolute_elevation_hk;

public class SimpleRate {
    /**
     * 10Hz
     */
    private int SENSOR_RATE_SLOW = 100000;
    /**
     * 50Hz
     */
    private int SENSOR_RATE_NORMAL = 20000;
    /**
     * 80Hz
     */
    private int SENSOR_RATE_MIDDLE = 12500;
    /**
     * 100Hz
     */
    private int SENSOR_RATE_FAST = 10000;

    public SimpleRate() {
    };

    /**
     * 10Hz
     * @return
     */
    public int get_SENSOR_RATE_SLOW() {
        return this.SENSOR_RATE_SLOW;
    }

    /**
     * 50Hz
     * @return
     */
    public int get_SENSOR_RATE_NORMAL() {
        return this.SENSOR_RATE_NORMAL;
    }

    /**
     * 80Hz
     *
     * @return
     */
    public int get_SENSOR_RATE_MIDDLE() {
        return this.SENSOR_RATE_MIDDLE;
    }

    /**
     * 100Hz
     *
     * @return
     */
    public int get_SENSOR_RATE_FAST() {
        return this.SENSOR_RATE_FAST;
    }
}
