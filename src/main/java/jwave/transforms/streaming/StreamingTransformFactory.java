/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import jwave.transforms.BasicTransform;
import jwave.transforms.FastWaveletTransform;
import jwave.transforms.wavelets.Wavelet;

/**
 * Factory for creating streaming transform instances.
 * 
 * This factory provides a convenient way to create streaming wrappers
 * for various transform types with appropriate configuration.
 * 
 * @author Prophetizo
 * @date 2025-07-12
 */
public class StreamingTransformFactory {
    
    // Buffer size constants
    private static final int FWT_LEVEL_BUFFER_FACTOR = 3;  // Extra levels for boundary handling
    private static final int FWT_MIN_BUFFER_POWER = 8;     // Minimum 2^8 = 256 samples
    private static final int MODWT_SAMPLES_PER_LEVEL = 128;
    private static final int MODWT_MIN_BUFFER_SIZE = 512;
    private static final int CWT_SAMPLES_PER_SCALE = 64;
    private static final int CWT_MIN_BUFFER_SIZE = 256;
    private static final int FFT_MIN_BUFFER_POWER = 10;    // Minimum 2^10 = 1024 samples
    private static final int MAX_BUFFER_POWER = 30;        // Maximum 2^30 = ~1 billion samples
    
    /**
     * Supported transform types for streaming.
     */
    public enum TransformType {
        /** Fast Wavelet Transform (DWT) */
        FWT,
        /** Wavelet Packet Transform */
        WPT,
        /** Maximal Overlap Discrete Wavelet Transform */
        MODWT,
        /** Continuous Wavelet Transform */
        CWT,
        /** Fast Fourier Transform */
        FFT,
        /** Discrete Fourier Transform */
        DFT
    }
    
    /**
     * Create a streaming transform with the specified configuration.
     * 
     * @param type The type of transform to create
     * @param wavelet The wavelet to use (may be null for FFT/DFT)
     * @param config The streaming configuration
     * @return A configured streaming transform instance
     * @throws IllegalArgumentException if parameters are invalid
     * @throws UnsupportedOperationException if transform type not yet implemented
     */
    public static StreamingTransform<?> create(
            TransformType type, 
            Wavelet wavelet, 
            StreamingTransformConfig config) {
        
        if (type == null) {
            throw new IllegalArgumentException("Transform type cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        
        // Validate wavelet requirement
        if (requiresWavelet(type) && wavelet == null) {
            throw new IllegalArgumentException(
                "Transform type " + type + " requires a wavelet"
            );
        }
        
        // Create appropriate streaming wrapper
        switch (type) {
            case FWT:
                throw new UnsupportedOperationException(
                    "Streaming FWT not yet implemented"
                );
                
            case WPT:
                throw new UnsupportedOperationException(
                    "Streaming WPT not yet implemented"
                );
                
            case MODWT:
                throw new UnsupportedOperationException(
                    "Streaming MODWT not yet implemented"
                );
                
            case CWT:
                throw new UnsupportedOperationException(
                    "Streaming CWT not yet implemented"
                );
                
            case FFT:
            case DFT:
                throw new UnsupportedOperationException(
                    "Streaming " + type + " not yet implemented"
                );
                
            default:
                throw new IllegalArgumentException(
                    "Unknown transform type: " + type
                );
        }
    }
    
    /**
     * Create a streaming transform with default configuration.
     * 
     * @param type The type of transform to create
     * @param wavelet The wavelet to use (may be null for FFT/DFT)
     * @param bufferSize The size of the circular buffer
     * @return A configured streaming transform instance
     */
    public static StreamingTransform<?> create(
            TransformType type,
            Wavelet wavelet,
            int bufferSize) {
        
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(bufferSize)
            .build();
            
        return create(type, wavelet, config);
    }
    
    /**
     * Check if a transform type requires a wavelet.
     * 
     * @param type The transform type
     * @return true if the transform requires a wavelet
     */
    private static boolean requiresWavelet(TransformType type) {
        switch (type) {
            case FWT:
            case WPT:
            case MODWT:
            case CWT:
                return true;
            case FFT:
            case DFT:
                return false;
            default:
                return false;
        }
    }
    
    /**
     * Calculate a power-of-2 buffer size with overflow protection.
     * 
     * @param desiredPower The desired power of 2
     * @param minPower The minimum allowed power
     * @param extraLevels Additional levels to add to desiredPower
     * @return Buffer size as 2^power, capped at MAX_BUFFER_POWER
     */
    private static int calculatePowerOfTwoSize(int desiredLevel, int minPower, int extraLevels) {
        int targetPower;
        if (desiredLevel > MAX_BUFFER_POWER - extraLevels) {
            // Prevent overflow when adding extraLevels
            targetPower = MAX_BUFFER_POWER;
        } else {
            targetPower = Math.max(desiredLevel + extraLevels, minPower);
        }
        return 1 << targetPower;
    }
    
    /**
     * Get the recommended buffer size for a transform type.
     * 
     * @param type The transform type
     * @param desiredLevel The desired decomposition level (if applicable)
     * @return Recommended buffer size
     */
    public static int getRecommendedBufferSize(TransformType type, int desiredLevel) {
        switch (type) {
            case FWT:
            case WPT:
                // FWT/WPT require power-of-2
                return calculatePowerOfTwoSize(desiredLevel, FWT_MIN_BUFFER_POWER, FWT_LEVEL_BUFFER_FACTOR);
                
            case MODWT:
                // MODWT can handle any size but benefits from larger buffers
                return Math.max(desiredLevel * MODWT_SAMPLES_PER_LEVEL, MODWT_MIN_BUFFER_SIZE);
                
            case CWT:
                // CWT needs enough samples for largest scale
                return Math.max(desiredLevel * CWT_SAMPLES_PER_SCALE, CWT_MIN_BUFFER_SIZE);
                
            case FFT:
            case DFT:
                // FFT performs best with power-of-2
                return calculatePowerOfTwoSize(desiredLevel, FFT_MIN_BUFFER_POWER, 0);
                
            default:
                throw new IllegalArgumentException("Unknown transform type: " + type);
        }
    }
}