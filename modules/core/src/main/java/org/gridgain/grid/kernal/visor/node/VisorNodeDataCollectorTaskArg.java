package org.gridgain.grid.kernal.visor.node;

import java.io.*;

/**
 * Data collector task arguments.
 */
public class VisorNodeDataCollectorTaskArg implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Whether task monitoring should be enabled. */
    private boolean taskMonitoringEnabled;

    /** Visor unique key to get last event order from node local storage. */
    private String evtOrderKey;

    /** Visor unique key to get lost events throttle counter from node local storage. */
    private String evtThrottleCntrKey;

    /** Cache sample size. */
    private int sample;

    /** If {@code true} then collect information about system caches. */
    private boolean systemCaches;

    public VisorNodeDataCollectorTaskArg() {
        // No-op.
    }

    /**
     * Create task arguments with given parameters.
     *
     * @param taskMonitoringEnabled If {@code true} then Visor should collect information about tasks.
     * @param evtOrderKey Event order key, unique for Visor instance.
     * @param evtThrottleCntrKey Event throttle counter key, unique for Visor instance.
     * @param sample How many entries use in sampling.
     * @param systemCaches If {@code true} then collect information about system caches.
     */
    public VisorNodeDataCollectorTaskArg(
        boolean taskMonitoringEnabled,
        String evtOrderKey,
        String evtThrottleCntrKey,
        int sample,
        boolean systemCaches
    ) {
        this.taskMonitoringEnabled = taskMonitoringEnabled;
        this.evtOrderKey = evtOrderKey;
        this.evtThrottleCntrKey = evtThrottleCntrKey;
        this.sample = sample;
        this.systemCaches = systemCaches;
    }

    /**
     * @return {@code true} if Visor should collect information about tasks.
     */
    public boolean taskMonitoringEnabled() {
        return taskMonitoringEnabled;
    }

    /**
     * @param taskMonitoringEnabled If {@code true} then Visor should collect information about tasks.
     */
    public void taskMonitoringEnabled(boolean taskMonitoringEnabled) {
        this.taskMonitoringEnabled = taskMonitoringEnabled;
    }

    /**
     * @return Key for store and read last event order number.
     */
    public String eventsOrderKey() {
        return evtOrderKey;
    }

    /**
     * @param evtOrderKey Key for store and read last event order number.
     */
    public void eventsOrderKey(String evtOrderKey) {
        this.evtOrderKey = evtOrderKey;
    }

    /**
     * @return Key for store and read events throttle counter.
     */
    public String eventsThrottleCounterKey() {
        return evtThrottleCntrKey;
    }

    /**
     * @param evtThrottleCntrKey Key for store and read events throttle counter.
     */
    public void eventsThrottleCounterKey(String evtThrottleCntrKey) {
        this.evtThrottleCntrKey = evtThrottleCntrKey;
    }

    /**
     * @return Number of items to evaluate cache size.
     */
    public int sample() {
        return sample;
    }

    /**
     * @param sample Number of items to evaluate cache size.
     */
    public void sample(int sample) {
        this.sample = sample;
    }

    /**
     * @return {@code true} if Visor should collect metrics for system caches.
     */
    public boolean systemCaches() {
        return systemCaches;
    }

    /**
     * @param systemCaches {@code true} if Visor should collect metrics for system caches.
     */
    public void systemCaches(boolean systemCaches) {
        this.systemCaches = systemCaches;
    }
}
