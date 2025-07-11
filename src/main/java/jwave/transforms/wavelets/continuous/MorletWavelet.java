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
package jwave.transforms.wavelets.continuous;

import jwave.datatypes.natives.Complex;

/**
 * Morlet wavelet implementation for Continuous Wavelet Transform.
 * The Morlet wavelet is a complex wavelet that provides good time-frequency
 * localization and is widely used in signal analysis.
 * 
 * The Morlet wavelet is defined as:
 * psi(t) = (1/sqrt(2*pi*fb)) * exp(2*pi*i*fc*t) * exp(-t^2/(2*fb))
 * 
 * where fc is the center frequency and fb is the bandwidth parameter.
 *
 * @author Stephen Romano
 * @date 07.01.2025
 */
public class MorletWavelet extends ContinuousWavelet {

  /**
   * Bandwidth parameter (controls the width of the Gaussian envelope).
   * Default value is 1.0.
   */
  private double _fb;

  /**
   * Center frequency parameter.
   * Default value is 1.0.
   */
  private double _fc;

  /**
   * Default constructor with standard parameters (fb=1, fc=1).
   */
  public MorletWavelet() {
    this(1.0, 1.0);
  }

  /**
   * Constructor with custom bandwidth and center frequency.
   * 
   * @param fb bandwidth parameter (must be positive)
   * @param fc center frequency (must be positive)
   */
  public MorletWavelet(double fb, double fc) {
    super();
    
    if (fb <= 0) {
      throw new IllegalArgumentException("Bandwidth parameter must be positive");
    }
    if (fc <= 0) {
      throw new IllegalArgumentException("Center frequency must be positive");
    }
    
    _name = "Morlet";
    _fb = fb;
    _fc = fc;
    _centerFrequency = fc;
  }

  /**
   * Evaluates the Morlet wavelet at a given time point.
   * psi(t) = (1/sqrt(2*pi*fb)) * exp(2*pi*i*fc*t) * exp(-t^2/(2*fb))
   * 
   * @param t time point
   * @return complex value of the wavelet at time t
   */
  @Override
  public Complex wavelet(double t) {
    // Normalization factor: 1/sqrt(2*pi*fb)
    double norm = 1.0 / Math.sqrt(2.0 * Math.PI * _fb);
    
    // Gaussian envelope: exp(-t^2/(2*fb))
    double envelope = Math.exp(-t * t / (2.0 * _fb));
    
    // Complex sinusoid: exp(2*pi*i*fc*t)
    double phase = 2.0 * Math.PI * _fc * t;
    double real = Math.cos(phase);
    double imag = Math.sin(phase);
    
    // Combine all parts
    return new Complex(norm * envelope * real, norm * envelope * imag);
  }

  /**
   * Evaluates the Fourier transform of the Morlet wavelet.
   * F[psi](omega) = sqrt(2*pi*fb) * exp(-2*pi^2*fb*(omega/(2*pi) - fc)^2)
   * 
   * @param omega angular frequency
   * @return Fourier transform value at frequency omega
   */
  @Override
  public Complex fourierTransform(double omega) {
    // Convert angular frequency to ordinary frequency
    double f = omega / (2.0 * Math.PI);
    
    // Fourier transform is real-valued for Morlet wavelet
    double norm = Math.sqrt(2.0 * Math.PI * _fb);
    double exponent = -2.0 * Math.PI * Math.PI * _fb * (f - _fc) * (f - _fc);
    double value = norm * Math.exp(exponent);
    
    return new Complex(value, 0.0);
  }

  /**
   * Computes the admissibility constant for the Morlet wavelet.
   * For Morlet wavelet, this is approximately 2*pi when fc > 0.8.
   * 
   * @return admissibility constant
   */
  @Override
  public double getAdmissibilityConstant() {
    // The Morlet wavelet is approximately admissible when fc > 0.8
    // The exact value depends on fc and fb, but for practical purposes
    // we return 2*pi as an approximation
    if (_fc < 0.8) {
      // Warning: Morlet wavelet may not be strictly admissible for low center frequencies
      return 2.0 * Math.PI * 1.1; // Slightly higher to indicate potential issues
    }
    return 2.0 * Math.PI;
  }

  /**
   * Returns the effective support of the Morlet wavelet in time domain.
   * The wavelet has significant values within approximately +/- 4*sqrt(fb).
   * 
   * @return array of [min_t, max_t]
   */
  @Override
  public double[] getEffectiveSupport() {
    double range = 4.0 * Math.sqrt(_fb);
    return new double[] { -range, range };
  }

  /**
   * Returns the bandwidth of the Morlet wavelet in frequency domain.
   * The wavelet has significant values around fc +/- 2/(sqrt(2*pi*fb)).
   * 
   * @return array of [min_freq, max_freq]
   */
  @Override
  public double[] getBandwidth() {
    double halfWidth = 2.0 / Math.sqrt(2.0 * Math.PI * _fb);
    return new double[] { _fc - halfWidth, _fc + halfWidth };
  }

  /**
   * Get the bandwidth parameter.
   * 
   * @return bandwidth parameter fb
   */
  public double getBandwidthParameter() {
    return _fb;
  }

  /**
   * Get the center frequency parameter.
   * 
   * @return center frequency fc
   */
  public double getCenterFrequencyParameter() {
    return _fc;
  }
}