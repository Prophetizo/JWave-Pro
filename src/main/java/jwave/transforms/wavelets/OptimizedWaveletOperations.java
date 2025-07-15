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
 * Optimized implementation of WaveletOperations using SIMD-friendly algorithms.
 * 
 * This implementation delegates to the OptimizedWavelet static methods
 * which provide vectorized operations for better performance.
 * 
 * @author Stephen Romano
 */
public class OptimizedWaveletOperations implements WaveletOperations {
    
    @Override
    public double[] circularConvolve(double[] signal, double[] filter, 
                                    int signalLength, int filterLength, int stride) {
        return OptimizedWavelet.circularConvolve(signal, filter, signalLength, filterLength, stride);
    }
    
    @Override
    public double[] circularConvolveAdjoint(double[] signal, double[] filter,
                                           int signalLength, int filterLength, int stride) {
        return OptimizedWavelet.circularConvolveAdjoint(signal, filter, signalLength, filterLength, stride);
    }
    
    @Override
    public String getImplementationName() {
        return "Optimized Wavelet Operations (SIMD-friendly)";
    }
    
    @Override
    public boolean isOptimized() {
        return true;
    }
}