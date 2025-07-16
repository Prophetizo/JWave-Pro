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

import jwave.utils.OptimizationConstants;

/**
 * SIMD-optimized wavelet convolution operations.
 * This class provides static methods for high-performance wavelet transforms
 * using SIMD-friendly algorithms.
 * 
 * Key optimizations:
 * - Loop unrolling for better pipelining
 * - Cache-friendly memory access patterns
 * - Minimized array bounds checking
 * - Vectorization-friendly operations
 * 
 * @author Stephen Romano
 */
public class OptimizedWavelet {
    
    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     * 
     * @throws AssertionError if instantiation is attempted
     */
    private OptimizedWavelet() {
        throw new AssertionError("Cannot instantiate utility class");
    }
    
    /**
     * Performs an unrolled convolution loop for better performance.
     * This helper method eliminates code duplication between forward transform passes.
     * 
     * @param arrTime input time domain signal
     * @param coefficients filter coefficients (scaling or wavelet)
     * @param baseIdx base index for circular indexing
     * @param length length of the signal for modulo operation
     * @param filterLength length of the filter (motherWavelength)
     * @return the computed sum
     */
    private static double unrolledConvolution(double[] arrTime, double[] coefficients, 
                                            int baseIdx, int length, int filterLength) {
        double sum = 0.0;
        
        // Check if we can avoid modulo operations entirely
        // This is true when baseIdx + filterLength <= length
        if (baseIdx + filterLength <= length) {
            // Fast path: no wrap-around needed
            int j = 0;
            for (; j + OptimizationConstants.UNROLL_FACTOR <= filterLength; j += OptimizationConstants.UNROLL_FACTOR) {
                // Direct array access without modulo
                sum += arrTime[baseIdx + j] * coefficients[j]
                     + arrTime[baseIdx + j + 1] * coefficients[j + 1]
                     + arrTime[baseIdx + j + 2] * coefficients[j + 2]
                     + arrTime[baseIdx + j + 3] * coefficients[j + 3];
            }
            
            // Handle remaining elements
            for (; j < filterLength; j++) {
                sum += arrTime[baseIdx + j] * coefficients[j];
            }
        } else {
            // Slow path: handle wrap-around with optimized modulo
            int j = 0;
            
            // Process elements before wrap-around
            int maxBeforeWrap = Math.min(filterLength, length - baseIdx);
            for (; j + OptimizationConstants.UNROLL_FACTOR <= maxBeforeWrap; j += OptimizationConstants.UNROLL_FACTOR) {
                // Direct access without modulo for elements before wrap
                sum += arrTime[baseIdx + j] * coefficients[j]
                     + arrTime[baseIdx + j + 1] * coefficients[j + 1]
                     + arrTime[baseIdx + j + 2] * coefficients[j + 2]
                     + arrTime[baseIdx + j + 3] * coefficients[j + 3];
            }
            
            // Handle remaining elements before wrap
            for (; j < maxBeforeWrap; j++) {
                sum += arrTime[baseIdx + j] * coefficients[j];
            }
            
            // Handle wrapped elements (now we know these indices wrap)
            for (; j < filterLength; j++) {
                int k = (baseIdx + j >= length) ? baseIdx + j - length : baseIdx + j; // Optimized wrap-around handling
                sum += arrTime[k] * coefficients[j];
            }
        }
        
        return sum;
    }
    
    /**
     * Performs an unrolled accumulation loop for reverse transform.
     * This helper method eliminates code duplication in the reverse transform.
     * 
     * @param arrTime output time domain signal (accumulated into)
     * @param scalingCoeff scaling coefficient
     * @param waveletCoeff wavelet coefficient
     * @param scalingReCon scaling reconstruction coefficients
     * @param waveletReCon wavelet reconstruction coefficients
     * @param baseIdx base index for circular indexing
     * @param length length of the signal for modulo operation
     * @param filterLength length of the filter (motherWavelength)
     */
    private static void unrolledAccumulation(double[] arrTime, double scalingCoeff, double waveletCoeff,
                                           double[] scalingReCon, double[] waveletReCon,
                                           int baseIdx, int length, int filterLength) {
        // Check if we can avoid modulo operations entirely
        if (baseIdx + filterLength <= length) {
            // Fast path: no wrap-around needed
            int j = 0;
            for (; j + OptimizationConstants.UNROLL_FACTOR <= filterLength; j += OptimizationConstants.UNROLL_FACTOR) {
                // Direct array access without modulo
                arrTime[baseIdx + j] += scalingCoeff * scalingReCon[j] + waveletCoeff * waveletReCon[j];
                arrTime[baseIdx + j + 1] += scalingCoeff * scalingReCon[j + 1] + waveletCoeff * waveletReCon[j + 1];
                arrTime[baseIdx + j + 2] += scalingCoeff * scalingReCon[j + 2] + waveletCoeff * waveletReCon[j + 2];
                arrTime[baseIdx + j + 3] += scalingCoeff * scalingReCon[j + 3] + waveletCoeff * waveletReCon[j + 3];
            }
            
            // Handle remaining elements
            for (; j < filterLength; j++) {
                arrTime[baseIdx + j] += scalingCoeff * scalingReCon[j] + waveletCoeff * waveletReCon[j];
            }
        } else {
            // Slow path: handle wrap-around with optimized modulo
            int j = 0;
            
            // Process elements before wrap-around
            int maxBeforeWrap = Math.min(filterLength, length - baseIdx);
            for (; j + OptimizationConstants.UNROLL_FACTOR <= maxBeforeWrap; j += OptimizationConstants.UNROLL_FACTOR) {
                // Direct access without modulo for elements before wrap
                arrTime[baseIdx + j] += scalingCoeff * scalingReCon[j] + waveletCoeff * waveletReCon[j];
                arrTime[baseIdx + j + 1] += scalingCoeff * scalingReCon[j + 1] + waveletCoeff * waveletReCon[j + 1];
                arrTime[baseIdx + j + 2] += scalingCoeff * scalingReCon[j + 2] + waveletCoeff * waveletReCon[j + 2];
                arrTime[baseIdx + j + 3] += scalingCoeff * scalingReCon[j + 3] + waveletCoeff * waveletReCon[j + 3];
            }
            
            // Handle remaining elements before wrap
            for (; j < maxBeforeWrap; j++) {
                arrTime[baseIdx + j] += scalingCoeff * scalingReCon[j] + waveletCoeff * waveletReCon[j];
            }
            
            // Handle wrapped elements
            for (; j < filterLength; j++) {
                int k = (baseIdx + j) % length; // Use modulo to ensure correct wrapping
                arrTime[k] += scalingCoeff * scalingReCon[j] + waveletCoeff * waveletReCon[j];
            }
        }
    }
    
    /**
     * Optimized forward wavelet transform using SIMD-friendly convolution.
     * This method performs the same computation as Wavelet.forward() but with
     * better performance characteristics.
     * 
     * @param arrTime input time domain signal
     * @param arrTimeLength length of the input signal
     * @param scalingDeCom scaling decomposition coefficients
     * @param waveletDeCom wavelet decomposition coefficients
     * @param motherWavelength length of the mother wavelet
     * @return transformed signal in Hilbert space
     */
    public static double[] forwardOptimized(double[] arrTime, int arrTimeLength,
                                           double[] scalingDeCom, double[] waveletDeCom,
                                           int motherWavelength) {
        double[] arrHilb = new double[arrTimeLength];
        int h = arrTimeLength >> 1; // half length
        
        // Process in two passes for better cache locality
        // First pass: compute scaling coefficients (low pass)
        for (int i = 0; i < h; i++) {
            int baseIdx = i << 1; // i * 2
            arrHilb[i] = unrolledConvolution(arrTime, scalingDeCom, baseIdx, arrTimeLength, motherWavelength);
        }
        
        // Second pass: compute wavelet coefficients (high pass)
        for (int i = 0; i < h; i++) {
            int baseIdx = i << 1; // i * 2
            arrHilb[i + h] = unrolledConvolution(arrTime, waveletDeCom, baseIdx, arrTimeLength, motherWavelength);
        }
        
        return arrHilb;
    }
    
    /**
     * Optimized reverse wavelet transform using SIMD-friendly convolution.
     * This method performs the same computation as Wavelet.reverse() but with
     * better performance characteristics.
     * 
     * @param arrHilb input Hilbert space signal
     * @param arrHilbLength length of the input signal
     * @param scalingReCon scaling reconstruction coefficients
     * @param waveletReCon wavelet reconstruction coefficients
     * @param motherWavelength length of the mother wavelet
     * @return reconstructed time domain signal
     */
    public static double[] reverseOptimized(double[] arrHilb, int arrHilbLength,
                                           double[] scalingReCon, double[] waveletReCon,
                                           int motherWavelength) {
        double[] arrTime = new double[arrHilbLength];
        int h = arrHilbLength >> 1; // half length
        
        // Process with better memory access pattern
        // Accumulate contributions from scaling and wavelet coefficients
        for (int i = 0; i < h; i++) {
            double scalingCoeff = arrHilb[i];
            double waveletCoeff = arrHilb[i + h];
            
            // Only process if coefficients are non-zero (sparse optimization)
            if (scalingCoeff != 0.0 || waveletCoeff != 0.0) {
                int baseIdx = i << 1; // i * 2
                unrolledAccumulation(arrTime, scalingCoeff, waveletCoeff, 
                                   scalingReCon, waveletReCon, 
                                   baseIdx, arrHilbLength, motherWavelength);
            }
        }
        
        return arrTime;
    }
    
    /**
     * Optimized circular convolution for MODWT.
     * Performs convolution with circular boundary conditions.
     * 
     * @param signal input signal
     * @param filter convolution filter
     * @param signalLength length of the signal
     * @param filterLength length of the filter
     * @param stride stride for downsampling (1 for MODWT)
     * @return convolved signal
     */
    public static double[] circularConvolve(double[] signal, double[] filter,
                                           int signalLength, int filterLength,
                                           int stride) {
        double[] result = new double[signalLength];
        
        // For MODWT, stride is typically 1
        if (stride == 1) {
            // Optimized path for stride = 1
            for (int n = 0; n < signalLength; n++) {
                double sum = 0.0;
                
                // Check if we can avoid modulo operations
                if (n >= filterLength - 1) {
                    // Fast path: no wrap-around needed
                    int k = 0;
                    for (; k + OptimizationConstants.UNROLL_FACTOR <= filterLength; k += OptimizationConstants.UNROLL_FACTOR) {
                        // Direct array access without modulo
                        sum += signal[n - k] * filter[k]
                             + signal[n - k - 1] * filter[k + 1]
                             + signal[n - k - 2] * filter[k + 2]
                             + signal[n - k - 3] * filter[k + 3];
                    }
                    
                    // Handle remaining elements
                    for (; k < filterLength; k++) {
                        sum += signal[n - k] * filter[k];
                    }
                } else {
                    // Slow path: handle wrap-around with optimized modulo
                    int k = 0;
                    
                    // Process elements before wrap-around
                    int maxBeforeWrap = n + 1;
                    for (; k + OptimizationConstants.UNROLL_FACTOR <= maxBeforeWrap; k += OptimizationConstants.UNROLL_FACTOR) {
                        // Direct access without modulo for elements before wrap
                        if (k + 3 < maxBeforeWrap) {
                            sum += signal[n - k] * filter[k]
                                 + signal[n - k - 1] * filter[k + 1]
                                 + signal[n - k - 2] * filter[k + 2]
                                 + signal[n - k - 3] * filter[k + 3];
                        } else {
                            // Handle partial unroll at boundary
                            for (int j = 0; j < OptimizationConstants.UNROLL_FACTOR && k + j < maxBeforeWrap; j++) {
                                sum += signal[n - k - j] * filter[k + j];
                            }
                            k += OptimizationConstants.UNROLL_FACTOR;
                            break;
                        }
                    }
                    
                    // Handle remaining elements before wrap
                    for (; k < maxBeforeWrap && k < filterLength; k++) {
                        sum += signal[n - k] * filter[k];
                    }
                    
                    // Handle wrapped elements (indices go from end of array)
                    for (; k < filterLength; k++) {
                        int idx = n - k + signalLength; // This wraps to the end of the array
                        sum += signal[idx] * filter[k];
                    }
                }
                
                result[n] = sum;
            }
        } else {
            // General case with arbitrary stride
            for (int n = 0; n < signalLength; n++) {
                double sum = 0.0;
                
                for (int k = 0; k < filterLength; k++) {
                    int idx = (n - k * stride + signalLength * stride) % signalLength;
                    sum += signal[idx] * filter[k];
                }
                
                result[n] = sum;
            }
        }
        
        return result;
    }
    
    /**
     * Optimized multilevel wavelet decomposition.
     * Performs multiple levels of wavelet transform with better cache usage.
     * 
     * @param signal input signal
     * @param levels number of decomposition levels
     * @param scalingDeCom scaling decomposition coefficients
     * @param waveletDeCom wavelet decomposition coefficients
     * @param motherWavelength length of the mother wavelet
     * @return array containing all decomposition levels
     */
    public static double[][] multiLevelDecomposition(double[] signal, int levels,
                                                    double[] scalingDeCom, double[] waveletDeCom,
                                                    int motherWavelength) {
        int signalLength = signal.length;
        double[][] decomposition = new double[levels + 1][];
        double[] current = signal.clone();
        
        for (int level = 0; level < levels; level++) {
            int currentLength = signalLength >> level;
            if (currentLength < motherWavelength) {
                break; // Cannot decompose further
            }
            
            // Perform one level of decomposition
            double[] transformed = forwardOptimized(current, currentLength,
                                                   scalingDeCom, waveletDeCom,
                                                   motherWavelength);
            
            // Store wavelet coefficients (high-pass)
            int h = currentLength >> 1;
            decomposition[level] = new double[h];
            System.arraycopy(transformed, h, decomposition[level], 0, h);
            
            // Prepare for next level (keep only scaling coefficients)
            current = new double[h];
            System.arraycopy(transformed, 0, current, 0, h);
        }
        
        // Store final scaling coefficients
        decomposition[levels] = current;
        
        return decomposition;
    }
}