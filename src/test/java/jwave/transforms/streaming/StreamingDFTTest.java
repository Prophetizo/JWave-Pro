/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;

/**
 * Comprehensive test suite for StreamingDFT implementation.
 * 
 * Tests cover:
 * - Basic functionality and configuration
 * - Sliding DFT incremental updates
 * - Arbitrary buffer sizes (non-power-of-2)
 * - Windowing functionality
 * - Spectral analysis features
 * - Update strategies (FULL, INCREMENTAL, LAZY)
 * - Edge cases and error handling
 * 
 * @author Prophetizo
 * @date 2025-07-13
 */
public class StreamingDFTTest {
    
    private static final double DELTA = 1e-9;
    private static final double RELAXED_DELTA = 1e-6;
    
    @Test
    public void testBasicConstruction() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(100) // Non-power-of-2
            .build();
        
        StreamingDFT dft = new StreamingDFT(config);
        
        assertEquals(100, dft.getBufferSize());
        assertEquals(100, dft.getDFTSize());
        assertFalse(dft.isUsingWindow());
    }
    
    @Test
    public void testNonPowerOfTwoBufferSize() {
        // DFT should accept non-power-of-2 sizes
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(150)
            .build();
        
        StreamingDFT dft = new StreamingDFT(config);
        assertEquals(150, dft.getDFTSize());
    }
    
    @Test
    public void testSingleToneSpectrum() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(200) // Non-power-of-2
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingDFT dft = new StreamingDFT(config);
        
        // Generate single tone at bin 10
        double[] signal = new double[200];
        for (int i = 0; i < 200; i++) {
            signal[i] = Math.cos(2 * Math.PI * 10 * i / 200);
        }
        
        dft.update(signal);
        double[] magnitude = dft.getMagnitudeSpectrum();
        
        // Find peak
        int peakBin = 0;
        double peakMag = 0.0;
        for (int i = 0; i < magnitude.length; i++) {
            if (magnitude[i] > peakMag) {
                peakMag = magnitude[i];
                peakBin = i;
            }
        }
        
        // For a 200-point DFT, positive frequencies are 0 to 100, negative are 101 to 199
        // The peak should be at bin 10 or its negative frequency counterpart at 190
        assertTrue("Peak should be at bin 10 or 190", peakBin == 10 || peakBin == 190);
        assertTrue("Peak magnitude should be significant", peakMag > 50.0);
        
        // Verify other bins are near zero (excluding both positive and negative frequency peaks)
        for (int i = 0; i < magnitude.length; i++) {
            if (i != 10 && i != 190) { // Skip both peaks
                assertTrue("Non-peak bins should be near zero (bin " + i + ")", magnitude[i] < 1.0);
            }
        }
    }
    
    @Test
    public void testWindowFunction() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(100)
            .build();
        
        StreamingDFT dft = new StreamingDFT(config);
        dft.setUseWindow(true);
        assertTrue(dft.isUsingWindow());
        
        // Generate a pure sine wave that doesn't align with DFT bins
        double[] signal = new double[100];
        double frequency = 10.5; // Non-integer frequency causes leakage
        for (int i = 0; i < 100; i++) {
            signal[i] = Math.sin(2 * Math.PI * frequency * i / 100);
        }
        
        dft.update(signal);
        double[] spectrumWindowed = dft.getMagnitudeSpectrum();
        
        // Compare with non-windowed
        dft.setUseWindow(false);
        dft.reset();
        dft.update(signal);
        double[] spectrumUnwindowed = dft.getMagnitudeSpectrum();
        
        // Find peak magnitude in each spectrum independently
        double maxWindowed = 0.0;
        double maxUnwindowed = 0.0;
        int peakBinWindowed = 0;
        int peakBinUnwindowed = 0;
        
        for (int i = 0; i < spectrumUnwindowed.length / 2; i++) { // Positive frequencies only
            if (spectrumUnwindowed[i] > maxUnwindowed) {
                maxUnwindowed = spectrumUnwindowed[i];
                peakBinUnwindowed = i;
            }
            if (spectrumWindowed[i] > maxWindowed) {
                maxWindowed = spectrumWindowed[i];
                peakBinWindowed = i;
            }
        }
        
        // Measure spectral leakage
        double leakageWindowed = 0.0;
        double leakageUnwindowed = 0.0;
        
        for (int i = 0; i < spectrumWindowed.length / 2; i++) {
            if (Math.abs(i - peakBinWindowed) > 2) {
                leakageWindowed += spectrumWindowed[i];
            }
            if (Math.abs(i - peakBinUnwindowed) > 2) {
                leakageUnwindowed += spectrumUnwindowed[i];
            }
        }
        
        // Windowing should reduce spectral leakage
        assertTrue("Windowing should reduce spectral leakage", 
                  leakageWindowed < leakageUnwindowed * 0.5);
    }
    
    @Test
    public void testIncrementalUpdateSingleSample() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(50)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingDFT dft = new StreamingDFT(config);
        
        // Fill buffer with zeros
        dft.update(new double[50]);
        
        // Add single non-zero sample
        dft.update(new double[]{1.0});
        
        double[] magnitude = dft.getMagnitudeSpectrum();
        
        // Verify spectrum was updated
        boolean hasNonZero = false;
        for (double mag : magnitude) {
            if (mag > DELTA) {
                hasNonZero = true;
                break;
            }
        }
        assertTrue("Spectrum should have non-zero magnitudes after update", hasNonZero);
    }
    
    @Test
    public void testIncrementalVsFullUpdate() {
        int bufferSize = 100;
        StreamingTransformConfig fullConfig = StreamingTransformConfig.builder()
            .bufferSize(bufferSize)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingTransformConfig incConfig = StreamingTransformConfig.builder()
            .bufferSize(bufferSize)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingDFT fullDFT = new StreamingDFT(fullConfig);
        StreamingDFT incDFT = new StreamingDFT(incConfig);
        
        // Generate initial signal
        double[] signal = new double[bufferSize];
        for (int i = 0; i < bufferSize; i++) {
            signal[i] = Math.sin(2 * Math.PI * 5 * i / bufferSize) +
                       0.5 * Math.cos(2 * Math.PI * 15 * i / bufferSize);
        }
        
        // Initial update
        fullDFT.update(signal);
        incDFT.update(signal);
        
        // Add a few more samples
        double[] newSamples = new double[5];
        for (int i = 0; i < 5; i++) {
            int idx = bufferSize + i;
            newSamples[i] = Math.sin(2 * Math.PI * 5 * idx / bufferSize) +
                           0.5 * Math.cos(2 * Math.PI * 15 * idx / bufferSize);
        }
        
        fullDFT.update(newSamples);
        incDFT.update(newSamples);
        
        // Compare spectra
        double[] fullSpectrum = fullDFT.getMagnitudeSpectrum();
        double[] incSpectrum = incDFT.getMagnitudeSpectrum();
        
        assertArrayEquals("Incremental and full update should produce same result",
                         fullSpectrum, incSpectrum, RELAXED_DELTA);
    }
    
    @Test
    public void testLazyUpdateStrategy() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(60)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.LAZY)
            .build();
        
        StreamingDFT dft = new StreamingDFT(config);
        
        // Initial signal
        double[] signal = generateTestSignal(60, 10.0);
        dft.update(signal);
        
        // Get spectrum (should trigger computation)
        double[] spectrum1 = dft.getMagnitudeSpectrum();
        
        // Update with new samples
        dft.update(generateTestSignal(10, 10.0));
        
        // Get spectrum again (should trigger recomputation)
        double[] spectrum2 = dft.getMagnitudeSpectrum();
        
        // Verify spectra are different
        boolean different = false;
        for (int i = 0; i < spectrum1.length; i++) {
            if (Math.abs(spectrum1[i] - spectrum2[i]) > DELTA) {
                different = true;
                break;
            }
        }
        assertTrue("Spectra should be different after update", different);
    }
    
    @Test
    public void testDominantFrequency() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(200)
            .build();
        
        StreamingDFT dft = new StreamingDFT(config);
        double samplingRate = 1000.0; // 1 kHz
        
        // Generate signal with 50 Hz tone
        double targetFreq = 50.0;
        double[] signal = new double[200];
        for (int i = 0; i < 200; i++) {
            double t = i / samplingRate;
            signal[i] = Math.sin(2 * Math.PI * targetFreq * t);
        }
        
        dft.update(signal);
        double dominantFreq = dft.getDominantFrequency(samplingRate);
        
        // Allow for some frequency resolution error
        double freqResolution = samplingRate / 200;
        assertEquals("Dominant frequency should be close to 50 Hz",
                    targetFreq, dominantFreq, freqResolution);
    }
    
    @Test
    public void testSpectralCentroid() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(150)
            .build();
        
        StreamingDFT dft = new StreamingDFT(config);
        double samplingRate = 1000.0;
        
        // Generate low frequency signal
        double[] lowFreqSignal = new double[150];
        for (int i = 0; i < 150; i++) {
            double t = i / samplingRate;
            lowFreqSignal[i] = Math.sin(2 * Math.PI * 50 * t);
        }
        
        dft.update(lowFreqSignal);
        double lowCentroid = dft.getSpectralCentroid(samplingRate);
        
        // Generate high frequency signal
        dft.reset();
        double[] highFreqSignal = new double[150];
        for (int i = 0; i < 150; i++) {
            double t = i / samplingRate;
            highFreqSignal[i] = Math.sin(2 * Math.PI * 200 * t);
        }
        
        dft.update(highFreqSignal);
        double highCentroid = dft.getSpectralCentroid(samplingRate);
        
        assertTrue("High frequency signal should have higher centroid",
                  highCentroid > lowCentroid);
    }
    
    @Test
    public void testPowerSpectrum() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(80)
            .build();
        
        StreamingDFT dft = new StreamingDFT(config);
        
        // Generate signal
        double[] signal = generateTestSignal(80, 5.0);
        dft.update(signal);
        
        double[] magnitude = dft.getMagnitudeSpectrum();
        double[] power = dft.getPowerSpectrum();
        
        // Verify power = magnitude^2
        for (int i = 0; i < magnitude.length; i++) {
            assertEquals("Power should equal magnitude squared",
                        magnitude[i] * magnitude[i], power[i], DELTA);
        }
    }
    
    @Test
    public void testPhaseSpectrum() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(100)
            .build();
        
        StreamingDFT dft = new StreamingDFT(config);
        
        // Generate cosine (phase = 0) and sine (phase = π/2)
        double[] cosSignal = new double[100];
        double[] sinSignal = new double[100];
        
        for (int i = 0; i < 100; i++) {
            cosSignal[i] = Math.cos(2 * Math.PI * 10 * i / 100);
            sinSignal[i] = Math.sin(2 * Math.PI * 10 * i / 100);
        }
        
        // Test cosine
        dft.update(cosSignal);
        double[] cosPhase = dft.getPhaseSpectrum();
        
        // Test sine
        dft.reset();
        dft.update(sinSignal);
        double[] sinPhase = dft.getPhaseSpectrum();
        
        // Phase at bin 10 should differ by π/2
        double phaseDiff = Math.abs(sinPhase[10] - cosPhase[10]);
        assertEquals("Phase difference should be π/2", Math.PI / 2, phaseDiff, 0.1);
    }
    
    @Test
    public void testFrequencyBins() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(200)
            .build();
        
        StreamingDFT dft = new StreamingDFT(config);
        double samplingRate = 1000.0;
        
        double[] frequencies = dft.getFrequencyBins(samplingRate);
        
        // Verify frequency bin spacing
        assertEquals("Should have N frequency bins", 200, frequencies.length);
        assertEquals("First bin should be 0 Hz", 0.0, frequencies[0], DELTA);
        
        // Verify linear spacing
        double binWidth = samplingRate / 200;
        for (int i = 1; i < frequencies.length; i++) {
            assertEquals("Bins should be linearly spaced",
                        binWidth, frequencies[i] - frequencies[i-1], DELTA);
        }
    }
    
    @Test
    public void testBufferWraparound() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(75) // Non-power-of-2
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingDFT dft = new StreamingDFT(config);
        
        // Fill buffer multiple times
        for (int round = 0; round < 5; round++) {
            double[] data = generateTestSignal(75, 10.0 + round);
            dft.update(data);
            
            double[] spectrum = dft.getMagnitudeSpectrum();
            assertNotNull("Spectrum should be valid after wraparound " + round, spectrum);
            assertEquals("Spectrum length should be consistent", 75, spectrum.length);
            
            // Verify spectrum has energy
            double totalEnergy = 0.0;
            for (double mag : spectrum) {
                totalEnergy += mag * mag;
            }
            assertTrue("Spectrum should have energy", totalEnergy > 0.0);
        }
    }
    
    @Test
    public void testReset() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(90)
            .build();
        
        StreamingDFT dft = new StreamingDFT(config);
        
        // Add signal
        dft.update(generateTestSignal(90, 20.0));
        double[] spectrumBefore = dft.getMagnitudeSpectrum();
        
        // Verify non-zero
        double energyBefore = 0.0;
        for (double mag : spectrumBefore) {
            energyBefore += mag * mag;
        }
        assertTrue("Spectrum should have energy before reset", energyBefore > 0.0);
        
        // Reset
        dft.reset();
        
        // Add zeros
        dft.update(new double[90]);
        double[] spectrumAfter = dft.getMagnitudeSpectrum();
        
        // Verify near zero
        double energyAfter = 0.0;
        for (double mag : spectrumAfter) {
            energyAfter += mag * mag;
        }
        assertTrue("Spectrum should be near zero after reset", energyAfter < 1e-10);
    }
    
    @Test
    public void testMultipleTones() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(300)
            .build();
        
        StreamingDFT dft = new StreamingDFT(config);
        double samplingRate = 1000.0;
        
        // Generate signal with three tones
        double[] freqs = {50.0, 150.0, 250.0};
        double[] signal = new double[300];
        
        for (int i = 0; i < 300; i++) {
            double t = i / samplingRate;
            signal[i] = Math.sin(2 * Math.PI * freqs[0] * t) +
                       0.7 * Math.sin(2 * Math.PI * freqs[1] * t) +
                       0.5 * Math.sin(2 * Math.PI * freqs[2] * t);
        }
        
        dft.update(signal);
        double[] magnitude = dft.getMagnitudeSpectrum();
        double[] freqBins = dft.getFrequencyBins(samplingRate);
        
        // Find peaks for positive frequencies only
        int maxBin = 150; // Half of buffer size
        for (double targetFreq : freqs) {
            // Find closest bin
            int closestBin = 0;
            double minDiff = Double.MAX_VALUE;
            
            for (int i = 0; i < maxBin; i++) {
                double diff = Math.abs(freqBins[i] - targetFreq);
                if (diff < minDiff) {
                    minDiff = diff;
                    closestBin = i;
                }
            }
            
            // Verify peak exists
            assertTrue("Should have peak near " + targetFreq + " Hz",
                      magnitude[closestBin] > 30.0);
        }
    }
    
    @Test
    public void testArbitraryBufferSizes() {
        // Test various non-power-of-2 sizes
        int[] sizes = {37, 63, 101, 127, 199, 255};
        
        for (int size : sizes) {
            StreamingTransformConfig config = StreamingTransformConfig.builder()
                .bufferSize(size)
                .build();
            
            StreamingDFT dft = new StreamingDFT(config);
            assertEquals("DFT size should match buffer size", size, dft.getDFTSize());
            
            // Verify it works with a simple signal
            double[] signal = new double[size];
            for (int i = 0; i < size; i++) {
                signal[i] = Math.sin(2 * Math.PI * 5 * i / size);
            }
            
            dft.update(signal);
            double[] spectrum = dft.getMagnitudeSpectrum();
            
            assertEquals("Spectrum length should match DFT size", size, spectrum.length);
            
            // Verify we have a peak
            double maxMag = 0.0;
            for (double mag : spectrum) {
                if (mag > maxMag) {
                    maxMag = mag;
                }
            }
            assertTrue("Should have a significant peak", maxMag > 10.0);
        }
    }
    
    /**
     * Generate a test signal with specified frequency.
     */
    private double[] generateTestSignal(int length, double frequency) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * frequency * i / length) +
                       0.3 * Math.cos(4 * Math.PI * frequency * i / length);
        }
        return signal;
    }
}