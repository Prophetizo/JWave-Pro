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
package jwave.operations;

/**
 * Factory for creating ComplexOperations implementations.
 * 
 * This factory provides:
 * - Default implementation selection based on system properties
 * - Singleton instances to avoid repeated allocation
 * - Easy switching between implementations for testing
 * 
 * System property: -Djwave.complex.operations=standard|simd
 * 
 * @author Stephen Romano
 */
public class ComplexOperationsFactory {
    
    /**
     * System property to control which implementation to use.
     */
    public static final String PROPERTY_NAME = "jwave.complex.operations";
    
    /**
     * Singleton instances.
     */
    private static final ComplexOperations STANDARD_INSTANCE = new StandardComplexOperations();
    private static final ComplexOperations SIMD_INSTANCE = new SIMDComplexOperations();
    
    /**
     * Default implementation (can be overridden by system property).
     */
    private static ComplexOperations defaultInstance;
    
    static {
        // Initialize default based on system property
        String impl = System.getProperty(PROPERTY_NAME, "simd").toLowerCase();
        switch (impl) {
            case "standard":
                defaultInstance = STANDARD_INSTANCE;
                break;
            case "simd":
            default:
                defaultInstance = SIMD_INSTANCE;
                break;
        }
    }
    
    /**
     * Get the default ComplexOperations implementation.
     * 
     * By default, returns the SIMD-optimized implementation unless
     * overridden by the system property jwave.complex.operations.
     * 
     * @return default implementation
     */
    public static ComplexOperations getDefault() {
        return defaultInstance;
    }
    
    /**
     * Get the standard ComplexOperations implementation.
     * 
     * @return standard implementation
     */
    public static ComplexOperations getStandard() {
        return STANDARD_INSTANCE;
    }
    
    /**
     * Get the SIMD-optimized ComplexOperations implementation.
     * 
     * @return SIMD implementation
     */
    public static ComplexOperations getSIMD() {
        return SIMD_INSTANCE;
    }
    
    /**
     * Set the default implementation.
     * Useful for testing and benchmarking.
     * 
     * @param operations the implementation to use as default
     */
    public static void setDefault(ComplexOperations operations) {
        defaultInstance = operations;
    }
    
    /**
     * Reset to the original default based on system property.
     */
    public static void resetDefault() {
        String impl = System.getProperty(PROPERTY_NAME, "simd").toLowerCase();
        switch (impl) {
            case "standard":
                defaultInstance = STANDARD_INSTANCE;
                break;
            case "simd":
            default:
                defaultInstance = SIMD_INSTANCE;
                break;
        }
    }
    
    // Private constructor to prevent instantiation
    private ComplexOperationsFactory() {
        throw new AssertionError("Cannot instantiate factory class");
    }
}