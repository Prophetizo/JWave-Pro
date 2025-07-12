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
import java.util.Arrays;

/**
 * Streaming implementation of the Maximal Overlap Discrete Wavelet Transform (MODWT).
 * 
 * This class wraps the standard MODWT transform to provide incremental processing
 * capabilities using a circular buffer. It maintains translation invariance while
 * supporting real-time and continuous data analysis.
 * 
 * Features:
 * - Incremental coefficient updates as new samples arrive
 * - Efficient circular buffer management
 * - Support for multiple decomposition levels
 * - Maintains MODWT's shift-invariance property
 * 
 * @author Prophetizo
 * @date 2025-07-12
 */
public class StreamingMODWT extends AbstractStreamingTransform<double[][]> {
    
    private final MODWTTransform modwt;
    private final Wavelet wavelet;
    private final StreamingTransformConfig config;
    private double[][] currentCoefficients;
    private int effectiveBufferSize;
    private boolean coefficientsDirty = false;
    
    /**
     * Create a new streaming MODWT transform.
     * 
     * @param wavelet The wavelet to use for the transform
     * @param config The streaming configuration
     */
    public StreamingMODWT(Wavelet wavelet, StreamingTransformConfig config) {
        super(new MODWTTransform(wavelet));
        
        if (wavelet == null) {
            throw new IllegalArgumentException("Wavelet cannot be null");
        }
        
        this.wavelet = wavelet;
        this.config = config;
        this.modwt = (MODWTTransform) transform;
        
        // For MODWT, we need a power-of-2 buffer size
        this.effectiveBufferSize = getNextPowerOfTwo(config.getBufferSize());
        
        // Initialize with config values
        initialize(config.getBufferSize(), config.getMaxLevel());
    }
    
    @Override
    protected void validateBufferSize(int bufferSize) {
        // MODWT requires power-of-2 for the underlying implementation
        // But we'll handle this internally by using effectiveBufferSize
    }
    
    @Override
    protected void initializeTransformState() {
        // Override with power-of-2 size for MODWT
        this.effectiveBufferSize = getNextPowerOfTwo(bufferSize);
        
        // Initialize coefficient storage
        // MODWT produces maxLevel + 1 arrays (details D1...Dj + approximation Aj)
        this.currentCoefficients = new double[maxLevel + 1][effectiveBufferSize];
        
        // Pre-compute filters for efficiency
        modwt.precomputeFilters(maxLevel);
    }
    
    protected double[][] computeTransform(double[] data) {
        try {
            // Perform MODWT on the buffer data
            return modwt.forwardMODWT(data, maxLevel);
        } catch (Exception e) {
            notifyError(e, false);
            // Return zeros on error
            double[][] zeros = new double[maxLevel + 1][data.length];
            return zeros;
        }
    }
    
    @Override
    protected double[][] performUpdate(double[] newSamples) {
        switch (config.getUpdateStrategy()) {
            case FULL:
                // Always recompute all coefficients immediately
                return recomputeCoefficients();
                
            case INCREMENTAL:
                // TODO: Implement true incremental MODWT update
                // This would require:
                // 1. Tracking which coefficients are affected by new samples
                // 2. Computing only the boundary effects for each level
                // 3. Updating circular convolution results incrementally
                // For now, fall back to full recomputation
                return recomputeCoefficients();
                
            case LAZY:
                // Just mark coefficients as dirty, don't compute yet
                coefficientsDirty = true;
                // Return current (possibly stale) coefficients
                return currentCoefficients != null ? currentCoefficients : 
                       new double[maxLevel + 1][effectiveBufferSize];
        }
        
        return currentCoefficients;
    }
    
    /**
     * Recompute coefficients from the current buffer state.
     */
    private double[][] recomputeCoefficients() {
        // Get current buffer data with power-of-2 padding if needed
        double[] bufferData = buffer.toArray();
        if (bufferData.length < effectiveBufferSize) {
            // Pad with zeros to reach power-of-2 size
            bufferData = Arrays.copyOf(bufferData, effectiveBufferSize);
        }
        
        currentCoefficients = computeTransform(bufferData);
        coefficientsDirty = false;
        return currentCoefficients;
    }
    
    @Override
    protected double[][] getCachedCoefficients() {
        // Check if we need to recompute due to LAZY strategy
        if (currentCoefficients == null || coefficientsDirty) {
            recomputeCoefficients();
        }
        
        // Return a copy to prevent external modification
        double[][] copy = new double[currentCoefficients.length][];
        for (int i = 0; i < currentCoefficients.length; i++) {
            copy[i] = Arrays.copyOf(currentCoefficients[i], currentCoefficients[i].length);
        }
        return copy;
    }
    
    @Override
    protected void resetTransformState() {
        currentCoefficients = null;
        coefficientsDirty = false;
        // Clear MODWT filter cache to free memory
        modwt.clearFilterCache();
    }
    
    /**
     * Get detail coefficients at a specific level.
     * 
     * @param level The decomposition level (1 to maxLevel)
     * @return The detail coefficients at the specified level
     * @throws IllegalArgumentException if level is out of range
     */
    public double[] getDetailCoefficients(int level) {
        if (level < 1 || level > maxLevel) {
            throw new IllegalArgumentException(
                "Level must be between 1 and " + maxLevel
            );
        }
        
        double[][] coeffs = getCachedCoefficients();
        return Arrays.copyOf(coeffs[level - 1], coeffs[level - 1].length);
    }
    
    /**
     * Get approximation coefficients at the maximum level.
     * 
     * @return The approximation coefficients
     */
    public double[] getApproximationCoefficients() {
        double[][] coeffs = getCachedCoefficients();
        int lastIndex = maxLevel;
        return Arrays.copyOf(coeffs[lastIndex], coeffs[lastIndex].length);
    }
    
    /**
     * Compute multi-resolution analysis components.
     * 
     * @return Array where each element is a signal component at different scales
     */
    public double[][] computeMRA() {
        // For MRA, we need to inverse transform each level individually
        double[][] coeffs = getCachedCoefficients();
        double[][] mra = new double[coeffs.length][];
        
        // Reconstruct each component
        for (int level = 0; level < coeffs.length; level++) {
            // Create temporary coefficient array with only the current level non-zero
            double[][] tempCoeffs = new double[coeffs.length][];
            for (int i = 0; i < coeffs.length; i++) {
                if (i == level) {
                    tempCoeffs[i] = Arrays.copyOf(coeffs[i], coeffs[i].length);
                } else {
                    tempCoeffs[i] = new double[coeffs[i].length];
                }
            }
            
            // Reconstruct this component
            try {
                mra[level] = modwt.inverseMODWT(tempCoeffs);
            } catch (Exception e) {
                notifyError(e, false);
                mra[level] = new double[effectiveBufferSize];
            }
        }
        
        return mra;
    }
    
    /**
     * Get the next power of two greater than or equal to n.
     * 
     * @param n The input value
     * @return The next power of two
     */
    private static int getNextPowerOfTwo(int n) {
        if (n <= 1) return 1;
        int power = 1;
        while (power < n) {
            power <<= 1;
        }
        return power;
    }
    
    /**
     * Get the wavelet used by this transform.
     * 
     * @return The wavelet
     */
    public Wavelet getWavelet() {
        return wavelet;
    }
    
    /**
     * Get the effective buffer size (power of 2).
     * 
     * @return The effective buffer size
     */
    public int getEffectiveBufferSize() {
        return effectiveBufferSize;
    }
}