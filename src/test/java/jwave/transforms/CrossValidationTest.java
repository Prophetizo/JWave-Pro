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

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.*;

import jwave.Transform;
import jwave.transforms.wavelets.haar.Haar1;
import jwave.TestDataLoader;

/**
 * Cross-validation tests comparing JWave implementations against Apache Commons Math.
 * This ensures our implementations produce results consistent with established libraries.
 *
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 10.01.2025
 */
public class CrossValidationTest {

  private static final double TOLERANCE = 1e-10;
  
  /**
   * Cross-validate FFT implementation against Apache Commons Math.
   */
  @Test
  public void testFFTAgainstApacheCommons() {
    // Test various signal lengths
    int[] testLengths = {8, 16, 32, 64, 128, 256};
    
    for (int n : testLengths) {
      // Generate test signal
      double[] signal = new double[n];
      for (int i = 0; i < n; i++) {
        signal[i] = Math.sin(2 * Math.PI * i / n) + 0.5 * Math.cos(4 * Math.PI * i / n);
      }
      
      // JWave FFT
      Transform jWaveTransform = new Transform(new FastFourierTransform());
      double[] jWaveResult = jWaveTransform.forward(signal);
      
      // Apache Commons Math FFT
      FastFourierTransformer apacheFFT = new FastFourierTransformer(DftNormalization.STANDARD);
      Complex[] apacheResult = apacheFFT.transform(signal, TransformType.FORWARD);
      
      // Compare results (JWave returns interleaved real/imag)
      // Both now use standard normalization (Forward: 1, Inverse: 1/N)
      assertEquals("FFT length mismatch for n=" + n, apacheResult.length * 2, jWaveResult.length);
      
      for (int i = 0; i < n; i++) {
        // Both use standard normalization convention now
        assertEquals("FFT real part mismatch at index " + i + " for n=" + n,
                     apacheResult[i].getReal(), jWaveResult[2 * i], TOLERANCE);
        assertEquals("FFT imaginary part mismatch at index " + i + " for n=" + n,
                     apacheResult[i].getImaginary(), jWaveResult[2 * i + 1], TOLERANCE);
      }
    }
  }
  
  /**
   * Cross-validate inverse FFT.
   */
  @Test
  public void testInverseFFTAgainstApacheCommons() {
    int n = 64;
    
    // First do a forward transform of a real signal to get valid frequency domain data
    double[] realSignal = new double[n];
    for (int i = 0; i < n; i++) {
      realSignal[i] = Math.random();
    }
    
    // Forward transform with both libraries
    Transform jWaveTransform = new Transform(new FastFourierTransform());
    double[] jWaveFreq = jWaveTransform.forward(realSignal);
    
    FastFourierTransformer apacheFFT = new FastFourierTransformer(DftNormalization.STANDARD);
    Complex[] apacheFreq = apacheFFT.transform(realSignal, TransformType.FORWARD);
    
    // Now do inverse transform
    double[] jWaveInverse = jWaveTransform.reverse(jWaveFreq);
    Complex[] apacheInverse = apacheFFT.transform(apacheFreq, TransformType.INVERSE);
    
    // Compare results - both should recover the original signal
    for (int i = 0; i < n; i++) {
      assertEquals("Inverse FFT should recover original signal at index " + i,
                   realSignal[i], jWaveInverse[i], TOLERANCE);
      assertEquals("Apache inverse FFT should recover original signal at index " + i,
                   realSignal[i], apacheInverse[i].getReal(), TOLERANCE);
      assertEquals("Apache inverse FFT imaginary part should be zero at index " + i,
                   0.0, apacheInverse[i].getImaginary(), TOLERANCE);
    }
  }
  
  /**
   * Test FFT with reference data files.
   */
  @Test
  public void testFFTWithReferenceData() throws Exception {
    // Load reference data
    double[] dcSignal = TestDataLoader.loadVector("fft_dc_input.txt");
    double[] expectedDCReal = TestDataLoader.loadVector("fft_dc_output_real.txt");
    double[] expectedDCImag = TestDataLoader.loadVector("fft_dc_output_imag.txt");
    
    // Compute FFT
    Transform transform = new Transform(new FastFourierTransform());
    double[] result = transform.forward(dcSignal);
    
    // Verify against reference (interleaved format)
    // Both now use standard normalization (Forward: 1, Inverse: 1/N)
    int n = dcSignal.length;
    for (int i = 0; i < n; i++) {
      assertEquals("DC FFT real part at " + i, expectedDCReal[i], result[2 * i], TOLERANCE);
      assertEquals("DC FFT imag part at " + i, expectedDCImag[i], result[2 * i + 1], TOLERANCE);
    }
    
    // Test impulse response
    double[] impulse = TestDataLoader.loadVector("fft_impulse_input.txt");
    double[] expectedImpulseReal = TestDataLoader.loadVector("fft_impulse_output_real.txt");
    double[] expectedImpulseImag = TestDataLoader.loadVector("fft_impulse_output_imag.txt");
    
    result = transform.forward(impulse);
    
    n = impulse.length;
    for (int i = 0; i < n; i++) {
      assertEquals("Impulse FFT real part at " + i, 
                   expectedImpulseReal[i], result[2 * i], TOLERANCE);
      assertEquals("Impulse FFT imag part at " + i, 
                   expectedImpulseImag[i], result[2 * i + 1], TOLERANCE);
    }
  }
  
  /**
   * Test Haar wavelet coefficients against reference.
   */
  @Test
  public void testHaarWaveletCoefficients() throws Exception {
    // Load reference Haar filter coefficients
    double[] refDecLo = TestDataLoader.loadVector("filter_haar_dec_lo.txt");
    double[] refDecHi = TestDataLoader.loadVector("filter_haar_dec_hi.txt");
    
    // Get JWave Haar coefficients
    Haar1 haar = new Haar1();
    double[] jwaveDecLo = haar.getScalingDeComposition();
    double[] jwaveDecHi = haar.getWaveletDeComposition();
    
    // Compare lengths
    assertEquals("Haar low-pass filter length", refDecLo.length, jwaveDecLo.length);
    assertEquals("Haar high-pass filter length", refDecHi.length, jwaveDecHi.length);
    
    // Compare values
    for (int i = 0; i < refDecLo.length; i++) {
      assertEquals("Haar low-pass coefficient " + i, refDecLo[i], jwaveDecLo[i], TOLERANCE);
    }
    
    for (int i = 0; i < refDecHi.length; i++) {
      assertEquals("Haar high-pass coefficient " + i, refDecHi[i], jwaveDecHi[i], TOLERANCE);
    }
  }
  
  /**
   * Test Haar transform with manually calculated reference.
   */
  @Test
  public void testHaarTransformWithReference() throws Exception {
    // Load test data
    double[] signal = TestDataLoader.loadVector("haar_simple_input.txt");
    double[] expectedApprox = TestDataLoader.loadVector("haar_level1_approx_manual.txt");
    double[] expectedDetail = TestDataLoader.loadVector("haar_level1_detail_manual.txt");
    
    // Perform Haar transform
    Transform transform = new Transform(new FastWaveletTransform(new Haar1()));
    double[] result = transform.forward(signal, 1); // 1 level decomposition
    
    // Extract approximation and detail coefficients
    int halfLength = signal.length / 2;
    double[] approx = new double[halfLength];
    double[] detail = new double[halfLength];
    
    System.arraycopy(result, 0, approx, 0, halfLength);
    System.arraycopy(result, halfLength, detail, 0, halfLength);
    
    // Compare with expected values
    assertArrayEquals("Haar approximation coefficients", expectedApprox, approx, TOLERANCE);
    assertArrayEquals("Haar detail coefficients", expectedDetail, detail, TOLERANCE);
  }
  
  /**
   * Test perfect reconstruction property.
   */
  @Test
  public void testPerfectReconstructionCrossValidation() {
    // Generate random signal
    int n = 64;
    double[] original = new double[n];
    for (int i = 0; i < n; i++) {
      original[i] = Math.random() * 10 - 5; // Random values between -5 and 5
    }
    
    // Test with FFT
    Transform fftTransform = new Transform(new FastFourierTransform());
    double[] fftForward = fftTransform.forward(original);
    double[] fftReverse = fftTransform.reverse(fftForward);
    
    // Verify perfect reconstruction
    assertArrayEquals("FFT perfect reconstruction", original, fftReverse, TOLERANCE);
    
    // Test with Haar wavelet
    Transform haarTransform = new Transform(new FastWaveletTransform(new Haar1()));
    double[] haarForward = haarTransform.forward(original);
    double[] haarReverse = haarTransform.reverse(haarForward);
    
    assertArrayEquals("Haar perfect reconstruction", original, haarReverse, TOLERANCE);
  }
  
  /**
   * Cross-validate Parseval's theorem (energy conservation).
   */
  @Test
  public void testParsevalsTheorem() {
    int n = 128;
    double[] signal = new double[n];
    
    // Generate test signal
    for (int i = 0; i < n; i++) {
      signal[i] = Math.sin(2 * Math.PI * 5 * i / n) + 
                  0.5 * Math.cos(2 * Math.PI * 13 * i / n);
    }
    
    // Calculate time-domain energy
    double timeEnergy = 0;
    for (double v : signal) {
      timeEnergy += v * v;
    }
    
    // JWave FFT
    Transform fftTransform = new Transform(new FastFourierTransform());
    double[] spectrum = fftTransform.forward(signal);
    
    // Calculate frequency-domain energy (from interleaved format)
    // Using standard normalization (Forward: 1, Inverse: 1/N)
    double freqEnergy = 0;
    for (int i = 0; i < n; i++) {
      double real = spectrum[2 * i];
      double imag = spectrum[2 * i + 1];
      freqEnergy += real * real + imag * imag;
    }
    
    // Apache Commons Math FFT for comparison
    FastFourierTransformer apacheFFT = new FastFourierTransformer(DftNormalization.STANDARD);
    Complex[] apacheSpectrum = apacheFFT.transform(signal, TransformType.FORWARD);
    
    double apacheFreqEnergy = 0;
    for (Complex c : apacheSpectrum) {
      apacheFreqEnergy += c.abs() * c.abs();
    }
    apacheFreqEnergy /= n;
    
    // Verify Parseval's theorem
    // With standard normalization: sum(|x[n]|^2) = (1/N) * sum(|X[k]|^2)
    assertEquals("JWave Parseval's theorem", timeEnergy, freqEnergy / n, TOLERANCE * timeEnergy);
    assertEquals("Apache Commons Parseval's theorem", timeEnergy, apacheFreqEnergy, TOLERANCE * timeEnergy);
  }
}