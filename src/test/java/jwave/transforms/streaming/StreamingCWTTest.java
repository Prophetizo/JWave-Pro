/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import jwave.transforms.CWTResult;
import jwave.transforms.wavelets.continuous.*;
import jwave.datatypes.natives.Complex;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive test suite for StreamingCWT implementation.
 * 
 * Tests cover:
 * - Basic functionality and configuration
 * - Scale parameter handling
 * - Time-frequency analysis correctness
 * - Update strategies (FULL, INCREMENTAL, LAZY)
 * - FFT vs direct computation
 * - Edge cases and error handling
 * 
 * @author Prophetizo
 * @date 2025-07-13
 */
public class StreamingCWTTest {
    
    private static final double DELTA = 1e-6;
    
    @Test
    public void testBasicConstruction() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(128)
            .build();
        
        StreamingCWT cwt = new StreamingCWT(new MorletWavelet(), config);
        
        assertEquals(128, cwt.getBufferSize());
        assertEquals(50, cwt.getNumScales()); // Default number of scales
        assertEquals(1.0, cwt.getSamplingRate(), DELTA);
        assertFalse(cwt.isUsingFFT()); // Default is direct computation
    }
    
    @Test
    public void testCustomScaleParameters() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(64)
            .build();
        
        StreamingCWT cwt = new StreamingCWT(
            new MorletWavelet(), config, 
            0.5, 10.0, 20, true  // min, max, num scales, log spacing
        );
        
        assertEquals(20, cwt.getNumScales());
        double[] scales = cwt.getScales();
        assertEquals(20, scales.length);
        
        // Verify log spacing
        double ratio = scales[1] / scales[0];
        for (int i = 2; i < scales.length; i++) {
            assertEquals(ratio, scales[i] / scales[i-1], DELTA * 10);
        }
    }
    
    @Test(expected = NullPointerException.class)
    public void testNullWavelet() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(128)
            .build();
        
        new StreamingCWT(null, config);
    }
    
    @Test(expected = NullPointerException.class)
    public void testNullConfig() {
        new StreamingCWT(new MorletWavelet(), null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidScaleRange() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(64)
            .build();
        
        new StreamingCWT(new MorletWavelet(), config, 10.0, 5.0, 10, true);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeScale() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(64)
            .build();
        
        new StreamingCWT(new MorletWavelet(), config, -1.0, 10.0, 10, true);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testTooFewScales() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(64)
            .build();
        
        new StreamingCWT(new MorletWavelet(), config, 1.0, 10.0, 1, true);
    }
    
    @Test
    public void testSingleUpdate() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(64)
            .maxLevel(3)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingCWT cwt = new StreamingCWT(
            new MorletWavelet(), config,
            1.0, 8.0, 8, false  // Linear scales for simplicity
        );
        cwt.setSamplingRate(100.0); // 100 Hz
        
        // Generate test signal - chirp
        double[] signal = new double[64];
        for (int i = 0; i < 64; i++) {
            double t = i / 100.0;
            double freq = 5.0 + 20.0 * t; // 5-25 Hz chirp
            signal[i] = Math.sin(2 * Math.PI * freq * t);
        }
        
        // Update and get result
        CWTResult result = cwt.update(signal);
        
        assertNotNull(result);
        assertEquals(8, result.getNumberOfScales());
        assertEquals(64, result.getNumberOfTimePoints());
        
        // Check that we have non-zero coefficients
        double[][] magnitude = result.getMagnitude();
        boolean hasNonZero = false;
        for (int i = 0; i < magnitude.length; i++) {
            for (int j = 0; j < magnitude[i].length; j++) {
                if (magnitude[i][j] > DELTA) {
                    hasNonZero = true;
                    break;
                }
            }
        }
        assertTrue("Should have non-zero coefficients", hasNonZero);
    }
    
    @Test
    public void testScalogramExtraction() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(32)
            .build();
        
        StreamingCWT cwt = new StreamingCWT(
            new MorletWavelet(), config,
            1.0, 4.0, 4, false
        );
        
        // Simple sinusoidal signal
        double[] signal = new double[32];
        for (int i = 0; i < 32; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 8); // Period of 8 samples
        }
        
        cwt.update(signal);
        double[][] scalogram = cwt.getScalogram();
        
        assertEquals(4, scalogram.length);      // 4 scales
        assertEquals(32, scalogram[0].length);  // 32 time points
        
        // Verify all values are non-negative (magnitudes)
        for (int i = 0; i < scalogram.length; i++) {
            for (int j = 0; j < scalogram[i].length; j++) {
                assertTrue("Magnitude should be non-negative", scalogram[i][j] >= 0);
            }
        }
    }
    
    @Test
    public void testPhaseExtraction() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(32)
            .build();
        
        StreamingCWT cwt = new StreamingCWT(new MorletWavelet(), config, 1.0, 4.0, 4, false);
        
        // Simple signal
        double[] signal = new double[32];
        Arrays.fill(signal, 1.0);
        
        cwt.update(signal);
        double[][] phase = cwt.getPhase();
        
        assertEquals(4, phase.length);
        assertEquals(32, phase[0].length);
        
        // Verify phase is in [-π, π]
        for (int i = 0; i < phase.length; i++) {
            for (int j = 0; j < phase[i].length; j++) {
                assertTrue("Phase should be in [-π, π], but was " + phase[i][j], 
                          phase[i][j] >= -Math.PI - 0.01 && phase[i][j] <= Math.PI + 0.01);
            }
        }
    }
    
    @Test
    public void testGetCoefficientsAtScale() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(16)
            .build();
        
        StreamingCWT cwt = new StreamingCWT(new MorletWavelet(), config, 1.0, 3.0, 3, false);
        
        double[] signal = generateTestSignal(16);
        cwt.update(signal);
        
        // Get coefficients at each scale
        for (int scale = 0; scale < 3; scale++) {
            Complex[] coeffs = cwt.getCoefficientsAtScale(scale);
            assertEquals(16, coeffs.length);
            
            // Verify we have complex coefficients
            for (Complex c : coeffs) {
                assertNotNull(c);
            }
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testGetCoefficientsAtInvalidScale() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(16)
            .build();
        
        StreamingCWT cwt = new StreamingCWT(new MorletWavelet(), config, 1.0, 3.0, 3, false);
        cwt.update(new double[16]);
        
        cwt.getCoefficientsAtScale(3); // Invalid - only 0, 1, 2 are valid
    }
    
    @Test
    public void testGetCoefficientsAtTime() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(16)
            .build();
        
        StreamingCWT cwt = new StreamingCWT(new MorletWavelet(), config, 1.0, 3.0, 3, false);
        
        double[] signal = generateTestSignal(16);
        cwt.update(signal);
        
        // Get coefficients at each time point
        for (int time = 0; time < 16; time++) {
            Complex[] coeffs = cwt.getCoefficientsAtTime(time);
            assertEquals(3, coeffs.length); // 3 scales
            
            for (Complex c : coeffs) {
                assertNotNull(c);
            }
        }
    }
    
    @Test
    public void testFrequencyConversion() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(32)
            .build();
        
        StreamingCWT cwt = new StreamingCWT(new MorletWavelet(), config, 1.0, 10.0, 5, false);
        cwt.setSamplingRate(1000.0); // 1000 Hz sampling
        
        double[] frequencies = cwt.getFrequencies();
        assertEquals(5, frequencies.length);
        
        // Frequencies should decrease as scale increases
        for (int i = 1; i < frequencies.length; i++) {
            assertTrue("Frequencies should decrease with scale", 
                      frequencies[i] < frequencies[i-1]);
        }
    }
    
    @Test
    public void testScaleEnergies() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(64)
            .build();
        
        StreamingCWT cwt = new StreamingCWT(new MorletWavelet(), config, 1.0, 8.0, 4, false);
        
        // Signal with specific frequency content
        double[] signal = new double[64];
        for (int i = 0; i < 64; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16); // Period matches scale ~2
        }
        
        cwt.update(signal);
        double[] energies = cwt.getScaleEnergies();
        
        assertEquals(4, energies.length);
        
        // All energies should be non-negative
        for (double energy : energies) {
            assertTrue("Energy should be non-negative", energy >= 0);
        }
    }
    
    @Test
    public void testIncrementalUpdates() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(64)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingCWT cwt = new StreamingCWT(new MorletWavelet(), config, 1.0, 4.0, 4, false);
        
        // Add data in chunks
        for (int chunk = 0; chunk < 4; chunk++) {
            double[] data = new double[16];
            for (int i = 0; i < 16; i++) {
                data[i] = Math.random() - 0.5;
            }
            cwt.update(data);
        }
        
        CWTResult result = cwt.getCurrentCoefficients();
        assertNotNull(result);
        assertEquals(4, result.getNumberOfScales());
        assertEquals(64, result.getNumberOfTimePoints());
    }
    
    @Test
    public void testLazyUpdateStrategy() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(32)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.LAZY)
            .build();
        
        StreamingCWT cwt = new StreamingCWT(new MorletWavelet(), config, 1.0, 2.0, 2, false);
        
        // Initial update
        double[] data1 = generateTestSignal(16);
        cwt.update(data1);
        
        // Get result (should trigger computation)
        CWTResult result1 = cwt.getCurrentCoefficients();
        
        // Another update
        double[] data2 = generateTestSignal(16);
        cwt.update(data2);
        
        // Results should be computed on demand
        CWTResult result2 = cwt.getCurrentCoefficients();
        
        // Verify results are different
        Complex[][] coeffs1 = result1.getCoefficients();
        Complex[][] coeffs2 = result2.getCoefficients();
        
        boolean changed = false;
        for (int i = 0; i < coeffs1.length; i++) {
            for (int j = 0; j < coeffs1[i].length; j++) {
                if (!coeffs1[i][j].equals(coeffs2[i][j])) {
                    changed = true;
                    break;
                }
            }
        }
        assertTrue("Coefficients should change with new data", changed);
    }
    
    @Test
    public void testFFTMode() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(64)
            .build();
        
        StreamingCWT cwt = new StreamingCWT(new MorletWavelet(), config, 1.0, 4.0, 4, false);
        cwt.setUseFFT(true);
        assertTrue(cwt.isUsingFFT());
        
        // Generate signal
        double[] signal = new double[64];
        for (int i = 0; i < 64; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16);
        }
        
        // Update with FFT mode
        CWTResult result = cwt.update(signal);
        assertNotNull(result);
        
        // Results should be valid
        double[][] magnitude = result.getMagnitude();
        assertEquals(4, magnitude.length);
        assertEquals(64, magnitude[0].length);
    }
    
    @Test
    public void testSamplingRateUpdate() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(32)
            .build();
        
        StreamingCWT cwt = new StreamingCWT(new MorletWavelet(), config);
        
        // Update sampling rate
        cwt.setSamplingRate(500.0);
        assertEquals(500.0, cwt.getSamplingRate(), DELTA);
        
        // Add data and verify time axis
        cwt.update(new double[32]);
        CWTResult result = cwt.getCurrentCoefficients();
        
        double[] timeAxis = result.getTimeAxis();
        assertEquals(32, timeAxis.length);
        assertEquals(0.002, timeAxis[1] - timeAxis[0], DELTA); // 1/500 = 0.002
    }
    
    @Test
    public void testScaleUpdate() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(32)
            .build();
        
        StreamingCWT cwt = new StreamingCWT(new MorletWavelet(), config, 1.0, 4.0, 4, false);
        
        // Add initial data
        cwt.update(generateTestSignal(32));
        CWTResult result1 = cwt.getCurrentCoefficients();
        assertEquals(4, result1.getNumberOfScales());
        
        // Update scales
        cwt.updateScales(2.0, 8.0, 6, true);
        assertEquals(6, cwt.getNumScales());
        
        // Get new result (should trigger recomputation)
        CWTResult result2 = cwt.getCurrentCoefficients();
        assertEquals(6, result2.getNumberOfScales());
    }
    
    @Test
    public void testBufferWraparound() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(16)
            .build();
        
        StreamingCWT cwt = new StreamingCWT(new MorletWavelet(), config, 1.0, 2.0, 2, false);
        
        // Fill buffer multiple times
        for (int i = 0; i < 5; i++) {
            double[] data = new double[16];
            for (int j = 0; j < 16; j++) {
                data[j] = i * 16 + j;
            }
            cwt.update(data);
            
            CWTResult result = cwt.getCurrentCoefficients();
            assertNotNull("Result after wraparound " + i, result);
            assertEquals(2, result.getNumberOfScales());
            assertEquals(16, result.getNumberOfTimePoints());
        }
    }
    
    @Test
    public void testReset() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(32)
            .build();
        
        StreamingCWT cwt = new StreamingCWT(new MorletWavelet(), config, 1.0, 3.0, 3, false);
        
        // Add some data
        cwt.update(generateTestSignal(32));
        CWTResult resultBefore = cwt.getCurrentCoefficients();
        
        // Reset
        cwt.reset();
        
        // Add zeros
        cwt.update(new double[32]);
        CWTResult resultAfter = cwt.getCurrentCoefficients();
        
        // Verify coefficients are near zero (some edge effects expected)
        double[][] magnitude = resultAfter.getMagnitude();
        double maxMag = 0;
        for (int i = 0; i < magnitude.length; i++) {
            for (int j = 0; j < magnitude[i].length; j++) {
                maxMag = Math.max(maxMag, magnitude[i][j]);
            }
        }
        assertTrue("Coefficients should be near zero after reset", maxMag < 0.1);
    }
    
    @Test
    public void testDifferentWavelets() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(64)
            .build();
        
        ContinuousWavelet[] wavelets = {
            new MorletWavelet(),
            new MexicanHatWavelet(),
            new DOGWavelet(1, 2)  // Difference of Gaussians
        };
        
        double[] signal = generateTestSignal(64);
        
        for (ContinuousWavelet wavelet : wavelets) {
            StreamingCWT cwt = new StreamingCWT(wavelet, config, 1.0, 8.0, 8, false);
            CWTResult result = cwt.update(signal);
            
            assertNotNull("Result for " + wavelet.getName(), result);
            assertEquals(wavelet.getName(), result.getWaveletName());
            assertEquals(8, result.getNumberOfScales());
            assertEquals(64, result.getNumberOfTimePoints());
        }
    }
    
    @Test
    public void testSmallBufferSizes() {
        int[] sizes = {8, 16, 32};
        
        for (int size : sizes) {
            StreamingTransformConfig config = StreamingTransformConfig.builder()
                .bufferSize(size)
                .build();
            
            StreamingCWT cwt = new StreamingCWT(new MorletWavelet(), config, 1.0, 4.0, 4, false);
            cwt.update(generateTestSignal(size));
            
            CWTResult result = cwt.getCurrentCoefficients();
            assertEquals("Buffer size " + size, size, result.getNumberOfTimePoints());
        }
    }
    
    @Test
    public void testContinuousStreaming() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(256)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingCWT cwt = new StreamingCWT(new MorletWavelet(), config, 1.0, 32.0, 16, true);
        cwt.setSamplingRate(1000.0); // 1 kHz
        
        // Simulate continuous data stream
        int totalSamples = 1000;
        int chunkSize = 10;
        
        for (int i = 0; i < totalSamples; i += chunkSize) {
            double[] chunk = new double[chunkSize];
            for (int j = 0; j < chunkSize; j++) {
                // Generate signal with time-varying frequency
                double t = (i + j) / 1000.0;
                double freq = 50.0 + 100.0 * Math.sin(2 * Math.PI * 0.5 * t);
                chunk[j] = Math.sin(2 * Math.PI * freq * t);
            }
            
            cwt.update(chunk);
            
            // Periodically check results
            if (i % 100 == 0) {
                CWTResult result = cwt.getCurrentCoefficients();
                assertNotNull("Result at sample " + i, result);
                assertEquals(16, result.getNumberOfScales());
                assertEquals(256, result.getNumberOfTimePoints());
            }
        }
    }
    
    /**
     * Generate a test signal with mixed frequency content.
     */
    private double[] generateTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 8) + 
                       0.5 * Math.cos(4 * Math.PI * i / 8) + 
                       0.25 * Math.sin(8 * Math.PI * i / 8);
        }
        return signal;
    }
}