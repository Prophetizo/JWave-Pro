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
 * Comprehensive test suite for StreamingFFT implementation.
 * 
 * Tests cover:
 * - Basic functionality and configuration
 * - Sliding DFT incremental updates
 * - Windowing functionality
 * - Spectral analysis features
 * - Update strategies (FULL, INCREMENTAL, LAZY)
 * - Edge cases and error handling
 * 
 * @author Prophetizo
 * @date 2025-07-13
 */
public class StreamingFFTTest {
    
    private static final double DELTA = 1e-9;
    private static final double RELAXED_DELTA = 1e-6;
    
    @Test
    public void testBasicConstruction() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(128)
            .build();
        
        StreamingFFT fft = new StreamingFFT(config);
        
        assertEquals(128, fft.getBufferSize());
        assertEquals(128, fft.getFFTSize());
        assertFalse(fft.isUsingWindow());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNonPowerOfTwoBufferSize() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(100) // Not a power of 2
            .build();
        
        new StreamingFFT(config);
    }
    
    @Test
    public void testSingleToneSpectrum() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(256)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingFFT fft = new StreamingFFT(config);
        
        // Generate single tone at bin 10
        double[] signal = new double[256];
        for (int i = 0; i < 256; i++) {
            signal[i] = Math.cos(2 * Math.PI * 10 * i / 256);
        }
        
        fft.update(signal);
        double[] magnitude = fft.getMagnitudeSpectrum();
        
        // Find peak
        int peakBin = 0;
        double peakMag = 0.0;
        for (int i = 0; i < magnitude.length; i++) {
            if (magnitude[i] > peakMag) {
                peakMag = magnitude[i];
                peakBin = i;
            }
        }
        
        // Verify peak is at bin 10
        assertEquals("Peak should be at bin 10", 10, peakBin);
        assertTrue("Peak magnitude should be significant", peakMag > 100.0);
        
        // Verify other bins are near zero
        for (int i = 0; i < magnitude.length; i++) {
            if (i != peakBin) { // Skip peak bin
                assertTrue("Non-peak bins should be near zero", magnitude[i] < 1.0);
            }
        }
    }
    
    @Test
    public void testWindowFunction() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(128)
            .build();
        
        StreamingFFT fft = new StreamingFFT(config);
        fft.setUseWindow(true);
        assertTrue(fft.isUsingWindow());
        
        // Generate a pure sine wave that doesn't align with FFT bins
        // This will show clear spectral leakage without windowing
        double[] signal = new double[128];
        double frequency = 10.5; // Non-integer frequency causes leakage
        for (int i = 0; i < 128; i++) {
            signal[i] = Math.sin(2 * Math.PI * frequency * i / 128);
        }
        
        fft.update(signal);
        double[] spectrumWindowed = fft.getMagnitudeSpectrum();
        
        // Compare with non-windowed
        fft.setUseWindow(false);
        fft.reset();
        fft.update(signal);
        double[] spectrumUnwindowed = fft.getMagnitudeSpectrum();
        
        // Find peak magnitude in both spectra
        double maxWindowed = 0.0;
        double maxUnwindowed = 0.0;
        int peakBin = 0;
        
        for (int i = 0; i < spectrumWindowed.length; i++) {
            if (spectrumUnwindowed[i] > maxUnwindowed) {
                maxUnwindowed = spectrumUnwindowed[i];
                peakBin = i;
            }
            if (spectrumWindowed[i] > maxWindowed) {
                maxWindowed = spectrumWindowed[i];
            }
        }
        
        // Measure spectral leakage in adjacent bins
        double leakageWindowed = 0.0;
        double leakageUnwindowed = 0.0;
        
        // Check bins around the peak (but not the peak itself)
        for (int i = 0; i < spectrumWindowed.length; i++) {
            if (Math.abs(i - peakBin) > 2) { // Skip peak and immediate neighbors
                leakageWindowed += spectrumWindowed[i];
                leakageUnwindowed += spectrumUnwindowed[i];
            }
        }
        
        // Windowing should significantly reduce spectral leakage
        assertTrue("Windowing should reduce spectral leakage (windowed: " + 
                  leakageWindowed + ", unwindowed: " + leakageUnwindowed + ")", 
                  leakageWindowed < leakageUnwindowed * 0.5); // Expect 50% reduction
    }
    
    @Test
    public void testIncrementalUpdateSingleSample() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(64)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingFFT fft = new StreamingFFT(config);
        
        // Fill buffer with zeros
        fft.update(new double[64]);
        
        // Add single non-zero sample
        fft.update(new double[]{1.0});
        
        double[] magnitude = fft.getMagnitudeSpectrum();
        
        // Verify spectrum was updated - at least one bin should have non-zero magnitude
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
        int bufferSize = 256;
        StreamingTransformConfig fullConfig = StreamingTransformConfig.builder()
            .bufferSize(bufferSize)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingTransformConfig incConfig = StreamingTransformConfig.builder()
            .bufferSize(bufferSize)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingFFT fullFFT = new StreamingFFT(fullConfig);
        StreamingFFT incFFT = new StreamingFFT(incConfig);
        
        // Generate initial signal
        double[] signal = new double[bufferSize];
        for (int i = 0; i < bufferSize; i++) {
            signal[i] = Math.sin(2 * Math.PI * 5 * i / bufferSize) +
                       0.5 * Math.cos(2 * Math.PI * 15 * i / bufferSize);
        }
        
        // Initial update
        fullFFT.update(signal);
        incFFT.update(signal);
        
        // Add a few more samples
        double[] newSamples = new double[5];
        for (int i = 0; i < 5; i++) {
            int idx = bufferSize + i;
            newSamples[i] = Math.sin(2 * Math.PI * 5 * idx / bufferSize) +
                           0.5 * Math.cos(2 * Math.PI * 15 * idx / bufferSize);
        }
        
        fullFFT.update(newSamples);
        incFFT.update(newSamples);
        
        // Compare spectra
        double[] fullSpectrum = fullFFT.getMagnitudeSpectrum();
        double[] incSpectrum = incFFT.getMagnitudeSpectrum();
        
        assertArrayEquals("Incremental and full update should produce same result",
                         fullSpectrum, incSpectrum, RELAXED_DELTA);
    }
    
    @Test
    public void testLazyUpdateStrategy() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(64)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.LAZY)
            .build();
        
        StreamingFFT fft = new StreamingFFT(config);
        
        // Initial signal
        double[] signal = generateTestSignal(64, 10.0);
        fft.update(signal);
        
        // Get spectrum (should trigger computation)
        double[] spectrum1 = fft.getMagnitudeSpectrum();
        
        // Update with new samples
        fft.update(generateTestSignal(10, 10.0));
        
        // Get spectrum again (should trigger recomputation)
        double[] spectrum2 = fft.getMagnitudeSpectrum();
        
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
            .bufferSize(256)
            .build();
        
        StreamingFFT fft = new StreamingFFT(config);
        double samplingRate = 1000.0; // 1 kHz
        
        // Generate signal with 50 Hz tone
        double targetFreq = 50.0;
        double[] signal = new double[256];
        for (int i = 0; i < 256; i++) {
            double t = i / samplingRate;
            signal[i] = Math.sin(2 * Math.PI * targetFreq * t);
        }
        
        fft.update(signal);
        double dominantFreq = fft.getDominantFrequency(samplingRate);
        
        // Allow for some frequency resolution error
        double freqResolution = samplingRate / 256;
        assertEquals("Dominant frequency should be close to 50 Hz",
                    targetFreq, dominantFreq, freqResolution);
    }
    
    @Test
    public void testSpectralCentroid() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(256)
            .build();
        
        StreamingFFT fft = new StreamingFFT(config);
        double samplingRate = 1000.0;
        
        // Generate low frequency signal
        double[] lowFreqSignal = new double[256];
        for (int i = 0; i < 256; i++) {
            double t = i / samplingRate;
            lowFreqSignal[i] = Math.sin(2 * Math.PI * 50 * t);
        }
        
        fft.update(lowFreqSignal);
        double lowCentroid = fft.getSpectralCentroid(samplingRate);
        
        // Generate high frequency signal
        fft.reset();
        double[] highFreqSignal = new double[256];
        for (int i = 0; i < 256; i++) {
            double t = i / samplingRate;
            highFreqSignal[i] = Math.sin(2 * Math.PI * 200 * t);
        }
        
        fft.update(highFreqSignal);
        double highCentroid = fft.getSpectralCentroid(samplingRate);
        
        assertTrue("High frequency signal should have higher centroid",
                  highCentroid > lowCentroid);
    }
    
    @Test
    public void testPowerSpectrum() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(128)
            .build();
        
        StreamingFFT fft = new StreamingFFT(config);
        
        // Generate signal
        double[] signal = generateTestSignal(128, 5.0);
        fft.update(signal);
        
        double[] magnitude = fft.getMagnitudeSpectrum();
        double[] power = fft.getPowerSpectrum();
        
        // Verify power = magnitude^2
        for (int i = 0; i < magnitude.length; i++) {
            assertEquals("Power should equal magnitude squared",
                        magnitude[i] * magnitude[i], power[i], DELTA);
        }
    }
    
    @Test
    public void testPhaseSpectrum() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(128)
            .build();
        
        StreamingFFT fft = new StreamingFFT(config);
        
        // Generate cosine (phase = 0) and sine (phase = π/2)
        double[] cosSignal = new double[128];
        double[] sinSignal = new double[128];
        
        for (int i = 0; i < 128; i++) {
            cosSignal[i] = Math.cos(2 * Math.PI * 10 * i / 128);
            sinSignal[i] = Math.sin(2 * Math.PI * 10 * i / 128);
        }
        
        // Test cosine
        fft.update(cosSignal);
        double[] cosPhase = fft.getPhaseSpectrum();
        
        // Test sine
        fft.reset();
        fft.update(sinSignal);
        double[] sinPhase = fft.getPhaseSpectrum();
        
        // Phase at bin 10 should differ by π/2
        double phaseDiff = Math.abs(sinPhase[10] - cosPhase[10]);
        assertEquals("Phase difference should be π/2", Math.PI / 2, phaseDiff, 0.1);
    }
    
    @Test
    public void testFrequencyBins() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(256)
            .build();
        
        StreamingFFT fft = new StreamingFFT(config);
        double samplingRate = 1000.0;
        
        double[] frequencies = fft.getFrequencyBins(samplingRate);
        
        // Verify frequency bin spacing
        assertEquals("Should have N/2+1 frequency bins", 129, frequencies.length);
        assertEquals("First bin should be 0 Hz", 0.0, frequencies[0], DELTA);
        assertEquals("Last bin should be Nyquist frequency", 
                    samplingRate / 2, frequencies[frequencies.length - 1], DELTA);
        
        // Verify linear spacing
        double binWidth = samplingRate / 256;
        for (int i = 1; i < frequencies.length; i++) {
            assertEquals("Bins should be linearly spaced",
                        binWidth, frequencies[i] - frequencies[i-1], DELTA);
        }
    }
    
    @Test
    public void testBufferWraparound() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(64)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingFFT fft = new StreamingFFT(config);
        
        // Fill buffer multiple times
        for (int round = 0; round < 5; round++) {
            double[] data = generateTestSignal(64, 10.0 + round);
            fft.update(data);
            
            double[] spectrum = fft.getMagnitudeSpectrum();
            assertNotNull("Spectrum should be valid after wraparound " + round, spectrum);
            assertEquals("Spectrum length should be consistent", 33, spectrum.length);
            
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
            .bufferSize(128)
            .build();
        
        StreamingFFT fft = new StreamingFFT(config);
        
        // Add signal
        fft.update(generateTestSignal(128, 20.0));
        double[] spectrumBefore = fft.getMagnitudeSpectrum();
        
        // Verify non-zero
        double energyBefore = 0.0;
        for (double mag : spectrumBefore) {
            energyBefore += mag * mag;
        }
        assertTrue("Spectrum should have energy before reset", energyBefore > 0.0);
        
        // Reset
        fft.reset();
        
        // Add zeros
        fft.update(new double[128]);
        double[] spectrumAfter = fft.getMagnitudeSpectrum();
        
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
            .bufferSize(512)
            .build();
        
        StreamingFFT fft = new StreamingFFT(config);
        double samplingRate = 1000.0;
        
        // Generate signal with three tones
        double[] freqs = {50.0, 150.0, 250.0};
        double[] signal = new double[512];
        
        for (int i = 0; i < 512; i++) {
            double t = i / samplingRate;
            signal[i] = Math.sin(2 * Math.PI * freqs[0] * t) +
                       0.7 * Math.sin(2 * Math.PI * freqs[1] * t) +
                       0.5 * Math.sin(2 * Math.PI * freqs[2] * t);
        }
        
        fft.update(signal);
        double[] magnitude = fft.getMagnitudeSpectrum();
        double[] freqBins = fft.getFrequencyBins(samplingRate);
        
        // Find peaks
        for (double targetFreq : freqs) {
            // Find closest bin
            int closestBin = 0;
            double minDiff = Double.MAX_VALUE;
            
            for (int i = 0; i < freqBins.length; i++) {
                double diff = Math.abs(freqBins[i] - targetFreq);
                if (diff < minDiff) {
                    minDiff = diff;
                    closestBin = i;
                }
            }
            
            // Verify peak exists
            assertTrue("Should have peak near " + targetFreq + " Hz",
                      magnitude[closestBin] > 50.0);
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