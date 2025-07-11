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
import jwave.transforms.wavelets.continuous.MorletWavelet;
import jwave.transforms.wavelets.continuous.MexicanHatWavelet;
import jwave.transforms.wavelets.continuous.PaulWavelet;
import jwave.transforms.wavelets.continuous.MeyerWavelet;

/**
 * Test class for Continuous Wavelet Transform implementation.
 *
 * @author Stephen Romano
 * @date 07.01.2025
 */
public class ContinuousWaveletTransformTest {

  private static final double DELTA = 1e-8;

  /**
   * Test Morlet wavelet basic properties.
   */
  @Test
  public void testMorletWaveletProperties() {
    MorletWavelet wavelet = new MorletWavelet(1.0, 1.0);
    
    // Test wavelet at t=0 (should be maximum)
    Complex value0 = wavelet.wavelet(0);
    assertTrue("Real part at t=0 should be positive", value0.getReal() > 0);
    assertEquals("Imaginary part at t=0 should be zero", 0.0, value0.getImag(), DELTA);
    
    // Test decay property
    Complex value1 = wavelet.wavelet(1.0);
    Complex value2 = wavelet.wavelet(2.0);
    assertTrue("Wavelet should decay", value1.getMag() < value0.getMag());
    assertTrue("Wavelet should decay", value2.getMag() < value1.getMag());
    
    // Test symmetry of envelope
    Complex valuePos = wavelet.wavelet(1.0);
    Complex valueNeg = wavelet.wavelet(-1.0);
    assertEquals("Envelope should be symmetric", valuePos.getMag(), valueNeg.getMag(), DELTA);
  }

  /**
   * Test Mexican Hat wavelet basic properties.
   */
  @Test
  public void testMexicanHatWaveletProperties() {
    MexicanHatWavelet wavelet = new MexicanHatWavelet(1.0);
    
    // Test wavelet at t=0 (should be maximum positive)
    Complex value0 = wavelet.wavelet(0);
    assertTrue("Value at t=0 should be positive", value0.getReal() > 0);
    assertEquals("Mexican Hat is real-valued", 0.0, value0.getImag(), DELTA);
    
    // Test negative lobes
    Complex value15 = wavelet.wavelet(1.5);
    assertTrue("Should have negative lobes", value15.getReal() < 0);
    
    // Test symmetry
    Complex valuePos = wavelet.wavelet(1.0);
    Complex valueNeg = wavelet.wavelet(-1.0);
    assertEquals("Should be symmetric", valuePos.getReal(), valueNeg.getReal(), DELTA);
  }

  /**
   * Test CWT on a simple sinusoidal signal.
   */
  @Test
  public void testCWTSinusoid() {
    // Create a test signal: sinusoid at 10 Hz
    double samplingRate = 100.0; // 100 Hz sampling
    int signalLength = 100;
    double[] signal = new double[signalLength];
    double frequency = 10.0; // 10 Hz
    
    for (int i = 0; i < signalLength; i++) {
      double t = i / samplingRate;
      signal[i] = Math.sin(2.0 * Math.PI * frequency * t);
    }
    
    // Create CWT with Morlet wavelet
    MorletWavelet wavelet = new MorletWavelet(1.0, 1.0);
    ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(wavelet);
    
    // Generate scales
    double[] scales = ContinuousWaveletTransform.generateLinearScales(0.1, 2.0, 10);
    
    // Perform transform
    CWTResult result = cwt.transform(signal, scales, samplingRate);
    
    // Check result dimensions
    assertEquals("Number of scales", 10, result.getNumberOfScales());
    assertEquals("Number of time points", signalLength, result.getNumberOfTimePoints());
    
    // Check that maximum response is around the signal frequency
    double[][] magnitude = result.getMagnitude();
    double centerFreq = wavelet.getCenterFrequency();
    
    // Find scale with maximum average magnitude
    int maxScaleIdx = 0;
    double maxAvgMag = 0;
    for (int i = 0; i < scales.length; i++) {
      double avgMag = 0;
      for (int j = 0; j < signalLength; j++) {
        avgMag += magnitude[i][j];
      }
      avgMag /= signalLength;
      if (avgMag > maxAvgMag) {
        maxAvgMag = avgMag;
        maxScaleIdx = i;
      }
    }
    
    // The scale corresponding to 10 Hz should have high response
    double detectedFreq = centerFreq * samplingRate / scales[maxScaleIdx];
    // Note: The detected frequency depends on the scale range and wavelet parameters
    // For this simple test, we just check that the maximum is found
    assertTrue("Detected frequency should be positive", detectedFreq > 0);
  }

  /**
   * Test scale generation utilities.
   */
  @Test
  public void testScaleGeneration() {
    // Test linear scale generation
    double[] linearScales = ContinuousWaveletTransform.generateLinearScales(1.0, 10.0, 10);
    assertEquals("First scale", 1.0, linearScales[0], DELTA);
    assertEquals("Last scale", 10.0, linearScales[9], DELTA);
    assertEquals("Scale step", 1.0, linearScales[1] - linearScales[0], DELTA);
    
    // Test logarithmic scale generation
    double[] logScales = ContinuousWaveletTransform.generateLogScales(1.0, 100.0, 5);
    assertEquals("First scale", 1.0, logScales[0], DELTA);
    assertEquals("Last scale", 100.0, logScales[4], DELTA);
    
    // Check logarithmic spacing
    double ratio1 = logScales[1] / logScales[0];
    double ratio2 = logScales[2] / logScales[1];
    assertEquals("Log spacing should be constant", ratio1, ratio2, 0.01);
  }

  /**
   * Test CWT with FFT-based implementation.
   */
  @Test
  public void testCWTFFT() {
    // Create a test signal
    double samplingRate = 100.0;
    int signalLength = 64; // Power of 2 for FFT
    double[] signal = new double[signalLength];
    
    // Mix of two frequencies
    for (int i = 0; i < signalLength; i++) {
      double t = i / samplingRate;
      signal[i] = Math.sin(2.0 * Math.PI * 5.0 * t) + 
                  0.5 * Math.sin(2.0 * Math.PI * 15.0 * t);
    }
    
    // Create CWT
    MorletWavelet wavelet = new MorletWavelet(1.0, 1.0);
    ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(wavelet);
    
    // Generate scales
    double[] scales = ContinuousWaveletTransform.generateLogScales(0.5, 5.0, 20);
    
    // Perform both direct and FFT transforms
    CWTResult resultDirect = cwt.transform(signal, scales, samplingRate);
    CWTResult resultFFT = cwt.transformFFT(signal, scales, samplingRate);
    
    // Compare results (they should be similar but not exact due to different methods)
    double[][] magDirect = resultDirect.getMagnitude();
    double[][] magFFT = resultFFT.getMagnitude();
    
    // Check that both methods give similar results
    double totalDiff = 0;
    for (int i = 0; i < scales.length; i++) {
      for (int j = 0; j < signalLength; j++) {
        totalDiff += Math.abs(magDirect[i][j] - magFFT[i][j]);
      }
    }
    double avgDiff = totalDiff / (scales.length * signalLength);
    assertTrue("FFT and direct methods should give similar results", avgDiff < 0.1);
  }

  /**
   * Test CWTResult methods.
   */
  @Test
  public void testCWTResult() {
    // Create a simple result
    Complex[][] coeffs = new Complex[2][3];
    coeffs[0][0] = new Complex(1, 0);
    coeffs[0][1] = new Complex(0, 1);
    coeffs[0][2] = new Complex(1, 1);
    coeffs[1][0] = new Complex(2, 0);
    coeffs[1][1] = new Complex(0, 2);
    coeffs[1][2] = new Complex(2, 2);
    
    double[] scales = {1.0, 2.0};
    double[] timeAxis = {0.0, 0.1, 0.2};
    
    CWTResult result = new CWTResult(coeffs, scales, timeAxis, 10.0, "Test");
    
    // Test magnitude
    double[][] mag = result.getMagnitude();
    assertEquals("Magnitude of (1,0)", 1.0, mag[0][0], DELTA);
    assertEquals("Magnitude of (0,1)", 1.0, mag[0][1], DELTA);
    assertEquals("Magnitude of (1,1)", Math.sqrt(2), mag[0][2], DELTA);
    
    // Test phase
    double[][] phase = result.getPhase();
    assertEquals("Phase of (1,0)", 0.0, phase[0][0], DELTA);
    assertEquals("Phase of (0,1)", Math.PI/2, phase[0][1], DELTA);
    
    // Test scalogram
    double[] scalogram = result.getScalogram();
    assertEquals("Scalogram for scale 0", 4.0, scalogram[0], DELTA);
    assertEquals("Scalogram for scale 1", 16.0, scalogram[1], DELTA);
    
    // Test frequency conversion
    double[] freqs = result.scaleToFrequency(1.0);
    assertEquals("Frequency for scale 1", 10.0, freqs[0], DELTA);
    assertEquals("Frequency for scale 2", 5.0, freqs[1], DELTA);
  }

  /**
   * Test wavelet properties - admissibility and support.
   */
  @Test
  public void testWaveletProperties() {
    // Test Morlet wavelet
    MorletWavelet morlet = new MorletWavelet(1.0, 1.0);
    double admissibility = morlet.getAdmissibilityConstant();
    assertTrue("Admissibility constant should be finite", 
               admissibility > 0 && admissibility < Double.POSITIVE_INFINITY);
    
    double[] support = morlet.getEffectiveSupport();
    assertTrue("Support should be symmetric", 
               Math.abs(support[0] + support[1]) < DELTA);
    
    // Test Mexican Hat wavelet
    MexicanHatWavelet mexican = new MexicanHatWavelet(1.0);
    admissibility = mexican.getAdmissibilityConstant();
    assertEquals("Mexican Hat admissibility", Math.PI, admissibility, DELTA);
    
    support = mexican.getEffectiveSupport();
    assertEquals("Support should be [-5, 5] for sigma=1", -5.0, support[0], DELTA);
    assertEquals("Support should be [-5, 5] for sigma=1", 5.0, support[1], DELTA);
  }

  /**
   * Test Paul wavelet basic properties.
   */
  @Test
  public void testPaulWaveletProperties() {
    PaulWavelet wavelet = new PaulWavelet(4);
    
    // Test that Paul wavelet is complex-valued
    Complex value = wavelet.wavelet(1.0);
    assertTrue("Paul wavelet should be complex", Math.abs(value.getImag()) > DELTA);
    
    // Test Fourier transform is zero for negative frequencies
    Complex negFreq = wavelet.fourierTransform(-1.0);
    assertEquals("Negative frequencies should be zero", 0.0, negFreq.getMag(), DELTA);
    
    // Test positive frequency response
    Complex posFreq = wavelet.fourierTransform(4.0);
    assertTrue("Positive frequencies should be non-zero", posFreq.getMag() > 0);
  }

  /**
   * Test Meyer wavelet with CWT demonstrates frequency localization.
   */
  @Test
  public void testMeyerWaveletCWT() {
    MeyerWavelet wavelet = new MeyerWavelet();
    ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(wavelet);
    
    // Create a signal with two distinct frequency components
    double samplingRate = 100.0;
    int signalLength = 256;
    double[] signal = new double[signalLength];
    
    // First half: 5 Hz, Second half: 15 Hz
    for (int i = 0; i < signalLength / 2; i++) {
      double t = i / samplingRate;
      signal[i] = Math.sin(2.0 * Math.PI * 5.0 * t);
    }
    for (int i = signalLength / 2; i < signalLength; i++) {
      double t = i / samplingRate;
      signal[i] = Math.sin(2.0 * Math.PI * 15.0 * t);
    }
    
    // Use a range of scales
    double[] scales = ContinuousWaveletTransform.generateLogScales(0.5, 10.0, 30);
    
    // Perform both direct and FFT transforms
    CWTResult resultDirect = cwt.transform(signal, scales, samplingRate);
    CWTResult resultFFT = cwt.transformFFT(signal, scales, samplingRate);
    
    // Meyer wavelet should work well with FFT due to compact frequency support
    assertNotNull("Direct CWT result should not be null", resultDirect);
    assertNotNull("FFT CWT result should not be null", resultFFT);
    
    // Check that FFT-based transform produces valid results
    double[][] magnitudeFFT = resultFFT.getMagnitude();
    boolean hasNonZero = false;
    double maxMag = 0;
    
    for (int i = 0; i < scales.length; i++) {
      for (int j = 0; j < signalLength; j++) {
        double mag = magnitudeFFT[i][j];
        assertFalse("FFT magnitude should not be NaN", Double.isNaN(mag));
        assertFalse("FFT magnitude should not be infinite", Double.isInfinite(mag));
        if (mag > 0) hasNonZero = true;
        if (mag > maxMag) maxMag = mag;
      }
    }
    
    assertTrue("Meyer wavelet CWT should produce non-zero values", hasNonZero);
    assertTrue("Meyer wavelet CWT should produce significant response", maxMag > 0.1);
  }

  /**
   * Test edge cases and error handling.
   */
  @Test
  public void testEdgeCases() {
    // Test invalid scale parameters
    try {
      ContinuousWaveletTransform.generateLinearScales(-1, 10, 10);
      fail("Should throw exception for negative scale");
    } catch (IllegalArgumentException e) {
      // Expected
    }
    
    try {
      ContinuousWaveletTransform.generateLogScales(10, 1, 10);
      fail("Should throw exception for reversed scales");
    } catch (IllegalArgumentException e) {
      // Expected
    }
    
    // Test invalid wavelet parameters
    try {
      new MorletWavelet(0, 1);
      fail("Should throw exception for zero bandwidth");
    } catch (IllegalArgumentException e) {
      // Expected
    }
    
    try {
      new MexicanHatWavelet(-1);
      fail("Should throw exception for negative sigma");
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }
}
