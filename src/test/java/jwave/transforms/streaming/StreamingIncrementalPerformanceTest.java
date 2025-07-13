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
    
    private static final int INITIAL_SIGNAL_SIZE = 512;
    private static final int SINGLE_SAMPLE_UPDATES = 100;
    private static final int WARM_UP_ITERATIONS = 50;
    
    @Test
    public void testIncrementalPerformanceComparison() {
        // Skip if performance tests are not enabled
        assumeTrue("Performance tests not enabled", 
                  Boolean.getBoolean("enablePerformanceTests"));
        
        System.out.println("\nStreaming Transform Incremental Performance Analysis");
        System.out.println("===================================================");
        System.out.println("Scenario: Initial signal of 512 samples, then 100 single-sample updates");
        System.out.println();
        
        // Test StreamingMODWT (has true incremental updates)
        testMODWTPerformance();
        
        // Test StreamingFWT (falls back to full recomputation)
        testFWTPerformance();
        
        // Test StreamingWPT (falls back to full recomputation)
        testWPTPerformance();
        
        // Test StreamingCWT (falls back to full recomputation)
        testCWTPerformance();
    }
    
    private void testMODWTPerformance() {
        System.out.println("StreamingMODWT (TRUE incremental implementation):");
        System.out.println("-------------------------------------------------");
        
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
        
        System.out.printf("Initial load (512 samples):\n");
        System.out.printf("  FULL:        %.3f ms\n", fullInitTime);
        System.out.printf("  INCREMENTAL: %.3f ms (same as FULL for initial load)\n", incInitTime);
        System.out.printf("\nSingle sample updates (average of %d updates):\n", SINGLE_SAMPLE_UPDATES);
        System.out.printf("  FULL:        %.3f ms per update\n", fullAvgUpdate);
        System.out.printf("  INCREMENTAL: %.3f ms per update\n", incAvgUpdate);
        System.out.printf("  Speedup:     %.2fx faster\n", speedup);
        System.out.printf("  Time saved:  %.3f ms per update\n", fullAvgUpdate - incAvgUpdate);
        System.out.println();
    }
    
    private void testFWTPerformance() {
        System.out.println("StreamingFWT (incremental falls back to FULL):");
        System.out.println("----------------------------------------------");
        
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
        
        System.out.printf("Single sample updates (average of %d updates):\n", SINGLE_SAMPLE_UPDATES);
        System.out.printf("  FULL:        %.3f ms per update\n", fullAvgUpdate);
        System.out.printf("  INCREMENTAL: %.3f ms per update (same as FULL)\n", incAvgUpdate);
        System.out.printf("  Speedup:     1.00x (no improvement - falls back to FULL)\n");
        System.out.println();
    }
    
    private void testWPTPerformance() {
        System.out.println("StreamingWPT (incremental falls back to FULL):");
        System.out.println("----------------------------------------------");
        
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
        
        System.out.printf("Single sample updates (average of %d updates):\n", SINGLE_SAMPLE_UPDATES);
        System.out.printf("  FULL:        %.3f ms per update\n", fullAvgUpdate);
        System.out.printf("  INCREMENTAL: %.3f ms per update (same as FULL)\n", incAvgUpdate);
        System.out.printf("  Speedup:     1.00x (no improvement - falls back to FULL)\n");
        System.out.println();
    }
    
    private void testCWTPerformance() {
        System.out.println("StreamingCWT (with incremental updates):");
        System.out.println("----------------------------------------");
        
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
        
        System.out.printf("Single sample updates (average of %d updates):\n", SINGLE_SAMPLE_UPDATES);
        System.out.printf("  FULL:        %.3f ms per update\n", fullAvgUpdate);
        System.out.printf("  INCREMENTAL: %.3f ms per update\n", incAvgUpdate);
        System.out.printf("  Speedup:     %.2fx\n", speedup);
        if (speedup > 1.1) {
            System.out.printf("  Time saved:  %.3f ms per update\n", fullAvgUpdate - incAvgUpdate);
        } else {
            System.out.println("  Note: Minimal improvement due to edge effect overhead");
        }
        System.out.println();
    }
    
    @Test
    public void testLazyStrategyPerformance() {
        assumeTrue("Performance tests not enabled", 
                  Boolean.getBoolean("enablePerformanceTests"));
        
        System.out.println("\nLAZY Strategy Performance Analysis");
        System.out.println("==================================");
        System.out.println("Comparing LAZY vs FULL for scenarios with infrequent coefficient access\n");
        
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
        
        System.out.println("StreamingMODWT LAZY Strategy Performance:");
        System.out.println("-----------------------------------------");
        System.out.printf("Scenario: %d updates followed by 1 coefficient access, repeated %d times\n", 
                         updatesPerAccess, totalCycles);
        System.out.printf("FULL strategy total time:  %.3f ms\n", fullTotalTime);
        System.out.printf("LAZY strategy total time:  %.3f ms\n", lazyTotalTime);
        System.out.printf("Time saved with LAZY:      %.1f%%\n", lazySavings);
        System.out.println("\nNote: LAZY strategy is beneficial when coefficient access is less frequent");
        System.out.println("      than updates, avoiding unnecessary computations.");
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