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
import jwave.datatypes.natives.OptimizedComplex;

/**
 * SIMD-optimized implementation of ComplexOperations.
 * 
 * This implementation:
 * - Uses OptimizedComplex static methods for bulk operations
 * - Converts between Complex arrays and separate real/imaginary arrays
 * - Minimizes object allocation during computation
 * - Leverages JVM's auto-vectorization capabilities
 * 
 * For best performance, reuse allocated arrays when possible.
 * Thread-safe for all operations.
 * 
 * @author Stephen Romano
 */
public class SIMDComplexOperations implements ComplexOperations {
    
    /**
     * Thread-local buffers to reduce allocation overhead.
     * Each thread gets its own set of buffers.
     */
    private static final ThreadLocal<BufferSet> threadLocalBuffers = 
        ThreadLocal.withInitial(BufferSet::new);
    
    /**
     * Container for thread-local work buffers.
     */
    private static class BufferSet {
        double[] real1;
        double[] imag1;
        double[] real2;
        double[] imag2;
        double[] realOut;
        double[] imagOut;
        int currentSize = 0;
        
        void ensureCapacity(int size) {
            if (size > currentSize) {
                real1 = new double[size];
                imag1 = new double[size];
                real2 = new double[size];
                imag2 = new double[size];
                realOut = new double[size];
                imagOut = new double[size];
                currentSize = size;
            }
        }
    }
    
    @Override
    public void add(Complex[] array1, Complex[] array2, Complex[] result, int length) {
        BufferSet buffers = threadLocalBuffers.get();
        buffers.ensureCapacity(length);
        
        // Convert to separate arrays
        OptimizedComplex.toSeparateArrays(array1, buffers.real1, buffers.imag1);
        OptimizedComplex.toSeparateArrays(array2, buffers.real2, buffers.imag2);
        
        // Perform optimized addition
        OptimizedComplex.addBulk(buffers.real1, buffers.imag1, 
                                buffers.real2, buffers.imag2,
                                buffers.realOut, buffers.imagOut, length);
        
        // Convert back to Complex array
        OptimizedComplex.fromSeparateArrays(buffers.realOut, buffers.imagOut, result);
    }
    
    @Override
    public void subtract(Complex[] array1, Complex[] array2, Complex[] result, int length) {
        BufferSet buffers = threadLocalBuffers.get();
        buffers.ensureCapacity(length);
        
        // Convert to separate arrays
        OptimizedComplex.toSeparateArrays(array1, buffers.real1, buffers.imag1);
        OptimizedComplex.toSeparateArrays(array2, buffers.real2, buffers.imag2);
        
        // Perform optimized subtraction
        OptimizedComplex.subtractBulk(buffers.real1, buffers.imag1, 
                                     buffers.real2, buffers.imag2,
                                     buffers.realOut, buffers.imagOut, length);
        
        // Convert back to Complex array
        OptimizedComplex.fromSeparateArrays(buffers.realOut, buffers.imagOut, result);
    }
    
    @Override
    public void multiply(Complex[] array1, Complex[] array2, Complex[] result, int length) {
        BufferSet buffers = threadLocalBuffers.get();
        buffers.ensureCapacity(length);
        
        // Convert to separate arrays
        OptimizedComplex.toSeparateArrays(array1, buffers.real1, buffers.imag1);
        OptimizedComplex.toSeparateArrays(array2, buffers.real2, buffers.imag2);
        
        // Perform optimized multiplication
        OptimizedComplex.multiplyBulk(buffers.real1, buffers.imag1, 
                                     buffers.real2, buffers.imag2,
                                     buffers.realOut, buffers.imagOut, length);
        
        // Convert back to Complex array
        OptimizedComplex.fromSeparateArrays(buffers.realOut, buffers.imagOut, result);
    }
    
    @Override
    public void multiplyScalar(Complex[] array, double scalar, Complex[] result, int length) {
        BufferSet buffers = threadLocalBuffers.get();
        buffers.ensureCapacity(length);
        
        // Convert to separate arrays
        OptimizedComplex.toSeparateArrays(array, buffers.real1, buffers.imag1);
        
        // Perform optimized scalar multiplication
        OptimizedComplex.multiplyScalarBulk(buffers.real1, buffers.imag1, scalar,
                                           buffers.realOut, buffers.imagOut, length);
        
        // Convert back to Complex array
        OptimizedComplex.fromSeparateArrays(buffers.realOut, buffers.imagOut, result);
    }
    
    @Override
    public void conjugate(Complex[] array, Complex[] result, int length) {
        BufferSet buffers = threadLocalBuffers.get();
        buffers.ensureCapacity(length);
        
        // Convert to separate arrays
        OptimizedComplex.toSeparateArrays(array, buffers.real1, buffers.imag1);
        
        // Perform optimized conjugate
        OptimizedComplex.conjugateBulk(buffers.real1, buffers.imag1,
                                      buffers.realOut, buffers.imagOut, length);
        
        // Convert back to Complex array
        OptimizedComplex.fromSeparateArrays(buffers.realOut, buffers.imagOut, result);
    }
    
    @Override
    public void magnitude(Complex[] array, double[] result, int length) {
        BufferSet buffers = threadLocalBuffers.get();
        buffers.ensureCapacity(length);
        
        // Convert to separate arrays
        OptimizedComplex.toSeparateArrays(array, buffers.real1, buffers.imag1);
        
        // Perform optimized magnitude calculation
        OptimizedComplex.magnitudeBulk(buffers.real1, buffers.imag1, result, length);
    }
    
    @Override
    public Complex multiplyAccumulate(Complex[] array1, Complex[] array2, int length) {
        BufferSet buffers = threadLocalBuffers.get();
        buffers.ensureCapacity(length);
        
        // Convert to separate arrays
        OptimizedComplex.toSeparateArrays(array1, buffers.real1, buffers.imag1);
        OptimizedComplex.toSeparateArrays(array2, buffers.real2, buffers.imag2);
        
        // Perform optimized multiply-accumulate
        double[] result = OptimizedComplex.multiplyAccumulate(buffers.real1, buffers.imag1,
                                                              buffers.real2, buffers.imag2, length);
        
        return new Complex(result[0], result[1]);
    }
    
    @Override
    public void toSeparateArrays(Complex[] complexArray, double[] realOut, double[] imagOut) {
        OptimizedComplex.toSeparateArrays(complexArray, realOut, imagOut);
    }
    
    @Override
    public void fromSeparateArrays(double[] real, double[] imag, Complex[] complexOut, int length) {
        OptimizedComplex.fromSeparateArrays(real, imag, complexOut);
    }
    
    @Override
    public String getImplementationName() {
        return "SIMD-Optimized Complex Operations";
    }
    
    @Override
    public boolean isOptimized() {
        return true;
    }
}