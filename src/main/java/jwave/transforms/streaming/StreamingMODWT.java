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
    
    // For incremental updates
    private int lastProcessedIndex = -1;
    private double[] previousBufferState;
    
    // Cache for upsampled filters to avoid recomputation
    private double[][] cachedGFilters;
    private double[][] cachedHFilters;
    
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
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
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
        
        // Initialize filter caches for incremental updates
        cachedGFilters = new double[maxLevel + 1][];
        cachedHFilters = new double[maxLevel + 1][];
        precomputeIncrementalFilters();
    }
    
    /**
     * Pre-compute and cache filters for incremental updates.
     */
    private void precomputeIncrementalFilters() {
        // Get base filters
        double[] baseGFilter = normalizeFilter(wavelet.getScalingDeComposition());
        double[] baseHFilter = normalizeFilter(wavelet.getWaveletDeComposition());
        
        // Scale for MODWT
        double scaleFactor = Math.sqrt(2.0);
        for (int i = 0; i < baseGFilter.length; i++) {
            baseGFilter[i] /= scaleFactor;
        }
        for (int i = 0; i < baseHFilter.length; i++) {
            baseHFilter[i] /= scaleFactor;
        }
        
        // Cache upsampled filters for each level
        for (int level = 1; level <= maxLevel; level++) {
            cachedGFilters[level] = upsampleFilter(baseGFilter, level);
            cachedHFilters[level] = upsampleFilter(baseHFilter, level);
        }
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
                // Perform true incremental update
                return performIncrementalUpdate(newSamples);
                
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
        
        // Store state for potential incremental updates
        previousBufferState = bufferData;
        lastProcessedIndex = buffer.size() - 1;
        
        return currentCoefficients;
    }
    
    /**
     * Perform incremental MODWT update for new samples.
     * 
     * This method efficiently updates only the coefficients affected by new samples,
     * using a sliding window approach for circular convolution.
     */
    private double[][] performIncrementalUpdate(double[] newSamples) {
        // First time or buffer wrapped around - need full computation
        if (currentCoefficients == null || lastProcessedIndex < 0 || 
            buffer.hasWrapped() || previousBufferState == null) {
            return recomputeCoefficients();
        }
        
        // Get current buffer state
        double[] currentBufferData = buffer.toArray();
        if (currentBufferData.length < effectiveBufferSize) {
            currentBufferData = Arrays.copyOf(currentBufferData, effectiveBufferSize);
        }
        
        // Calculate how many new samples were actually added
        int currentSize = buffer.size();
        int numNewSamples = currentSize - (lastProcessedIndex + 1);
        
        // If buffer hasn't moved forward, return existing coefficients
        if (numNewSamples <= 0) {
            return currentCoefficients;
        }
        
        // For incremental MODWT, we optimize by only updating affected coefficients
        // Since MODWT is hierarchical, changes propagate through levels, but we can
        // still save computation by limiting the update range at each level
        
        // Start with the input signal
        double[] vCurrent = Arrays.copyOf(currentBufferData, effectiveBufferSize);
        
        // Process each decomposition level
        for (int j = 1; j <= maxLevel; j++) {
            double[] gFilter = cachedGFilters[j];
            double[] hFilter = cachedHFilters[j];
            
            // Calculate the range of coefficients affected at this level
            // The affected range grows with each level due to filter upsampling
            int filterLength = Math.max(gFilter.length, hFilter.length);
            int levelAffectedStart = Math.max(0, effectiveBufferSize - numNewSamples - filterLength + 1);
            
            // Update detail coefficients W_j for the affected range
            updateCoefficientRange(vCurrent, hFilter, currentCoefficients[j - 1], 
                                 levelAffectedStart, effectiveBufferSize);
            
            // Compute approximation coefficients V_j
            double[] vNext = new double[effectiveBufferSize];
            
            // For efficiency, we compute the full approximation at each level
            // This is necessary because V_j feeds into the next level
            for (int n = 0; n < effectiveBufferSize; n++) {
                double sum = 0.0;
                for (int m = 0; m < gFilter.length; m++) {
                    int signalIndex = (n - m + effectiveBufferSize) % effectiveBufferSize;
                    sum += vCurrent[signalIndex] * gFilter[m];
                }
                vNext[n] = sum;
            }
            
            // Store final approximation coefficients
            if (j == maxLevel) {
                currentCoefficients[maxLevel] = vNext;
            }
            
            // Use approximation as input for next level
            vCurrent = vNext;
        }
        
        // Update tracking variables
        previousBufferState = currentBufferData;
        lastProcessedIndex = currentSize - 1;
        coefficientsDirty = false;
        
        return currentCoefficients;
    }
    
    /**
     * Update a range of coefficients using circular convolution.
     */
    private void updateCoefficientRange(double[] input, double[] filter, 
                                      double[] output, int startIndex, int endIndex) {
        int N = input.length;
        int filterLength = filter.length;
        
        for (int n = startIndex; n < endIndex; n++) {
            double sum = 0.0;
            for (int m = 0; m < filterLength; m++) {
                int signalIndex = (n - m + N) % N;
                sum += input[signalIndex] * filter[m];
            }
            output[n] = sum;
        }
    }
    
    
    /**
     * Normalize a filter to have unit energy (L2 norm = 1).
     */
    private double[] normalizeFilter(double[] filter) {
        double[] normalized = Arrays.copyOf(filter, filter.length);
        double energy = 0.0;
        for (double c : normalized) {
            energy += c * c;
        }
        double norm = Math.sqrt(energy);
        if (norm > 1e-12) {
            for (int i = 0; i < normalized.length; i++) {
                normalized[i] /= norm;
            }
        }
        return normalized;
    }
    
    /**
     * Upsample a filter for a specific decomposition level.
     */
    private double[] upsampleFilter(double[] filter, int level) {
        if (level <= 1) return filter;
        
        int gap = (1 << (level - 1)) - 1;
        int newLength = filter.length + (filter.length - 1) * gap;
        
        double[] upsampled = new double[newLength];
        for (int i = 0; i < filter.length; i++) {
            upsampled[i * (gap + 1)] = filter[i];
        }
        return upsampled;
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
        // Note: The buffer is cleared by the parent class's reset() method
        // This method only handles transform-specific state
        currentCoefficients = null;
        coefficientsDirty = false;
        
        // Reset incremental update state
        lastProcessedIndex = -1;
        previousBufferState = null;
        cachedGFilters = null;
        cachedHFilters = null;
        
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