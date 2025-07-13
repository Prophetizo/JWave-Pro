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
    private volatile double samplingRate = 1.0;
    
    // Current CWT coefficients
    private Complex[][] coefficients;
    private double[] timeAxis;
    private volatile boolean coefficientsDirty = false;
    
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
        // Regenerate time axis with proper synchronization
        coeffLock.writeLock().lock();
        try {
            if (timeAxis != null) {
                double dt = 1.0 / samplingRate;
                for (int i = 0; i < timeAxis.length; i++) {
                    timeAxis[i] = i * dt;
                }
            }
        } finally {
            coeffLock.writeLock().unlock();
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
                coeffLock.writeLock().lock();
                try {
                    coefficientsDirty = true;
                } finally {
                    coeffLock.writeLock().unlock();
                }
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
        // Check if we should fall back to full computation
        if (shouldFallbackToFullComputation(newSamples)) {
            return recomputeCoefficients();
        }
        
        // If no new samples, return existing result
        if (newSamples.length == 0) {
            return createResult();
        }
        
        // Perform selective coefficient updates
        updateAffectedCoefficients(newSamples);
        
        return createResult();
    }
    
    /**
     * Determine if we should fall back to full computation instead of incremental.
     * 
     * @param newSamples The new samples added
     * @return true if full computation is needed
     */
    private boolean shouldFallbackToFullComputation(double[] newSamples) {
        // First time or buffer wrapped - need full computation
        if (coefficients == null || buffer.hasWrapped()) {
            return true;
        }
        
        // For large updates, full recomputation might be more efficient
        if (newSamples.length > bufferSize / 4) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Update only the coefficients affected by new samples.
     * 
     * @param newSamples The new samples added
     */
    private void updateAffectedCoefficients(double[] newSamples) {
        double[] bufferData = buffer.toArray();
        double[] support = wavelet.getEffectiveSupport();
        int newSampleStartIdx = bufferSize - newSamples.length;
        
        coeffLock.writeLock().lock();
        try {
            for (int scaleIdx = 0; scaleIdx < numScales; scaleIdx++) {
                double scale = scales[scaleIdx];
                int supportRadius = calculateSupportRadius(scale, support);
                
                // Update coefficients at this scale
                updateScaleCoefficients(scaleIdx, scale, supportRadius, 
                                      newSampleStartIdx, bufferData, support);
            }
            
            coefficientsDirty = false;
        } finally {
            coeffLock.writeLock().unlock();
        }
    }
    
    /**
     * Calculate the effective support radius for a given scale.
     * 
     * @param scale The scale value
     * @param support The wavelet support bounds
     * @return The support radius in samples
     */
    private int calculateSupportRadius(double scale, double[] support) {
        return (int) Math.ceil(Math.max(Math.abs(support[0]), Math.abs(support[1])) 
                              * scale * samplingRate);
    }
    
    /**
     * Update coefficients for a single scale.
     * 
     * @param scaleIdx Scale index
     * @param scale Scale value
     * @param supportRadius Support radius for this scale
     * @param newSampleStartIdx Starting index of new samples
     * @param bufferData Current buffer data
     * @param support Pre-computed wavelet support bounds
     */
    private void updateScaleCoefficients(int scaleIdx, double scale, int supportRadius,
                                       int newSampleStartIdx, double[] bufferData, 
                                       double[] support) {
        // For very large support, update all coefficients at this scale
        if (supportRadius > bufferSize / 3) {
            updateAllCoefficientsAtScale(scaleIdx, scale, bufferData, support);
            return;
        }
        
        // Update main range of affected coefficients
        updateMainCoefficientRange(scaleIdx, scale, supportRadius, 
                                 newSampleStartIdx, bufferData, support);
        
        // Handle edge effects for wraparound
        updateEdgeCoefficients(scaleIdx, scale, supportRadius, 
                             newSampleStartIdx, bufferData, support);
    }
    
    /**
     * Update all coefficients at a given scale.
     * 
     * @param scaleIdx Scale index
     * @param scale Scale value
     * @param bufferData Current buffer data
     * @param support Pre-computed wavelet support bounds
     */
    private void updateAllCoefficientsAtScale(int scaleIdx, double scale, 
                                            double[] bufferData, double[] support) {
        for (int timeIdx = 0; timeIdx < bufferSize; timeIdx++) {
            coefficients[scaleIdx][timeIdx] = computeCoefficientDirect(
                bufferData, timeIdx, scale, samplingRate, support
            );
        }
    }
    
    /**
     * Update the main range of coefficients affected by new samples.
     * 
     * @param scaleIdx Scale index
     * @param scale Scale value
     * @param supportRadius Support radius for this scale
     * @param newSampleStartIdx Starting index of new samples
     * @param bufferData Current buffer data
     * @param support Pre-computed wavelet support bounds
     */
    private void updateMainCoefficientRange(int scaleIdx, double scale, int supportRadius,
                                          int newSampleStartIdx, double[] bufferData,
                                          double[] support) {
        int startUpdateIdx = Math.max(0, newSampleStartIdx - supportRadius);
        int endUpdateIdx = bufferSize - 1;
        
        for (int timeIdx = startUpdateIdx; timeIdx <= endUpdateIdx; timeIdx++) {
            coefficients[scaleIdx][timeIdx] = computeCoefficientDirect(
                bufferData, timeIdx, scale, samplingRate, support
            );
        }
    }
    
    /**
     * Update edge coefficients that wrap around due to circular buffer.
     * 
     * @param scaleIdx Scale index
     * @param scale Scale value
     * @param supportRadius Support radius for this scale
     * @param newSampleStartIdx Starting index of new samples
     * @param bufferData Current buffer data
     * @param support Pre-computed wavelet support bounds
     */
    private void updateEdgeCoefficients(int scaleIdx, double scale, int supportRadius,
                                      int newSampleStartIdx, double[] bufferData,
                                      double[] support) {
        // Handle coefficients at the beginning whose support extends to new samples
        if (supportRadius > newSampleStartIdx) {
            int startUpdateIdx = Math.max(0, newSampleStartIdx - supportRadius);
            int edgeUpdateCount = supportRadius - newSampleStartIdx;
            
            for (int timeIdx = 0; timeIdx < edgeUpdateCount && timeIdx < bufferSize; timeIdx++) {
                // Skip if already updated in main range
                if (timeIdx >= startUpdateIdx) {
                    break;
                }
                coefficients[scaleIdx][timeIdx] = computeCoefficientDirect(
                    bufferData, timeIdx, scale, samplingRate, support
                );
            }
        }
    }
    
    /**
     * Compute a single CWT coefficient directly.
     * This is more efficient than using the CWT transform for individual coefficients.
     * 
     * @param signal The signal data
     * @param timeIdx Time index for the coefficient
     * @param scale Scale value
     * @param samplingRate Sampling rate
     * @param support Pre-computed wavelet support bounds
     * @return Complex coefficient value
     */
    private Complex computeCoefficientDirect(double[] signal, int timeIdx, double scale, 
                                           double samplingRate, double[] support) {
        Complex sum = new Complex(0, 0);
        double dt = 1.0 / samplingRate;
        
        int minIdx = Math.max(0, timeIdx + (int)(support[0] * scale * samplingRate));
        int maxIdx = Math.min(signal.length - 1, timeIdx + (int)(support[1] * scale * samplingRate));
        
        // Perform convolution
        for (int i = minIdx; i <= maxIdx; i++) {
            double t = (i - timeIdx) * dt;
            Complex waveletValue = wavelet.wavelet(t, scale, 0);
            // Take complex conjugate for convolution
            waveletValue = waveletValue.conjugate();
            sum = sum.add(waveletValue.mul(signal[i]));
        }
        
        // Scale by dt for proper normalization
        return sum.mul(dt);
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
            for (int i = 0; i < numScales && i < coefficients.length; i++) {
                for (int j = 0; j < bufferSize && j < coefficients[i].length; j++) {
                    if (coefficients[i][j] != null) {
                        coeffCopy[i][j] = new Complex(
                            coefficients[i][j].getReal(),
                            coefficients[i][j].getImag()
                        );
                    } else {
                        coeffCopy[i][j] = new Complex(0, 0);
                    }
                }
                // Fill remaining with zeros if needed
                for (int j = coefficients[i].length; j < bufferSize; j++) {
                    coeffCopy[i][j] = new Complex(0, 0);
                }
            }
            // Fill remaining scales with zeros if needed
            for (int i = coefficients.length; i < numScales; i++) {
                for (int j = 0; j < bufferSize; j++) {
                    coeffCopy[i][j] = new Complex(0, 0);
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