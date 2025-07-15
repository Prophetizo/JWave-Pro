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
package jwave.datatypes.natives;

/**
 * SIMD-friendly optimized complex number operations.
 * This class provides static methods for bulk complex number operations
 * that are optimized for SIMD vectorization by the JVM.
 * 
 * Key optimizations:
 * - Separate real and imaginary arrays for better memory layout
 * - Bulk operations to enable auto-vectorization
 * - Loop unrolling for improved performance
 * - Cache-friendly memory access patterns
 * 
 * @author Stephen Romano
 * @date 15.07.2025
 */
public class OptimizedComplex {
    
    private static final int UNROLL_FACTOR = 4;
    
    /**
     * Private constructor to prevent instantiation.
     */
    private OptimizedComplex() {}
    
    /**
     * Add two complex arrays in bulk.
     * SIMD-friendly: operates on separate real/imaginary arrays.
     * 
     * @param real1 real parts of first array
     * @param imag1 imaginary parts of first array
     * @param real2 real parts of second array
     * @param imag2 imaginary parts of second array
     * @param realOut output real parts
     * @param imagOut output imaginary parts
     * @param length number of complex numbers
     */
    public static void addBulk(double[] real1, double[] imag1,
                              double[] real2, double[] imag2,
                              double[] realOut, double[] imagOut,
                              int length) {
        int i = 0;
        
        // Unrolled loop for better performance
        for (; i + UNROLL_FACTOR <= length; i += UNROLL_FACTOR) {
            realOut[i] = real1[i] + real2[i];
            imagOut[i] = imag1[i] + imag2[i];
            
            realOut[i+1] = real1[i+1] + real2[i+1];
            imagOut[i+1] = imag1[i+1] + imag2[i+1];
            
            realOut[i+2] = real1[i+2] + real2[i+2];
            imagOut[i+2] = imag1[i+2] + imag2[i+2];
            
            realOut[i+3] = real1[i+3] + real2[i+3];
            imagOut[i+3] = imag1[i+3] + imag2[i+3];
        }
        
        // Handle remaining elements
        for (; i < length; i++) {
            realOut[i] = real1[i] + real2[i];
            imagOut[i] = imag1[i] + imag2[i];
        }
    }
    
    /**
     * Subtract two complex arrays in bulk.
     * SIMD-friendly: operates on separate real/imaginary arrays.
     * 
     * @param real1 real parts of first array
     * @param imag1 imaginary parts of first array
     * @param real2 real parts of second array
     * @param imag2 imaginary parts of second array
     * @param realOut output real parts
     * @param imagOut output imaginary parts
     * @param length number of complex numbers
     */
    public static void subtractBulk(double[] real1, double[] imag1,
                                   double[] real2, double[] imag2,
                                   double[] realOut, double[] imagOut,
                                   int length) {
        int i = 0;
        
        // Unrolled loop for better performance
        for (; i + UNROLL_FACTOR <= length; i += UNROLL_FACTOR) {
            realOut[i] = real1[i] - real2[i];
            imagOut[i] = imag1[i] - imag2[i];
            
            realOut[i+1] = real1[i+1] - real2[i+1];
            imagOut[i+1] = imag1[i+1] - imag2[i+1];
            
            realOut[i+2] = real1[i+2] - real2[i+2];
            imagOut[i+2] = imag1[i+2] - imag2[i+2];
            
            realOut[i+3] = real1[i+3] - real2[i+3];
            imagOut[i+3] = imag1[i+3] - imag2[i+3];
        }
        
        // Handle remaining elements
        for (; i < length; i++) {
            realOut[i] = real1[i] - real2[i];
            imagOut[i] = imag1[i] - imag2[i];
        }
    }
    
    /**
     * Multiply two complex arrays in bulk.
     * SIMD-friendly: operates on separate real/imaginary arrays.
     * Complex multiplication: (a + bi)(c + di) = (ac - bd) + (ad + bc)i
     * 
     * @param real1 real parts of first array
     * @param imag1 imaginary parts of first array
     * @param real2 real parts of second array
     * @param imag2 imaginary parts of second array
     * @param realOut output real parts
     * @param imagOut output imaginary parts
     * @param length number of complex numbers
     */
    public static void multiplyBulk(double[] real1, double[] imag1,
                                   double[] real2, double[] imag2,
                                   double[] realOut, double[] imagOut,
                                   int length) {
        int i = 0;
        
        // Unrolled loop for better performance
        for (; i + UNROLL_FACTOR <= length; i += UNROLL_FACTOR) {
            // Complex multiplication for index i
            realOut[i] = real1[i] * real2[i] - imag1[i] * imag2[i];
            imagOut[i] = real1[i] * imag2[i] + imag1[i] * real2[i];
            
            // Complex multiplication for index i+1
            realOut[i+1] = real1[i+1] * real2[i+1] - imag1[i+1] * imag2[i+1];
            imagOut[i+1] = real1[i+1] * imag2[i+1] + imag1[i+1] * real2[i+1];
            
            // Complex multiplication for index i+2
            realOut[i+2] = real1[i+2] * real2[i+2] - imag1[i+2] * imag2[i+2];
            imagOut[i+2] = real1[i+2] * imag2[i+2] + imag1[i+2] * real2[i+2];
            
            // Complex multiplication for index i+3
            realOut[i+3] = real1[i+3] * real2[i+3] - imag1[i+3] * imag2[i+3];
            imagOut[i+3] = real1[i+3] * imag2[i+3] + imag1[i+3] * real2[i+3];
        }
        
        // Handle remaining elements
        for (; i < length; i++) {
            realOut[i] = real1[i] * real2[i] - imag1[i] * imag2[i];
            imagOut[i] = real1[i] * imag2[i] + imag1[i] * real2[i];
        }
    }
    
    /**
     * Multiply complex array by scalar in bulk.
     * SIMD-friendly: simple scalar multiplication on arrays.
     * 
     * @param real real parts of array
     * @param imag imaginary parts of array
     * @param scalar scalar value
     * @param realOut output real parts
     * @param imagOut output imaginary parts
     * @param length number of complex numbers
     */
    public static void multiplyScalarBulk(double[] real, double[] imag,
                                         double scalar,
                                         double[] realOut, double[] imagOut,
                                         int length) {
        int i = 0;
        
        // Unrolled loop for better performance
        for (; i + UNROLL_FACTOR <= length; i += UNROLL_FACTOR) {
            realOut[i] = real[i] * scalar;
            imagOut[i] = imag[i] * scalar;
            
            realOut[i+1] = real[i+1] * scalar;
            imagOut[i+1] = imag[i+1] * scalar;
            
            realOut[i+2] = real[i+2] * scalar;
            imagOut[i+2] = imag[i+2] * scalar;
            
            realOut[i+3] = real[i+3] * scalar;
            imagOut[i+3] = imag[i+3] * scalar;
        }
        
        // Handle remaining elements
        for (; i < length; i++) {
            realOut[i] = real[i] * scalar;
            imagOut[i] = imag[i] * scalar;
        }
    }
    
    /**
     * Compute conjugate of complex array in bulk.
     * SIMD-friendly: simple negation of imaginary parts.
     * 
     * @param real real parts of array
     * @param imag imaginary parts of array
     * @param realOut output real parts (same as input)
     * @param imagOut output imaginary parts (negated)
     * @param length number of complex numbers
     */
    public static void conjugateBulk(double[] real, double[] imag,
                                    double[] realOut, double[] imagOut,
                                    int length) {
        int i = 0;
        
        // Unrolled loop for better performance
        for (; i + UNROLL_FACTOR <= length; i += UNROLL_FACTOR) {
            realOut[i] = real[i];
            imagOut[i] = -imag[i];
            
            realOut[i+1] = real[i+1];
            imagOut[i+1] = -imag[i+1];
            
            realOut[i+2] = real[i+2];
            imagOut[i+2] = -imag[i+2];
            
            realOut[i+3] = real[i+3];
            imagOut[i+3] = -imag[i+3];
        }
        
        // Handle remaining elements
        for (; i < length; i++) {
            realOut[i] = real[i];
            imagOut[i] = -imag[i];
        }
    }
    
    /**
     * Compute magnitude of complex array in bulk.
     * SIMD-friendly: sqrt(real^2 + imag^2) for each element.
     * 
     * @param real real parts of array
     * @param imag imaginary parts of array
     * @param magOut output magnitudes
     * @param length number of complex numbers
     */
    public static void magnitudeBulk(double[] real, double[] imag,
                                    double[] magOut, int length) {
        int i = 0;
        
        // Unrolled loop for better performance
        for (; i + UNROLL_FACTOR <= length; i += UNROLL_FACTOR) {
            magOut[i] = Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
            magOut[i+1] = Math.sqrt(real[i+1] * real[i+1] + imag[i+1] * imag[i+1]);
            magOut[i+2] = Math.sqrt(real[i+2] * real[i+2] + imag[i+2] * imag[i+2]);
            magOut[i+3] = Math.sqrt(real[i+3] * real[i+3] + imag[i+3] * imag[i+3]);
        }
        
        // Handle remaining elements
        for (; i < length; i++) {
            magOut[i] = Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
        }
    }
    
    /**
     * Convert Complex array to separate real/imaginary arrays.
     * Enables SIMD-friendly operations on existing Complex data.
     * 
     * @param complexArray array of Complex objects
     * @param realOut output real parts
     * @param imagOut output imaginary parts
     */
    public static void toSeparateArrays(Complex[] complexArray,
                                       double[] realOut, double[] imagOut) {
        int length = complexArray.length;
        int i = 0;
        
        // Unrolled loop for better performance
        for (; i + UNROLL_FACTOR <= length; i += UNROLL_FACTOR) {
            realOut[i] = complexArray[i].getReal();
            imagOut[i] = complexArray[i].getImag();
            
            realOut[i+1] = complexArray[i+1].getReal();
            imagOut[i+1] = complexArray[i+1].getImag();
            
            realOut[i+2] = complexArray[i+2].getReal();
            imagOut[i+2] = complexArray[i+2].getImag();
            
            realOut[i+3] = complexArray[i+3].getReal();
            imagOut[i+3] = complexArray[i+3].getImag();
        }
        
        // Handle remaining elements
        for (; i < length; i++) {
            realOut[i] = complexArray[i].getReal();
            imagOut[i] = complexArray[i].getImag();
        }
    }
    
    /**
     * Convert separate real/imaginary arrays to Complex array.
     * 
     * @param real real parts
     * @param imag imaginary parts
     * @param complexOut output Complex array
     */
    public static void fromSeparateArrays(double[] real, double[] imag,
                                         Complex[] complexOut) {
        int length = complexOut.length;
        int i = 0;
        
        // Unrolled loop for better performance
        for (; i + UNROLL_FACTOR <= length; i += UNROLL_FACTOR) {
            complexOut[i] = new Complex(real[i], imag[i]);
            complexOut[i+1] = new Complex(real[i+1], imag[i+1]);
            complexOut[i+2] = new Complex(real[i+2], imag[i+2]);
            complexOut[i+3] = new Complex(real[i+3], imag[i+3]);
        }
        
        // Handle remaining elements
        for (; i < length; i++) {
            complexOut[i] = new Complex(real[i], imag[i]);
        }
    }
    
    /**
     * Perform element-wise complex multiplication and accumulation.
     * This is useful for convolution operations.
     * result = sum(array1[i] * array2[i])
     * 
     * @param real1 real parts of first array
     * @param imag1 imaginary parts of first array
     * @param real2 real parts of second array
     * @param imag2 imaginary parts of second array
     * @param length number of complex numbers
     * @return result as [real, imag]
     */
    public static double[] multiplyAccumulate(double[] real1, double[] imag1,
                                             double[] real2, double[] imag2,
                                             int length) {
        double sumReal = 0.0;
        double sumImag = 0.0;
        int i = 0;
        
        // Unrolled loop for better performance
        // Use local variables to reduce memory traffic
        double r0, r1, r2, r3;
        double im0, im1, im2, im3;
        
        for (; i + UNROLL_FACTOR <= length; i += UNROLL_FACTOR) {
            // Complex multiplication: (a + bi)(c + di) = (ac - bd) + (ad + bc)i
            r0 = real1[i] * real2[i] - imag1[i] * imag2[i];
            im0 = real1[i] * imag2[i] + imag1[i] * real2[i];
            
            r1 = real1[i+1] * real2[i+1] - imag1[i+1] * imag2[i+1];
            im1 = real1[i+1] * imag2[i+1] + imag1[i+1] * real2[i+1];
            
            r2 = real1[i+2] * real2[i+2] - imag1[i+2] * imag2[i+2];
            im2 = real1[i+2] * imag2[i+2] + imag1[i+2] * real2[i+2];
            
            r3 = real1[i+3] * real2[i+3] - imag1[i+3] * imag2[i+3];
            im3 = real1[i+3] * imag2[i+3] + imag1[i+3] * real2[i+3];
            
            // Accumulate
            sumReal += r0 + r1 + r2 + r3;
            sumImag += im0 + im1 + im2 + im3;
        }
        
        // Handle remaining elements
        for (; i < length; i++) {
            sumReal += real1[i] * real2[i] - imag1[i] * imag2[i];
            sumImag += real1[i] * imag2[i] + imag1[i] * real2[i];
        }
        
        return new double[] { sumReal, sumImag };
    }
}