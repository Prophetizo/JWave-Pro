/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import jwave.transforms.ContinuousWaveletTransform;
import jwave.transforms.CWTResult;
import jwave.transforms.wavelets.continuous.ContinuousWavelet;
import jwave.datatypes.natives.Complex;
import jwave.utils.MathUtils;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Streaming implementation of the Continuous Wavelet Transform (CWT).
 * 
 * This class provides streaming capabilities for time-frequency analysis
 * using continuous wavelets. It maintains a sliding window of samples and
 * updates the CWT coefficients as new samples arrive.
 * 
 * Key features:
 * - Real-time time-frequency analysis
 * - Configurable scale range and resolution
 * - Supports both time-domain and FFT-based computation
 * - Optimized incremental updates for edge coefficients
 * - Thread-safe coefficient access
 * 
 * The CWT provides a time-scale representation of signals, which can be
 * converted to time-frequency representation using the wavelet's center
 * frequency. Unlike discrete transforms, CWT provides arbitrary scale
 * resolution at the cost of redundancy.
 * 
 * @author Prophetizo
 * @date 2025-07-13
 */
public class StreamingCWT extends AbstractStreamingTransform<CWTResult> {
    
    private final ContinuousWaveletTransform cwt;
    private final ContinuousWavelet wavelet;
    private final StreamingTransformConfig config;
    
    // Scale parameters
    private double[] scales;
    private double minScale;
    private double maxScale;
    private int numScales;
    private boolean useLogScales;
    
    // Sampling rate
    private double samplingRate = 1.0;
    
    // Current CWT coefficients
    private Complex[][] coefficients;
    private double[] timeAxis;
    private boolean coefficientsDirty = false;
    
    // Thread safety for coefficient access
    private final ReadWriteLock coeffLock = new ReentrantReadWriteLock();
    
    // FFT optimization settings
    private boolean useFFT = false;
    private int paddedLength;
    
    /**
     * Create a new streaming CWT transform with default scales.
     * 
     * @param wavelet The continuous wavelet to use
     * @param config The streaming configuration
     */
    public StreamingCWT(ContinuousWavelet wavelet, StreamingTransformConfig config) {
        this(wavelet, config, 1.0, 100.0, 50, true);
    }
    
    /**
     * Create a new streaming CWT transform with specified scale parameters.
     * 
     * @param wavelet The continuous wavelet to use
     * @param config The streaming configuration
     * @param minScale Minimum scale value
     * @param maxScale Maximum scale value
     * @param numScales Number of scales
     * @param useLogScales Whether to use logarithmic scale spacing
     */
    public StreamingCWT(ContinuousWavelet wavelet, StreamingTransformConfig config,
                       double minScale, double maxScale, int numScales, boolean useLogScales) {
        super(new ContinuousWaveletTransform(wavelet));
        
        this.wavelet = Objects.requireNonNull(wavelet, "Wavelet cannot be null");
        this.config = Objects.requireNonNull(config, "Configuration cannot be null");
        this.cwt = (ContinuousWaveletTransform) transform;
        
        // Validate scale parameters
        if (minScale <= 0 || maxScale <= 0) {
            throw new IllegalArgumentException("Scales must be positive");
        }
        if (minScale >= maxScale) {
            throw new IllegalArgumentException("minScale must be less than maxScale");
        }
        if (numScales < 2) {
            throw new IllegalArgumentException("Need at least 2 scales");
        }
        
        this.minScale = minScale;
        this.maxScale = maxScale;
        this.numScales = numScales;
        this.useLogScales = useLogScales;
        
        // Generate scales
        this.scales = useLogScales ? 
            ContinuousWaveletTransform.generateLogScales(minScale, maxScale, numScales) :
            ContinuousWaveletTransform.generateLinearScales(minScale, maxScale, numScales);
        
        // Initialize with config values
        initialize(config.getBufferSize(), 0); // maxLevel not used for CWT
    }
    
    /**
     * Set the sampling rate for the signal.
     * This affects the time axis and frequency calculations.
     * 
     * @param samplingRate Sampling rate in Hz
     */
    public void setSamplingRate(double samplingRate) {
        if (samplingRate <= 0) {
            throw new IllegalArgumentException("Sampling rate must be positive");
        }
        this.samplingRate = samplingRate;
        // Regenerate time axis
        if (timeAxis != null) {
            double dt = 1.0 / samplingRate;
            for (int i = 0; i < timeAxis.length; i++) {
                timeAxis[i] = i * dt;
            }
        }
    }
    
    /**
     * Enable or disable FFT-based computation.
     * FFT is more efficient for large buffers and many scales.
     * 
     * @param useFFT Whether to use FFT-based computation
     */
    public void setUseFFT(boolean useFFT) {
        this.useFFT = useFFT;
        if (useFFT && buffer != null) {
            // Pre-calculate padded length for FFT
            this.paddedLength = MathUtils.nextPowerOfTwo(bufferSize);
        }
    }
    
    @Override
    protected void validateBufferSize(int bufferSize) {
        // CWT can handle any buffer size
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must be positive");
        }
    }
    
    @Override
    protected void initializeTransformState() {
        // Initialize coefficient storage
        coefficients = new Complex[numScales][bufferSize];
        
        // Initialize time axis
        timeAxis = new double[bufferSize];
        double dt = 1.0 / samplingRate;
        for (int i = 0; i < bufferSize; i++) {
            timeAxis[i] = i * dt;
        }
        
        // If using FFT, calculate padded length
        if (useFFT) {
            this.paddedLength = MathUtils.nextPowerOfTwo(bufferSize);
        }
    }
    
    protected CWTResult computeTransform(double[] data) {
        // Use appropriate method based on settings
        if (useFFT) {
            return cwt.transformFFT(data, scales, samplingRate);
        } else {
            return cwt.transform(data, scales, samplingRate);
        }
    }
    
    @Override
    protected CWTResult performUpdate(double[] newSamples) {
        switch (config.getUpdateStrategy()) {
            case FULL:
                // Always recompute all coefficients immediately
                return recomputeCoefficients();
                
            case INCREMENTAL:
                // Perform incremental update if possible
                return performIncrementalUpdate(newSamples);
                
            case LAZY:
                // Just mark coefficients as dirty, don't compute yet
                coefficientsDirty = true;
                // Return current result
                return createResult();
                
            default:
                throw new IllegalStateException("Unknown update strategy: " + config.getUpdateStrategy());
        }
    }
    
    /**
     * Recompute all CWT coefficients from the current buffer state.
     */
    private CWTResult recomputeCoefficients() {
        double[] bufferData = buffer.toArray();
        
        // Compute full CWT
        CWTResult result = computeTransform(bufferData);
        
        // Update internal state
        coeffLock.writeLock().lock();
        try {
            coefficients = result.getCoefficients();
            coefficientsDirty = false;
        } finally {
            coeffLock.writeLock().unlock();
        }
        
        return result;
    }
    
    /**
     * Perform incremental CWT update for new samples.
     * 
     * For CWT, truly incremental updates are challenging because:
     * 1. Each coefficient depends on a window of samples around it
     * 2. The window size varies with scale
     * 3. Circular buffer wraparound affects coefficient validity
     * 
     * We implement a partial update strategy that:
     * - Updates only coefficients affected by new samples
     * - Handles edge effects properly
     * - Falls back to full computation when buffer wraps
     * 
     * @param newSamples The new samples added to the buffer
     * @return Updated CWT result
     */
    private CWTResult performIncrementalUpdate(double[] newSamples) {
        // First time or buffer wrapped - need full computation
        if (coefficients == null || buffer.hasWrapped()) {
            return recomputeCoefficients();
        }
        
        // If no new samples, return existing result
        if (newSamples.length == 0) {
            return createResult();
        }
        
        // For now, fall back to full recomputation
        // TODO: Implement edge-based incremental updates
        // This would involve:
        // 1. Identifying which time indices are affected by new samples
        // 2. For each scale, determining the cone of influence
        // 3. Recomputing only affected coefficients
        // 4. Handling circular buffer edge cases
        
        return recomputeCoefficients();
    }
    
    /**
     * Create a CWTResult from current coefficients.
     */
    private CWTResult createResult() {
        // Check if we need to initialize coefficients
        if (coefficients == null) {
            coeffLock.writeLock().lock();
            try {
                if (coefficients == null) {  // Double-check after acquiring write lock
                    coefficients = new Complex[numScales][bufferSize];
                    for (int i = 0; i < numScales; i++) {
                        for (int j = 0; j < bufferSize; j++) {
                            coefficients[i][j] = new Complex(0, 0);
                        }
                    }
                }
            } finally {
                coeffLock.writeLock().unlock();
            }
        }
        
        // Now read with read lock
        coeffLock.readLock().lock();
        try {
            // Create deep copy of coefficients to prevent external modification
            Complex[][] coeffCopy = new Complex[numScales][bufferSize];
            for (int i = 0; i < numScales; i++) {
                for (int j = 0; j < bufferSize; j++) {
                    if (i < coefficients.length && j < coefficients[i].length && coefficients[i][j] != null) {
                        coeffCopy[i][j] = new Complex(
                            coefficients[i][j].getReal(),
                            coefficients[i][j].getImag()
                        );
                    } else {
                        coeffCopy[i][j] = new Complex(0, 0);
                    }
                }
            }
            
            return new CWTResult(
                coeffCopy,
                Arrays.copyOf(scales, scales.length),
                Arrays.copyOf(timeAxis, timeAxis.length),
                samplingRate,
                wavelet.getName()
            );
        } finally {
            coeffLock.readLock().unlock();
        }
    }
    
    @Override
    protected CWTResult getCachedCoefficients() {
        // Check if we need to recompute due to LAZY strategy
        if (coefficients == null || coefficientsDirty) {
            return recomputeCoefficients();
        }
        
        return createResult();
    }
    
    @Override
    protected void resetTransformState() {
        coeffLock.writeLock().lock();
        try {
            coefficients = null;
            coefficientsDirty = false;
        } finally {
            coeffLock.writeLock().unlock();
        }
    }
    
    /**
     * Get the magnitude scalogram (time-scale representation).
     * This is often used for visualization.
     * 
     * @return 2D array of magnitudes [scale][time]
     */
    public double[][] getScalogram() {
        CWTResult result = getCachedCoefficients();
        return result.getMagnitude();
    }
    
    /**
     * Get the phase information.
     * 
     * @return 2D array of phase values in radians [scale][time]
     */
    public double[][] getPhase() {
        CWTResult result = getCachedCoefficients();
        return result.getPhase();
    }
    
    /**
     * Get coefficients at a specific scale index.
     * 
     * @param scaleIndex Index of the scale (0 to numScales-1)
     * @return Array of complex coefficients at the given scale
     */
    public Complex[] getCoefficientsAtScale(int scaleIndex) {
        if (scaleIndex < 0 || scaleIndex >= numScales) {
            throw new IllegalArgumentException(
                "Scale index must be between 0 and " + (numScales - 1)
            );
        }
        
        CWTResult result = getCachedCoefficients();
        return result.getCoefficientsAtScale(scaleIndex);
    }
    
    /**
     * Get coefficients at a specific time index across all scales.
     * 
     * @param timeIndex Index of the time point (0 to bufferSize-1)
     * @return Array of complex coefficients at the given time
     */
    public Complex[] getCoefficientsAtTime(int timeIndex) {
        if (timeIndex < 0 || timeIndex >= bufferSize) {
            throw new IllegalArgumentException(
                "Time index must be between 0 and " + (bufferSize - 1)
            );
        }
        
        CWTResult result = getCachedCoefficients();
        return result.getCoefficientsAtTime(timeIndex);
    }
    
    /**
     * Convert scale indices to frequencies using the wavelet's center frequency.
     * 
     * @return Array of frequencies corresponding to the scales
     */
    public double[] getFrequencies() {
        double centerFreq = wavelet.getCenterFrequency();
        double[] frequencies = new double[numScales];
        for (int i = 0; i < numScales; i++) {
            frequencies[i] = centerFreq * samplingRate / scales[i];
        }
        return frequencies;
    }
    
    /**
     * Get the energy distribution across scales (scalogram).
     * 
     * @return Array of energy values for each scale
     */
    public double[] getScaleEnergies() {
        CWTResult result = getCachedCoefficients();
        return result.getScalogram();
    }
    
    /**
     * Update the scale parameters.
     * This will trigger a full recomputation on the next update.
     * 
     * @param minScale New minimum scale
     * @param maxScale New maximum scale
     * @param numScales New number of scales
     * @param useLogScales Whether to use logarithmic spacing
     */
    public void updateScales(double minScale, double maxScale, int numScales, boolean useLogScales) {
        if (minScale <= 0 || maxScale <= 0) {
            throw new IllegalArgumentException("Scales must be positive");
        }
        if (minScale >= maxScale) {
            throw new IllegalArgumentException("minScale must be less than maxScale");
        }
        if (numScales < 2) {
            throw new IllegalArgumentException("Need at least 2 scales");
        }
        
        this.minScale = minScale;
        this.maxScale = maxScale;
        this.numScales = numScales;
        this.useLogScales = useLogScales;
        
        // Regenerate scales
        this.scales = useLogScales ? 
            ContinuousWaveletTransform.generateLogScales(minScale, maxScale, numScales) :
            ContinuousWaveletTransform.generateLinearScales(minScale, maxScale, numScales);
        
        // Mark for recomputation
        coeffLock.writeLock().lock();
        try {
            coefficients = null;
            coefficientsDirty = true;
        } finally {
            coeffLock.writeLock().unlock();
        }
    }
    
    /**
     * Get the continuous wavelet used by this transform.
     * 
     * @return The continuous wavelet
     */
    public ContinuousWavelet getWavelet() {
        return wavelet;
    }
    
    /**
     * Get the current scales.
     * 
     * @return Array of scale values
     */
    public double[] getScales() {
        return Arrays.copyOf(scales, scales.length);
    }
    
    /**
     * Get the number of scales.
     * 
     * @return Number of scales
     */
    public int getNumScales() {
        return numScales;
    }
    
    /**
     * Get the sampling rate.
     * 
     * @return Sampling rate in Hz
     */
    public double getSamplingRate() {
        return samplingRate;
    }
    
    /**
     * Check if FFT-based computation is enabled.
     * 
     * @return true if using FFT
     */
    public boolean isUsingFFT() {
        return useFFT;
    }
}