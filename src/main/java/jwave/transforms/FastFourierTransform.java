/**
 * JWave is distributed under the MIT License (MIT); this file is part of.
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
 * Fast Fourier Transform implementation supporting both power-of-2 
 * (Cooley-Tukey) and arbitrary length (Bluestein's chirp z-transform) inputs.
 * 
 * This implementation provides O(n log n) complexity for all input sizes,
 * making it suitable for real-time signal processing applications.
 * 
 * @author Stephen Romano
 * @date 07.01.2025
 */
public class FastFourierTransform extends BasicTransform {

    /**
     * Constructor
     */
    public FastFourierTransform() {
        super();
        _name = "Fast Fourier Transform";
    }

    /**
     * Performs forward FFT on real-valued input.
     * 
     * @param arrTime time domain signal
     * @return frequency domain signal (interleaved real/imaginary)
     */
    @Override
    public double[] forward(double[] arrTime) throws JWaveException {
        int n = arrTime.length;
        
        // Convert to complex
        Complex[] complex = new Complex[n];
        for (int i = 0; i < n; i++) {
            complex[i] = new Complex(arrTime[i], 0);
        }
        
        // Perform FFT
        Complex[] result = forward(complex);
        
        // Convert back to interleaved real/imaginary
        double[] output = new double[2 * n];
        for (int i = 0; i < n; i++) {
            output[2 * i] = result[i].getReal();
            output[2 * i + 1] = result[i].getImag();
        }
        
        return output;
    }

    /**
     * Performs inverse FFT on frequency domain input.
     * 
     * @param arrFreq frequency domain signal (interleaved real/imaginary)
     * @return time domain signal
     */
    @Override
    public double[] reverse(double[] arrFreq) throws JWaveException {
        int n = arrFreq.length / 2;
        
        // Convert to complex
        Complex[] complex = new Complex[n];
        for (int i = 0; i < n; i++) {
            complex[i] = new Complex(arrFreq[2 * i], arrFreq[2 * i + 1]);
        }
        
        // Perform inverse FFT
        Complex[] result = reverse(complex);
        
        // Extract real part
        double[] output = new double[n];
        for (int i = 0; i < n; i++) {
            output[i] = result[i].getReal();
        }
        
        return output;
    }

    /**
     * Performs forward FFT on complex input.
     * Automatically selects the best algorithm based on input length.
     * 
     * @param x complex input signal
     * @return complex frequency domain signal
     */
    public Complex[] forward(Complex[] x) {
        int n = x.length;
        
        if (n == 0) {
            return new Complex[0];
        }
        
        if (n == 1) {
            return new Complex[] { new Complex(x[0]) };
        }
        
        // Check if power of 2
        if (MathUtils.isPowerOfTwo(n)) {
            // Use Cooley-Tukey for power-of-2 lengths
            Complex[] result = new Complex[n];
            System.arraycopy(x, 0, result, 0, n);
            fftCooleyTukey(result, false);
            return result;
        } else {
            // Use Bluestein for arbitrary lengths
            return fftBluestein(x, false);
        }
    }

    /**
     * Performs inverse FFT on complex input.
     * 
     * @param x complex frequency domain signal
     * @return complex time domain signal
     */
    public Complex[] reverse(Complex[] x) {
        int n = x.length;
        
        if (n == 0) {
            return new Complex[0];
        }
        
        if (n == 1) {
            return new Complex[] { new Complex(x[0]) };
        }
        
        // Check if power of 2
        if (MathUtils.isPowerOfTwo(n)) {
            // Use Cooley-Tukey for power-of-2 lengths
            Complex[] result = new Complex[n];
            System.arraycopy(x, 0, result, 0, n);
            fftCooleyTukey(result, true);
            return result;
        } else {
            // Use Bluestein for arbitrary lengths
            return fftBluestein(x, true);
        }
    }

    /**
     * In-place Cooley-Tukey FFT algorithm for power-of-2 lengths.
     * 
     * @param x complex array (modified in place)
     * @param inverse true for inverse transform
     */
    private void fftCooleyTukey(Complex[] x, boolean inverse) {
        int n = x.length;
        
        // Bit reversal
        int shift = 1 + Integer.numberOfLeadingZeros(n);
        for (int k = 0; k < n; k++) {
            int j = Integer.reverse(k) >>> shift;
            if (j > k) {
                Complex temp = x[j];
                x[j] = x[k];
                x[k] = temp;
            }
        }
        
        // Cooley-Tukey decimation-in-time
        for (int size = 2; size <= n; size *= 2) {
            double angle = 2 * Math.PI / size * (inverse ? 1 : -1);
            Complex w = new Complex(Math.cos(angle), Math.sin(angle));
            
            for (int start = 0; start < n; start += size) {
                Complex wn = new Complex(1, 0);
                int halfSize = size / 2;
                
                for (int k = 0; k < halfSize; k++) {
                    Complex u = x[start + k];
                    Complex t = wn.mul(x[start + k + halfSize]);
                    x[start + k] = u.add(t);
                    x[start + k + halfSize] = u.sub(t);
                    wn = wn.mul(w);
                }
            }
        }
        
        // Normalize: Standard convention (Forward: 1, Inverse: 1/N)
        // This matches MATLAB, NumPy, SciPy, and most other implementations
        if (inverse) {
            for (int i = 0; i < n; i++) {
                x[i] = x[i].mul(1.0 / n);
            }
        }
    }

    /**
     * Internal Cooley-Tukey FFT without JWave normalization convention.
     * Used internally by Bluestein's algorithm.
     */
    private void fftCooleyTukeyInternal(Complex[] x, boolean inverse) {
        int n = x.length;
        
        // Bit reversal
        int shift = 1 + Integer.numberOfLeadingZeros(n);
        for (int k = 0; k < n; k++) {
            int j = Integer.reverse(k) >>> shift;
            if (j > k) {
                Complex temp = x[j];
                x[j] = x[k];
                x[k] = temp;
            }
        }
        
        // Cooley-Tukey decimation-in-time
        for (int size = 2; size <= n; size *= 2) {
            double angle = 2 * Math.PI / size * (inverse ? 1 : -1);
            Complex w = new Complex(Math.cos(angle), Math.sin(angle));
            
            for (int start = 0; start < n; start += size) {
                Complex wn = new Complex(1, 0);
                int halfSize = size / 2;
                
                for (int k = 0; k < halfSize; k++) {
                    Complex u = x[start + k];
                    Complex t = wn.mul(x[start + k + halfSize]);
                    x[start + k] = u.add(t);
                    x[start + k + halfSize] = u.sub(t);
                    wn = wn.mul(w);
                }
            }
        }
    }

    /**
     * Bluestein's chirp z-transform algorithm for arbitrary length FFT.
     * 
     * @param x complex input signal
     * @param inverse true for inverse transform
     * @return complex output signal
     */
    private Complex[] fftBluestein(Complex[] x, boolean inverse) {
        int n = x.length;
        
        // Find next power of 2 that is at least 2n-1
        int m = 1;
        while (m < 2 * n - 1) {
            m *= 2;
        }
        
        // Create chirp sequence
        Complex[] chirp = new Complex[n];
        for (int i = 0; i < n; i++) {
            double angle = Math.PI * i * i / n * (inverse ? 1 : -1);
            chirp[i] = new Complex(Math.cos(angle), Math.sin(angle));
        }
        
        // Create sequences a and b
        Complex[] a = new Complex[m];
        Complex[] b = new Complex[m];
        
        // Initialize with zeros
        for (int i = 0; i < m; i++) {
            a[i] = new Complex(0, 0);
            b[i] = new Complex(0, 0);
        }
        
        // Fill a with x * chirp
        for (int i = 0; i < n; i++) {
            a[i] = x[i].mul(chirp[i]);
        }
        
        // Fill b with conjugate chirp
        b[0] = chirp[0].conjugate();
        for (int i = 1; i < n; i++) {
            b[i] = chirp[i].conjugate();
            b[m - i] = chirp[i].conjugate();
        }
        
        // Compute convolution using FFT (without normalization for internal use)
        fftCooleyTukeyInternal(a, false);
        fftCooleyTukeyInternal(b, false);
        
        // Pointwise multiplication
        for (int i = 0; i < m; i++) {
            a[i] = a[i].mul(b[i]);
        }
        
        // Inverse FFT (with normalization)
        fftCooleyTukeyInternal(a, true);
        // Apply inverse normalization
        for (int i = 0; i < m; i++) {
            a[i] = a[i].mul(1.0 / m);
        }
        
        // Extract result and multiply by chirp
        Complex[] result = new Complex[n];
        for (int i = 0; i < n; i++) {
            result[i] = a[i].mul(chirp[i]);
            // Apply standard normalization convention
            if (inverse) {
                result[i] = result[i].mul(1.0 / n);
            }
        }
        
        return result;
    }


    /**
     * Not used for FFT - throws exception.
     */
    @Override
    public double[] forward(double[] arrTime, int level) throws JWaveException {
        throw new JWaveException("FFT does not support decomposition levels");
    }

    /**
     * Not used for FFT - throws exception.
     */
    @Override
    public double[] reverse(double[] arrFreq, int level) throws JWaveException {
        throw new JWaveException("FFT does not support decomposition levels");
    }
}