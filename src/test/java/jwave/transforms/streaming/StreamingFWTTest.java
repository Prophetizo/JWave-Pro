/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import jwave.transforms.FastWaveletTransform;
import jwave.transforms.wavelets.Wavelet;
import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.transforms.wavelets.symlets.Symlet8;
import jwave.exceptions.JWaveException;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive test suite for StreamingFWT implementation.
 * 
 * Tests cover:
 * - Basic functionality and configuration
 * - Coefficient correctness
 * - Power-of-2 buffer handling
 * - Update strategies (FULL, INCREMENTAL, LAZY)
 * - Multi-level decomposition
 * - Edge cases and error handling
 * 
 * @author Prophetizo
 * @date 2025-07-12
 */
public class StreamingFWTTest {
    
    private static final double DELTA = 1e-10;
    
    @Test
    public void testBasicConstruction() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(128)
            .maxLevel(3)
            .build();
        
        StreamingFWT fwt = new StreamingFWT(new Haar1(), config);
        
        assertEquals(128, fwt.getBufferSize());
        assertEquals(128, fwt.getEffectiveBufferSize());
        assertEquals(3, fwt.getMaxLevel());
        assertTrue(fwt.isPowerOfTwo());
    }
    
    @Test
    public void testNonPowerOfTwoBuffer() {
        // Test that non-power-of-2 buffer sizes are padded correctly
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(100)  // Not a power of 2
            .maxLevel(3)
            .build();
        
        StreamingFWT fwt = new StreamingFWT(new Haar1(), config);
        
        assertEquals(100, fwt.getBufferSize());
        assertEquals(128, fwt.getEffectiveBufferSize()); // Next power of 2
        assertFalse(fwt.isPowerOfTwo());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNullWavelet() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(128)
            .build();
        
        new StreamingFWT(null, config);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNullConfig() {
        new StreamingFWT(new Haar1(), null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testExcessiveMaxLevel() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(16)  // max level should be 4 for size 16
            .maxLevel(5)     // This exceeds the maximum
            .build();
        
        new StreamingFWT(new Haar1(), config);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeBufferSize() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(-10)
            .build();
        
        new StreamingFWT(new Haar1(), config);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testZeroBufferSize() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(0)
            .build();
        
        new StreamingFWT(new Haar1(), config);
    }
    
    @Test
    public void testDirectComparison() {
        // Simple test to verify the streaming FWT produces same results as standard FWT
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(8)  // Small size for debugging
            .maxLevel(2)
            .build();
        
        StreamingFWT streaming = new StreamingFWT(new Haar1(), config);
        FastWaveletTransform standard = new FastWaveletTransform(new Haar1());
        
        // Simple test signal
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        // Update streaming transform
        streaming.update(signal);
        double[] streamingCoeffs = streaming.getCurrentCoefficients();
        
        // Compute standard transform
        double[] standardCoeffs;
        try {
            standardCoeffs = standard.forward(signal, 2);
        } catch (JWaveException e) {
            fail("Standard FWT should not throw exception: " + e.getMessage());
            return;
        }
        
        // Compare
        assertArrayEquals("Coefficients should match", standardCoeffs, streamingCoeffs, DELTA);
    }
    
    @Test
    public void testSingleUpdate() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(16)
            .maxLevel(2)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingFWT fwt = new StreamingFWT(new Haar1(), config);
        
        // Generate test signal
        double[] signal = new double[16];
        for (int i = 0; i < 16; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16);
        }
        
        // Update and get coefficients
        fwt.update(signal);
        double[] coeffs = fwt.getCurrentCoefficients();
        
        assertNotNull(coeffs);
        assertEquals(16, coeffs.length);
        
        // Verify non-zero coefficients exist
        boolean hasNonZero = false;
        for (double c : coeffs) {
            if (Math.abs(c) > DELTA) {
                hasNonZero = true;
                break;
            }
        }
        assertTrue("Should have non-zero coefficients", hasNonZero);
    }
    
    @Test
    public void testCoefficientsMatchStandardFWT() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(32)
            .maxLevel(3)
            .build();
        
        StreamingFWT streaming = new StreamingFWT(new Haar1(), config);
        FastWaveletTransform standard = new FastWaveletTransform(new Haar1());
        
        // Generate test signal
        double[] signal = new double[32];
        for (int i = 0; i < 32; i++) {
            signal[i] = Math.cos(2 * Math.PI * i / 8) + 0.5 * Math.sin(4 * Math.PI * i / 8);
        }
        
        // Update streaming transform
        streaming.update(signal);
        double[] streamingCoeffs = streaming.getCurrentCoefficients();
        
        // Compute standard transform
        double[] standardCoeffs;
        try {
            standardCoeffs = standard.forward(signal, 3);
        } catch (JWaveException e) {
            fail("Standard FWT should not throw exception: " + e.getMessage());
            return;
        }
        
        // Compare coefficients
        assertArrayEquals("Coefficients should match standard FWT", 
                         standardCoeffs, streamingCoeffs, DELTA);
    }
    
    @Test
    public void testIncrementalUpdates() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(64)
            .maxLevel(4)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingFWT fwt = new StreamingFWT(new Haar1(), config);
        
        // Add data in chunks
        for (int chunk = 0; chunk < 4; chunk++) {
            double[] data = new double[16];
            for (int i = 0; i < 16; i++) {
                data[i] = chunk + i * 0.1;
            }
            fwt.update(data);
        }
        
        double[] coeffs = fwt.getCurrentCoefficients();
        assertNotNull(coeffs);
        assertEquals(64, coeffs.length);
    }
    
    @Test
    public void testLazyUpdateStrategy() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(32)
            .maxLevel(2)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.LAZY)
            .build();
        
        StreamingFWT fwt = new StreamingFWT(new Haar1(), config);
        
        // Initial update
        double[] data1 = generateRandomSignal(16);
        fwt.update(data1);
        
        // Get coefficients (should trigger computation)
        double[] coeffs1 = fwt.getCurrentCoefficients();
        
        // Another update
        double[] data2 = generateRandomSignal(16);
        fwt.update(data2);
        
        // Coefficients should be computed on demand
        double[] coeffs2 = fwt.getCurrentCoefficients();
        
        // Verify coefficients changed
        boolean changed = false;
        for (int i = 0; i < coeffs1.length; i++) {
            if (Math.abs(coeffs1[i] - coeffs2[i]) > DELTA) {
                changed = true;
                break;
            }
        }
        assertTrue("Coefficients should change with new data", changed);
    }
    
    @Test
    public void testGetCoefficientsAtLevelStructure() {
        // Test that coefficient extraction follows the correct FWT structure
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(16)
            .maxLevel(4)
            .build();
        
        StreamingFWT fwt = new StreamingFWT(new Haar1(), config);
        
        // Use a simple signal to verify structure
        double[] signal = new double[16];
        for (int i = 0; i < 16; i++) {
            signal[i] = i;
        }
        fwt.update(signal);
        
        // Verify coefficient sizes at each level
        for (int level = 1; level <= 4; level++) {
            double[][] levelCoeffs = fwt.getCoefficientsAtLevel(level);
            int expectedSize = 16 >> level; // 16/2^level
            
            assertEquals("Level " + level + " should have 2 arrays", 2, levelCoeffs.length);
            assertEquals("Approximation size at level " + level, expectedSize, levelCoeffs[0].length);
            assertEquals("Detail size at level " + level, expectedSize, levelCoeffs[1].length);
            
            // The approximation at each level should be the first expectedSize elements
            double[] fullCoeffs = fwt.getCurrentCoefficients();
            for (int i = 0; i < expectedSize; i++) {
                assertEquals("Approximation coefficient " + i + " at level " + level,
                           fullCoeffs[i], levelCoeffs[0][i], DELTA);
            }
        }
    }
    
    @Test
    public void testGetCoefficientsAtLevel() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(64)
            .maxLevel(4)
            .build();
        
        StreamingFWT fwt = new StreamingFWT(new Daubechies4(), config);
        
        // Generate and process signal
        double[] signal = new double[64];
        for (int i = 0; i < 64; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16) + 0.3 * Math.cos(8 * Math.PI * i / 16);
        }
        fwt.update(signal);
        
        // Test getting coefficients at different levels
        for (int level = 1; level <= 4; level++) {
            double[][] levelCoeffs = fwt.getCoefficientsAtLevel(level);
            
            assertEquals("Should have 2 arrays (approximation and detail)", 
                        2, levelCoeffs.length);
            
            int expectedSize = 64 >> level; // 64 / 2^level
            assertEquals("Approximation size at level " + level, 
                        expectedSize, levelCoeffs[0].length);
            assertEquals("Detail size at level " + level, 
                        expectedSize, levelCoeffs[1].length);
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testGetCoefficientsAtInvalidLevel() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(32)
            .maxLevel(3)
            .build();
        
        StreamingFWT fwt = new StreamingFWT(new Haar1(), config);
        fwt.update(new double[32]);
        
        // Should throw exception for level > maxLevel
        fwt.getCoefficientsAtLevel(4);
    }
    
    @Test
    public void testReconstruction() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(128)
            .maxLevel(5)
            .build();
        
        StreamingFWT fwt = new StreamingFWT(new Symlet8(), config);
        
        // Generate test signal
        double[] original = generateRandomSignal(128);
        
        // Process signal
        fwt.update(original);
        
        // Full reconstruction (level 0)
        double[] reconstructed = fwt.reconstruct(0);
        
        // Verify perfect reconstruction
        assertArrayEquals("Perfect reconstruction should match original", 
                         original, reconstructed, 1e-8);
        
        // Partial reconstruction at different levels
        for (int level = 1; level <= 5; level++) {
            double[] partial = fwt.reconstruct(level);
            assertNotNull("Partial reconstruction at level " + level, partial);
            assertEquals(128, partial.length);
        }
    }
    
    @Test
    public void testBufferWraparound() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(16)
            .maxLevel(2)
            .build();
        
        StreamingFWT fwt = new StreamingFWT(new Haar1(), config);
        
        // Fill buffer multiple times to test wraparound
        for (int i = 0; i < 5; i++) {
            double[] data = new double[16];
            for (int j = 0; j < 16; j++) {
                data[j] = i * 16 + j;
            }
            fwt.update(data);
            
            double[] coeffs = fwt.getCurrentCoefficients();
            assertNotNull("Coefficients after wraparound " + i, coeffs);
            assertEquals(16, coeffs.length);
        }
    }
    
    @Test
    public void testReset() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(32)
            .maxLevel(3)
            .build();
        
        StreamingFWT fwt = new StreamingFWT(new Haar1(), config);
        
        // Add some data
        fwt.update(generateRandomSignal(32));
        double[] coeffsBefore = fwt.getCurrentCoefficients();
        
        // Reset
        fwt.reset();
        
        // Add different data
        fwt.update(new double[32]); // All zeros
        double[] coeffsAfter = fwt.getCurrentCoefficients();
        
        // Verify all coefficients are zero after processing zeros
        assertArrayEquals("All coefficients should be zero", 
                         new double[coeffsAfter.length], coeffsAfter, DELTA);
    }
    
    @Test
    public void testStreamingWithDifferentWavelets() {
        int bufferSize = 64;
        int maxLevel = 3;
        double[] signal = generateRandomSignal(bufferSize);
        
        // Test with different wavelets
        Wavelet[] wavelets = {
            new Haar1(),
            new Daubechies4(),
            new Symlet8()
        };
        
        for (Wavelet wavelet : wavelets) {
            StreamingTransformConfig config = StreamingTransformConfig.builder()
                .bufferSize(bufferSize)
                .maxLevel(maxLevel)
                .build();
            
            StreamingFWT streaming = new StreamingFWT(wavelet, config);
            streaming.update(signal);
            
            double[] coeffs = streaming.getCurrentCoefficients();
            assertNotNull("Coefficients for " + wavelet.getName(), coeffs);
            assertEquals(bufferSize, coeffs.length);
            
            // Verify reconstruction
            double[] reconstructed = streaming.reconstruct(0);
            assertArrayEquals("Reconstruction for " + wavelet.getName(), 
                            signal, reconstructed, 1e-8);
        }
    }
    
    @Test
    public void testSmallBufferSizes() {
        // Test with minimum buffer sizes
        int[] sizes = {2, 4, 8};
        
        for (int size : sizes) {
            StreamingTransformConfig config = StreamingTransformConfig.builder()
                .bufferSize(size)
                .maxLevel(1)
                .build();
            
            StreamingFWT fwt = new StreamingFWT(new Haar1(), config);
            fwt.update(generateRandomSignal(size));
            
            double[] coeffs = fwt.getCurrentCoefficients();
            assertEquals("Buffer size " + size, size, coeffs.length);
        }
    }
    
    @Test
    public void testContinuousStreaming() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(256)
            .maxLevel(6)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingFWT fwt = new StreamingFWT(new Daubechies4(), config);
        
        // Simulate continuous data stream
        int totalSamples = 1000;
        int chunkSize = 10;
        
        for (int i = 0; i < totalSamples; i += chunkSize) {
            double[] chunk = new double[chunkSize];
            for (int j = 0; j < chunkSize; j++) {
                // Generate signal with varying frequency
                double t = (i + j) / 100.0;
                chunk[j] = Math.sin(2 * Math.PI * t) + 
                          0.5 * Math.sin(10 * Math.PI * t);
            }
            
            fwt.update(chunk);
            
            // Periodically check coefficients
            if (i % 100 == 0) {
                double[] coeffs = fwt.getCurrentCoefficients();
                assertNotNull("Coefficients at sample " + i, coeffs);
                assertEquals(256, coeffs.length);
            }
        }
    }
    
    /**
     * Generate a random signal for testing.
     * 
     * @param length The length of the signal
     * @return Array of random values between -1 and 1
     */
    private double[] generateRandomSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.random() * 2.0 - 1.0;
        }
        return signal;
    }
}