/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import jwave.transforms.FastFourierTransform;
import jwave.datatypes.natives.Complex;
import jwave.utils.MathUtils;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Streaming implementation of the Fast Fourier Transform (FFT).
 * 
 * This class provides streaming capabilities for frequency analysis using
 * sliding DFT for incremental updates and overlap-save method for efficient
 * processing of continuous data streams.
 * 
 * Key features:
 * - Sliding DFT for single-sample updates (O(N) complexity)
 * - Overlap-save method for batch processing
 * - Power-of-2 buffer sizes for optimal FFT performance
 * - Real-time spectral analysis
 * - Thread-safe coefficient access
 * 
 * The sliding DFT algorithm allows efficient updates when individual samples
 * are added to the buffer, avoiding full FFT recomputation. For larger
 * updates, the implementation falls back to standard FFT computation.
 * 
 * @author Prophetizo
 * @date 2025-07-13
 */
public class StreamingFFT extends AbstractStreamingTransform<double[]> {
    
    private final FastFourierTransform fft;
    private final StreamingTransformConfig config;
    
    // FFT parameters
    private int fftSize;
    private int halfSize;
    
    // Current FFT coefficients (complex interleaved format)
    private double[] spectrum;
    private boolean spectrumDirty = false;
    
    // Sliding DFT state
    private Complex[] dftCoefficients;
    private double[] window;
    private boolean useWindow = false;
    
    // Thread safety for coefficient access
    private final ReadWriteLock spectrumLock = new ReentrantReadWriteLock();
    
    // Twiddle factors for sliding DFT
    private Complex[] twiddleFactors;
    
    /**
     * Create a new streaming FFT transform.
     * 
     * @param config The streaming configuration
     */
    public StreamingFFT(StreamingTransformConfig config) {
        super(new FastFourierTransform());
        
        this.config = Objects.requireNonNull(config, "Configuration cannot be null");
        this.fft = (FastFourierTransform) transform;
        
        // Initialize with config values
        initialize(config.getBufferSize(), 0); // maxLevel not used for FFT
    }
    
    /**
     * Enable or disable windowing.
     * When enabled, a Hamming window is applied before FFT computation.
     * 
     * @param useWindow Whether to use windowing
     */
    public void setUseWindow(boolean useWindow) {
        this.useWindow = useWindow;
        if (useWindow && window == null && fftSize > 0) {
            initializeWindow();
        }
        // Mark spectrum as dirty to force recomputation with new window setting
        spectrumLock.writeLock().lock();
        try {
            spectrumDirty = true;
        } finally {
            spectrumLock.writeLock().unlock();
        }
    }
    
    /**
     * Check if windowing is enabled.
     * 
     * @return true if windowing is enabled
     */
    public boolean isUsingWindow() {
        return useWindow;
    }
    
    @Override
    protected void validateBufferSize(int bufferSize) {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must be positive");
        }
        // For optimal FFT performance, buffer size should be power of 2
        if (!MathUtils.isPowerOfTwo(bufferSize)) {
            throw new IllegalArgumentException(
                "Buffer size must be a power of 2 for StreamingFFT. Got: " + bufferSize
            );
        }
    }
    
    @Override
    protected void initializeTransformState() {
        // Set FFT size to buffer size
        fftSize = bufferSize;
        halfSize = fftSize / 2;
        
        // Initialize spectrum storage (complex interleaved format)
        spectrum = new double[fftSize * 2];
        
        // Initialize sliding DFT state
        dftCoefficients = new Complex[fftSize];
        for (int i = 0; i < fftSize; i++) {
            dftCoefficients[i] = new Complex(0, 0);
        }
        
        // Pre-compute twiddle factors for sliding DFT
        initializeTwiddleFactors();
        
        // Initialize window if needed
        if (useWindow) {
            initializeWindow();
        }
    }
    
    /**
     * Initialize twiddle factors for sliding DFT algorithm.
     */
    private void initializeTwiddleFactors() {
        twiddleFactors = new Complex[fftSize];
        double angleIncrement = -2.0 * Math.PI / fftSize;
        
        for (int k = 0; k < fftSize; k++) {
            double angle = angleIncrement * k;
            twiddleFactors[k] = new Complex(Math.cos(angle), Math.sin(angle));
        }
    }
    
    /**
     * Initialize Hamming window.
     */
    private void initializeWindow() {
        window = new double[fftSize];
        for (int i = 0; i < fftSize; i++) {
            window[i] = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (fftSize - 1));
        }
    }
    
    /**
     * Apply window to signal if windowing is enabled.
     */
    private double[] applyWindow(double[] signal) {
        if (!useWindow || window == null) {
            // Always return a copy to avoid modifying the original
            return Arrays.copyOf(signal, signal.length);
        }
        
        double[] windowed = new double[signal.length];
        for (int i = 0; i < signal.length; i++) {
            windowed[i] = signal[i] * window[i];
        }
        return windowed;
    }
    
    @Override
    protected double[] performUpdate(double[] newSamples) {
        switch (config.getUpdateStrategy()) {
            case FULL:
                // Always recompute full FFT immediately
                return recomputeFFT();
                
            case INCREMENTAL:
                // Use sliding DFT for single-sample updates
                return performIncrementalUpdate(newSamples);
                
            case LAZY:
                // Just mark spectrum as dirty, don't compute yet
                spectrumLock.writeLock().lock();
                try {
                    spectrumDirty = true;
                } finally {
                    spectrumLock.writeLock().unlock();
                }
                // Return current spectrum
                return getSpectrum();
                
            default:
                throw new IllegalStateException("Unknown update strategy: " + config.getUpdateStrategy());
        }
    }
    
    /**
     * Recompute full FFT from current buffer state.
     */
    private double[] recomputeFFT() {
        double[] bufferData = buffer.toArray();
        
        // Apply window if enabled
        double[] processedData = applyWindow(bufferData);
        
        // Compute FFT
        double[] newSpectrum;
        try {
            newSpectrum = fft.forward(processedData);
        } catch (jwave.exceptions.JWaveException e) {
            throw new RuntimeException("FFT computation failed", e);
        }
        
        // Update internal state
        spectrumLock.writeLock().lock();
        try {
            spectrum = newSpectrum;
            spectrumDirty = false;
            
            // Update DFT coefficients for future sliding updates
            for (int k = 0; k < fftSize; k++) {
                dftCoefficients[k] = new Complex(
                    spectrum[2 * k],
                    spectrum[2 * k + 1]
                );
            }
        } finally {
            spectrumLock.writeLock().unlock();
        }
        
        return spectrum;
    }
    
    /**
     * Perform incremental FFT update using sliding DFT algorithm.
     * 
     * The sliding DFT efficiently updates the spectrum when samples
     * are shifted in/out of the buffer:
     * X[k] = (X_old[k] - x_oldest + x_newest) * W^k
     * where W = exp(-j*2*pi/N) is the twiddle factor.
     * 
     * @param newSamples The new samples added to the buffer
     * @return Updated spectrum
     */
    private double[] performIncrementalUpdate(double[] newSamples) {
        // For large updates, full FFT is more efficient
        if (newSamples.length > fftSize / 4 || buffer.hasWrapped()) {
            return recomputeFFT();
        }
        
        // If no new samples, return existing spectrum
        if (newSamples.length == 0) {
            return getSpectrum();
        }
        
        spectrumLock.writeLock().lock();
        try {
            // Get the current buffer data
            double[] bufferData = buffer.toArray();
            
            // For each new sample, update using sliding DFT
            for (int sampleIdx = 0; sampleIdx < newSamples.length; sampleIdx++) {
                // Determine which sample was removed (oldest)
                int removedIdx = (buffer.size() - newSamples.length + sampleIdx) % fftSize;
                double removedSample = 0.0; // Default for first fill
                
                if (buffer.size() >= fftSize) {
                    // Buffer is full, so we're removing an old sample
                    // The oldest sample index when buffer has wrapped is at writeIndex position
                    int writeIdx = buffer.size() % fftSize; // Current write position
                    removedIdx = (writeIdx - newSamples.length + sampleIdx + fftSize) % fftSize;
                    removedSample = bufferData[removedIdx];
                }
                
                // The new sample
                double newSample = newSamples[sampleIdx];
                
                // Apply window if enabled
                if (useWindow && window != null) {
                    int newIdx = (buffer.size() - newSamples.length + sampleIdx) % fftSize;
                    newSample *= window[newIdx];
                    if (buffer.size() >= fftSize) {
                        removedSample *= window[removedIdx];
                    }
                }
                
                // Update each frequency bin using sliding DFT
                for (int k = 0; k < fftSize; k++) {
                    // Subtract oldest sample contribution
                    Complex oldContribution = new Complex(removedSample, 0);
                    
                    // Add newest sample contribution
                    Complex newContribution = new Complex(newSample, 0);
                    
                    // Update DFT coefficient
                    dftCoefficients[k] = dftCoefficients[k]
                        .sub(oldContribution)
                        .add(newContribution)
                        .mul(twiddleFactors[k]);
                }
            }
            
            // Convert back to interleaved format
            for (int k = 0; k < fftSize; k++) {
                spectrum[2 * k] = dftCoefficients[k].getReal();
                spectrum[2 * k + 1] = dftCoefficients[k].getImag();
            }
            
            spectrumDirty = false;
        } finally {
            spectrumLock.writeLock().unlock();
        }
        
        return spectrum;
    }
    
    /**
     * Get the current spectrum.
     * 
     * @return Complex spectrum in interleaved format [re0, im0, re1, im1, ...]
     */
    public double[] getSpectrum() {
        spectrumLock.readLock().lock();
        try {
            return Arrays.copyOf(spectrum, spectrum.length);
        } finally {
            spectrumLock.readLock().unlock();
        }
    }
    
    /**
     * Get the magnitude spectrum (absolute values).
     * 
     * @return Magnitude spectrum of length N/2+1 for real input
     */
    public double[] getMagnitudeSpectrum() {
        double[] spec = getCachedCoefficients();
        double[] magnitude = new double[halfSize + 1];
        
        for (int i = 0; i <= halfSize; i++) {
            double real = spec[2 * i];
            double imag = spec[2 * i + 1];
            magnitude[i] = Math.sqrt(real * real + imag * imag);
        }
        
        return magnitude;
    }
    
    /**
     * Get the power spectrum (squared magnitudes).
     * 
     * @return Power spectrum of length N/2+1 for real input
     */
    public double[] getPowerSpectrum() {
        double[] spec = getCachedCoefficients();
        double[] power = new double[halfSize + 1];
        
        for (int i = 0; i <= halfSize; i++) {
            double real = spec[2 * i];
            double imag = spec[2 * i + 1];
            power[i] = real * real + imag * imag;
        }
        
        return power;
    }
    
    /**
     * Get the phase spectrum.
     * 
     * @return Phase spectrum in radians of length N/2+1 for real input
     */
    public double[] getPhaseSpectrum() {
        double[] spec = getCachedCoefficients();
        double[] phase = new double[halfSize + 1];
        
        for (int i = 0; i <= halfSize; i++) {
            double real = spec[2 * i];
            double imag = spec[2 * i + 1];
            phase[i] = Math.atan2(imag, real);
        }
        
        return phase;
    }
    
    /**
     * Get frequency values for each bin.
     * 
     * @param samplingRate The sampling rate in Hz
     * @return Array of frequency values in Hz
     */
    public double[] getFrequencyBins(double samplingRate) {
        double[] frequencies = new double[halfSize + 1];
        double binWidth = samplingRate / fftSize;
        
        for (int i = 0; i <= halfSize; i++) {
            frequencies[i] = i * binWidth;
        }
        
        return frequencies;
    }
    
    @Override
    protected double[] getCachedCoefficients() {
        // Check if we need to recompute due to LAZY strategy
        spectrumLock.readLock().lock();
        boolean needsCompute = spectrumDirty;
        spectrumLock.readLock().unlock();
        
        if (needsCompute) {
            return recomputeFFT();
        }
        
        return getSpectrum();
    }
    
    @Override
    protected void resetTransformState() {
        spectrumLock.writeLock().lock();
        try {
            // Clear spectrum
            Arrays.fill(spectrum, 0.0);
            
            // Clear DFT coefficients
            for (int i = 0; i < fftSize; i++) {
                dftCoefficients[i] = new Complex(0, 0);
            }
            
            spectrumDirty = false;
        } finally {
            spectrumLock.writeLock().unlock();
        }
    }
    
    /**
     * Get the FFT size.
     * 
     * @return FFT size (same as buffer size)
     */
    public int getFFTSize() {
        return fftSize;
    }
    
    /**
     * Compute the dominant frequency in the current spectrum.
     * 
     * @param samplingRate The sampling rate in Hz
     * @return The frequency with highest magnitude, or 0 if no clear peak
     */
    public double getDominantFrequency(double samplingRate) {
        double[] magnitude = getMagnitudeSpectrum();
        
        // Find peak magnitude (skip DC component)
        int peakBin = 0;
        double peakMag = 0.0;
        
        for (int i = 1; i < magnitude.length; i++) {
            if (magnitude[i] > peakMag) {
                peakMag = magnitude[i];
                peakBin = i;
            }
        }
        
        // Convert bin to frequency
        return peakBin * samplingRate / fftSize;
    }
    
    /**
     * Estimate the spectral centroid (center of mass of spectrum).
     * 
     * @param samplingRate The sampling rate in Hz
     * @return Spectral centroid in Hz
     */
    public double getSpectralCentroid(double samplingRate) {
        double[] magnitude = getMagnitudeSpectrum();
        double[] frequencies = getFrequencyBins(samplingRate);
        
        double weightedSum = 0.0;
        double totalMagnitude = 0.0;
        
        for (int i = 0; i < magnitude.length; i++) {
            weightedSum += frequencies[i] * magnitude[i];
            totalMagnitude += magnitude[i];
        }
        
        return totalMagnitude > 0 ? weightedSum / totalMagnitude : 0.0;
    }
}