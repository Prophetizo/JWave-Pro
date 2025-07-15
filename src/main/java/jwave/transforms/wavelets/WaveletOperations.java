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
package jwave.transforms.wavelets;

/**
 * Interface for wavelet convolution operations that can be implemented
 * with different optimization strategies.
 * 
 * This interface abstracts circular convolution operations used in wavelet
 * transforms to allow for dependency injection of different implementations.
 * 
 * @author Stephen Romano
 */
public interface WaveletOperations {
    
    /**
     * Perform circular convolution of a signal with a filter.
     * 
     * This operation is fundamental to wavelet transforms, computing:
     * - For stride=1: output[n] = sum_{m=0}^{M-1} signal[(n-m) mod N] * filter[m]
     * - For stride>1: output[n] = sum_{m=0}^{M-1} signal[(n-m*stride) mod N] * filter[m]
     * 
     * Note: The output array always has the same length as the input signal.
     * The stride parameter affects how the filter samples the signal, not the output size.
     * 
     * @param signal input signal
     * @param filter convolution filter (wavelet coefficients)
     * @param signalLength length of the signal (N)
     * @param filterLength length of the filter (M)
     * @param stride stride for filter sampling (typically 1 for MODWT)
     * @return convolved output signal of length signalLength
     */
    double[] circularConvolve(double[] signal, double[] filter, 
                              int signalLength, int filterLength, int stride);
    
    /**
     * Perform adjoint circular convolution (transpose convolution).
     * 
     * This is the adjoint operation of circular convolution, used in
     * inverse wavelet transforms. It computes:
     * - For stride=1: output[n] = sum_{m=0}^{M-1} signal[(n+m) mod N] * filter[m]
     * - For stride>1: output[n] = sum_{m=0}^{M-1} signal[(n+m*stride) mod N] * filter[m]
     * 
     * Note: The output array always has the same length as the input signal.
     * The stride parameter affects how the filter samples the signal, not the output size.
     * 
     * @param signal input signal
     * @param filter convolution filter (wavelet coefficients)
     * @param signalLength length of the signal (N)
     * @param filterLength length of the filter (M)
     * @param stride stride for filter sampling (typically 1 for MODWT)
     * @return adjoint convolved output signal of length signalLength
     */
    double[] circularConvolveAdjoint(double[] signal, double[] filter,
                                     int signalLength, int filterLength, int stride);
    
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