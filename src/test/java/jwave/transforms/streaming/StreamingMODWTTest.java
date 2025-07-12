/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import jwave.transforms.MODWTTransform;
import jwave.transforms.wavelets.Wavelet;
import jwave.transforms.wavelets.WaveletBuilder;
import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.util.Arrays;

/**
 * Unit tests for StreamingMODWT implementation.
 * 
 * @author Prophetizo
 * @date 2025-07-12
 */
public class StreamingMODWTTest {
    
    private Wavelet haar;
    private Wavelet daub4;
    private StreamingTransformConfig defaultConfig;
    
    @Before
    public void setUp() {
        haar = new Haar1();
        daub4 = new Daubechies4();
        defaultConfig = StreamingTransformConfig.builder()
            .bufferSize(512)
            .maxLevel(3)
            .build();
    }
    
    @Test
    public void testInitialization() {
        StreamingMODWT transform = new StreamingMODWT(haar, defaultConfig);
        
        assertNotNull(transform);
        assertEquals(haar, transform.getWavelet());
        assertEquals(512, transform.getEffectiveBufferSize()); // Already power of 2
    }
    
    @Test
    public void testPowerOfTwoBufferSize() {
        // Test non-power-of-2 buffer size
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(500) // Not power of 2
            .maxLevel(3)
            .build();
            
        StreamingMODWT transform = new StreamingMODWT(haar, config);
        transform.initialize(500, 3);
        
        // Should round up to next power of 2
        assertEquals(512, transform.getEffectiveBufferSize());
    }
    
    @Test
    public void testSingleSampleUpdate() {
        StreamingMODWT transform = new StreamingMODWT(haar, defaultConfig);
        
        // Add single samples
        for (int i = 0; i < 100; i++) {
            double[][] coeffs = transform.update(Math.sin(2 * Math.PI * i / 50));
            assertNotNull(coeffs);
            assertEquals(4, coeffs.length); // 3 detail levels + 1 approximation
        }
    }
    
    @Test
    public void testBatchUpdate() {
        StreamingMODWT transform = new StreamingMODWT(haar, defaultConfig);
        
        // Create test signal
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16);
        }
        
        double[][] coeffs = transform.update(signal);
        assertNotNull(coeffs);
        assertEquals(4, coeffs.length); // 3 detail levels + 1 approximation
        
        // Verify coefficient structure
        for (int level = 0; level < coeffs.length; level++) {
            assertNotNull(coeffs[level]);
            assertEquals(512, coeffs[level].length); // Effective buffer size
        }
    }
    
    @Test
    public void testComparisonWithStandardMODWT() {
        // Create a full buffer of test data
        double[] testSignal = new double[256];
        for (int i = 0; i < testSignal.length; i++) {
            testSignal[i] = Math.sin(2 * Math.PI * i / 32) + 
                           0.5 * Math.sin(2 * Math.PI * i / 8);
        }
        
        // Standard MODWT
        MODWTTransform standardMODWT = new MODWTTransform(haar);
        double[][] standardCoeffs = standardMODWT.forwardMODWT(testSignal, 3);
        
        // Streaming MODWT with appropriate buffer size
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(256)
            .maxLevel(3)
            .build();
        StreamingMODWT streamingMODWT = new StreamingMODWT(haar, config);
        
        // Fill buffer completely
        streamingMODWT.update(testSignal);
        double[][] streamingCoeffs = streamingMODWT.getCurrentCoefficients();
        
        // Compare coefficients
        assertEquals(standardCoeffs.length, streamingCoeffs.length);
        
        for (int level = 0; level < standardCoeffs.length; level++) {
            assertArrayEquals(
                "Coefficients should match at level " + level,
                standardCoeffs[level], 
                streamingCoeffs[level], 
                1e-10
            );
        }
    }
    
    @Test
    public void testDetailAndApproximationAccess() {
        StreamingMODWT transform = new StreamingMODWT(daub4, defaultConfig);
        
        // Add some data
        double[] signal = new double[128];
        Arrays.fill(signal, 1.0);
        transform.update(signal);
        
        // Test detail coefficient access
        for (int level = 1; level <= 3; level++) {
            double[] details = transform.getDetailCoefficients(level);
            assertNotNull(details);
            assertEquals(512, details.length);
        }
        
        // Test approximation coefficient access
        double[] approx = transform.getApproximationCoefficients();
        assertNotNull(approx);
        assertEquals(512, approx.length);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidDetailLevel() {
        StreamingMODWT transform = new StreamingMODWT(haar, defaultConfig);
        transform.getDetailCoefficients(4); // Max level is 3
    }
    
    @Test
    public void testBufferFullNotification() {
        StreamingMODWT transform = new StreamingMODWT(haar, defaultConfig);
        
        TestListener listener = new TestListener();
        transform.addListener(listener);
        
        // Fill the buffer
        double[] data = new double[512];
        transform.update(data);
        
        assertTrue("Buffer full should be notified", listener.bufferFullCalled);
    }
    
    @Test
    public void testReset() {
        StreamingMODWT transform = new StreamingMODWT(haar, defaultConfig);
        
        // Add data
        double[] signal = new double[100];
        Arrays.fill(signal, 1.0);
        transform.update(signal);
        
        // Reset
        transform.reset();
        
        // Verify buffer is empty
        double[] buffer = transform.getCurrentBuffer();
        assertEquals(0, buffer.length);
        
        // Coefficients should be recomputed as zeros
        double[][] coeffs = transform.getCurrentCoefficients();
        for (int level = 0; level < coeffs.length; level++) {
            for (int i = 0; i < coeffs[level].length; i++) {
                assertEquals(0.0, coeffs[level][i], 1e-10);
            }
        }
    }
    
    @Test
    public void testMultiResolutionAnalysis() {
        StreamingMODWT transform = new StreamingMODWT(haar, defaultConfig);
        
        // Create composite signal
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 64) +    // Low frequency
                       0.5 * Math.sin(2 * Math.PI * i / 16) + // Mid frequency
                       0.25 * Math.sin(2 * Math.PI * i / 4);  // High frequency
        }
        
        transform.update(signal);
        
        // Compute MRA
        double[][] mra = transform.computeMRA();
        assertNotNull(mra);
        assertEquals(4, mra.length); // 3 details + 1 approximation
        
        // Each component should have the same length
        for (double[] component : mra) {
            assertEquals(512, component.length);
        }
    }
    
    @Test
    public void testIncrementalProcessing() {
        StreamingMODWT transform = new StreamingMODWT(haar, defaultConfig);
        
        TestListener listener = new TestListener();
        transform.addListener(listener);
        
        // Process data in chunks
        int chunkSize = 50;
        int numChunks = 11; // 11 * 50 = 550 > 512, so buffer will be full
        
        for (int chunk = 0; chunk < numChunks; chunk++) {
            double[] data = new double[chunkSize];
            for (int i = 0; i < chunkSize; i++) {
                data[i] = Math.random();
            }
            
            double[][] coeffs = transform.update(data);
            assertNotNull(coeffs);
            assertEquals(chunk + 1, listener.updateCount);
        }
        
        // Verify final state
        assertEquals(numChunks, listener.updateCount);
        assertTrue("Buffer should be full after processing 550 samples", listener.bufferFullCalled);
    }
    
    @Test
    public void testDifferentWavelets() {
        Wavelet[] wavelets = {
            new Haar1(),
            new Daubechies4(),
            WaveletBuilder.create("Symlet 8"),
            WaveletBuilder.create("Coiflet 2")
        };
        
        double[] signal = new double[128];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.cos(2 * Math.PI * i / 32);
        }
        
        for (Wavelet wavelet : wavelets) {
            StreamingMODWT transform = new StreamingMODWT(wavelet, defaultConfig);
            double[][] coeffs = transform.update(signal);
            
            assertNotNull("Coefficients should not be null for " + wavelet.getName(), coeffs);
            assertEquals("Should have correct number of levels for " + wavelet.getName(), 
                        4, coeffs.length);
        }
    }
    
    @Test
    public void testUpdateStrategies() {
        StreamingTransformConfig.UpdateStrategy[] strategies = {
            StreamingTransformConfig.UpdateStrategy.FULL,
            StreamingTransformConfig.UpdateStrategy.INCREMENTAL,
            StreamingTransformConfig.UpdateStrategy.LAZY
        };
        
        double[] signal = new double[100];
        Arrays.fill(signal, 1.0);
        
        for (StreamingTransformConfig.UpdateStrategy strategy : strategies) {
            StreamingTransformConfig config = StreamingTransformConfig.builder()
                .bufferSize(256)
                .maxLevel(2)
                .updateStrategy(strategy)
                .build();
                
            StreamingMODWT transform = new StreamingMODWT(haar, config);
            double[][] coeffs = transform.update(signal);
            
            assertNotNull("Coefficients should not be null for " + strategy, coeffs);
            assertEquals("Should have correct number of levels for " + strategy, 
                        3, coeffs.length);
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNullWavelet() {
        new StreamingMODWT(null, defaultConfig);
    }
    
    @Test
    public void testMultipleInitialization() {
        StreamingMODWT transform = new StreamingMODWT(haar, defaultConfig);
        
        // First initialization happens in constructor
        assertTrue(transform.isInitialized());
        
        // Additional initialization calls should work (replaces buffer)
        transform.initialize(256, 3);
        assertEquals(256, transform.getBufferSize());
        assertEquals(3, transform.getMaxLevel());
        
        // Verify we can still use the transform
        double[] data = new double[50];
        Arrays.fill(data, 1.0);
        double[][] coeffs = transform.update(data);
        assertNotNull(coeffs);
    }
    
    /**
     * Test listener for verifying callbacks.
     */
    private static class TestListener implements StreamingTransformListener<double[][]> {
        int updateCount = 0;
        boolean bufferFullCalled = false;
        boolean resetCalled = false;
        boolean errorCalled = false;
        
        @Override
        public void onCoefficientsUpdated(double[][] coefficients, int newSamplesCount) {
            updateCount++;
        }
        
        @Override
        public void onBufferFull() {
            bufferFullCalled = true;
        }
        
        @Override
        public void onReset() {
            resetCalled = true;
        }
        
        @Override
        public void onError(Exception e, boolean recoverable) {
            errorCalled = true;
        }
    }
}