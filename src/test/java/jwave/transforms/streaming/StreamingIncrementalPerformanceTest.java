/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.transforms.wavelets.continuous.MorletWavelet;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Performance comparison test for incremental updates across streaming transforms.
 * 
 * This test measures the actual performance difference between FULL and INCREMENTAL
 * update strategies for the scenario of:
 * - Initial signal of 512 samples
 * - Single sample updates every second
 * 
 * @author Prophetizo
 * @date 2025-07-13
 */
public class StreamingIncrementalPerformanceTest {
    
    private static final Logger LOGGER = Logger.getLogger(StreamingIncrementalPerformanceTest.class.getName());
    
    private static final int INITIAL_SIGNAL_SIZE = 512;
    private static final int SINGLE_SAMPLE_UPDATES = 100;
    private static final int WARM_UP_ITERATIONS = 50;
    
    // Performance thresholds for assertions
    private static final double MIN_MODWT_SPEEDUP = 1.3;  // Expect at least 30% improvement
    private static final double MAX_OVERHEAD_RATIO = 1.1;  // Allow up to 10% overhead for "no improvement" cases
    
    @Test
    public void testIncrementalPerformanceComparison() {
        // Skip if performance tests are not enabled
        assumeTrue("Performance tests not enabled", 
                  Boolean.getBoolean("enablePerformanceTests"));
        
        LOGGER.log(Level.INFO, "Running incremental performance analysis with {0} samples and {1} updates", 
                   new Object[]{INITIAL_SIGNAL_SIZE, SINGLE_SAMPLE_UPDATES});
        
        // Test StreamingMODWT (has true incremental updates)
        double modwtSpeedup = testMODWTPerformance();
        assertTrue("StreamingMODWT should show significant speedup with incremental updates", 
                   modwtSpeedup >= MIN_MODWT_SPEEDUP);
        
        // Test StreamingFWT (falls back to full recomputation)
        double fwtSpeedup = testFWTPerformance();
        assertTrue("StreamingFWT incremental should not have significant overhead", 
                   fwtSpeedup >= 1.0 / MAX_OVERHEAD_RATIO && fwtSpeedup <= MAX_OVERHEAD_RATIO);
        
        // Test StreamingWPT (falls back to full recomputation)
        double wptSpeedup = testWPTPerformance();
        assertTrue("StreamingWPT incremental should not have significant overhead", 
                   wptSpeedup >= 1.0 / MAX_OVERHEAD_RATIO && wptSpeedup <= MAX_OVERHEAD_RATIO);
        
        // Test StreamingCWT (with incremental updates)
        double cwtSpeedup = testCWTPerformance();
        // CWT incremental updates have minimal benefit due to edge effects
        assertTrue("StreamingCWT incremental should not have significant overhead", 
                   cwtSpeedup >= 0.9);  // Allow up to 10% slower due to overhead
    }
    
    private double testMODWTPerformance() {
        LOGGER.log(Level.FINE, "Testing StreamingMODWT incremental performance");
        
        // Test with FULL strategy
        StreamingTransformConfig fullConfig = StreamingTransformConfig.builder()
            .bufferSize(INITIAL_SIGNAL_SIZE)
            .maxLevel(4)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingMODWT fullMODWT = new StreamingMODWT(new Haar1(), fullConfig);
        
        // Test with INCREMENTAL strategy
        StreamingTransformConfig incConfig = StreamingTransformConfig.builder()
            .bufferSize(INITIAL_SIGNAL_SIZE)
            .maxLevel(4)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingMODWT incMODWT = new StreamingMODWT(new Haar1(), incConfig);
        
        // Generate initial signal
        double[] initialSignal = generateSignal(INITIAL_SIGNAL_SIZE);
        
        // Warm up
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            fullMODWT.update(new double[]{Math.random()});
            incMODWT.update(new double[]{Math.random()});
        }
        
        // Initial load
        long fullInitStart = System.nanoTime();
        fullMODWT.update(initialSignal);
        long fullInitEnd = System.nanoTime();
        double fullInitTime = (fullInitEnd - fullInitStart) / 1e6;
        
        long incInitStart = System.nanoTime();
        incMODWT.update(initialSignal);
        long incInitEnd = System.nanoTime();
        double incInitTime = (incInitEnd - incInitStart) / 1e6;
        
        // Single sample updates
        long fullUpdateStart = System.nanoTime();
        for (int i = 0; i < SINGLE_SAMPLE_UPDATES; i++) {
            fullMODWT.update(new double[]{Math.random()});
        }
        long fullUpdateEnd = System.nanoTime();
        double fullAvgUpdate = (fullUpdateEnd - fullUpdateStart) / (1e6 * SINGLE_SAMPLE_UPDATES);
        
        long incUpdateStart = System.nanoTime();
        for (int i = 0; i < SINGLE_SAMPLE_UPDATES; i++) {
            incMODWT.update(new double[]{Math.random()});
        }
        long incUpdateEnd = System.nanoTime();
        double incAvgUpdate = (incUpdateEnd - incUpdateStart) / (1e6 * SINGLE_SAMPLE_UPDATES);
        
        // Calculate speedup
        double speedup = fullAvgUpdate / incAvgUpdate;
        
        LOGGER.log(Level.FINE, "MODWT Initial load - FULL: {0}ms, INCREMENTAL: {1}ms", 
                   new Object[]{String.format("%.3f", fullInitTime), String.format("%.3f", incInitTime)});
        LOGGER.log(Level.FINE, "MODWT Updates - FULL: {0}ms/update, INCREMENTAL: {1}ms/update, Speedup: {2}x", 
                   new Object[]{String.format("%.3f", fullAvgUpdate), String.format("%.3f", incAvgUpdate), 
                               String.format("%.2f", speedup)});
        
        return speedup;
    }
    
    private double testFWTPerformance() {
        LOGGER.log(Level.FINE, "Testing StreamingFWT incremental performance");
        
        // Test with FULL strategy
        StreamingTransformConfig fullConfig = StreamingTransformConfig.builder()
            .bufferSize(INITIAL_SIGNAL_SIZE)
            .maxLevel(4)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingFWT fullFWT = new StreamingFWT(new Daubechies4(), fullConfig);
        
        // Test with INCREMENTAL strategy
        StreamingTransformConfig incConfig = StreamingTransformConfig.builder()
            .bufferSize(INITIAL_SIGNAL_SIZE)
            .maxLevel(4)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingFWT incFWT = new StreamingFWT(new Daubechies4(), incConfig);
        
        // Generate initial signal
        double[] initialSignal = generateSignal(INITIAL_SIGNAL_SIZE);
        
        // Warm up
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            fullFWT.update(new double[]{Math.random()});
            incFWT.update(new double[]{Math.random()});
        }
        
        // Initial load
        fullFWT.update(initialSignal);
        incFWT.update(initialSignal);
        
        // Single sample updates
        long fullUpdateStart = System.nanoTime();
        for (int i = 0; i < SINGLE_SAMPLE_UPDATES; i++) {
            fullFWT.update(new double[]{Math.random()});
        }
        long fullUpdateEnd = System.nanoTime();
        double fullAvgUpdate = (fullUpdateEnd - fullUpdateStart) / (1e6 * SINGLE_SAMPLE_UPDATES);
        
        long incUpdateStart = System.nanoTime();
        for (int i = 0; i < SINGLE_SAMPLE_UPDATES; i++) {
            incFWT.update(new double[]{Math.random()});
        }
        long incUpdateEnd = System.nanoTime();
        double incAvgUpdate = (incUpdateEnd - incUpdateStart) / (1e6 * SINGLE_SAMPLE_UPDATES);
        
        double speedup = fullAvgUpdate / incAvgUpdate;
        
        LOGGER.log(Level.FINE, "FWT Updates - FULL: {0}ms/update, INCREMENTAL: {1}ms/update, Speedup: {2}x", 
                   new Object[]{String.format("%.3f", fullAvgUpdate), String.format("%.3f", incAvgUpdate), 
                               String.format("%.2f", speedup)});
        
        return speedup;
    }
    
    private double testWPTPerformance() {
        LOGGER.log(Level.FINE, "Testing StreamingWPT incremental performance");
        
        // Test with FULL strategy
        StreamingTransformConfig fullConfig = StreamingTransformConfig.builder()
            .bufferSize(INITIAL_SIGNAL_SIZE)
            .maxLevel(4)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingWPT fullWPT = new StreamingWPT(new Haar1(), fullConfig);
        
        // Test with INCREMENTAL strategy
        StreamingTransformConfig incConfig = StreamingTransformConfig.builder()
            .bufferSize(INITIAL_SIGNAL_SIZE)
            .maxLevel(4)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingWPT incWPT = new StreamingWPT(new Haar1(), incConfig);
        
        // Generate initial signal
        double[] initialSignal = generateSignal(INITIAL_SIGNAL_SIZE);
        
        // Warm up
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            fullWPT.update(new double[]{Math.random()});
            incWPT.update(new double[]{Math.random()});
        }
        
        // Initial load
        fullWPT.update(initialSignal);
        incWPT.update(initialSignal);
        
        // Single sample updates
        long fullUpdateStart = System.nanoTime();
        for (int i = 0; i < SINGLE_SAMPLE_UPDATES; i++) {
            fullWPT.update(new double[]{Math.random()});
        }
        long fullUpdateEnd = System.nanoTime();
        double fullAvgUpdate = (fullUpdateEnd - fullUpdateStart) / (1e6 * SINGLE_SAMPLE_UPDATES);
        
        long incUpdateStart = System.nanoTime();
        for (int i = 0; i < SINGLE_SAMPLE_UPDATES; i++) {
            incWPT.update(new double[]{Math.random()});
        }
        long incUpdateEnd = System.nanoTime();
        double incAvgUpdate = (incUpdateEnd - incUpdateStart) / (1e6 * SINGLE_SAMPLE_UPDATES);
        
        double speedup = fullAvgUpdate / incAvgUpdate;
        
        LOGGER.log(Level.FINE, "FWT Updates - FULL: {0}ms/update, INCREMENTAL: {1}ms/update, Speedup: {2}x", 
                   new Object[]{String.format("%.3f", fullAvgUpdate), String.format("%.3f", incAvgUpdate), 
                               String.format("%.2f", speedup)});
        
        return speedup;
    }
    
    private double testCWTPerformance() {
        LOGGER.log(Level.FINE, "Testing StreamingCWT incremental performance");
        
        // Test with FULL strategy
        StreamingTransformConfig fullConfig = StreamingTransformConfig.builder()
            .bufferSize(INITIAL_SIGNAL_SIZE)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingCWT fullCWT = new StreamingCWT(new MorletWavelet(), fullConfig, 1.0, 32.0, 16, true);
        
        // Test with INCREMENTAL strategy
        StreamingTransformConfig incConfig = StreamingTransformConfig.builder()
            .bufferSize(INITIAL_SIGNAL_SIZE)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingCWT incCWT = new StreamingCWT(new MorletWavelet(), incConfig, 1.0, 32.0, 16, true);
        
        // Generate initial signal
        double[] initialSignal = generateSignal(INITIAL_SIGNAL_SIZE);
        
        // Warm up
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            fullCWT.update(new double[]{Math.random()});
            incCWT.update(new double[]{Math.random()});
        }
        
        // Initial load
        fullCWT.update(initialSignal);
        incCWT.update(initialSignal);
        
        // Single sample updates
        long fullUpdateStart = System.nanoTime();
        for (int i = 0; i < SINGLE_SAMPLE_UPDATES; i++) {
            fullCWT.update(new double[]{Math.random()});
        }
        long fullUpdateEnd = System.nanoTime();
        double fullAvgUpdate = (fullUpdateEnd - fullUpdateStart) / (1e6 * SINGLE_SAMPLE_UPDATES);
        
        long incUpdateStart = System.nanoTime();
        for (int i = 0; i < SINGLE_SAMPLE_UPDATES; i++) {
            incCWT.update(new double[]{Math.random()});
        }
        long incUpdateEnd = System.nanoTime();
        double incAvgUpdate = (incUpdateEnd - incUpdateStart) / (1e6 * SINGLE_SAMPLE_UPDATES);
        
        // Calculate speedup
        double speedup = fullAvgUpdate / incAvgUpdate;
        
        LOGGER.log(Level.FINE, "CWT Updates - FULL: {0}ms/update, INCREMENTAL: {1}ms/update, Speedup: {2}x", 
                   new Object[]{String.format("%.3f", fullAvgUpdate), String.format("%.3f", incAvgUpdate), 
                               String.format("%.2f", speedup)});
        
        return speedup;
    }
    
    @Test
    public void testLazyStrategyPerformance() {
        assumeTrue("Performance tests not enabled", 
                  Boolean.getBoolean("enablePerformanceTests"));
        
        LOGGER.log(Level.INFO, "Testing LAZY strategy performance with infrequent coefficient access");
        
        // Test scenario: 10 updates followed by 1 coefficient access
        int updatesPerAccess = 10;
        int totalCycles = 10;
        
        // Test with StreamingMODWT
        StreamingTransformConfig fullConfig = StreamingTransformConfig.builder()
            .bufferSize(INITIAL_SIGNAL_SIZE)
            .maxLevel(4)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingTransformConfig lazyConfig = StreamingTransformConfig.builder()
            .bufferSize(INITIAL_SIGNAL_SIZE)
            .maxLevel(4)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.LAZY)
            .build();
        
        StreamingMODWT fullMODWT = new StreamingMODWT(new Haar1(), fullConfig);
        StreamingMODWT lazyMODWT = new StreamingMODWT(new Haar1(), lazyConfig);
        
        // Initial load
        double[] initialSignal = generateSignal(INITIAL_SIGNAL_SIZE);
        fullMODWT.update(initialSignal);
        lazyMODWT.update(initialSignal);
        
        // Test FULL strategy
        long fullStart = System.nanoTime();
        for (int cycle = 0; cycle < totalCycles; cycle++) {
            // Multiple updates
            for (int i = 0; i < updatesPerAccess; i++) {
                fullMODWT.update(new double[]{Math.random()});
            }
            // Access coefficients
            fullMODWT.getCurrentCoefficients();
        }
        long fullEnd = System.nanoTime();
        double fullTotalTime = (fullEnd - fullStart) / 1e6;
        
        // Test LAZY strategy
        long lazyStart = System.nanoTime();
        for (int cycle = 0; cycle < totalCycles; cycle++) {
            // Multiple updates (these are very fast with LAZY)
            for (int i = 0; i < updatesPerAccess; i++) {
                lazyMODWT.update(new double[]{Math.random()});
            }
            // Access coefficients (triggers computation)
            lazyMODWT.getCurrentCoefficients();
        }
        long lazyEnd = System.nanoTime();
        double lazyTotalTime = (lazyEnd - lazyStart) / 1e6;
        
        double lazySavings = ((fullTotalTime - lazyTotalTime) / fullTotalTime) * 100;
        
        LOGGER.log(Level.FINE, "LAZY Strategy - Updates per access: {0}, Total cycles: {1}", 
                   new Object[]{updatesPerAccess, totalCycles});
        LOGGER.log(Level.FINE, "LAZY Strategy Results - FULL: {0}ms, LAZY: {1}ms, Savings: {2}%", 
                   new Object[]{String.format("%.3f", fullTotalTime), 
                               String.format("%.3f", lazyTotalTime),
                               String.format("%.1f", lazySavings)});
        
        // Assert that LAZY strategy provides measurable benefit
        assertTrue("LAZY strategy should provide time savings for infrequent access patterns", 
                   lazySavings > 10.0);  // Expect at least 10% savings
    }
    
    private double[] generateSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 64) + 
                       0.5 * Math.cos(4 * Math.PI * i / 64) + 
                       0.1 * Math.random();
        }
        return signal;
    }
}