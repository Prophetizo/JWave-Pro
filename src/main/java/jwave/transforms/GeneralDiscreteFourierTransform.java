/**
 * JWave is distributed under the MIT License (MIT); this file is part of.
 *
 * Copyright (c) 2008-2024 Christian (graetz23@gmail.com)
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

/**
 * General Discrete Fourier Transform (DFT) implementation that supports
 * arbitrary input lengths (not restricted to powers of 2).
 * 
 * This implementation uses the direct DFT algorithm with O(N²) complexity.
 * For power-of-2 lengths, use FastFourierTransform for O(N log N) performance.
 * 
 * @author Prophetizo
 * @date 2025-07-13
 */
public class GeneralDiscreteFourierTransform extends BasicTransform {

    /**
     * Constructor
     */
    public GeneralDiscreteFourierTransform() {
        _name = "General Discrete Fourier Transform";
    }

    /**
     * Performs forward DFT on real-valued input of arbitrary length.
     * 
     * @param arrTime time domain signal
     * @return frequency domain signal (interleaved real/imaginary)
     */
    @Override
    public double[] forward(double[] arrTime) throws JWaveException {
        int n = arrTime.length;
        
        // Convert to complex
        Complex[] input = new Complex[n];
        for (int i = 0; i < n; i++) {
            input[i] = new Complex(arrTime[i], 0);
        }
        
        // Perform DFT
        Complex[] output = dft(input, false);
        
        // Convert back to interleaved real/imaginary
        double[] result = new double[2 * n];
        for (int i = 0; i < n; i++) {
            result[2 * i] = output[i].getReal();
            result[2 * i + 1] = output[i].getImag();
        }
        
        return result;
    }

    /**
     * Performs inverse DFT on frequency domain input.
     * 
     * @param arrFreq frequency domain signal (interleaved real/imaginary)
     * @return time domain signal
     */
    @Override
    public double[] reverse(double[] arrFreq) throws JWaveException {
        int n = arrFreq.length / 2;
        
        // Convert to complex
        Complex[] input = new Complex[n];
        for (int i = 0; i < n; i++) {
            input[i] = new Complex(arrFreq[2 * i], arrFreq[2 * i + 1]);
        }
        
        // Perform inverse DFT
        Complex[] output = dft(input, true);
        
        // Extract real part
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = output[i].getReal();
        }
        
        return result;
    }

    /**
     * Direct DFT implementation using the definition.
     * X[k] = sum(x[n] * exp(-j*2*pi*k*n/N)) for forward
     * x[n] = (1/N) * sum(X[k] * exp(j*2*pi*k*n/N)) for inverse
     * 
     * @param x complex input signal
     * @param inverse true for inverse transform
     * @return complex output signal
     */
    private Complex[] dft(Complex[] x, boolean inverse) {
        int n = x.length;
        Complex[] result = new Complex[n];
        
        // Normalization factor (1/N for inverse, 1 for forward)
        double norm = inverse ? 1.0 / n : 1.0;
        
        // Sign of exponent (+1 for inverse, -1 for forward)
        double sign = inverse ? 1.0 : -1.0;
        
        // Precompute twiddle factors for efficiency
        // twiddle[k][m] = exp(sign * j * 2 * pi * k * m / n)
        // But we only need to store the base twiddles and compute products
        Complex[] baseTwiddles = new Complex[n];
        double angleIncrement = sign * 2.0 * Math.PI / n;
        
        for (int m = 0; m < n; m++) {
            double angle = angleIncrement * m;
            baseTwiddles[m] = new Complex(Math.cos(angle), Math.sin(angle));
        }
        
        // Compute each output sample
        for (int k = 0; k < n; k++) {
            Complex sum = new Complex(0, 0);
            
            // Sum over all input samples
            for (int m = 0; m < n; m++) {
                // Use precomputed twiddle: exp(sign * j * 2 * pi * k * m / n)
                // This is baseTwiddles[m]^k, but we can compute it as baseTwiddles[(k*m) % n]
                int twiddleIndex = (k * m) % n;
                Complex twiddle = baseTwiddles[twiddleIndex];
                
                // Multiply and accumulate
                sum = sum.add(x[m].mul(twiddle));
            }
            
            // Apply normalization
            result[k] = sum.mul(norm);
        }
        
        return result;
    }

    /**
     * Not used for DFT - throws exception.
     */
    @Override
    public double[] forward(double[] arrTime, int level) throws JWaveException {
        throw new JWaveException("DFT does not support decomposition levels");
    }

    /**
     * Not used for DFT - throws exception.
     */
    @Override
    public double[] reverse(double[] arrFreq, int level) throws JWaveException {
        throw new JWaveException("DFT does not support decomposition levels");
    }
}