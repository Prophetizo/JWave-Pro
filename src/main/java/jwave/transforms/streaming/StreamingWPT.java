/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import jwave.transforms.WaveletPacketTransform;
import jwave.transforms.wavelets.Wavelet;
import jwave.exceptions.JWaveException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Streaming implementation of the Wavelet Packet Transform (WPT).
 * 
 * This class provides streaming capabilities for Wavelet Packet Decomposition
 * by maintaining a power-of-2 sized buffer and performing updates as new 
 * samples arrive.
 * 
 * Key features:
 * - Full binary tree decomposition (both approximation and detail at each level)
 * - Maintains power-of-2 buffer requirement for WPT
 * - Multi-level decomposition with packet-based structure
 * - Packet energy analysis and time-based path traversal
 * - Optimized for real-time signal processing
 * 
 * The WPT differs from FWT by decomposing both approximation and detail
 * coefficients at each level, creating a complete binary tree of coefficients.
 * This provides a richer time-frequency representation but at higher
 * computational cost.
 * 
 * IMPORTANT: The INCREMENTAL update strategy currently falls back to full
 * recomputation due to the complexity of updating the entire packet tree.
 * This means INCREMENTAL and FULL strategies have identical performance.
 * True incremental updates for WPT remain a future optimization.
 * 
 * @author Prophetizo
 * @date 2025-07-13
 */
public class StreamingWPT extends AbstractStreamingTransform<double[]> {
    
    private final WaveletPacketTransform wpt;
    private final Wavelet wavelet;
    private final StreamingTransformConfig config;
    private double[] currentCoefficients;
    private int effectiveBufferSize;
    private boolean coefficientsDirty = false;
    
    // Cache for packet structure at each level
    private int[] packetSizes;  // Size of each packet at each level
    private int[] packetCounts; // Number of packets at each level
    
    /**
     * Create a new streaming WPT transform.
     * 
     * @param wavelet The wavelet to use for the transform
     * @param config The streaming configuration
     */
    public StreamingWPT(Wavelet wavelet, StreamingTransformConfig config) {
        super(new WaveletPacketTransform(wavelet));
        
        this.wavelet = Objects.requireNonNull(wavelet, "Wavelet cannot be null");
        this.config = Objects.requireNonNull(config, "Configuration cannot be null");
        this.wpt = (WaveletPacketTransform) transform;
        
        // Initialize with config values
        initialize(config.getBufferSize(), config.getMaxLevel());
    }
    
    @Override
    protected void validateBufferSize(int bufferSize) {
        // WPT requires power-of-2, but we handle this internally
        // by padding to effectiveBufferSize
    }
    
    @Override
    protected void initializeTransformState() {
        // Override with power-of-2 size for WPT
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
        this.currentCoefficients = new double[effectiveBufferSize];
        
        // Pre-calculate packet structure for efficient access
        initializePacketStructure();
    }
    
    /**
     * Initialize the packet structure arrays for efficient coefficient access.
     * For WPT, at each level L:
     * - Number of packets = 2^L
     * - Size of each packet = N / 2^L
     */
    private void initializePacketStructure() {
        packetSizes = new int[maxLevel + 1];
        packetCounts = new int[maxLevel + 1];
        
        for (int level = 0; level <= maxLevel; level++) {
            packetCounts[level] = 1 << level;  // 2^level
            packetSizes[level] = effectiveBufferSize >> level;  // N / 2^level
        }
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
            // Perform WPT on the buffer data
            return wpt.forward(data, maxLevel);
        } catch (JWaveException e) {
            // Convert JWaveException to Exception for listener notification
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
        
        // For WPT, we need exactly effectiveBufferSize samples
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
     * Perform incremental WPT update for new samples.
     * 
     * IMPORTANT: Due to the full binary tree structure of WPT where every node
     * is decomposed (not just approximations), implementing truly incremental
     * updates is extremely complex with minimal performance benefit. Currently,
     * this method falls back to full recomputation, making INCREMENTAL behave
     * the same as FULL strategy.
     * 
     * For WPT, incremental updates face these challenges:
     * 1. We need to update the entire packet tree, not just approximation branch
     * 2. Each packet at level L affects two packets at level L+1
     * 3. The full binary tree structure means O(N) coefficients to update
     * 4. Boundary effects propagate through all decomposition levels
     * 
     * Future optimization opportunities:
     * - Track which packets are affected by new samples
     * - Use boundary wavelets for localized updates
     * - Implement packet-wise lazy evaluation
     * - Cache intermediate packet decompositions
     * 
     * @param newSamples The new samples added to the buffer
     * @return Updated wavelet packet coefficients
     */
    private double[] performIncrementalUpdate(double[] newSamples) {
        // First time or buffer wrapped - need full computation
        if (currentCoefficients == null || buffer.hasWrapped()) {
            return recomputeCoefficients();
        }
        
        // If we have new samples, the buffer has changed
        if (newSamples.length > 0) {
            // TODO: Implement true incremental WPT update
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
     * Get a specific packet from the WPT decomposition.
     * 
     * In WPT, packets are indexed by (level, position):
     * - Level 0: 1 packet (the original signal)
     * - Level 1: 2 packets [A1, D1]
     * - Level 2: 4 packets [AA2, AD2, DA2, DD2]
     * - Level 3: 8 packets [AAA3, AAD3, ADA3, ADD3, DAA3, DAD3, DDA3, DDD3]
     * 
     * @param level The decomposition level (0 to maxLevel)
     * @param packetIndex The packet index at that level (0 to 2^level - 1)
     * @return The packet coefficients
     * @throws IllegalArgumentException if level or packetIndex is out of range
     */
    public double[] getPacket(int level, int packetIndex) {
        if (level < 0 || level > maxLevel) {
            throw new IllegalArgumentException(
                "Level must be between 0 and " + maxLevel
            );
        }
        
        int packetsAtLevel = packetCounts[level];
        if (packetIndex < 0 || packetIndex >= packetsAtLevel) {
            throw new IllegalArgumentException(
                "Packet index must be between 0 and " + (packetsAtLevel - 1) + 
                " for level " + level
            );
        }
        
        double[] coeffs = getCachedCoefficients();
        
        // Calculate packet location in the coefficient array
        int packetSize = packetSizes[level];
        int packetOffset = packetIndex * packetSize;
        
        // Extract the packet
        double[] packet = new double[packetSize];
        System.arraycopy(coeffs, packetOffset, packet, 0, packetSize);
        
        return packet;
    }
    
    /**
     * Get all packets at a specific decomposition level.
     * 
     * @param level The decomposition level (0 to maxLevel)
     * @return Array of all packets at the specified level
     * @throws IllegalArgumentException if level is out of range
     */
    public double[][] getAllPacketsAtLevel(int level) {
        if (level < 0 || level > maxLevel) {
            throw new IllegalArgumentException(
                "Level must be between 0 and " + maxLevel
            );
        }
        
        // Get coefficients once to avoid repeated computation/copying
        double[] coeffs = getCachedCoefficients();
        
        int packetsAtLevel = packetCounts[level];
        int packetSize = packetSizes[level];
        double[][] packets = new double[packetsAtLevel][];
        
        // Extract all packets at this level efficiently
        for (int i = 0; i < packetsAtLevel; i++) {
            int packetOffset = i * packetSize;
            packets[i] = new double[packetSize];
            System.arraycopy(coeffs, packetOffset, packets[i], 0, packetSize);
        }
        
        return packets;
    }
    
    /**
     * Get the packet tree path for a specific time index.
     * This returns the sequence of packets from root to leaf that
     * contain the specified time point.
     * 
     * In WPT, each decomposition creates a binary tree where:
     * - At each level, a packet is split into two sub-packets
     * - The path through the tree is determined by the time location
     * - Each packet covers a specific time interval
     * 
     * @param timeIndex The time index (0 to effectiveBufferSize - 1)
     * @return Array of packets from root to leaf, one per level
     */
    public double[][] getPacketPath(int timeIndex) {
        if (timeIndex < 0 || timeIndex >= effectiveBufferSize) {
            throw new IllegalArgumentException(
                "Time index must be between 0 and " + (effectiveBufferSize - 1)
            );
        }
        
        double[][] path = new double[maxLevel + 1][];
        
        // Start at root (level 0)
        path[0] = getPacket(0, 0);
        
        // Traverse down the tree based on time location
        int currentIndex = timeIndex;
        
        for (int level = 1; level <= maxLevel; level++) {
            // At each level, determine which packet contains this time index
            int packetSize = packetSizes[level];
            int packetIndex = currentIndex / packetSize;
            
            path[level] = getPacket(level, packetIndex);
            
            // Update index relative to the current packet
            currentIndex %= packetSize;
        }
        
        return path;
    }
    
    /**
     * Reconstruct signal from coefficients up to a specified level.
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
            return wpt.reverse(coeffs, level);
        } catch (JWaveException e) {
            // Convert JWaveException to Exception for listener notification
            notifyError(new Exception(e.getMessage(), e), false);
            return new double[effectiveBufferSize];
        }
    }
    
    /**
     * Get the energy distribution across packets at a specific level.
     * This is useful for analyzing which time-frequency regions contain
     * the most signal energy.
     * 
     * @param level The decomposition level
     * @return Array of energy values for each packet at the level
     */
    public double[] getPacketEnergies(int level) {
        if (level < 0 || level > maxLevel) {
            throw new IllegalArgumentException(
                "Level must be between 0 and " + maxLevel
            );
        }
        
        // Get coefficients once to avoid repeated computation/copying
        double[] coeffs = getCachedCoefficients();
        
        int packetsAtLevel = packetCounts[level];
        int packetSize = packetSizes[level];
        double[] energies = new double[packetsAtLevel];
        
        // Calculate energy for each packet directly from coefficients
        for (int i = 0; i < packetsAtLevel; i++) {
            double energy = 0.0;
            int packetOffset = i * packetSize;
            int packetEnd = packetOffset + packetSize;
            
            for (int j = packetOffset; j < packetEnd; j++) {
                energy += coeffs[j] * coeffs[j];
            }
            energies[i] = energy;
        }
        
        return energies;
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
    
    /**
     * Get the number of packets at a specific level.
     * 
     * @param level The decomposition level
     * @return Number of packets (2^level)
     */
    public int getPacketCount(int level) {
        if (level < 0 || level > maxLevel) {
            throw new IllegalArgumentException(
                "Level must be between 0 and " + maxLevel
            );
        }
        return packetCounts[level];
    }
    
    /**
     * Get the size of each packet at a specific level.
     * 
     * @param level The decomposition level
     * @return Size of each packet (N / 2^level)
     */
    public int getPacketSize(int level) {
        if (level < 0 || level > maxLevel) {
            throw new IllegalArgumentException(
                "Level must be between 0 and " + maxLevel
            );
        }
        return packetSizes[level];
    }
}