/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import jwave.transforms.GeneralDiscreteFourierTransform;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Streaming implementation of the Discrete Fourier Transform (DFT).
 * 
 * This class provides streaming capabilities for frequency analysis using
 * the sliding DFT algorithm for incremental updates. Unlike StreamingFFT,
 * this implementation supports arbitrary buffer sizes, not just powers of 2.
 * 
 * Key features:
 * - Sliding DFT for single-sample updates (O(N) complexity)
 * - Supports arbitrary buffer sizes (no power-of-2 restriction)
 * - Real-time spectral analysis
 * - Thread-safe coefficient access
 * - Optional windowing support
 * 
 * The sliding DFT algorithm allows efficient updates when individual samples
 * are added to the buffer, avoiding full DFT recomputation. For larger
 * updates, the implementation falls back to standard DFT computation.
 * 
 * Trade-offs compared to StreamingFFT:
 * - Supports any buffer size (more flexible)
 * - Full DFT computation is O(N²) instead of O(N log N)
 * - Single-sample updates remain O(N) for both
 * 
 * @author Prophetizo
 * @date 2025-07-13
 */
public class StreamingDFT extends AbstractStreamingTransform<double[]> {
    
    // Note: Threshold ratio is now defined in AbstractStreamingTransform
    
    private final GeneralDiscreteFourierTransform dft;
    private final StreamingTransformConfig config;
    
    // DFT parameters
    private int dftSize;
    
    // Current DFT coefficients (complex interleaved format)
    private double[] spectrum;
    private boolean spectrumDirty = false;
    
    // Sliding DFT state - using primitive arrays for better performance
    private double[] dftCoefficientsReal;
    private double[] dftCoefficientsImag;
    private double[] window;
    private boolean useWindow = false;
    
    // Thread safety for coefficient access
    private final ReadWriteLock spectrumLock = new ReentrantReadWriteLock();
    
    // Twiddle factors for sliding DFT - using primitive arrays
    private double[] twiddleFactorsReal;
    private double[] twiddleFactorsImag;
    
    /**
     * Create a new streaming DFT transform.
     * 
     * @param config The streaming configuration
     */
    public StreamingDFT(StreamingTransformConfig config) {
        super(new GeneralDiscreteFourierTransform());
        
        this.config = Objects.requireNonNull(config, "Configuration cannot be null");
        this.dft = (GeneralDiscreteFourierTransform) transform;
        
        // Initialize with config values
        initialize(config.getBufferSize(), 0); // maxLevel not used for DFT
    }
    
    /**
     * Enable or disable windowing.
     * When enabled, a Hamming window is applied before DFT computation.
     * 
     * @param useWindow Whether to use windowing
     */
    public void setUseWindow(boolean useWindow) {
        this.useWindow = useWindow;
        if (useWindow && window == null && dftSize > 0) {
            initializeWindow();
        } else if (!useWindow) {
            // Clear window array to free memory when windowing is disabled
            window = null;
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
        // DFT can handle any buffer size (no power-of-2 restriction)
    }
    
    @Override
    protected void initializeTransformState() {
        // Set DFT size to buffer size
        dftSize = bufferSize;
        
        // Initialize spectrum storage (complex interleaved format)
        spectrum = new double[dftSize * 2];
        spectrumDirty = true;  // Mark as dirty so it gets computed on first access
        
        // Initialize sliding DFT state with primitive arrays
        dftCoefficientsReal = new double[dftSize];
        dftCoefficientsImag = new double[dftSize];
        // Arrays are already initialized to 0.0 by Java
        
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
        twiddleFactorsReal = new double[dftSize];
        twiddleFactorsImag = new double[dftSize];
        double angleIncrement = -2.0 * Math.PI / dftSize;
        
        for (int k = 0; k < dftSize; k++) {
            double angle = angleIncrement * k;
            twiddleFactorsReal[k] = Math.cos(angle);
            twiddleFactorsImag[k] = Math.sin(angle);
        }
    }
    
    /**
     * Initialize Hamming window.
     */
    private void initializeWindow() {
        window = new double[dftSize];
        for (int i = 0; i < dftSize; i++) {
            window[i] = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (dftSize - 1));
        }
    }
    
    /**
     * Apply window to signal if windowing is enabled.
     * 
     * @param signal The input signal
     * @return The windowed signal if windowing is enabled, otherwise the original signal
     */
    private double[] applyWindow(double[] signal) {
        if (!useWindow || window == null) {
            // No windowing needed, return original array
            // This is safe because DFT.forward() doesn't modify its input
            return signal;
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
                // Always recompute full DFT immediately
                return recomputeDFT();
                
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
     * Recompute full DFT from current buffer state.
     * This method handles proper locking and delegates to the actual computation.
     */
    private double[] recomputeDFT() {
        spectrumLock.writeLock().lock();
        try {
            // Double-check pattern - another thread may have already recomputed
            if (!spectrumDirty) {
                // Another thread already recomputed, just return the spectrum
                return spectrum;
            }
            
            // Perform the actual DFT computation
            performDFTComputation();
            
            return spectrum;
        } finally {
            spectrumLock.writeLock().unlock();
        }
    }
    
    /**
     * Perform the actual DFT computation and update internal state.
     * This method assumes the caller holds the write lock.
     */
    private void performDFTComputation() {
        double[] bufferData = buffer.toArray();
        
        // Ensure buffer is full size for DFT
        double[] fullSizeData;
        if (bufferData.length < dftSize) {
            // Pad with zeros if buffer is not full
            fullSizeData = new double[dftSize];
            System.arraycopy(bufferData, 0, fullSizeData, 0, bufferData.length);
        } else {
            fullSizeData = bufferData;
        }
        
        double[] processedData = applyWindow(fullSizeData);
        
        double[] newSpectrum;
        try {
            newSpectrum = dft.forward(processedData);
        } catch (jwave.exceptions.JWaveException e) {
            throw new RuntimeException("DFT computation failed", e);
        }
        
        spectrum = newSpectrum;
        spectrumDirty = false;
        
        // Update DFT coefficients for future sliding updates
        for (int k = 0; k < dftSize; k++) {
            dftCoefficientsReal[k] = spectrum[2 * k];
            dftCoefficientsImag[k] = spectrum[2 * k + 1];
        }
    }
    
    /**
     * Perform incremental DFT update using sliding DFT algorithm.
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
        // For large updates, full DFT is more efficient
        int threshold = calculateIncrementalThreshold(dftSize, DFT_INCREMENTAL_THRESHOLD_RATIO);
        if (newSamples.length > threshold || buffer.hasWrapped()) {
            spectrumLock.writeLock().lock();
            try {
                spectrumDirty = true;
            } finally {
                spectrumLock.writeLock().unlock();
            }
            return recomputeDFT();
        }
        
        // If no new samples, return existing spectrum
        if (newSamples.length == 0) {
            return getSpectrum();
        }
        
        spectrumLock.writeLock().lock();
        try {
            // If spectrum is dirty, we need to recompute from scratch first
            if (spectrumDirty) {
                performDFTComputation();
            }
            
            // Get the current buffer data before the new samples were added
            double[] bufferData = buffer.toArray();
            
            // For each new sample, update using sliding DFT
            for (int sampleIdx = 0; sampleIdx < newSamples.length; sampleIdx++) {
                // Calculate which old sample is being removed from the buffer
                RemovedSampleInfo removed = calculateRemovedSample(bufferData, sampleIdx, newSamples.length);
                
                // The new sample being added
                double newSample = newSamples[sampleIdx];
                
                // Apply window if enabled
                if (useWindow && window != null) {
                    // Calculate position of the new sample in the circular buffer
                    int newIdx = calculateNewSampleIndex(sampleIdx, newSamples.length);
                    newSample *= window[newIdx];
                    
                    // Apply window to removed sample if buffer was full
                    if (buffer.size() >= dftSize && removed.isValid) {
                        removed.value *= window[removed.index];
                    }
                }
                
                // Update each frequency bin using sliding DFT with in-place method
                updateSlidingDFTCoefficientsInPlace(
                    dftCoefficientsReal, dftCoefficientsImag,
                    twiddleFactorsReal, twiddleFactorsImag,
                    removed.value, newSample, dftSize);
            }
            
            // Copy coefficients back to spectrum in interleaved format
            for (int k = 0; k < dftSize; k++) {
                spectrum[2 * k] = dftCoefficientsReal[k];
                spectrum[2 * k + 1] = dftCoefficientsImag[k];
            }
            
            spectrumDirty = false;
            return spectrum;
        } finally {
            spectrumLock.writeLock().unlock();
        }
    }
    
    /**
     * Helper class to encapsulate information about a removed sample.
     */
    private static class RemovedSampleInfo {
        final int index;
        double value;
        final boolean isValid;
        
        RemovedSampleInfo(int index, double value, boolean isValid) {
            this.index = index;
            this.value = value;
            this.isValid = isValid;
        }
    }
    
    /**
     * Calculate which sample is being removed when adding new samples.
     * 
     * When the circular buffer is full and we add new samples, old samples
     * are overwritten. This method calculates which sample is being removed
     * for a given position in the new samples array.
     * 
     * @param bufferData Current buffer contents
     * @param sampleIdx Index within the new samples being added (0-based)
     * @param numNewSamples Total number of new samples being added
     * @return Information about the removed sample
     */
    private RemovedSampleInfo calculateRemovedSample(double[] bufferData, int sampleIdx, int numNewSamples) {
        if (buffer.size() < dftSize) {
            // Buffer not full yet, no sample is being removed
            return new RemovedSampleInfo(0, 0.0, false);
        }
        
        // Buffer is full, calculate which sample is being overwritten
        // Use the actual write index from the circular buffer for accuracy
        
        // Get the current write position from the buffer
        int currentWritePos = buffer.getWriteIndex();
        
        // Calculate which sample position will be overwritten by this specific new sample
        // We go back by (numNewSamples - sampleIdx) positions from current write position
        int removedIdx = (currentWritePos - numNewSamples + sampleIdx + dftSize) % dftSize;
        
        return new RemovedSampleInfo(removedIdx, bufferData[removedIdx], true);
    }
    
    /**
     * Calculate the buffer index where a new sample will be placed.
     * 
     * @param sampleIdx Index within the new samples being added (0-based)
     * @param numNewSamples Total number of new samples being added
     * @return Buffer index where the new sample will be placed
     */
    private int calculateNewSampleIndex(int sampleIdx, int numNewSamples) {
        // Position in buffer where this new sample will be placed
        // Use the actual write index for accuracy
        int currentWritePos = buffer.getWriteIndex();
        return ((currentWritePos - numNewSamples + sampleIdx) % dftSize + dftSize) % dftSize;
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
     * @return Magnitude spectrum of length N for real input
     */
    public double[] getMagnitudeSpectrum() {
        return computeSpectralProperty(SpectrumType.MAGNITUDE);
    }
    
    /**
     * Enum for different spectrum computation types.
     */
    private enum SpectrumType {
        MAGNITUDE, POWER, PHASE
    }
    
    /**
     * Common method to compute spectral properties with proper locking.
     * 
     * @param type The type of spectrum to compute
     * @return The computed spectrum array
     */
    private double[] computeSpectralProperty(SpectrumType type) {
        // First check with read lock
        spectrumLock.readLock().lock();
        try {
            if (!spectrumDirty) {
                // Fast path - compute property while holding read lock
                return computeFromSpectrum(spectrum, type);
            }
        } finally {
            spectrumLock.readLock().unlock();
        }
        
        // Spectrum is dirty, need to recompute with write lock
        spectrumLock.writeLock().lock();
        try {
            // Double-check pattern
            if (spectrumDirty) {
                performDFTComputation();
            }
            
            // Compute property while holding write lock
            return computeFromSpectrum(spectrum, type);
        } finally {
            spectrumLock.writeLock().unlock();
        }
    }
    
    /**
     * Extract spectral property from complex spectrum data.
     * 
     * @param spectrum Complex spectrum in interleaved format
     * @param type The type of property to extract
     * @return The extracted property array
     */
    private double[] computeFromSpectrum(double[] spectrum, SpectrumType type) {
        double[] result = new double[dftSize];
        
        for (int i = 0; i < dftSize; i++) {
            double real = spectrum[2 * i];
            double imag = spectrum[2 * i + 1];
            
            switch (type) {
                case MAGNITUDE:
                    result[i] = Math.sqrt(real * real + imag * imag);
                    break;
                case POWER:
                    result[i] = real * real + imag * imag;
                    break;
                case PHASE:
                    result[i] = Math.atan2(imag, real);
                    break;
            }
        }
        
        return result;
    }
    
    /**
     * Get the power spectrum (squared magnitudes).
     * 
     * @return Power spectrum of length N for real input
     */
    public double[] getPowerSpectrum() {
        return computeSpectralProperty(SpectrumType.POWER);
    }
    
    /**
     * Get the phase spectrum.
     * 
     * @return Phase spectrum in radians of length N for real input
     */
    public double[] getPhaseSpectrum() {
        return computeSpectralProperty(SpectrumType.PHASE);
    }
    
    /**
     * Get frequency values for each bin.
     * 
     * @param samplingRate The sampling rate in Hz
     * @return Array of frequency values in Hz
     */
    public double[] getFrequencyBins(double samplingRate) {
        double[] frequencies = new double[dftSize];
        double binWidth = samplingRate / dftSize;
        
        for (int i = 0; i < dftSize; i++) {
            frequencies[i] = i * binWidth;
        }
        
        return frequencies;
    }
    
    @Override
    protected double[] getCachedCoefficients() {
        // First check with read lock (fast path for non-dirty case)
        spectrumLock.readLock().lock();
        try {
            if (!spectrumDirty) {
                // Fast path - spectrum is clean, return it
                return spectrum;
            }
        } finally {
            spectrumLock.readLock().unlock();
        }
        
        // Spectrum is dirty, need to recompute with proper locking
        return recomputeDFT();
    }
    
    @Override
    protected void resetTransformState() {
        spectrumLock.writeLock().lock();
        try {
            // Clear spectrum
            Arrays.fill(spectrum, 0.0);
            
            // Clear DFT coefficients
            Arrays.fill(dftCoefficientsReal, 0.0);
            Arrays.fill(dftCoefficientsImag, 0.0);
            
            spectrumDirty = false;
        } finally {
            spectrumLock.writeLock().unlock();
        }
    }
    
    /**
     * Get the DFT size.
     * 
     * @return DFT size (same as buffer size)
     */
    public int getDFTSize() {
        return dftSize;
    }
    
    /**
     * Compute the dominant frequency in the current spectrum.
     * Note: For DFT, the full spectrum includes both positive and negative frequencies.
     * This method considers only the positive frequencies (0 to N/2).
     * 
     * @param samplingRate The sampling rate in Hz
     * @return The frequency with highest magnitude, or 0 if no clear peak
     */
    public double getDominantFrequency(double samplingRate) {
        double[] magnitude = getMagnitudeSpectrum();
        
        // Find peak magnitude in positive frequencies (skip DC component)
        int peakBin = 0;
        double peakMag = 0.0;
        int maxBin = dftSize / 2; // Only consider positive frequencies
        
        for (int i = 1; i <= maxBin; i++) {
            if (magnitude[i] > peakMag) {
                peakMag = magnitude[i];
                peakBin = i;
            }
        }
        
        // Convert bin to frequency
        return peakBin * samplingRate / dftSize;
    }
    
    /**
     * Estimate the spectral centroid (center of mass of spectrum).
     * Only considers positive frequencies.
     * 
     * @param samplingRate The sampling rate in Hz
     * @return Spectral centroid in Hz
     */
    public double getSpectralCentroid(double samplingRate) {
        double[] magnitude = getMagnitudeSpectrum();
        double[] frequencies = getFrequencyBins(samplingRate);
        
        double weightedSum = 0.0;
        double totalMagnitude = 0.0;
        int maxBin = dftSize / 2; // Only consider positive frequencies
        
        for (int i = 0; i <= maxBin; i++) {
            weightedSum += frequencies[i] * magnitude[i];
            totalMagnitude += magnitude[i];
        }
        
        return totalMagnitude > 0 ? weightedSum / totalMagnitude : 0.0;
    }
}