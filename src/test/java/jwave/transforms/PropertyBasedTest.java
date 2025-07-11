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

import org.junit.Test;
import java.util.Random;

import jwave.Transform;
import jwave.datatypes.natives.Complex;
import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.transforms.wavelets.symlets.Symlet4;

import static org.junit.Assert.*;

/**
 * Property-based tests for JWave transforms.
 * These tests verify mathematical properties that should hold for all valid inputs
 * by testing with multiple random inputs.
 *
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 10.01.2025
 */
public class PropertyBasedTest {

  private static final double TOLERANCE = 1e-8;
  private static final int NUM_RANDOM_TESTS = 100;
  private static final Random random = new Random(42); // Fixed seed for reproducibility

  /**
   * Property: FFT of a real signal should have conjugate symmetry.
   * X[k] = conj(X[N-k]) for k = 1, ..., N/2-1
   */
  @Test
  public void testFFTConjugateSymmetry() {
    for (int test = 0; test < NUM_RANDOM_TESTS; test++) {
      int power = 3 + random.nextInt(5); // Powers from 3 to 7
      int length = 1 << power; // 8 to 128
      double[] signal = generateRandomSignal(length);
      
      Transform fftTransform = new Transform(new FastFourierTransform());
      double[] resultInterleaved = fftTransform.forward(signal);
      
      // Convert interleaved to Complex array for easier testing
      int n = signal.length;
      Complex[] result = new Complex[n];
      for (int i = 0; i < n; i++) {
        result[i] = new Complex(resultInterleaved[2 * i], resultInterleaved[2 * i + 1]);
      }
      
      for (int k = 1; k < length / 2; k++) {
        Complex expected = result[k].conjugate();
        Complex actual = result[length - k];
        
        assertEquals("Test " + test + ": Real part symmetry at k=" + k, 
                     expected.getReal(), actual.getReal(), TOLERANCE);
        assertEquals("Test " + test + ": Imaginary part symmetry at k=" + k, 
                     expected.getImag(), actual.getImag(), TOLERANCE);
      }
    }
  }

  /**
   * Property: FFT should be linear.
   * FFT(a*x + b*y) = a*FFT(x) + b*FFT(y)
   */
  @Test
  public void testFFTLinearity() {
    for (int test = 0; test < NUM_RANDOM_TESTS / 10; test++) { // Fewer tests as this is expensive
      int length = 1 << (3 + random.nextInt(4)); // 8 to 64
      double[] x = generateRandomSignal(length);
      double[] y = generateRandomSignal(length);
      double a = -10 + random.nextDouble() * 20;
      double b = -10 + random.nextDouble() * 20;
      
      Transform fftTransform = new Transform(new FastFourierTransform());
      
      // Compute FFT(x) and FFT(y)
      double[] fftXInterleaved = fftTransform.forward(x);
      double[] fftYInterleaved = fftTransform.forward(y);
      
      // Convert to Complex arrays
      Complex[] fftX = new Complex[length];
      Complex[] fftY = new Complex[length];
      for (int i = 0; i < length; i++) {
        fftX[i] = new Complex(fftXInterleaved[2 * i], fftXInterleaved[2 * i + 1]);
        fftY[i] = new Complex(fftYInterleaved[2 * i], fftYInterleaved[2 * i + 1]);
      }
      
      // Compute a*x + b*y
      double[] combination = new double[length];
      for (int i = 0; i < length; i++) {
        combination[i] = a * x[i] + b * y[i];
      }
      
      // Compute FFT(a*x + b*y)
      double[] fftCombinationInterleaved = fftTransform.forward(combination);
      Complex[] fftCombination = new Complex[length];
      for (int i = 0; i < length; i++) {
        fftCombination[i] = new Complex(fftCombinationInterleaved[2 * i], fftCombinationInterleaved[2 * i + 1]);
      }
      
      // Verify linearity
      for (int i = 0; i < length; i++) {
        Complex expected = fftX[i].mul(a).add(fftY[i].mul(b));
        assertEquals("Test " + test + ": FFT linearity real at " + i, 
                     expected.getReal(), fftCombination[i].getReal(), TOLERANCE);
        assertEquals("Test " + test + ": FFT linearity imag at " + i, 
                     expected.getImag(), fftCombination[i].getImag(), TOLERANCE);
      }
    }
  }

  /**
   * Property: Wavelet transform should preserve energy (Parseval's theorem).
   * sum(x^2) = sum(coefficients^2)
   */
  @Test
  public void testWaveletEnergyConservation() {
    String[] waveletTypes = {"haar", "db4", "sym4"};
    
    for (String waveletType : waveletTypes) {
      for (int test = 0; test < NUM_RANDOM_TESTS / 3; test++) {
        int length = 1 << (3 + random.nextInt(5)); // 8 to 128
        double[] signal = generateRandomSignal(length);
        
        Transform transform = createTransform(waveletType);
        
        // Calculate original signal energy
        double signalEnergy = 0;
        for (double v : signal) {
          signalEnergy += v * v;
        }
        
        // Transform signal
        double[] coefficients = transform.forward(signal);
        
        // Calculate coefficient energy
        double coeffEnergy = 0;
        for (double v : coefficients) {
          coeffEnergy += v * v;
        }
        
        // Energy should be preserved
        assertEquals("Test " + test + ": Energy conservation for " + waveletType, 
                     signalEnergy, coeffEnergy, TOLERANCE * signalEnergy);
      }
    }
  }

  /**
   * Property: Perfect reconstruction - forward followed by inverse should return original.
   */
  @Test
  public void testPerfectReconstruction() {
    String[] waveletTypes = {"haar", "db4", "sym4"};
    
    for (String waveletType : waveletTypes) {
      for (int test = 0; test < NUM_RANDOM_TESTS / 3; test++) {
        int length = 1 << (3 + random.nextInt(5)); // 8 to 128
        double[] signal = generateRandomSignal(length);
        
        Transform transform = createTransform(waveletType);
        
        // Forward transform
        double[] coefficients = transform.forward(signal);
        
        // Inverse transform
        double[] reconstructed = transform.reverse(coefficients);
        
        // Should match original
        assertEquals("Length after reconstruction", signal.length, reconstructed.length);
        
        for (int i = 0; i < signal.length; i++) {
          assertEquals("Test " + test + ": Perfect reconstruction at " + i + " for " + waveletType, 
                       signal[i], reconstructed[i], TOLERANCE);
        }
      }
    }
  }

  /**
   * Property: Wavelet transform of constant signal should have all detail coefficients zero.
   */
  @Test
  public void testConstantSignalProperty() {
    for (int test = 0; test < NUM_RANDOM_TESTS; test++) {
      double constant = -100 + random.nextDouble() * 200;
      int powerOfTwo = 3 + random.nextInt(5); // 3 to 7
      int length = 1 << powerOfTwo;
      
      double[] signal = new double[length];
      for (int i = 0; i < length; i++) {
        signal[i] = constant;
      }
      
      Transform transform = new Transform(new FastWaveletTransform(new Haar1()));
      double[] coefficients = transform.forward(signal);
      
      // All detail coefficients (second half) should be zero
      for (int i = length / 2; i < length; i++) {
        assertEquals("Test " + test + ": Detail coefficient should be zero at " + i, 
                     0.0, coefficients[i], TOLERANCE);
      }
      
      // First approximation coefficient should be constant * sqrt(length)
      double expectedFirst = constant * Math.sqrt(length);
      assertEquals("Test " + test + ": First coefficient for constant signal", 
                   expectedFirst, coefficients[0], TOLERANCE * Math.abs(expectedFirst));
    }
  }

  /**
   * Property: FFT magnitude should be invariant to circular shift.
   */
  @Test
  public void testFFTShiftInvariance() {
    for (int test = 0; test < NUM_RANDOM_TESTS / 2; test++) {
      int length = 1 << (3 + random.nextInt(5)); // 8 to 128
      double[] signal = generateRandomSignal(length);
      int shift = random.nextInt(length);
      
      Transform fftTransform = new Transform(new FastFourierTransform());
      
      // Original FFT magnitude
      double[] originalInterleaved = fftTransform.forward(signal);
      double[] originalMag = new double[length];
      for (int i = 0; i < length; i++) {
        double real = originalInterleaved[2 * i];
        double imag = originalInterleaved[2 * i + 1];
        originalMag[i] = Math.sqrt(real * real + imag * imag);
      }
      
      // Shifted signal
      double[] shifted = new double[length];
      for (int i = 0; i < length; i++) {
        shifted[i] = signal[(i + shift) % length];
      }
      
      // Shifted FFT magnitude
      double[] shiftedInterleaved = fftTransform.forward(shifted);
      double[] shiftedMag = new double[length];
      for (int i = 0; i < length; i++) {
        double real = shiftedInterleaved[2 * i];
        double imag = shiftedInterleaved[2 * i + 1];
        shiftedMag[i] = Math.sqrt(real * real + imag * imag);
      }
      
      // Magnitudes should be equal
      for (int i = 0; i < length; i++) {
        assertEquals("Test " + test + ": FFT magnitude invariance at " + i, 
                     originalMag[i], shiftedMag[i], TOLERANCE);
      }
    }
  }

  /**
   * Property: Wavelet transform should be linear.
   */
  @Test
  public void testWaveletLinearity() {
    for (int test = 0; test < NUM_RANDOM_TESTS / 10; test++) {
      int length = 1 << (3 + random.nextInt(3)); // 8 to 32 (smaller for performance)
      double[] x = generateRandomSignal(length);
      double[] y = generateRandomSignal(length);
      double a = -5 + random.nextDouble() * 10;
      double b = -5 + random.nextDouble() * 10;
      
      Transform transform = new Transform(new FastWaveletTransform(new Haar1()));
      
      // Transform x and y
      double[] transX = transform.forward(x);
      double[] transY = transform.forward(y);
      
      // Compute a*x + b*y
      double[] combination = new double[length];
      for (int i = 0; i < length; i++) {
        combination[i] = a * x[i] + b * y[i];
      }
      
      // Transform combination
      double[] transCombination = transform.forward(combination);
      
      // Verify linearity
      for (int i = 0; i < length; i++) {
        double expected = a * transX[i] + b * transY[i];
        assertEquals("Test " + test + ": Wavelet linearity at " + i, 
                     expected, transCombination[i], TOLERANCE);
      }
    }
  }

  /**
   * Property: MODWT should be shift-invariant.
   */
  @Test
  public void testMODWTShiftInvariance() {
    for (int test = 0; test < NUM_RANDOM_TESTS / 4; test++) {
      int length = 8 + random.nextInt(57); // 8 to 64, arbitrary length
      double[] signal = generateRandomSignal(length);
      int shift = 1 + random.nextInt(5);
      
      MODWTTransform modwt = new MODWTTransform(new Haar1());
      
      // Original MODWT
      double[][] original = modwt.forwardMODWT(signal, 1);
      
      // Shifted signal
      double[] shifted = new double[length];
      for (int i = 0; i < length; i++) {
        shifted[i] = signal[(i + shift) % length];
      }
      
      // Shifted MODWT
      double[][] shiftedResult = modwt.forwardMODWT(shifted, 1);
      
      // Results should be circular shifts of each other
      for (int level = 0; level < original.length; level++) {
        double[] originalLevel = original[level];
        double[] shiftedLevel = shiftedResult[level];
        
        // Check if shiftedLevel is a circular shift of originalLevel
        boolean isShifted = true;
        for (int i = 0; i < length && isShifted; i++) {
          double expected = originalLevel[(i + shift) % length];
          if (Math.abs(expected - shiftedLevel[i]) > TOLERANCE) {
            isShifted = false;
          }
        }
        
        assertTrue("Test " + test + ": MODWT should be shift-invariant at level " + level, isShifted);
      }
    }
  }

  /**
   * Property: Sum of wavelet and scaling function coefficients should equal signal sum.
   * This is true for wavelets with sum of scaling coefficients = sqrt(2).
   */
  @Test
  public void testSumPreservation() {
    for (int test = 0; test < NUM_RANDOM_TESTS; test++) {
      int length = 1 << (3 + random.nextInt(5)); // 8 to 128
      double[] signal = generateRandomSignal(length);
      
      // Use Haar wavelet (simplest case)
      Transform transform = new Transform(new FastWaveletTransform(new Haar1()));
      
      // Calculate signal sum
      double signalSum = 0;
      for (double v : signal) {
        signalSum += v;
      }
      
      // Transform signal
      double[] coefficients = transform.forward(signal);
      
      // The first coefficient (DC component) should be related to the sum
      // For Haar with normalization, it's sum / sqrt(length)
      double expectedDC = signalSum / Math.sqrt(length);
      
      assertEquals("Test " + test + ": DC coefficient preserves sum information", 
                   expectedDC, coefficients[0], TOLERANCE * Math.abs(expectedDC));
    }
  }

  // Helper methods

  private double[] generateRandomSignal(int length) {
    double[] signal = new double[length];
    for (int i = 0; i < length; i++) {
      signal[i] = -10 + random.nextDouble() * 20; // Range [-10, 10]
    }
    return signal;
  }

  private Transform createTransform(String waveletType) {
    switch (waveletType) {
      case "haar":
        return new Transform(new FastWaveletTransform(new Haar1()));
      case "db4":
        return new Transform(new FastWaveletTransform(new Daubechies4()));
      case "sym4":
        return new Transform(new FastWaveletTransform(new Symlet4()));
      default:
        throw new IllegalArgumentException("Unknown wavelet type: " + waveletType);
    }
  }
}