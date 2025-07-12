/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import jwave.transforms.wavelets.haar.Haar1;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.*;

/**
 * Performance tests for StreamingMODWT update strategies.
 * 
 * These tests are ignored by default to avoid slowing down CI/CD pipelines.
 * To run performance tests locally, remove @Ignore or use:
 * mvn test -Dtest=StreamingMODWTPerformanceTest -DenablePerformanceTests=true
 * 
 * @author Prophetizo
 * @date 2025-07-12
 */
@Ignore("Performance tests - exclude from CI/CD")
public class StreamingMODWTPerformanceTest {
    
    @Test
    public void compareUpdateStrategyPerformance() {
        int bufferSize = 4096;
        int maxLevel = 8;
        int numUpdates = 1000;
        int updateSize = 100;
        
        Haar1 wavelet = new Haar1();
        
        // Test FULL strategy
        StreamingTransformConfig fullConfig = StreamingTransformConfig.builder()
            .bufferSize(bufferSize)
            .maxLevel(maxLevel)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingMODWT fullTransform = new StreamingMODWT(wavelet, fullConfig);
        
        long fullStart = System.nanoTime();
        for (int i = 0; i < numUpdates; i++) {
            double[] data = generateData(updateSize, i);
            fullTransform.update(data);
        }
        long fullTime = System.nanoTime() - fullStart;
        
        // Test LAZY strategy with infrequent access
        StreamingTransformConfig lazyConfig = StreamingTransformConfig.builder()
            .bufferSize(bufferSize)
            .maxLevel(maxLevel)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.LAZY)
            .build();
        
        StreamingMODWT lazyTransform = new StreamingMODWT(wavelet, lazyConfig);
        
        long lazyStart = System.nanoTime();
        for (int i = 0; i < numUpdates; i++) {
            double[] data = generateData(updateSize, i);
            lazyTransform.update(data);
            
            // Only access coefficients every 100 updates
            if (i % 100 == 0) {
                lazyTransform.getCurrentCoefficients();
            }
        }
        long lazyTime = System.nanoTime() - lazyStart;
        
        // Test INCREMENTAL strategy
        StreamingTransformConfig incConfig = StreamingTransformConfig.builder()
            .bufferSize(bufferSize)
            .maxLevel(maxLevel)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingMODWT incTransform = new StreamingMODWT(wavelet, incConfig);
        
        long incStart = System.nanoTime();
        for (int i = 0; i < numUpdates; i++) {
            double[] data = generateData(updateSize, i);
            incTransform.update(data);
        }
        long incTime = System.nanoTime() - incStart;
        
        // Print results
        System.out.println("Performance comparison for " + numUpdates + " updates:");
        System.out.println("FULL strategy: " + (fullTime / 1_000_000) + " ms");
        System.out.println("INCREMENTAL strategy: " + (incTime / 1_000_000) + " ms");
        System.out.println("LAZY strategy: " + (lazyTime / 1_000_000) + " ms");
        System.out.println("INCREMENTAL speedup over FULL: " + String.format("%.2fx", (double)fullTime / incTime));
        System.out.println("LAZY speedup over FULL: " + String.format("%.2fx", (double)fullTime / lazyTime));
        
        // INCREMENTAL should be faster than FULL
        assertTrue("INCREMENTAL should be faster than FULL", incTime < fullTime);
        
        // LAZY should be significantly faster when coefficients are accessed infrequently
        assertTrue("LAZY should be faster than FULL for infrequent access", 
                  lazyTime < fullTime);
    }
    
    @Test
    public void compareIncrementalPerformanceWithSmallUpdates() {
        // Test with larger buffer and smaller updates to show INCREMENTAL benefits
        int bufferSize = 8192;
        int maxLevel = 6;
        int numUpdates = 500;
        int updateSize = 10; // Small updates
        
        Haar1 wavelet = new Haar1();
        
        // Test FULL strategy
        StreamingTransformConfig fullConfig = StreamingTransformConfig.builder()
            .bufferSize(bufferSize)
            .maxLevel(maxLevel)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingMODWT fullTransform = new StreamingMODWT(wavelet, fullConfig);
        
        // Fill buffer initially
        fullTransform.update(generateData(bufferSize, -1));
        
        long fullStart = System.nanoTime();
        for (int i = 0; i < numUpdates; i++) {
            double[] data = generateData(updateSize, i);
            fullTransform.update(data);
        }
        long fullTime = System.nanoTime() - fullStart;
        
        // Test INCREMENTAL strategy
        StreamingTransformConfig incConfig = StreamingTransformConfig.builder()
            .bufferSize(bufferSize)
            .maxLevel(maxLevel)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingMODWT incTransform = new StreamingMODWT(wavelet, incConfig);
        
        // Fill buffer initially
        incTransform.update(generateData(bufferSize, -1));
        
        long incStart = System.nanoTime();
        for (int i = 0; i < numUpdates; i++) {
            double[] data = generateData(updateSize, i);
            incTransform.update(data);
        }
        long incTime = System.nanoTime() - incStart;
        
        // Print results
        System.out.println("\nPerformance with small updates (buffer=" + bufferSize + ", update=" + updateSize + "):");
        System.out.println("FULL strategy: " + (fullTime / 1_000_000) + " ms");
        System.out.println("INCREMENTAL strategy: " + (incTime / 1_000_000) + " ms");
        System.out.println("Speedup: " + String.format("%.2fx", (double)fullTime / incTime));
        
        // With small updates, INCREMENTAL should show better improvement
        assertTrue("INCREMENTAL should be faster than FULL with small updates", incTime < fullTime);
    }
    
    @Test
    public void testRealtimeStreamingScenario() {
        // Realistic scenario: initial buffer load, then single samples
        int initialLoad = 512;
        int bufferSize = 1024;
        int maxLevel = 6;
        int numSingleSamples = 1000; // 1000 individual samples
        
        Haar1 wavelet = new Haar1();
        
        System.out.println("\nReal-time streaming scenario:");
        System.out.println("Initial load: " + initialLoad + " samples");
        System.out.println("Then: " + numSingleSamples + " individual samples");
        System.out.println();
        
        // Test FULL strategy
        StreamingTransformConfig fullConfig = StreamingTransformConfig.builder()
            .bufferSize(bufferSize)
            .maxLevel(maxLevel)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingMODWT fullTransform = new StreamingMODWT(wavelet, fullConfig);
        
        // Initial load
        fullTransform.update(generateData(initialLoad, 0));
        
        // Time single sample updates
        long fullStart = System.nanoTime();
        for (int i = 0; i < numSingleSamples; i++) {
            fullTransform.update(new double[] { Math.sin(2 * Math.PI * i / 100) });
        }
        long fullTime = System.nanoTime() - fullStart;
        
        // Test INCREMENTAL strategy
        StreamingTransformConfig incConfig = StreamingTransformConfig.builder()
            .bufferSize(bufferSize)
            .maxLevel(maxLevel)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingMODWT incTransform = new StreamingMODWT(wavelet, incConfig);
        
        // Initial load
        incTransform.update(generateData(initialLoad, 0));
        
        // Time single sample updates
        long incStart = System.nanoTime();
        for (int i = 0; i < numSingleSamples; i++) {
            incTransform.update(new double[] { Math.sin(2 * Math.PI * i / 100) });
        }
        long incTime = System.nanoTime() - incStart;
        
        // Calculate per-sample timing
        double fullPerSample = (fullTime / 1_000_000.0) / numSingleSamples;
        double incPerSample = (incTime / 1_000_000.0) / numSingleSamples;
        
        // Print results
        System.out.println("Processing " + numSingleSamples + " single-sample updates:");
        System.out.println("FULL strategy:");
        System.out.println("  Total time: " + (fullTime / 1_000_000) + " ms");
        System.out.println("  Per sample: " + String.format("%.3f", fullPerSample) + " ms");
        System.out.println("  Sample rate: " + String.format("%.0f", 1000.0 / fullPerSample) + " samples/second");
        
        System.out.println("\nINCREMENTAL strategy:");
        System.out.println("  Total time: " + (incTime / 1_000_000) + " ms");
        System.out.println("  Per sample: " + String.format("%.3f", incPerSample) + " ms");
        System.out.println("  Sample rate: " + String.format("%.0f", 1000.0 / incPerSample) + " samples/second");
        
        System.out.println("\nSpeedup: " + String.format("%.2fx", (double)fullTime / incTime));
        System.out.println("Additional throughput: " + 
            String.format("%.0f", (1000.0 / incPerSample) - (1000.0 / fullPerSample)) + 
            " more samples/second");
        
        // Verify correctness - coefficients should match
        double[][] fullCoeffs = fullTransform.getCurrentCoefficients();
        double[][] incCoeffs = incTransform.getCurrentCoefficients();
        
        double maxDiff = 0.0;
        for (int i = 0; i < fullCoeffs.length; i++) {
            for (int j = 0; j < Math.min(fullCoeffs[i].length, incCoeffs[i].length); j++) {
                double diff = Math.abs(fullCoeffs[i][j] - incCoeffs[i][j]);
                maxDiff = Math.max(maxDiff, diff);
            }
        }
        
        System.out.println("\nCorrectness check - max coefficient difference: " + 
            String.format("%.2e", maxDiff));
        assertTrue("Coefficients should match", maxDiff < 1e-10);
        assertTrue("INCREMENTAL should be faster for single-sample updates", incTime < fullTime);
    }
    
    @Test
    public void measureLazyComputationOverhead() {
        int bufferSize = 2048;
        int maxLevel = 6;
        
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(bufferSize)
            .maxLevel(maxLevel)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.LAZY)
            .build();
        
        StreamingMODWT transform = new StreamingMODWT(new Haar1(), config);
        
        // Fill buffer
        double[] data = generateData(bufferSize, 0);
        transform.update(data);
        
        // Add more data (marks coefficients as dirty)
        transform.update(generateData(100, 1));
        
        // Measure first access (triggers computation)
        long firstAccessStart = System.nanoTime();
        double[][] coeffs1 = transform.getCurrentCoefficients();
        long firstAccessTime = System.nanoTime() - firstAccessStart;
        
        // Measure second access (uses cache)
        long secondAccessStart = System.nanoTime();
        double[][] coeffs2 = transform.getCurrentCoefficients();
        long secondAccessTime = System.nanoTime() - secondAccessStart;
        
        System.out.println("LAZY computation overhead:");
        System.out.println("First access (with computation): " + (firstAccessTime / 1_000) + " μs");
        System.out.println("Second access (cached): " + (secondAccessTime / 1_000) + " μs");
        System.out.println("Overhead ratio: " + String.format("%.1fx", 
                          (double)firstAccessTime / secondAccessTime));
        
        // First access should be significantly slower due to computation
        assertTrue("First access should be slower than cached access", 
                  firstAccessTime > secondAccessTime * 5);
    }
    
    private double[] generateData(int size, int seed) {
        double[] data = new double[size];
        for (int i = 0; i < size; i++) {
            data[i] = Math.sin(2 * Math.PI * (i + seed) / 50) + 
                     0.5 * Math.cos(2 * Math.PI * (i + seed) / 13);
        }
        return data;
    }
}