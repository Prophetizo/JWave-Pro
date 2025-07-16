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
package jwave;

/**
 * Centralized configuration for performance tests across JWave.
 * 
 * Performance tests are disabled by default to keep regular test runs fast.
 * Enable them by setting the system property: -DenablePerformanceTests=true
 * 
 * Example usage:
 * <pre>
 * {@code
 * @Test
 * public void testPerformance() {
 *     Assume.assumeTrue("Performance tests disabled", PerformanceTestConfig.isEnabled());
 *     // ... performance test code ...
 * }
 * }
 * </pre>
 * 
 * @author Stephen Romano
 */
public final class PerformanceTestConfig {
    
    /**
     * System property name for enabling performance tests.
     */
    public static final String ENABLE_PROPERTY = "enablePerformanceTests";
    
    /**
     * Default number of warmup iterations for JIT optimization.
     */
    public static final int DEFAULT_WARMUP_ITERATIONS = 100;
    
    /**
     * Default number of test iterations for benchmarking.
     */
    public static final int DEFAULT_TEST_ITERATIONS = 1000;
    
    /**
     * Common test sizes for performance benchmarking.
     */
    public static final int[] STANDARD_TEST_SIZES = { 256, 512, 1024, 2048, 4096 };
    
    /**
     * Extended test sizes for comprehensive benchmarking.
     */
    public static final int[] EXTENDED_TEST_SIZES = { 256, 512, 1024, 2048, 4096, 8192, 16384 };
    
    /**
     * Fixed random seed for reproducible performance tests.
     */
    public static final long RANDOM_SEED = 42L;
    
    private static final boolean ENABLED = Boolean.getBoolean(ENABLE_PROPERTY);
    
    /**
     * Private constructor to prevent instantiation.
     */
    private PerformanceTestConfig() {
        throw new AssertionError("Cannot instantiate utility class");
    }
    
    /**
     * Check if performance tests are enabled.
     * 
     * @return true if performance tests should run, false otherwise
     */
    public static boolean isEnabled() {
        return ENABLED;
    }
    
    /**
     * Get a descriptive message for skipped performance tests.
     * 
     * @return message explaining how to enable performance tests
     */
    public static String getSkipMessage() {
        return "Performance tests disabled. Enable with -D" + ENABLE_PROPERTY + "=true";
    }
}