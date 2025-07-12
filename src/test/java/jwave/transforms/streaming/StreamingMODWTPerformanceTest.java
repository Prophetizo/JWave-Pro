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
 * Note: This class uses System.out.println for immediate developer feedback.
 * For production benchmarking, consider using:
 * - JMH (Java Microbenchmark Harness) for more accurate measurements
 * - SLF4J/Logback for configurable logging
 * - Dedicated performance monitoring tools
 * 
 * @author Prophetizo
 * @date 2025-07-12
 */
@Ignore("Performance tests - exclude from CI/CD")
public class StreamingMODWTPerformanceTest {
    
    // Constants for test data generation
    private static final int INITIAL_LOAD_OFFSET = -1;
    
    /**
     * Helper class to hold performance test results.
     */
    private static class PerformanceResult {
        final String strategyName;
        final long totalTimeNanos;
        final int numUpdates;
        final StreamingMODWT transform;
        
        PerformanceResult(String strategyName, long totalTimeNanos, int numUpdates, StreamingMODWT transform) {
            this.strategyName = strategyName;
            this.totalTimeNanos = totalTimeNanos;
            this.numUpdates = numUpdates;
            this.transform = transform;
        }
        
        long getTotalTimeMillis() {
            return totalTimeNanos / 1_000_000;
        }
        
        double getTimePerUpdateMillis() {
            return getTotalTimeMillis() / (double) numUpdates;
        }
    }
    
    /**
     * Run performance test for a specific strategy and configuration.
     */
    private PerformanceResult runPerformanceTest(
            StreamingTransformConfig.UpdateStrategy strategy,
            int bufferSize,
            int maxLevel,
            int initialLoad,
            double[][] updates,
            boolean accessCoefficientsInfrequently) {
        
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(bufferSize)
            .maxLevel(maxLevel)
            .updateStrategy(strategy)
            .build();
        
        StreamingMODWT transform = new StreamingMODWT(new Haar1(), config);
        
        // Initial load if specified
        if (initialLoad > 0) {
            transform.update(generateData(initialLoad, -1));
        }
        
        // Measure update performance
        long startTime = System.nanoTime();
        for (int i = 0; i < updates.length; i++) {
            transform.update(updates[i]);
            
            // For LAZY strategy with infrequent access pattern
            if (accessCoefficientsInfrequently && i % 100 == 0) {
                transform.getCurrentCoefficients();
            }
        }
        long totalTime = System.nanoTime() - startTime;
        
        return new PerformanceResult(strategy.name(), totalTime, updates.length, transform);
    }
    
    /**
     * Generate an array of updates for testing.
     * 
     * @param numUpdates Number of updates to generate
     * @param updateSize Size of each update
     * @return Array of update arrays
     */
    private double[][] generateUpdates(int numUpdates, int updateSize) {
        double[][] updates = new double[numUpdates][];
        for (int i = 0; i < numUpdates; i++) {
            updates[i] = generateData(updateSize, i);
        }
        return updates;
    }
    
    /**
     * Generate single-sample updates for real-time streaming scenario.
     * 
     * @param numSamples Number of single-sample updates to generate
     * @return Array of single-element arrays
     */
    private double[][] generateSingleSampleUpdates(int numSamples) {
        double[][] singleSamples = new double[numSamples][];
        for (int i = 0; i < numSamples; i++) {
            singleSamples[i] = new double[] { Math.sin(2 * Math.PI * i / 100) };
        }
        return singleSamples;
    }
    
    @Test
    public void compareUpdateStrategyPerformance() {
        int bufferSize = 4096;
        int maxLevel = 8;
        int numUpdates = 1000;
        int updateSize = 100;
        
        // Prepare updates
        double[][] updates = generateUpdates(numUpdates, updateSize);
        
        // Run tests for each strategy
        PerformanceResult fullResult = runPerformanceTest(
            StreamingTransformConfig.UpdateStrategy.FULL, 
            bufferSize, maxLevel, 0, updates, false);
        
        PerformanceResult incResult = runPerformanceTest(
            StreamingTransformConfig.UpdateStrategy.INCREMENTAL,
            bufferSize, maxLevel, 0, updates, false);
        
        PerformanceResult lazyResult = runPerformanceTest(
            StreamingTransformConfig.UpdateStrategy.LAZY,
            bufferSize, maxLevel, 0, updates, true);
        
        // Print results
        System.out.println("Performance comparison for " + numUpdates + " updates:");
        System.out.println("FULL strategy: " + fullResult.getTotalTimeMillis() + " ms");
        System.out.println("INCREMENTAL strategy: " + incResult.getTotalTimeMillis() + " ms");
        System.out.println("LAZY strategy: " + lazyResult.getTotalTimeMillis() + " ms");
        System.out.println("INCREMENTAL speedup over FULL: " + 
            String.format("%.2fx", (double)fullResult.totalTimeNanos / incResult.totalTimeNanos));
        System.out.println("LAZY speedup over FULL: " + 
            String.format("%.2fx", (double)fullResult.totalTimeNanos / lazyResult.totalTimeNanos));
        
        // Assertions
        assertTrue("INCREMENTAL should be faster than FULL", 
            incResult.totalTimeNanos < fullResult.totalTimeNanos);
        assertTrue("LAZY should be faster than FULL for infrequent access", 
            lazyResult.totalTimeNanos < fullResult.totalTimeNanos);
    }
    
    @Test
    public void compareIncrementalPerformanceWithSmallUpdates() {
        int bufferSize = 8192;
        int maxLevel = 6;
        int numUpdates = 500;
        int updateSize = 10; // Small updates
        
        // Prepare updates
        double[][] updates = generateUpdates(numUpdates, updateSize);
        
        // Test with initial buffer fill
        PerformanceResult fullResult = runPerformanceTest(
            StreamingTransformConfig.UpdateStrategy.FULL,
            bufferSize, maxLevel, bufferSize, updates, false);
        
        PerformanceResult incResult = runPerformanceTest(
            StreamingTransformConfig.UpdateStrategy.INCREMENTAL,
            bufferSize, maxLevel, bufferSize, updates, false);
        
        // Print results
        System.out.println("\nPerformance with small updates (buffer=" + bufferSize + ", update=" + updateSize + "):");
        System.out.println("FULL strategy: " + fullResult.getTotalTimeMillis() + " ms");
        System.out.println("INCREMENTAL strategy: " + incResult.getTotalTimeMillis() + " ms");
        System.out.println("Speedup: " + 
            String.format("%.2fx", (double)fullResult.totalTimeNanos / incResult.totalTimeNanos));
        
        assertTrue("INCREMENTAL should be faster than FULL with small updates", 
            incResult.totalTimeNanos < fullResult.totalTimeNanos);
    }
    
    @Test
    public void testRealtimeStreamingScenario() {
        int initialLoad = 512;
        int bufferSize = 1024;
        int maxLevel = 6;
        int numSingleSamples = 1000;
        
        System.out.println("\nReal-time streaming scenario:");
        System.out.println("Initial load: " + initialLoad + " samples");
        System.out.println("Then: " + numSingleSamples + " individual samples");
        System.out.println();
        
        // Prepare single-sample updates with custom generation
        double[][] singleSamples = generateSingleSampleUpdates(numSingleSamples);
        
        // Run tests
        PerformanceResult fullResult = runPerformanceTest(
            StreamingTransformConfig.UpdateStrategy.FULL,
            bufferSize, maxLevel, initialLoad, singleSamples, false);
        
        PerformanceResult incResult = runPerformanceTest(
            StreamingTransformConfig.UpdateStrategy.INCREMENTAL,
            bufferSize, maxLevel, initialLoad, singleSamples, false);
        
        // Calculate per-sample timing
        double fullPerSample = fullResult.getTimePerUpdateMillis();
        double incPerSample = incResult.getTimePerUpdateMillis();
        
        // Print results
        System.out.println("Processing " + numSingleSamples + " single-sample updates:");
        System.out.println("FULL strategy:");
        System.out.println("  Total time: " + fullResult.getTotalTimeMillis() + " ms");
        System.out.println("  Per sample: " + String.format("%.3f", fullPerSample) + " ms");
        System.out.println("  Sample rate: " + String.format("%.0f", 1000.0 / fullPerSample) + " samples/second");
        
        System.out.println("\nINCREMENTAL strategy:");
        System.out.println("  Total time: " + incResult.getTotalTimeMillis() + " ms");
        System.out.println("  Per sample: " + String.format("%.3f", incPerSample) + " ms");
        System.out.println("  Sample rate: " + String.format("%.0f", 1000.0 / incPerSample) + " samples/second");
        
        System.out.println("\nSpeedup: " + 
            String.format("%.2fx", (double)fullResult.totalTimeNanos / incResult.totalTimeNanos));
        System.out.println("Additional throughput: " + 
            String.format("%.0f", (1000.0 / incPerSample) - (1000.0 / fullPerSample)) + 
            " more samples/second");
        
        // Verify correctness
        double[][] fullCoeffs = fullResult.transform.getCurrentCoefficients();
        double[][] incCoeffs = incResult.transform.getCurrentCoefficients();
        
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
        assertTrue("INCREMENTAL should be faster for single-sample updates", 
            incResult.totalTimeNanos < fullResult.totalTimeNanos);
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
        
        // Measure second access (should be cached)
        long secondAccessStart = System.nanoTime();
        double[][] coeffs2 = transform.getCurrentCoefficients();
        long secondAccessTime = System.nanoTime() - secondAccessStart;
        
        System.out.println("LAZY computation overhead:");
        System.out.println("First access (with computation): " + (firstAccessTime / 1000) + " μs");
        System.out.println("Second access (cached): " + (secondAccessTime / 1000) + " μs");
        System.out.println("Overhead ratio: " + 
            String.format("%.1fx", (double)firstAccessTime / secondAccessTime));
        
        // Verify same results - deep comparison since getCachedCoefficients returns a copy
        assertNotNull("First coefficients should not be null", coeffs1);
        assertNotNull("Second coefficients should not be null", coeffs2);
        assertEquals("Should have same number of levels", coeffs1.length, coeffs2.length);
        
        for (int i = 0; i < coeffs1.length; i++) {
            assertArrayEquals("Level " + i + " coefficients should be identical", 
                            coeffs1[i], coeffs2[i], 1e-10);
        }
        
        // First access should be significantly slower
        assertTrue("First access should trigger computation", 
                  firstAccessTime > secondAccessTime * 10);
    }
    
    private double[] generateData(int size, int offset) {
        double[] data = new double[size];
        for (int i = 0; i < size; i++) {
            data[i] = Math.sin(2 * Math.PI * (offset * size + i) / size);
        }
        return data;
    }
}