/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import jwave.transforms.wavelets.Wavelet;
import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.transforms.wavelets.symlets.Symlet8;
import jwave.transforms.wavelets.coiflet.Coiflet2;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive integration tests for streaming transform framework.
 * 
 * These tests verify the complete functionality of the streaming transform
 * system including edge cases, error handling, concurrency, and real-world
 * usage patterns.
 * 
 * @author Prophetizo
 * @date 2025-07-12
 */
public class StreamingTransformIntegrationTest {
    
    private Wavelet[] testWavelets;
    private Random random;
    
    @Before
    public void setUp() {
        testWavelets = new Wavelet[] {
            new Haar1(),
            new Daubechies4(),
            new Symlet8(),
            new Coiflet2()
        };
        random = new Random(42); // Fixed seed for reproducibility
    }
    
    /**
     * Test that streaming transforms handle real-world signal processing scenarios.
     */
    @Test
    public void testRealWorldSignalProcessing() {
        // Simulate ECG-like signal with noise
        int sampleRate = 360; // Hz
        int duration = 10; // seconds
        int totalSamples = sampleRate * duration;
        
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(1024)
            .maxLevel(5)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingMODWT transform = new StreamingMODWT(new Daubechies4(), config);
        
        // Add listener to track processing
        SignalAnalysisListener listener = new SignalAnalysisListener();
        transform.addListener(listener);
        
        // Process signal in realistic chunks (like from ADC)
        int chunkSize = 64; // Typical ADC buffer size
        for (int i = 0; i < totalSamples; i += chunkSize) {
            double[] chunk = generateECGChunk(i, Math.min(chunkSize, totalSamples - i), sampleRate);
            transform.update(chunk);
        }
        
        // Verify processing
        assertTrue("Should have processed multiple updates", listener.updateCount > 0);
        assertTrue("Buffer should be full", listener.bufferFull);
        assertFalse("Should not have errors", listener.hadError);
        
        // Analyze final coefficients
        double[][] coeffs = transform.getCurrentCoefficients();
        
        // Level 1 (high freq) should have noise
        double level1Energy = computeEnergy(coeffs[0]);
        
        // Level 3-4 should have main ECG components
        double level3Energy = computeEnergy(coeffs[2]);
        double level4Energy = computeEnergy(coeffs[3]);
        
        // Approximation should have baseline
        double approxEnergy = computeEnergy(coeffs[5]);
        
        assertTrue("ECG energy should be concentrated in mid-levels",
                  level3Energy + level4Energy > level1Energy);
    }
    
    /**
     * Test streaming transforms with various buffer sizes and signal lengths.
     */
    @Test
    public void testVariousBufferAndSignalSizes() {
        int[] bufferSizes = {64, 128, 256, 512, 1024, 2048};
        int[] signalLengths = {50, 100, 500, 1000, 5000};
        
        for (int bufferSize : bufferSizes) {
            for (int signalLength : signalLengths) {
                StreamingTransformConfig config = StreamingTransformConfig.builder()
                    .bufferSize(bufferSize)
                    .maxLevel(3)
                    .build();
                
                StreamingMODWT transform = new StreamingMODWT(new Haar1(), config);
                
                // Generate and process signal
                double[] signal = generateRandomSignal(signalLength);
                transform.update(signal);
                
                // Verify transform produces valid coefficients
                double[][] coeffs = transform.getCurrentCoefficients();
                assertNotNull("Coefficients should not be null for buffer=" + 
                             bufferSize + ", signal=" + signalLength, coeffs);
                assertEquals("Should have correct number of levels", 4, coeffs.length);
                
                // Verify no NaN or Inf values
                for (int level = 0; level < coeffs.length; level++) {
                    for (double c : coeffs[level]) {
                        assertFalse("No NaN values", Double.isNaN(c));
                        assertFalse("No Inf values", Double.isInfinite(c));
                    }
                }
            }
        }
    }
    
    /**
     * Test listener notification ordering and consistency.
     */
    @Test
    public void testListenerNotifications() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(256)
            .maxLevel(3)
            .build();
        
        StreamingMODWT transform = new StreamingMODWT(new Haar1(), config);
        
        // Add multiple listeners
        List<OrderedListener> listeners = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            OrderedListener listener = new OrderedListener(i);
            listeners.add(listener);
            transform.addListener(listener);
        }
        
        // Process data
        transform.update(generateRandomSignal(100));
        transform.update(generateRandomSignal(200)); // This should trigger buffer full
        transform.reset();
        
        // Verify all listeners received notifications
        for (OrderedListener listener : listeners) {
            assertEquals("Should receive 2 updates", 2, listener.updateCount);
            assertTrue("Should receive buffer full", listener.bufferFullReceived);
            assertTrue("Should receive reset", listener.resetReceived);
        }
        
        // Verify notification order is consistent
        long firstUpdateTime = listeners.get(0).firstUpdateTime;
        for (OrderedListener listener : listeners) {
            // All listeners should be notified within a reasonable time window
            // Allow up to 1ms difference (1,000,000 nanoseconds)
            assertTrue("Notifications should be nearly simultaneous",
                      Math.abs(listener.firstUpdateTime - firstUpdateTime) < 1_000_000);
        }
    }
    
    /**
     * Test error handling and recovery.
     */
    @Test
    public void testErrorHandlingAndRecovery() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(256)
            .maxLevel(8) // Max possible for buffer size 256
            .build();
        
        StreamingMODWT transform = new StreamingMODWT(new Haar1(), config);
        
        ErrorTrackingListener listener = new ErrorTrackingListener();
        transform.addListener(listener);
        
        // This should work despite the high level
        transform.update(generateRandomSignal(100));
        
        // Verify transform still works
        double[][] coeffs = transform.getCurrentCoefficients();
        assertNotNull("Should still produce coefficients", coeffs);
        
        // Test recovery after reset
        transform.reset();
        transform.update(generateRandomSignal(50));
        
        coeffs = transform.getCurrentCoefficients();
        assertNotNull("Should produce coefficients after reset", coeffs);
    }
    
    /**
     * Test memory efficiency with large data streams.
     */
    @Test
    public void testMemoryEfficiency() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(1024)
            .maxLevel(5)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.LAZY)
            .build();
        
        StreamingMODWT transform = new StreamingMODWT(new Daubechies4(), config);
        
        // Process a large amount of data
        long totalSamples = 1_000_000;
        int chunkSize = 1000;
        
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        for (long i = 0; i < totalSamples; i += chunkSize) {
            transform.update(generateRandomSignal(chunkSize));
            
            // Only access coefficients occasionally
            if (i % 100000 == 0) {
                transform.getCurrentCoefficients();
            }
        }
        
        runtime.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        
        // Memory usage should be bounded by buffer size, not total data processed
        long memoryIncrease = memoryAfter - memoryBefore;
        long expectedMaxMemory = 1024 * 8 * 10; // buffer * sizeof(double) * levels * overhead
        
        assertTrue("Memory usage should be bounded: " + memoryIncrease + " bytes",
                  memoryIncrease < expectedMaxMemory * 10); // Allow 10x for JVM overhead
    }
    
    /**
     * Test concurrent access to streaming transforms.
     */
    @Test
    public void testConcurrentAccess() throws InterruptedException {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(512)
            .maxLevel(4)
            .build();
        
        StreamingMODWT transform = new StreamingMODWT(new Haar1(), config);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(10);
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        // Multiple threads updating and reading
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        transform.update(generateRandomSignal(10));
                        Thread.sleep(1);
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Multiple threads reading coefficients
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        double[][] coeffs = transform.getCurrentCoefficients();
                        assertNotNull(coeffs);
                        Thread.sleep(1);
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue("All threads should complete", latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        
        assertEquals("No errors should occur", 0, errorCount.get());
        assertEquals("All threads should succeed", 10, successCount.get());
    }
    
    /**
     * Test edge cases and boundary conditions.
     */
    @Test
    public void testEdgeCases() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(256)
            .maxLevel(3)
            .build();
        
        StreamingMODWT transform = new StreamingMODWT(new Haar1(), config);
        
        // Empty array
        transform.update(new double[0]);
        double[][] coeffs = transform.getCurrentCoefficients();
        assertNotNull("Should handle empty array", coeffs);
        
        // Single sample
        transform.update(new double[]{1.0});
        coeffs = transform.getCurrentCoefficients();
        assertNotNull("Should handle single sample", coeffs);
        
        // Very large array
        transform.update(generateRandomSignal(10000));
        coeffs = transform.getCurrentCoefficients();
        assertNotNull("Should handle large array", coeffs);
        
        // All zeros
        transform.reset();
        transform.update(new double[100]); // zeros
        coeffs = transform.getCurrentCoefficients();
        assertNotNull("Should handle zero signal", coeffs);
        
        // All same value
        double[] constant = new double[100];
        java.util.Arrays.fill(constant, 5.0);
        transform.update(constant);
        coeffs = transform.getCurrentCoefficients();
        assertNotNull("Should handle constant signal", coeffs);
        
        // Alternating values
        double[] alternating = new double[100];
        for (int i = 0; i < alternating.length; i++) {
            alternating[i] = (i % 2 == 0) ? 1.0 : -1.0;
        }
        transform.update(alternating);
        coeffs = transform.getCurrentCoefficients();
        assertNotNull("Should handle alternating signal", coeffs);
    }
    
    /**
     * Test update strategy behavior.
     */
    @Test
    public void testUpdateStrategyBehavior() {
        double[] testSignal = generateRandomSignal(500);
        
        for (StreamingTransformConfig.UpdateStrategy strategy : 
             StreamingTransformConfig.UpdateStrategy.values()) {
            
            StreamingTransformConfig config = StreamingTransformConfig.builder()
                .bufferSize(512)
                .maxLevel(4)
                .updateStrategy(strategy)
                .build();
            
            StreamingMODWT transform = new StreamingMODWT(new Daubechies4(), config);
            StrategyTestListener listener = new StrategyTestListener();
            transform.addListener(listener);
            
            // Process in chunks
            int chunkSize = 50;
            for (int i = 0; i < testSignal.length; i += chunkSize) {
                int end = Math.min(i + chunkSize, testSignal.length);
                double[] chunk = java.util.Arrays.copyOfRange(testSignal, i, end);
                transform.update(chunk);
            }
            
            // Verify behavior based on strategy
            assertTrue("Should have updates for " + strategy, 
                      listener.updateCount > 0);
            
            // Access coefficients
            double[][] coeffs = transform.getCurrentCoefficients();
            assertNotNull("Should have coefficients for " + strategy, coeffs);
            
            // Verify coefficients are valid
            for (double[] level : coeffs) {
                boolean hasNonZero = false;
                for (double c : level) {
                    if (Math.abs(c) > 1e-10) {
                        hasNonZero = true;
                        break;
                    }
                }
                assertTrue("Should have non-zero coefficients for " + strategy, 
                          hasNonZero);
            }
        }
    }
    
    /**
     * Test listener removal and management.
     */
    @Test
    public void testListenerManagement() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(256)
            .maxLevel(3)
            .build();
        
        StreamingMODWT transform = new StreamingMODWT(new Haar1(), config);
        
        // Add listeners
        TestListener listener1 = new TestListener();
        TestListener listener2 = new TestListener();
        TestListener listener3 = new TestListener();
        
        transform.addListener(listener1);
        transform.addListener(listener2);
        transform.addListener(listener3);
        
        assertEquals("Should have 3 listeners", 3, transform.getListenerCount());
        
        // Update and verify all receive notification
        transform.update(generateRandomSignal(50));
        assertEquals(1, listener1.updateCount);
        assertEquals(1, listener2.updateCount);
        assertEquals(1, listener3.updateCount);
        
        // Remove one listener
        assertTrue("Should remove listener", transform.removeListener(listener2));
        assertEquals("Should have 2 listeners", 2, transform.getListenerCount());
        
        // Update again
        transform.update(generateRandomSignal(50));
        assertEquals(2, listener1.updateCount);
        assertEquals(1, listener2.updateCount); // Should not increase
        assertEquals(2, listener3.updateCount);
        
        // Clear all listeners
        transform.clearListeners();
        assertEquals("Should have no listeners", 0, transform.getListenerCount());
        
        // Update should not throw even with no listeners
        transform.update(generateRandomSignal(50));
    }
    
    // Helper methods
    
    private double[] generateECGChunk(int startSample, int length, int sampleRate) {
        double[] chunk = new double[length];
        for (int i = 0; i < length; i++) {
            double t = (startSample + i) / (double) sampleRate;
            // Simple ECG model: P-QRS-T complex
            chunk[i] = 0.1 * Math.sin(2 * Math.PI * 1.2 * t) + // Heart rate
                      0.05 * Math.sin(2 * Math.PI * 50 * t) + // Power line noise
                      0.02 * random.nextGaussian(); // Random noise
        }
        return chunk;
    }
    
    private double[] generateRandomSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = random.nextGaussian();
        }
        return signal;
    }
    
    private double computeEnergy(double[] coeffs) {
        double energy = 0;
        for (double c : coeffs) {
            energy += c * c;
        }
        return energy;
    }
    
    // Test listener implementations
    
    private static class SignalAnalysisListener implements StreamingTransformListener<double[][]> {
        int updateCount = 0;
        boolean bufferFull = false;
        boolean hadError = false;
        
        @Override
        public void onCoefficientsUpdated(double[][] coefficients, int newSamplesCount) {
            updateCount++;
        }
        
        @Override
        public void onBufferFull() {
            bufferFull = true;
        }
        
        @Override
        public void onError(Exception error, boolean recoverable) {
            hadError = true;
        }
    }
    
    private static class OrderedListener implements StreamingTransformListener<double[][]> {
        final int id;
        int updateCount = 0;
        long firstUpdateTime = 0;
        boolean bufferFullReceived = false;
        boolean resetReceived = false;
        
        OrderedListener(int id) {
            this.id = id;
        }
        
        @Override
        public void onCoefficientsUpdated(double[][] coefficients, int newSamplesCount) {
            updateCount++;
            if (firstUpdateTime == 0) {
                firstUpdateTime = System.nanoTime();
            }
        }
        
        @Override
        public void onBufferFull() {
            bufferFullReceived = true;
        }
        
        @Override
        public void onReset() {
            resetReceived = true;
        }
    }
    
    private static class ErrorTrackingListener implements StreamingTransformListener<double[][]> {
        List<Exception> errors = new ArrayList<>();
        
        @Override
        public void onCoefficientsUpdated(double[][] coefficients, int newSamplesCount) {
            // Check for validity
            if (coefficients == null) {
                errors.add(new IllegalStateException("Null coefficients"));
            }
        }
        
        @Override
        public void onError(Exception error, boolean recoverable) {
            errors.add(error);
        }
    }
    
    private static class StrategyTestListener implements StreamingTransformListener<double[][]> {
        int updateCount = 0;
        
        @Override
        public void onCoefficientsUpdated(double[][] coefficients, int newSamplesCount) {
            updateCount++;
        }
    }
    
    private static class TestListener implements StreamingTransformListener<double[][]> {
        int updateCount = 0;
        
        @Override
        public void onCoefficientsUpdated(double[][] coefficients, int newSamplesCount) {
            updateCount++;
        }
    }
}