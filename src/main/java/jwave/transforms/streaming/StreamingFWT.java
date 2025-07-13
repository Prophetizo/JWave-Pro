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
import jwave.exceptions.JWaveException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Streaming implementation of the Fast Wavelet Transform (FWT).
 * 
 * This class provides streaming capabilities for the standard Discrete Wavelet
 * Transform (DWT) by maintaining a power-of-2 sized buffer and performing
 * updates as new samples arrive.
 * 
 * Key features:
 * - Maintains power-of-2 buffer requirement for FWT
 * - Multi-level decomposition support
 * - Optimized for real-time signal processing
 * 
 * The FWT decomposes a signal into approximation and detail coefficients
 * at multiple scales, with each level halving the number of coefficients.
 * 
 * IMPORTANT: The INCREMENTAL update strategy currently falls back to full
 * recomputation. This means INCREMENTAL and FULL strategies have identical
 * performance. True incremental updates for FWT remain a future optimization.
 * 
 * @author Prophetizo
 * @date 2025-07-12
 */
public class StreamingFWT extends AbstractStreamingTransform<double[]> {
    
    private final FastWaveletTransform fwt;
    private final Wavelet wavelet;
    private final StreamingTransformConfig config;
    private double[] currentCoefficients;
    private int effectiveBufferSize;
    private boolean coefficientsDirty = false;
    
    
    /**
     * Create a new streaming FWT transform.
     * 
     * @param wavelet The wavelet to use for the transform
     * @param config The streaming configuration
     */
    public StreamingFWT(Wavelet wavelet, StreamingTransformConfig config) {
        super(new FastWaveletTransform(wavelet));
        
        this.wavelet = Objects.requireNonNull(wavelet, "Wavelet cannot be null");
        this.config = Objects.requireNonNull(config, "Configuration cannot be null");
        this.fwt = (FastWaveletTransform) transform;
        
        // Initialize with config values
        // Note: effectiveBufferSize will be calculated in initializeTransformState()
        initialize(config.getBufferSize(), config.getMaxLevel());
    }
    
    @Override
    protected void validateBufferSize(int bufferSize) {
        // FWT requires power-of-2, but we handle this internally
        // by padding to effectiveBufferSize
    }
    
    @Override
    protected void initializeTransformState() {
        // Override with power-of-2 size for FWT
        this.effectiveBufferSize = getNextPowerOfTwo(bufferSize);
        
        // Validate that maxLevel is appropriate for buffer size
        int maxPossibleLevel = calculateMaxLevel(effectiveBufferSize);
        if (maxLevel > maxPossibleLevel) {
            throw new IllegalArgumentException(
                "Max level " + maxLevel + " exceeds maximum possible level " + 
                maxPossibleLevel + " for buffer size " + effectiveBufferSize
            );
        }
        
        // Initialize coefficient storage
        // FWT produces a single array with interleaved coefficients
        this.currentCoefficients = new double[effectiveBufferSize];
    }
    
    /**
     * Calculate the maximum decomposition level for a given buffer size.
     * 
     * @param size The buffer size (must be power of 2)
     * @return Maximum decomposition level
     */
    private int calculateMaxLevel(int size) {
        if (size <= 0) return 0;
        // log2(size) = 31 - numberOfLeadingZeros(size) for powers of 2
        return 31 - Integer.numberOfLeadingZeros(size);
    }
    
    protected double[] computeTransform(double[] data) {
        try {
            // Perform FWT on the buffer data
            return fwt.forward(data, maxLevel);
        } catch (JWaveException e) {
            // Convert JWaveException (extends Throwable) to Exception for listener notification
            notifyError(new Exception(e.getMessage(), e), false);
            // Return zeros on error
            return new double[data.length];
        }
    }
    
    @Override
    protected double[] performUpdate(double[] newSamples) {
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
                // Return current (possibly stale) coefficients
                return currentCoefficients != null ? currentCoefficients : 
                       new double[effectiveBufferSize];
                       
            default:
                // Defensive programming: handle any future enum values
                throw new IllegalStateException("Unknown update strategy: " + config.getUpdateStrategy());
        }
    }
    
    /**
     * Recompute coefficients from the current buffer state.
     */
    private double[] recomputeCoefficients() {
        // Get current buffer data
        double[] bufferData = buffer.toArray();
        
        // For FWT, we need exactly effectiveBufferSize samples
        double[] transformData;
        if (bufferData.length >= effectiveBufferSize) {
            // If we have enough data, use the most recent effectiveBufferSize samples
            int offset = bufferData.length - effectiveBufferSize;
            transformData = Arrays.copyOfRange(bufferData, offset, bufferData.length);
        } else {
            // If we don't have enough data, pad with zeros at the beginning
            transformData = new double[effectiveBufferSize];
            int offset = effectiveBufferSize - bufferData.length;
            System.arraycopy(bufferData, 0, transformData, offset, bufferData.length);
        }
        
        currentCoefficients = computeTransform(transformData);
        coefficientsDirty = false;
        
        return currentCoefficients;
    }
    
    /**
     * Perform incremental FWT update for new samples.
     * 
     * IMPORTANT: Due to the recursive nature of FWT, implementing truly incremental
     * updates is complex with limited performance benefit. Currently, this method 
     * falls back to full recomputation, making INCREMENTAL behave the same as 
     * FULL strategy.
     * 
     * For FWT, incremental updates face these challenges:
     * 1. The transform is recursive with each level depending on the previous
     * 2. Changes propagate through all decomposition levels
     * 3. The dyadic structure means updates affect multiple coefficients
     * 4. Boundary effects propagate through the transform
     * 
     * Future optimization opportunities:
     * - Lifting scheme implementation for in-place updates
     * - Boundary wavelets for localized updates
     * - Lazy evaluation of unaffected coefficient blocks
     * - Cache intermediate decomposition results
     * 
     * @param newSamples The new samples added to the buffer
     * @return Updated wavelet coefficients
     */
    private double[] performIncrementalUpdate(double[] newSamples) {
        // First time or buffer wrapped - need full computation
        if (currentCoefficients == null || buffer.hasWrapped()) {
            return recomputeCoefficients();
        }
        
        // If we have new samples, the buffer has changed
        if (newSamples.length > 0) {
            // TODO: Implement true incremental FWT update
            // Currently falls back to full recomputation
            return recomputeCoefficients();
        }
        
        // No new samples, return existing coefficients
        return currentCoefficients;
    }
    
    
    @Override
    protected double[] getCachedCoefficients() {
        // Check if we need to recompute due to LAZY strategy
        if (currentCoefficients == null || coefficientsDirty) {
            recomputeCoefficients();
        }
        
        // Return a copy to prevent external modification
        return Arrays.copyOf(currentCoefficients, currentCoefficients.length);
    }
    
    @Override
    protected void resetTransformState() {
        // Note: The buffer is cleared by the parent class's reset() method
        // This method only handles transform-specific state
        currentCoefficients = null;
        coefficientsDirty = false;
    }
    
    /**
     * Get coefficients at a specific decomposition level.
     * 
     * The FWT stores coefficients in a hierarchical pattern:
     * For a signal of length N:
     * - Level 1: [A1 (N/2) | D1 (N/2)]
     * - Level 2: [A2 (N/4) | D2 (N/4) | D1 (N/2)]
     * - Level 3: [A3 (N/8) | D3 (N/8) | D2 (N/4) | D1 (N/2)]
     * 
     * At each level, we decompose the approximation from the previous level.
     * 
     * @param level The decomposition level (1 to maxLevel)
     * @return Array containing [approximation, detail] coefficients at the level
     * @throws IllegalArgumentException if level is out of range
     */
    public double[][] getCoefficientsAtLevel(int level) {
        if (level < 1 || level > maxLevel) {
            throw new IllegalArgumentException(
                "Level must be between 1 and " + maxLevel
            );
        }
        
        double[] coeffs = getCachedCoefficients();
        
        // For FWT, at level L:
        // - Approximation coefficients are at indices [0, N/2^L)
        // - Detail coefficients are at indices [N/2^L, N/2^(L-1))
        
        int approxSize = effectiveBufferSize >> level;  // N/2^L
        
        // Approximation is always at the beginning
        double[] approximation = new double[approxSize];
        System.arraycopy(coeffs, 0, approximation, 0, approxSize);
        
        // Detail coefficients start right after approximation
        double[] detail = new double[approxSize];
        System.arraycopy(coeffs, approxSize, detail, 0, approxSize);
        
        return new double[][] { approximation, detail };
    }
    
    /**
     * Reconstruct signal from coefficients up to a specified level.
     * 
     * This allows for multi-resolution analysis by reconstructing
     * only up to a certain decomposition level.
     * 
     * @param level The level up to which to reconstruct (0 = full reconstruction)
     * @return Reconstructed signal
     * @throws IllegalArgumentException if level is out of range
     */
    public double[] reconstruct(int level) {
        if (level < 0 || level > maxLevel) {
            throw new IllegalArgumentException(
                "Level must be between 0 and " + maxLevel
            );
        }
        
        double[] coeffs = getCachedCoefficients();
        
        try {
            return fwt.reverse(coeffs, level);
        } catch (JWaveException e) {
            // Convert JWaveException (extends Throwable) to Exception for listener notification
            notifyError(new Exception(e.getMessage(), e), false);
            return new double[effectiveBufferSize];
        }
    }
    
    /**
     * Get the next power of two greater than or equal to n.
     * 
     * @param n The input value (must be positive)
     * @return The next power of two
     * @throws IllegalArgumentException if n is not positive
     */
    private static int getNextPowerOfTwo(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("Buffer size must be positive, got: " + n);
        }
        if (n == 1) {
            return 1;
        }
        
        // Find the next power of 2
        // For efficiency, use bit manipulation
        // This handles overflow by returning Integer.MIN_VALUE (which is negative)
        int power = Integer.highestOneBit(n);
        if (power < n) {
            power <<= 1;
        }
        
        // Check for overflow
        if (power < 0) {
            throw new IllegalArgumentException("Buffer size too large, would overflow: " + n);
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
    
    /**
     * Check if the buffer size matches the effective size.
     * 
     * @return true if no padding is needed
     */
    public boolean isPowerOfTwo() {
        return bufferSize == effectiveBufferSize;
    }
}