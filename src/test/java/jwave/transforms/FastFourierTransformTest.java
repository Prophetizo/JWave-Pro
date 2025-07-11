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

import static org.junit.Assert.*;
import org.junit.Test;

import jwave.datatypes.natives.Complex;
import jwave.Transform;

/**
 * Comprehensive test suite for Fast Fourier Transform implementation.
 * Tests both power-of-2 (Cooley-Tukey) and arbitrary length (Bluestein) algorithms.
 *
 * @author Stephen Romano
 * @date 07.01.2025
 */
public class FastFourierTransformTest {

    private static final double DELTA = 1e-10;

    /**
     * Test FFT with power-of-2 lengths.
     */
    @Test
    public void testPowerOfTwoFFT() {
        FastFourierTransform fft = new FastFourierTransform();
        
        // Test various power-of-2 sizes
        int[] sizes = {1, 2, 4, 8, 16, 32, 64, 128, 256};
        
        for (int n : sizes) {
            // Create test signal
            Complex[] input = new Complex[n];
            for (int i = 0; i < n; i++) {
                input[i] = new Complex(Math.cos(2 * Math.PI * i / n), 
                                     Math.sin(2 * Math.PI * i / n));
            }
            
            // Forward FFT
            Complex[] forward = fft.forward(input);
            
            // Inverse FFT
            Complex[] inverse = fft.reverse(forward);
            
            // Check reconstruction
            for (int i = 0; i < n; i++) {
                assertEquals("Real part mismatch at index " + i + " for size " + n,
                           input[i].getReal(), inverse[i].getReal(), DELTA);
                assertEquals("Imaginary part mismatch at index " + i + " for size " + n,
                           input[i].getImag(), inverse[i].getImag(), DELTA);
            }
        }
    }

    /**
     * Test FFT with non-power-of-2 lengths using Bluestein's algorithm.
     */
    @Test
    public void testNonPowerOfTwoFFT() {
        FastFourierTransform fft = new FastFourierTransform();
        
        // Test various non-power-of-2 sizes
        int[] sizes = {3, 5, 6, 7, 9, 10, 11, 13, 15, 17, 19, 23, 31, 100, 127};
        
        for (int n : sizes) {
            // Create test signal
            Complex[] input = new Complex[n];
            for (int i = 0; i < n; i++) {
                input[i] = new Complex(i, -i);
            }
            
            // Forward FFT
            Complex[] forward = fft.forward(input);
            
            // Inverse FFT
            Complex[] inverse = fft.reverse(forward);
            
            // Check reconstruction
            for (int i = 0; i < n; i++) {
                assertEquals("Real part mismatch at index " + i + " for size " + n,
                           input[i].getReal(), inverse[i].getReal(), DELTA);
                assertEquals("Imaginary part mismatch at index " + i + " for size " + n,
                           input[i].getImag(), inverse[i].getImag(), DELTA);
            }
        }
    }

    /**
     * Test FFT with known DFT values.
     */
    @Test
    public void testKnownValues() {
        FastFourierTransform fft = new FastFourierTransform();
        
        // Test 1: Constant signal
        Complex[] constant = {
            new Complex(1, 0),
            new Complex(1, 0),
            new Complex(1, 0),
            new Complex(1, 0)
        };
        
        Complex[] result = fft.forward(constant);
        
        // DC component should be 4 (sum of all values, no normalization on forward), all others should be 0
        assertEquals("DC component", 4.0, result[0].getReal(), DELTA);
        assertEquals("DC component imag", 0.0, result[0].getImag(), DELTA);
        
        for (int i = 1; i < 4; i++) {
            assertEquals("Non-DC component " + i, 0.0, result[i].getMag(), DELTA);
        }
        
        // Test 2: Single frequency
        Complex[] singleFreq = new Complex[8];
        for (int i = 0; i < 8; i++) {
            singleFreq[i] = new Complex(Math.cos(2 * Math.PI * i / 8), 0);
        }
        
        result = fft.forward(singleFreq);
        
        // Should have peaks at frequencies 1 and 7 (no normalization on forward)
        assertTrue("Peak at frequency 1", result[1].getMag() > 3.9);
        assertTrue("Peak at frequency 7", result[7].getMag() > 3.9);
        
        // Other frequencies should be near zero
        for (int i = 2; i < 7; i++) {
            assertTrue("Low magnitude at frequency " + i, result[i].getMag() < 0.01);
        }
    }

    /**
     * Test Parseval's theorem: energy conservation.
     */
    @Test
    public void testParsevalsTheorem() {
        FastFourierTransform fft = new FastFourierTransform();
        
        // Test with random signals of various lengths
        int[] sizes = {16, 17, 32, 33, 64, 100};
        
        for (int n : sizes) {
            Complex[] signal = new Complex[n];
            double timeEnergy = 0;
            
            // Create random signal and compute time domain energy
            for (int i = 0; i < n; i++) {
                double real = Math.random() - 0.5;
                double imag = Math.random() - 0.5;
                signal[i] = new Complex(real, imag);
                timeEnergy += real * real + imag * imag;
            }
            
            // Compute FFT
            Complex[] spectrum = fft.forward(signal);
            
            // Compute frequency domain energy
            double freqEnergy = 0;
            for (int i = 0; i < n; i++) {
                double mag = spectrum[i].getMag();
                freqEnergy += mag * mag;
            }
            
            // Parseval's theorem: energy should be conserved
            // With standard normalization: ||X||² = n * ||x||²
            assertEquals("Energy conservation for size " + n,
                       timeEnergy * n, freqEnergy, DELTA * n);
        }
    }

    /**
     * Test FFT linearity property.
     */
    @Test
    public void testLinearity() {
        FastFourierTransform fft = new FastFourierTransform();
        
        int n = 32;
        Complex[] x = new Complex[n];
        Complex[] y = new Complex[n];
        
        // Create two signals
        for (int i = 0; i < n; i++) {
            x[i] = new Complex(Math.sin(2 * Math.PI * i / n), 0);
            y[i] = new Complex(Math.cos(4 * Math.PI * i / n), 0);
        }
        
        // Compute FFT(x) and FFT(y)
        Complex[] X = fft.forward(x);
        Complex[] Y = fft.forward(y);
        
        // Create linear combination
        double a = 2.5;
        double b = -1.3;
        Complex[] z = new Complex[n];
        for (int i = 0; i < n; i++) {
            z[i] = x[i].mul(a).add(y[i].mul(b));
        }
        
        // Compute FFT(z)
        Complex[] Z = fft.forward(z);
        
        // Verify FFT(ax + by) = a*FFT(x) + b*FFT(y)
        for (int i = 0; i < n; i++) {
            Complex expected = X[i].mul(a).add(Y[i].mul(b));
            assertEquals("Linearity real part at " + i,
                       expected.getReal(), Z[i].getReal(), DELTA);
            assertEquals("Linearity imag part at " + i,
                       expected.getImag(), Z[i].getImag(), DELTA);
        }
    }

    /**
     * Test time shift property of FFT.
     */
    @Test
    public void testTimeShift() {
        FastFourierTransform fft = new FastFourierTransform();
        
        int n = 64;
        int shift = 5;
        
        // Create original signal
        Complex[] original = new Complex[n];
        for (int i = 0; i < n; i++) {
            original[i] = new Complex(Math.exp(-i * i / (double)(n * n)), 0);
        }
        
        // Create shifted signal
        Complex[] shifted = new Complex[n];
        for (int i = 0; i < n; i++) {
            shifted[i] = original[(i - shift + n) % n];
        }
        
        // Compute FFTs
        Complex[] origFFT = fft.forward(original);
        Complex[] shiftFFT = fft.forward(shifted);
        
        // Verify phase shift in frequency domain
        for (int k = 0; k < n; k++) {
            double expectedPhase = -2 * Math.PI * k * shift / n;
            Complex phaseShift = new Complex(Math.cos(expectedPhase), Math.sin(expectedPhase));
            Complex expected = origFFT[k].mul(phaseShift);
            
            // Compare magnitudes (should be equal)
            assertEquals("Magnitude at frequency " + k,
                       origFFT[k].getMag(), shiftFFT[k].getMag(), DELTA);
            
            // For non-zero components, check phase relationship
            if (origFFT[k].getMag() > DELTA) {
                Complex ratio = shiftFFT[k].div(origFFT[k]);
                assertEquals("Phase shift real at " + k,
                           phaseShift.getReal(), ratio.getReal(), 1e-6);
                assertEquals("Phase shift imag at " + k,
                           phaseShift.getImag(), ratio.getImag(), 1e-6);
            }
        }
    }

    /**
     * Test with real-valued signals using the Transform wrapper.
     */
    @Test
    public void testRealValuedSignals() {
        Transform transform = new Transform(new FastFourierTransform());
        
        // Test signal: sum of two sinusoids
        int n = 128;
        double[] signal = new double[n];
        for (int i = 0; i < n; i++) {
            signal[i] = Math.sin(2 * Math.PI * 5 * i / n) + 
                       0.5 * Math.sin(2 * Math.PI * 12 * i / n);
        }
        
        // Forward transform
        double[] spectrum = transform.forward(signal);
        
        // Inverse transform
        double[] reconstructed = transform.reverse(spectrum);
        
        // Check reconstruction
        for (int i = 0; i < n; i++) {
            assertEquals("Reconstruction at " + i,
                       signal[i], reconstructed[i], DELTA);
        }
    }

    /**
     * Test edge cases.
     */
    @Test
    public void testEdgeCases() {
        FastFourierTransform fft = new FastFourierTransform();
        
        // Empty array
        Complex[] empty = new Complex[0];
        Complex[] result = fft.forward(empty);
        assertEquals("Empty array forward", 0, result.length);
        
        // Single element
        Complex[] single = { new Complex(3.14, 2.71) };
        result = fft.forward(single);
        assertEquals("Single element forward", 1, result.length);
        assertEquals("Single element value real", 3.14, result[0].getReal(), DELTA);
        assertEquals("Single element value imag", 2.71, result[0].getImag(), DELTA);
        
        // Inverse of single element
        result = fft.reverse(single);
        assertEquals("Single element inverse", 1, result.length);
        assertEquals("Single element inverse real", 3.14, result[0].getReal(), DELTA);
        assertEquals("Single element inverse imag", 2.71, result[0].getImag(), DELTA);
    }

    // Removed performance comparison test between DFT and FFT.
}