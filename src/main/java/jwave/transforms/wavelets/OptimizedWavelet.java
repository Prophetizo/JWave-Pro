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
     */
    private OptimizedWavelet() {}
    
    /**
     * Computes circular index with proper handling of negative values.
     * This is equivalent to ((index % length) + length) % length
     * but more efficient for the common case.
     * 
     * @param index the index to wrap
     * @param length the length of the array
     * @return the wrapped index in range [0, length)
     */
    private static int circularIndex(int index, int length) {
        if (index >= 0) {
            return index % length;
        } else {
            return ((index % length) + length) % length;
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
            double sum = 0.0;
            int baseIdx = i << 1; // i * 2
            
            // Unrolled inner loop
            int j = 0;
            for (; j + OptimizationConstants.UNROLL_FACTOR <= motherWavelength; j += OptimizationConstants.UNROLL_FACTOR) {
                int k0 = (baseIdx + j) % arrTimeLength;
                int k1 = (baseIdx + j + 1) % arrTimeLength;
                int k2 = (baseIdx + j + 2) % arrTimeLength;
                int k3 = (baseIdx + j + 3) % arrTimeLength;
                
                sum += arrTime[k0] * scalingDeCom[j]
                     + arrTime[k1] * scalingDeCom[j + 1]
                     + arrTime[k2] * scalingDeCom[j + 2]
                     + arrTime[k3] * scalingDeCom[j + 3];
            }
            
            // Handle remaining elements
            for (; j < motherWavelength; j++) {
                int k = (baseIdx + j) % arrTimeLength;
                sum += arrTime[k] * scalingDeCom[j];
            }
            
            arrHilb[i] = sum;
        }
        
        // Second pass: compute wavelet coefficients (high pass)
        for (int i = 0; i < h; i++) {
            double sum = 0.0;
            int baseIdx = i << 1; // i * 2
            
            // Unrolled inner loop
            int j = 0;
            for (; j + OptimizationConstants.UNROLL_FACTOR <= motherWavelength; j += OptimizationConstants.UNROLL_FACTOR) {
                int k0 = (baseIdx + j) % arrTimeLength;
                int k1 = (baseIdx + j + 1) % arrTimeLength;
                int k2 = (baseIdx + j + 2) % arrTimeLength;
                int k3 = (baseIdx + j + 3) % arrTimeLength;
                
                sum += arrTime[k0] * waveletDeCom[j]
                     + arrTime[k1] * waveletDeCom[j + 1]
                     + arrTime[k2] * waveletDeCom[j + 2]
                     + arrTime[k3] * waveletDeCom[j + 3];
            }
            
            // Handle remaining elements
            for (; j < motherWavelength; j++) {
                int k = (baseIdx + j) % arrTimeLength;
                sum += arrTime[k] * waveletDeCom[j];
            }
            
            arrHilb[i + h] = sum;
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
                
                // Unrolled inner loop
                int j = 0;
                for (; j + OptimizationConstants.UNROLL_FACTOR <= motherWavelength; j += OptimizationConstants.UNROLL_FACTOR) {
                    int k0 = (baseIdx + j) % arrHilbLength;
                    int k1 = (baseIdx + j + 1) % arrHilbLength;
                    int k2 = (baseIdx + j + 2) % arrHilbLength;
                    int k3 = (baseIdx + j + 3) % arrHilbLength;
                    
                    arrTime[k0] += scalingCoeff * scalingReCon[j] + waveletCoeff * waveletReCon[j];
                    arrTime[k1] += scalingCoeff * scalingReCon[j + 1] + waveletCoeff * waveletReCon[j + 1];
                    arrTime[k2] += scalingCoeff * scalingReCon[j + 2] + waveletCoeff * waveletReCon[j + 2];
                    arrTime[k3] += scalingCoeff * scalingReCon[j + 3] + waveletCoeff * waveletReCon[j + 3];
                }
                
                // Handle remaining elements
                for (; j < motherWavelength; j++) {
                    int k = (baseIdx + j) % arrHilbLength;
                    arrTime[k] += scalingCoeff * scalingReCon[j] + waveletCoeff * waveletReCon[j];
                }
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
                
                // Unrolled inner loop
                int k = 0;
                for (; k + OptimizationConstants.UNROLL_FACTOR <= filterLength; k += OptimizationConstants.UNROLL_FACTOR) {
                    // Circular indexing with modulo optimization
                    int idx0 = circularIndex(n - k, signalLength);
                    int idx1 = circularIndex(n - k - 1, signalLength);
                    int idx2 = circularIndex(n - k - 2, signalLength);
                    int idx3 = circularIndex(n - k - 3, signalLength);
                    
                    sum += signal[idx0] * filter[k]
                         + signal[idx1] * filter[k + 1]
                         + signal[idx2] * filter[k + 2]
                         + signal[idx3] * filter[k + 3];
                }
                
                // Handle remaining elements
                for (; k < filterLength; k++) {
                    int idx = circularIndex(n - k, signalLength);
                    sum += signal[idx] * filter[k];
                }
                
                result[n] = sum;
            }
        } else {
            // General case with arbitrary stride
            for (int n = 0; n < signalLength; n++) {
                double sum = 0.0;
                
                for (int k = 0; k < filterLength; k++) {
                    int idx = circularIndex(n - k * stride, signalLength);
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
    
    /**
     * Performs the adjoint (transpose) of circular convolution with SIMD optimizations.
     * 
     * This operation is crucial for the inverse MODWT. If H is the convolution matrix,
     * this computes H^T * signal. The adjoint operation differs from standard convolution
     * in that it adds to indices rather than subtracting.
     * 
     * @param signal input signal
     * @param filter filter for adjoint convolution
     * @param signalLength length of the signal
     * @param filterLength length of the filter
     * @param stride stride for downsampling (1 for MODWT)
     * @return adjoint convolution result
     */
    public static double[] circularConvolveAdjoint(double[] signal, double[] filter,
                                                   int signalLength, int filterLength,
                                                   int stride) {
        double[] result = new double[signalLength];
        
        // For MODWT, stride is typically 1
        if (stride == 1) {
            // Optimized path for stride = 1
            for (int n = 0; n < signalLength; n++) {
                double sum = 0.0;
                
                // Unrolled inner loop for SIMD optimization
                int m = 0;
                for (; m + OptimizationConstants.UNROLL_FACTOR <= filterLength; m += OptimizationConstants.UNROLL_FACTOR) {
                    // For adjoint operation, we add m instead of subtracting
                    int idx0 = circularIndex(n + m, signalLength);
                    int idx1 = circularIndex(n + m + 1, signalLength);
                    int idx2 = circularIndex(n + m + 2, signalLength);
                    int idx3 = circularIndex(n + m + 3, signalLength);
                    
                    sum += signal[idx0] * filter[m]
                         + signal[idx1] * filter[m + 1]
                         + signal[idx2] * filter[m + 2]
                         + signal[idx3] * filter[m + 3];
                }
                
                // Handle remaining elements
                for (; m < filterLength; m++) {
                    int idx = circularIndex(n + m, signalLength);
                    sum += signal[idx] * filter[m];
                }
                
                result[n] = sum;
            }
        } else {
            // General case with arbitrary stride
            for (int n = 0; n < signalLength; n++) {
                double sum = 0.0;
                for (int m = 0; m < filterLength; m++) {
                    int idx = circularIndex(n + m * stride, signalLength);
                    sum += signal[idx] * filter[m];
                }
                result[n] = sum;
            }
        }
        
        return result;
    }
}