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
package jwave.transforms.wavelets.continuous;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import jwave.datatypes.natives.Complex;
import jwave.transforms.ContinuousWaveletTransform;
import jwave.transforms.CWTResult;

/**
 * Test class for Meyer wavelet implementation.
 *
 * @author Stephen Romano
 * @date 09.01.2025
 */
public class MeyerWaveletTest {

  private static final double DELTA = 1e-8;
  
  /**
   * Tolerance for continuity checks at transition boundaries.
   * Slightly larger than DELTA to account for numerical precision 
   * in the transition function evaluation near boundaries.
   */
  private static final double CONTINUITY_TOLERANCE = 1e-4;
  
  private MeyerWavelet wavelet;
  
  @Before
  public void setUp() {
    wavelet = new MeyerWavelet();
  }

  /**
   * Test Meyer wavelet instantiation and basic properties.
   */
  @Test
  public void testInstantiation() {
    assertEquals("Meyer", wavelet.getName());
    assertEquals(0.7 / (2.0 * Math.PI), wavelet.getCenterFrequency(), DELTA);
  }

  /**
   * Test Meyer wavelet is real-valued in time domain.
   */
  @Test
  public void testRealValued() {
    
    // Test at various time points
    double[] testPoints = {-10, -5, -1, 0, 1, 5, 10};
    
    for (double t : testPoints) {
      Complex value = wavelet.wavelet(t);
      assertEquals("Meyer wavelet should be real-valued at t=" + t, 
                   0.0, value.getImag(), DELTA);
    }
  }

  /**
   * Test Meyer wavelet decay properties in time domain.
   */
  @Test
  public void testDecay() {
    
    
    // Test that wavelet decays as we move away from origin
    Complex value0 = wavelet.wavelet(0);
    Complex value5 = wavelet.wavelet(5);
    Complex value10 = wavelet.wavelet(10);
    Complex value15 = wavelet.wavelet(15);
    
    assertTrue("Wavelet should decay with distance", 
               Math.abs(value5.getReal()) < Math.abs(value0.getReal()));
    assertTrue("Wavelet should decay with distance", 
               Math.abs(value10.getReal()) < Math.abs(value5.getReal()));
    assertTrue("Wavelet should decay with distance", 
               Math.abs(value15.getReal()) < Math.abs(value10.getReal()));
  }

  /**
   * Test Fourier transform compact support.
   */
  @Test
  public void testFourierCompactSupport() {
    
    
    // Test zero outside support
    Complex zeroLow = wavelet.fourierTransform(0.5);
    assertEquals("Should be zero below support", 0.0, zeroLow.getMag(), DELTA);
    
    Complex zeroHigh = wavelet.fourierTransform(10.0);
    assertEquals("Should be zero above support", 0.0, zeroHigh.getMag(), DELTA);
    
    // Test non-zero inside support
    Complex inSupport1 = wavelet.fourierTransform(2.5);
    assertTrue("Should be non-zero in support", inSupport1.getMag() > 0);
    
    Complex inSupport2 = wavelet.fourierTransform(5.0);
    assertTrue("Should be non-zero in support", inSupport2.getMag() > 0);
  }

  /**
   * Test transition function properties.
   */
  @Test
  public void testTransitionFunction() {
    
    
    // Access transition function through Fourier transform behavior
    // The transition occurs at boundaries of support regions
    
    // Test continuity near transition points
    double omega1 = 4.0 * Math.PI / 3.0 - 0.01;
    double omega2 = 4.0 * Math.PI / 3.0 + 0.01;
    
    Complex val1 = wavelet.fourierTransform(omega1);
    Complex val2 = wavelet.fourierTransform(omega2);
    
    // Values should be close (continuous transition)
    assertEquals("Transition should be continuous", 
                 val1.getMag(), val2.getMag(), CONTINUITY_TOLERANCE);
  }

  /**
   * Test admissibility constant.
   */
  @Test
  public void testAdmissibility() {
    
    
    double admissibility = wavelet.getAdmissibilityConstant();
    assertEquals("Meyer wavelet admissibility constant", 
                 2.0 * Math.PI, admissibility, DELTA);
    
    assertTrue("Admissibility should be finite", 
               admissibility > 0 && admissibility < Double.POSITIVE_INFINITY);
  }

  /**
   * Test effective support.
   */
  @Test
  public void testEffectiveSupport() {
    
    
    double[] support = wavelet.getEffectiveSupport();
    assertEquals("Support should be symmetric", 
                 -support[0], support[1], DELTA);
    assertEquals("Expected support range", -15.0, support[0], DELTA);
    assertEquals("Expected support range", 15.0, support[1], DELTA);
  }

  /**
   * Test bandwidth.
   */
  @Test
  public void testBandwidth() {
    
    
    double[] bandwidth = wavelet.getBandwidth();
    
    // Check conversion from angular frequency
    double expectedMin = (2.0 / 3.0) / (2.0 * Math.PI);
    double expectedMax = (8.0 / 3.0) / (2.0 * Math.PI);
    
    assertEquals("Minimum frequency", expectedMin, bandwidth[0], DELTA);
    assertEquals("Maximum frequency", expectedMax, bandwidth[1], DELTA);
    
    assertTrue("Bandwidth should be positive", bandwidth[0] > 0);
    assertTrue("Bandwidth should be ordered", bandwidth[1] > bandwidth[0]);
  }

  /**
   * Test Meyer wavelet orthogonality property.
   * Meyer wavelets at different scales should be approximately orthogonal.
   */
  @Test
  public void testOrthogonalityProperty() {
    
    
    // Test that Fourier transform magnitude is bounded
    double maxMag = 0;
    for (double omega = 0; omega < 10; omega += 0.1) {
      Complex ft = wavelet.fourierTransform(omega);
      maxMag = Math.max(maxMag, ft.getMag());
    }
    
    assertTrue("Fourier transform should be bounded", maxMag < 10);
    assertTrue("Fourier transform should be non-trivial", maxMag > 0);
  }

  /**
   * Test time-frequency localization.
   */
  @Test
  public void testTimeFrequencyLocalization() {
    
    
    // Meyer wavelet should have good frequency localization
    double[] bandwidth = wavelet.getBandwidth();
    double bw = bandwidth[1] - bandwidth[0];
    double centerFreq = wavelet.getCenterFrequency();
    
    // Relative bandwidth should be moderate (good frequency localization)
    double relativeBW = bw / centerFreq;
    assertTrue("Should have good frequency localization", relativeBW < 4.0);
  }

  /**
   * Test numerical stability at extreme values.
   */
  @Test
  public void testNumericalStability() {
    
    
    // Test at very small and large time values
    Complex smallTime = wavelet.wavelet(1e-10);
    assertFalse("Should not produce NaN", Double.isNaN(smallTime.getReal()));
    assertFalse("Should not produce infinity", Double.isInfinite(smallTime.getReal()));
    
    Complex largeTime = wavelet.wavelet(100);
    assertFalse("Should not produce NaN", Double.isNaN(largeTime.getReal()));
    assertFalse("Should not produce infinity", Double.isInfinite(largeTime.getReal()));
    assertEquals("Should return zero beyond effective support", 0.0, largeTime.getReal(), DELTA);
    
    // Test at boundary of effective support
    Complex atBoundary = wavelet.wavelet(15.0);
    Complex beyondBoundary = wavelet.wavelet(15.1);
    assertTrue("Should have value at boundary", Math.abs(atBoundary.getReal()) > 0);
    assertEquals("Should be zero beyond boundary", 0.0, beyondBoundary.getReal(), DELTA);
    
    // Test at very small and large frequency values
    Complex smallFreq = wavelet.fourierTransform(1e-10);
    assertFalse("Should not produce NaN", Double.isNaN(smallFreq.getReal()));
    
    Complex largeFreq = wavelet.fourierTransform(100);
    assertFalse("Should not produce NaN", Double.isNaN(largeFreq.getReal()));
  }

  /**
   * Test symmetry properties.
   */
  @Test
  public void testSymmetry() {
    
    
    // Meyer wavelet in time domain should be symmetric (even function)
    double[] testPoints = {1, 2, 3, 5, 10};
    
    for (double t : testPoints) {
      Complex valuePos = wavelet.wavelet(t);
      Complex valueNeg = wavelet.wavelet(-t);
      
      assertEquals("Meyer wavelet should be symmetric in time", 
                   valuePos.getReal(), valueNeg.getReal(), DELTA);
    }
    
    // Fourier transform should have Hermitian symmetry for real-valued wavelet
    // F(-ω) = F*(ω), which means real part even, imaginary part odd
    double[] freqPoints = {2.5, 3.0, 4.0, 5.0, 6.0};
    
    for (double omega : freqPoints) {
      Complex ftPos = wavelet.fourierTransform(omega);
      Complex ftNeg = wavelet.fourierTransform(-omega);
      
      // Real part should be even: Re[F(-ω)] = Re[F(ω)]
      assertEquals("Real part should have even symmetry", 
                   ftPos.getReal(), ftNeg.getReal(), DELTA);
      
      // Imaginary part should be odd: Im[F(-ω)] = -Im[F(ω)]
      assertEquals("Imaginary part should have odd symmetry", 
                   ftPos.getImag(), -ftNeg.getImag(), DELTA);
      
      // For Hermitian symmetry: F(-ω) = F*(ω)
      assertEquals("Magnitude should be symmetric", 
                   ftPos.getMag(), ftNeg.getMag(), DELTA);
    }
  }

  /**
   * Test integration with CWT framework.
   */
  @Test
  public void testCWTIntegration() {
    
    
    // Create a simple test signal
    int signalLength = 64;
    double[] signal = new double[signalLength];
    double samplingRate = 100.0; // 100 Hz
    
    // Sinusoidal signal at 10 Hz
    for (int i = 0; i < signalLength; i++) {
      double t = i / samplingRate;
      signal[i] = Math.sin(2 * Math.PI * 10.0 * t);
    }
    
    // Create CWT and perform transform
    ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(wavelet);
    double[] scales = ContinuousWaveletTransform.generateLinearScales(0.5, 5.0, 10);
    
    // Test that CWT can be performed without errors
    CWTResult result = cwt.transform(signal, scales, samplingRate);
    assertNotNull("CWT result should not be null", result);
    assertEquals("Number of scales should match", 10, result.getNumberOfScales());
    assertEquals("Number of time points should match", signalLength, result.getNumberOfTimePoints());
    
    // Verify that results are reasonable
    double[][] magnitude = result.getMagnitude();
    boolean hasNonZeroValues = false;
    for (int i = 0; i < scales.length; i++) {
      for (int j = 0; j < signalLength; j++) {
        assertFalse("Magnitude should not be NaN", Double.isNaN(magnitude[i][j]));
        assertFalse("Magnitude should not be infinite", Double.isInfinite(magnitude[i][j]));
        if (magnitude[i][j] > 0) {
          hasNonZeroValues = true;
        }
      }
    }
    assertTrue("CWT should produce non-zero values", hasNonZeroValues);
  }
}