/**
 * JWave is distributed under the MIT License (MIT); this file is part of.
 *
 * Copyright (c) 2025 Prophetizo
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jwave.utils;

/**
 * Shared constants for SIMD and optimization parameters across JWave.
 * 
 * These constants control various optimization strategies including:
 * - Loop unrolling factors
 * - SIMD vector sizes
 * - Cache-friendly block sizes
 * 
 * @author Stephen Romano
 */
public final class OptimizationConstants {
    
    /**
     * Loop unroll factor for SIMD-friendly operations.
     * Value of 4 chosen because:
     * - Matches common SIMD vector widths (4 doubles in AVX)
     * - Provides good instruction-level parallelism
     * - Balances code size vs performance
     */
    public static final int UNROLL_FACTOR = 4;
    
    /**
     * Minimum array size for parallel processing.
     * Below this threshold, sequential processing is more efficient.
     */
    public static final int PARALLEL_THRESHOLD = 8192;
    
    /**
     * Cache line size in bytes (typical for modern CPUs).
     * Used for data alignment and padding decisions.
     */
    public static final int CACHE_LINE_SIZE = 64;
    
    /**
     * Number of doubles that fit in a cache line.
     */
    public static final int DOUBLES_PER_CACHE_LINE = CACHE_LINE_SIZE / 8;
    
    /**
     * Preferred alignment for SIMD operations in bytes.
     * 32 bytes = 256 bits = AVX register width
     */
    public static final int SIMD_ALIGNMENT = 32;
    
    // Private constructor to prevent instantiation
    private OptimizationConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}