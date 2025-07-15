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
 * Standard implementation of WaveletOperations using basic algorithms.
 * 
 * This implementation provides baseline circular convolution operations
 * without any specific optimizations.
 * 
 * @author Stephen Romano
 */
public class StandardWaveletOperations implements WaveletOperations {
    
    @Override
    public double[] circularConvolve(double[] signal, double[] filter, 
                                    int signalLength, int filterLength, int stride) {
        double[] output = new double[signalLength];
        
        for (int n = 0; n < signalLength; n += stride) {
            double sum = 0.0;
            for (int m = 0; m < filterLength; m++) {
                int signalIndex = (n - m + signalLength) % signalLength;
                sum += signal[signalIndex] * filter[m];
            }
            output[n / stride] = sum;
        }
        
        return output;
    }
    
    @Override
    public double[] circularConvolveAdjoint(double[] signal, double[] filter,
                                           int signalLength, int filterLength, int stride) {
        double[] output = new double[signalLength];
        
        for (int n = 0; n < signalLength; n++) {
            double sum = 0.0;
            for (int m = 0; m < filterLength; m++) {
                int signalIndex = (n + m) % signalLength;
                sum += signal[signalIndex] * filter[m];
            }
            output[n] = sum;
        }
        
        return output;
    }
    
    @Override
    public String getImplementationName() {
        return "Standard Wavelet Operations";
    }
    
    @Override
    public boolean isOptimized() {
        return false;
    }
}