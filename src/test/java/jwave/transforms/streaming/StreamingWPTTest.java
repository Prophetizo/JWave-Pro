/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import jwave.transforms.WaveletPacketTransform;
import jwave.transforms.wavelets.Wavelet;
import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.transforms.wavelets.symlets.Symlet8;
import jwave.exceptions.JWaveException;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive test suite for StreamingWPT implementation.
 * 
 * Tests cover:
 * - Basic functionality and configuration
 * - Packet structure and coefficient correctness
 * - Power-of-2 buffer handling
 * - Update strategies (FULL, INCREMENTAL, LAZY)
 * - Multi-level packet decomposition
 * - Energy distribution analysis
 * - Edge cases and error handling
 * 
 * @author Prophetizo
 * @date 2025-07-13
 */
public class StreamingWPTTest {
    
    private static final double DELTA = 1e-10;
    
    @Test
    public void testBasicConstruction() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(128)
            .maxLevel(3)
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        
        assertEquals(128, wpt.getBufferSize());
        assertEquals(128, wpt.getEffectiveBufferSize());
        assertEquals(3, wpt.getMaxLevel());
        assertTrue(wpt.isPowerOfTwo());
    }
    
    @Test
    public void testNonPowerOfTwoBuffer() {
        // Test that non-power-of-2 buffer sizes are padded correctly
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(100)  // Not a power of 2
            .maxLevel(3)
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        
        assertEquals(100, wpt.getBufferSize());
        assertEquals(128, wpt.getEffectiveBufferSize()); // Next power of 2
        assertFalse(wpt.isPowerOfTwo());
    }
    
    @Test(expected = NullPointerException.class)
    public void testNullWavelet() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(128)
            .build();
        
        new StreamingWPT(null, config);
    }
    
    @Test(expected = NullPointerException.class)
    public void testNullConfig() {
        new StreamingWPT(new Haar1(), null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testExcessiveMaxLevel() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(16)  // max level should be 4 for size 16
            .maxLevel(5)     // This exceeds the maximum
            .build();
        
        new StreamingWPT(new Haar1(), config);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeBufferSize() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(-10)
            .build();
        
        new StreamingWPT(new Haar1(), config);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testZeroBufferSize() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(0)
            .build();
        
        new StreamingWPT(new Haar1(), config);
    }
    
    @Test
    public void testPacketStructure() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(16)
            .maxLevel(4)
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        
        // Verify packet counts at each level
        assertEquals(1, wpt.getPacketCount(0));   // Level 0: 1 packet
        assertEquals(2, wpt.getPacketCount(1));   // Level 1: 2 packets
        assertEquals(4, wpt.getPacketCount(2));   // Level 2: 4 packets
        assertEquals(8, wpt.getPacketCount(3));   // Level 3: 8 packets
        assertEquals(16, wpt.getPacketCount(4));  // Level 4: 16 packets
        
        // Verify packet sizes at each level
        assertEquals(16, wpt.getPacketSize(0));   // Level 0: size 16
        assertEquals(8, wpt.getPacketSize(1));    // Level 1: size 8
        assertEquals(4, wpt.getPacketSize(2));    // Level 2: size 4
        assertEquals(2, wpt.getPacketSize(3));    // Level 3: size 2
        assertEquals(1, wpt.getPacketSize(4));    // Level 4: size 1
    }
    
    @Test
    public void testDirectComparison() {
        // Simple test to verify the streaming WPT produces same results as standard WPT
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(8)  // Small size for debugging
            .maxLevel(2)
            .build();
        
        StreamingWPT streaming = new StreamingWPT(new Haar1(), config);
        WaveletPacketTransform standard = new WaveletPacketTransform(new Haar1());
        
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
            fail("Standard WPT should not throw exception: " + e.getMessage());
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
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        
        // Generate test signal
        double[] signal = new double[16];
        for (int i = 0; i < 16; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16);
        }
        
        // Update and get coefficients
        wpt.update(signal);
        double[] coeffs = wpt.getCurrentCoefficients();
        
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
    public void testCoefficientsMatchStandardWPT() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(32)
            .maxLevel(3)
            .build();
        
        StreamingWPT streaming = new StreamingWPT(new Haar1(), config);
        WaveletPacketTransform standard = new WaveletPacketTransform(new Haar1());
        
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
            fail("Standard WPT should not throw exception: " + e.getMessage());
            return;
        }
        
        // Compare coefficients
        assertArrayEquals("Coefficients should match standard WPT", 
                         standardCoeffs, streamingCoeffs, DELTA);
    }
    
    @Test
    public void testGetPacket() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(16)
            .maxLevel(3)
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        
        // Use a simple signal to verify packet extraction
        double[] signal = new double[16];
        for (int i = 0; i < 16; i++) {
            signal[i] = i;
        }
        wpt.update(signal);
        
        // Test getting packets at different levels
        // Level 0: 1 packet of size 16
        double[] packet00 = wpt.getPacket(0, 0);
        assertEquals(16, packet00.length);
        
        // Level 1: 2 packets of size 8
        double[] packet10 = wpt.getPacket(1, 0);
        double[] packet11 = wpt.getPacket(1, 1);
        assertEquals(8, packet10.length);
        assertEquals(8, packet11.length);
        
        // Level 2: 4 packets of size 4
        for (int i = 0; i < 4; i++) {
            double[] packet = wpt.getPacket(2, i);
            assertEquals(4, packet.length);
        }
        
        // Level 3: 8 packets of size 2
        for (int i = 0; i < 8; i++) {
            double[] packet = wpt.getPacket(3, i);
            assertEquals(2, packet.length);
        }
    }
    
    @Test
    public void testGetAllPacketsAtLevel() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(32)
            .maxLevel(3)
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Daubechies4(), config);
        
        // Generate and process signal
        double[] signal = new double[32];
        for (int i = 0; i < 32; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16) + 0.3 * Math.cos(8 * Math.PI * i / 16);
        }
        wpt.update(signal);
        
        // Test getting all packets at different levels
        for (int level = 0; level <= 3; level++) {
            double[][] packets = wpt.getAllPacketsAtLevel(level);
            
            int expectedPackets = 1 << level;  // 2^level
            assertEquals("Level " + level + " should have " + expectedPackets + " packets",
                        expectedPackets, packets.length);
            
            int expectedSize = 32 >> level;  // 32 / 2^level
            for (int i = 0; i < packets.length; i++) {
                assertEquals("Packet " + i + " at level " + level + " should have size " + expectedSize,
                            expectedSize, packets[i].length);
            }
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testGetPacketInvalidLevel() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(32)
            .maxLevel(3)
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        wpt.update(new double[32]);
        
        // Should throw exception for level > maxLevel
        wpt.getPacket(4, 0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testGetPacketInvalidIndex() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(32)
            .maxLevel(3)
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        wpt.update(new double[32]);
        
        // Should throw exception for invalid packet index
        // Level 2 has 4 packets (0-3), so index 4 is invalid
        wpt.getPacket(2, 4);
    }
    
    @Test
    public void testIncrementalUpdates() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(64)
            .maxLevel(4)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        
        // Add data in chunks
        for (int chunk = 0; chunk < 4; chunk++) {
            double[] data = new double[16];
            for (int i = 0; i < 16; i++) {
                data[i] = chunk + i * 0.1;
            }
            wpt.update(data);
        }
        
        double[] coeffs = wpt.getCurrentCoefficients();
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
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        
        // Initial update
        double[] data1 = generateRandomSignal(16);
        wpt.update(data1);
        
        // Get coefficients (should trigger computation)
        double[] coeffs1 = wpt.getCurrentCoefficients();
        
        // Another update
        double[] data2 = generateRandomSignal(16);
        wpt.update(data2);
        
        // Coefficients should be computed on demand
        double[] coeffs2 = wpt.getCurrentCoefficients();
        
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
    public void testPacketEnergies() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(64)
            .maxLevel(3)
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        
        // Create a signal with energy concentrated in specific frequency bands
        double[] signal = new double[64];
        for (int i = 0; i < 64; i++) {
            // Low frequency component
            signal[i] = Math.sin(2 * Math.PI * i / 32);
            // High frequency component in second half
            if (i >= 32) {
                signal[i] += 0.5 * Math.sin(16 * Math.PI * i / 32);
            }
        }
        
        wpt.update(signal);
        
        // Get energy distribution at level 2
        double[] energies = wpt.getPacketEnergies(2);
        assertEquals(4, energies.length);  // 4 packets at level 2
        
        // Verify all energies are non-negative
        for (double energy : energies) {
            assertTrue("Energy should be non-negative", energy >= 0);
        }
        
        // Verify total energy is preserved (approximately)
        double totalEnergy = 0;
        for (double energy : energies) {
            totalEnergy += energy;
        }
        
        double signalEnergy = 0;
        for (double sample : signal) {
            signalEnergy += sample * sample;
        }
        
        // Energy should be approximately preserved (within numerical tolerance)
        assertEquals("Total energy should be preserved", signalEnergy, totalEnergy, signalEnergy * 0.01);
    }
    
    @Test
    public void testPacketPath() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(16)
            .maxLevel(4)
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        
        // Simple signal
        double[] signal = new double[16];
        Arrays.fill(signal, 1.0);
        wpt.update(signal);
        
        // Get packet path for a specific time location
        double[][] path = wpt.getPacketPath(8);
        
        // Should have maxLevel + 1 packets in the path
        assertEquals(5, path.length);
        
        // Verify packet sizes along the path
        assertEquals(16, path[0].length);  // Level 0
        assertEquals(8, path[1].length);   // Level 1
        assertEquals(4, path[2].length);   // Level 2
        assertEquals(2, path[3].length);   // Level 3
        assertEquals(1, path[4].length);   // Level 4
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testPacketPathInvalidTimeIndex() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(16)
            .maxLevel(3)
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        wpt.update(new double[16]);
        
        // Time index out of bounds
        wpt.getPacketPath(16);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testReconstructInvalidLevelNegative() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(32)
            .maxLevel(3)
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        wpt.update(new double[32]);
        
        // Should throw exception for negative level
        wpt.reconstruct(-1);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testReconstructInvalidLevelTooHigh() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(32)
            .maxLevel(3)
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        wpt.update(new double[32]);
        
        // Should throw exception for level > maxLevel
        wpt.reconstruct(4);
    }
    
    @Test
    public void testBufferWraparound() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(16)
            .maxLevel(2)
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        
        // Fill buffer multiple times to test wraparound
        for (int i = 0; i < 5; i++) {
            double[] data = new double[16];
            for (int j = 0; j < 16; j++) {
                data[j] = i * 16 + j;
            }
            wpt.update(data);
            
            double[] coeffs = wpt.getCurrentCoefficients();
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
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        
        // Add some data
        wpt.update(generateRandomSignal(32));
        double[] coeffsBefore = wpt.getCurrentCoefficients();
        
        // Reset
        wpt.reset();
        
        // Add different data
        wpt.update(new double[32]); // All zeros
        double[] coeffsAfter = wpt.getCurrentCoefficients();
        
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
            
            StreamingWPT streaming = new StreamingWPT(wavelet, config);
            streaming.update(signal);
            
            double[] coeffs = streaming.getCurrentCoefficients();
            assertNotNull("Coefficients for " + wavelet.getName(), coeffs);
            assertEquals(bufferSize, coeffs.length);
            
            // Test packet extraction
            double[][] packets = streaming.getAllPacketsAtLevel(2);
            assertEquals("Should have 4 packets at level 2", 4, packets.length);
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
            
            StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
            wpt.update(generateRandomSignal(size));
            
            double[] coeffs = wpt.getCurrentCoefficients();
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
        
        StreamingWPT wpt = new StreamingWPT(new Daubechies4(), config);
        
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
            
            wpt.update(chunk);
            
            // Periodically check coefficients and packet structure
            if (i % 100 == 0) {
                double[] coeffs = wpt.getCurrentCoefficients();
                assertNotNull("Coefficients at sample " + i, coeffs);
                assertEquals(256, coeffs.length);
                
                // Check energy distribution
                double[] energies = wpt.getPacketEnergies(3);
                assertEquals(8, energies.length);  // 8 packets at level 3
            }
        }
    }
    
    @Test
    public void testFullBinaryTreeStructure() {
        // Verify that WPT creates a full binary tree structure
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(32)
            .maxLevel(5)  // Full decomposition
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        
        // Generate signal with mixed frequencies
        double[] signal = new double[32];
        for (int i = 0; i < 32; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 8) + 
                       Math.cos(4 * Math.PI * i / 8) + 
                       0.5 * Math.sin(8 * Math.PI * i / 8);
        }
        wpt.update(signal);
        
        // Verify total number of coefficients at each level
        int totalCoeffs = 0;
        for (int level = 0; level <= 5; level++) {
            double[][] packets = wpt.getAllPacketsAtLevel(level);
            int coeffsAtLevel = 0;
            for (double[] packet : packets) {
                coeffsAtLevel += packet.length;
            }
            
            // Each level should have exactly 32 coefficients total
            assertEquals("Level " + level + " should have 32 total coefficients",
                        32, coeffsAtLevel);
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