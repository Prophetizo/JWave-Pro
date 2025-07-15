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

import jwave.datatypes.natives.Complex;

/**
 * Interface for complex number operations that can be implemented
 * with different optimization strategies.
 * 
 * This interface abstracts complex number operations to allow for:
 * - Standard implementations using Complex objects
 * - SIMD-optimized implementations using separate arrays
 * - Future hardware-accelerated implementations
 * 
 * Implementations should be thread-safe for read operations.
 * 
 * @author Stephen Romano
 */
public interface ComplexOperations {
    
    /**
     * Add two complex arrays element-wise.
     * result[i] = array1[i] + array2[i]
     * 
     * @param array1 first complex array
     * @param array2 second complex array
     * @param result output array (can be same as input for in-place operation)
     * @param length number of elements to process
     */
    void add(Complex[] array1, Complex[] array2, Complex[] result, int length);
    
    /**
     * Subtract two complex arrays element-wise.
     * result[i] = array1[i] - array2[i]
     * 
     * @param array1 first complex array
     * @param array2 second complex array
     * @param result output array (can be same as input for in-place operation)
     * @param length number of elements to process
     */
    void subtract(Complex[] array1, Complex[] array2, Complex[] result, int length);
    
    /**
     * Multiply two complex arrays element-wise.
     * result[i] = array1[i] * array2[i]
     * 
     * @param array1 first complex array
     * @param array2 second complex array
     * @param result output array (can be same as input for in-place operation)
     * @param length number of elements to process
     */
    void multiply(Complex[] array1, Complex[] array2, Complex[] result, int length);
    
    /**
     * Multiply complex array by a scalar.
     * result[i] = array[i] * scalar
     * 
     * @param array complex array
     * @param scalar scalar value
     * @param result output array (can be same as input for in-place operation)
     * @param length number of elements to process
     */
    void multiplyScalar(Complex[] array, double scalar, Complex[] result, int length);
    
    /**
     * Compute conjugate of complex array.
     * result[i] = conjugate(array[i])
     * 
     * @param array complex array
     * @param result output array (can be same as input for in-place operation)
     * @param length number of elements to process
     */
    void conjugate(Complex[] array, Complex[] result, int length);
    
    /**
     * Compute magnitude of complex array.
     * result[i] = |array[i]|
     * 
     * @param array complex array
     * @param result output magnitude array
     * @param length number of elements to process
     */
    void magnitude(Complex[] array, double[] result, int length);
    
    /**
     * Perform element-wise multiplication and accumulation.
     * Useful for convolution operations.
     * result = sum(array1[i] * array2[i])
     * 
     * @param array1 first complex array
     * @param array2 second complex array
     * @param length number of elements to process
     * @return accumulated complex result
     */
    Complex multiplyAccumulate(Complex[] array1, Complex[] array2, int length);
    
    /**
     * Convert between standard Complex array and separate real/imaginary arrays.
     * This is useful for transitioning between object-based and SIMD-friendly formats.
     * 
     * @param complexArray array of Complex objects
     * @param realOut output real parts (must be at least complexArray.length)
     * @param imagOut output imaginary parts (must be at least complexArray.length)
     */
    void toSeparateArrays(Complex[] complexArray, double[] realOut, double[] imagOut);
    
    /**
     * Convert from separate real/imaginary arrays to Complex array.
     * 
     * @param real real parts
     * @param imag imaginary parts
     * @param complexOut output Complex array (must be pre-allocated)
     * @param length number of elements to convert
     */
    void fromSeparateArrays(double[] real, double[] imag, Complex[] complexOut, int length);
    
    /**
     * Get the name/description of this implementation.
     * Useful for logging and performance comparisons.
     * 
     * @return implementation name
     */
    String getImplementationName();
    
    /**
     * Check if this implementation supports SIMD operations.
     * 
     * @return true if SIMD-optimized
     */
    boolean isOptimized();
}