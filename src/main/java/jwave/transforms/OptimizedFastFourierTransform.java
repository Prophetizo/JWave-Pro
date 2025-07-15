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
package jwave.transforms;

import jwave.datatypes.natives.Complex;
import jwave.exceptions.JWaveException;
import jwave.utils.MathUtils;

/**
 * Optimized Fast Fourier Transform implementation with SIMD-friendly algorithms.
 * 
 * This implementation uses techniques that enable modern JVMs to auto-vectorize
 * the code using SIMD instructions without requiring preview features:
 * - Loop unrolling for better instruction-level parallelism
 * - Cache-friendly memory access patterns
 * - Separate real/imaginary arrays for better vectorization
 * - Pre-computed twiddle factors
 * 
 * @author Stephen Romano
 * @date 15.01.2025
 */
public class OptimizedFastFourierTransform extends FastFourierTransform {
    
    // Cache line size (typically 64 bytes = 8 doubles)
    private static final int CACHE_LINE_DOUBLES = 8;
    
    // Unroll factor for butterfly operations
    private static final int UNROLL_FACTOR = 4;
    
    /**
     * Constructor
     */
    public OptimizedFastFourierTransform() {
        super();
        _name = "Optimized Fast Fourier Transform";
    }
    
    @Override
    public Complex[] forward(Complex[] x) {
        int n = x.length;
        
        // Use parent implementation for small or non-power-of-2 sizes
        if (n < 64 || !MathUtils.isPowerOfTwo(n)) {
            return super.forward(x);
        }
        
        // Use optimized implementation
        Complex[] result = new Complex[n];
        System.arraycopy(x, 0, result, 0, n);
        fftOptimized(result, false);
        return result;
    }
    
    @Override
    public Complex[] reverse(Complex[] x) {
        int n = x.length;
        
        // Use parent implementation for small or non-power-of-2 sizes
        if (n < 64 || !MathUtils.isPowerOfTwo(n)) {
            return super.reverse(x);
        }
        
        // Use optimized implementation
        Complex[] result = new Complex[n];
        System.arraycopy(x, 0, result, 0, n);
        fftOptimized(result, true);
        return result;
    }
    
    /**
     * Optimized FFT using separate arrays for real and imaginary parts.
     * This allows the JVM to better vectorize the operations.
     */
    private void fftOptimized(Complex[] x, boolean inverse) {
        int n = x.length;
        
        // Convert to separate arrays for better vectorization
        double[] real = new double[n];
        double[] imag = new double[n];
        
        for (int i = 0; i < n; i++) {
            real[i] = x[i].getReal();
            imag[i] = x[i].getImag();
        }
        
        // Bit reversal
        bitReversalOptimized(real, imag);
        
        // Pre-compute twiddle factors for better cache usage
        double[][] twiddleReal = precomputeTwiddleFactors(n, inverse, true);
        double[][] twiddleImag = precomputeTwiddleFactors(n, inverse, false);
        
        // Cooley-Tukey with optimizations
        int logN = Integer.numberOfTrailingZeros(n);
        
        for (int s = 1; s <= logN; s++) {
            int m = 1 << s;        // 2^s
            int m2 = m >> 1;       // m/2
            
            // Get pre-computed twiddle factors for this stage
            double[] wReal = twiddleReal[s - 1];
            double[] wImag = twiddleImag[s - 1];
            
            // Process all groups
            for (int k = 0; k < n; k += m) {
                // Unrolled butterfly operations
                butterflyOptimized(real, imag, k, m2, wReal, wImag);
            }
        }
        
        // Copy back and normalize if inverse
        if (inverse) {
            double norm = 1.0 / n;
            for (int i = 0; i < n; i++) {
                x[i] = new Complex(real[i] * norm, imag[i] * norm);
            }
        } else {
            for (int i = 0; i < n; i++) {
                x[i] = new Complex(real[i], imag[i]);
            }
        }
    }
    
    /**
     * Optimized bit reversal using cache-friendly access patterns.
     */
    private void bitReversalOptimized(double[] real, double[] imag) {
        int n = real.length;
        int shift = 1 + Integer.numberOfLeadingZeros(n);
        
        // Process in cache-friendly blocks
        for (int i = 0; i < n; i += CACHE_LINE_DOUBLES) {
            int blockEnd = Math.min(i + CACHE_LINE_DOUBLES, n);
            
            for (int k = i; k < blockEnd; k++) {
                int j = Integer.reverse(k) >>> shift;
                if (j > k) {
                    // Swap real parts
                    double tempReal = real[j];
                    real[j] = real[k];
                    real[k] = tempReal;
                    
                    // Swap imaginary parts
                    double tempImag = imag[j];
                    imag[j] = imag[k];
                    imag[k] = tempImag;
                }
            }
        }
    }
    
    /**
     * Pre-compute twiddle factors for all stages.
     */
    private double[][] precomputeTwiddleFactors(int n, boolean inverse, boolean isReal) {
        int logN = Integer.numberOfTrailingZeros(n);
        double[][] twiddles = new double[logN][];
        
        for (int s = 1; s <= logN; s++) {
            int m = 1 << s;
            int m2 = m >> 1;
            twiddles[s - 1] = new double[m2];
            
            double angle = 2 * Math.PI / m * (inverse ? 1 : -1);
            
            for (int j = 0; j < m2; j++) {
                if (isReal) {
                    twiddles[s - 1][j] = Math.cos(angle * j);
                } else {
                    twiddles[s - 1][j] = Math.sin(angle * j);
                }
            }
        }
        
        return twiddles;
    }
    
    /**
     * Optimized butterfly operations with loop unrolling.
     */
    private void butterflyOptimized(double[] real, double[] imag, int offset, int halfSize,
                                   double[] wReal, double[] wImag) {
        int j = 0;
        
        // Unrolled loop for better performance
        for (; j + UNROLL_FACTOR <= halfSize; j += UNROLL_FACTOR) {
            // Unroll 4 iterations
            for (int u = 0; u < UNROLL_FACTOR; u++) {
                int upperIdx = offset + j + u;
                int lowerIdx = upperIdx + halfSize;
                
                // Load values
                double uReal = real[upperIdx];
                double uImag = imag[upperIdx];
                double lReal = real[lowerIdx];
                double lImag = imag[lowerIdx];
                
                // Get twiddle factor
                double wR = wReal[j + u];
                double wI = wImag[j + u];
                
                // Complex multiplication: (wR + i*wI) * (lReal + i*lImag)
                double prodReal = wR * lReal - wI * lImag;
                double prodImag = wR * lImag + wI * lReal;
                
                // Butterfly operation
                real[upperIdx] = uReal + prodReal;
                imag[upperIdx] = uImag + prodImag;
                real[lowerIdx] = uReal - prodReal;
                imag[lowerIdx] = uImag - prodImag;
            }
        }
        
        // Handle remaining elements
        for (; j < halfSize; j++) {
            int upperIdx = offset + j;
            int lowerIdx = upperIdx + halfSize;
            
            double uReal = real[upperIdx];
            double uImag = imag[upperIdx];
            double lReal = real[lowerIdx];
            double lImag = imag[lowerIdx];
            
            double wR = wReal[j];
            double wI = wImag[j];
            
            double prodReal = wR * lReal - wI * lImag;
            double prodImag = wR * lImag + wI * lReal;
            
            real[upperIdx] = uReal + prodReal;
            imag[upperIdx] = uImag + prodImag;
            real[lowerIdx] = uReal - prodReal;
            imag[lowerIdx] = uImag - prodImag;
        }
    }
    
    /**
     * Optimized forward transform for real input.
     */
    @Override
    public double[] forward(double[] arrTime) throws JWaveException {
        int n = arrTime.length;
        
        // Use optimized algorithm for large power-of-2 sizes
        if (n >= 64 && MathUtils.isPowerOfTwo(n)) {
            // Convert to complex using optimized approach
            Complex[] complex = new Complex[n];
            for (int i = 0; i < n; i++) {
                complex[i] = new Complex(arrTime[i], 0);
            }
            
            // Perform optimized FFT
            Complex[] result = forward(complex);
            
            // Convert back to interleaved real/imaginary
            double[] output = new double[2 * n];
            for (int i = 0; i < n; i++) {
                output[2 * i] = result[i].getReal();
                output[2 * i + 1] = result[i].getImag();
            }
            
            return output;
        }
        
        // Fall back to parent implementation
        return super.forward(arrTime);
    }
}