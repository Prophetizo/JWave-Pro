/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

/**
 * Shared constants for the streaming transform package.
 * 
 * @author Prophetizo
 * @date 2025-07-12
 */
public final class StreamingConstants {
    
    /**
     * Natural logarithm of 2, pre-calculated for efficiency.
     * Used for converting to log base 2: log2(x) = log(x) / log(2)
     */
    public static final double LOG_2 = Math.log(2);
    
    // Private constructor to prevent instantiation
    private StreamingConstants() {
        throw new AssertionError("StreamingConstants is a utility class and should not be instantiated");
    }
}