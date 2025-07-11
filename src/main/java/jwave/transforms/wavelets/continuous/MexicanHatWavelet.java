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
 * Mexican Hat wavelet (also known as Ricker wavelet) implementation for 
 * Continuous Wavelet Transform. This is the negative normalized second 
 * derivative of a Gaussian function.
 * 
 * The Mexican Hat wavelet is defined as:
 * psi(t) = (2/(sqrt(3*sigma)*pi^(1/4))) * (1 - (t/sigma)^2) * exp(-t^2/(2*sigma^2))
 * 
 * This is a real-valued wavelet that is particularly useful for edge detection
 * and identifying singularities in signals.
 *
 * @author Stephen Romano
 * @date 07.01.2025
 */
public class MexicanHatWavelet extends ContinuousWavelet {

  /**
   * Width parameter (standard deviation of the Gaussian).
   * Default value is 1.0.
   */
  private double _sigma;

  /**
   * Normalization constant computed once in constructor.
   */
  private double _normConstant;

  /**
   * Default constructor with standard width parameter (sigma=1).
   */
  public MexicanHatWavelet() {
    this(1.0);
  }

  /**
   * Constructor with custom width parameter.
   * 
   * @param sigma width parameter (must be positive)
   */
  public MexicanHatWavelet(double sigma) {
    super();
    
    if (sigma <= 0) {
      throw new IllegalArgumentException("Width parameter sigma must be positive");
    }
    
    _name = "Mexican Hat (Ricker)";
    _sigma = sigma;
    
    // Compute normalization constant: 2/(sqrt(3*sigma)*pi^(1/4))
    _normConstant = 2.0 / (Math.sqrt(3.0 * sigma) * Math.pow(Math.PI, 0.25));
    
    // Center frequency for Mexican Hat wavelet
    _centerFrequency = 1.0 / (2.0 * Math.PI * sigma);
  }

  /**
   * Evaluates the Mexican Hat wavelet at a given time point.
   * psi(t) = norm * (1 - (t/sigma)^2) * exp(-t^2/(2*sigma^2))
   * 
   * @param t time point
   * @return complex value of the wavelet at time t (imaginary part is always 0)
   */
  @Override
  public Complex wavelet(double t) {
    double tNorm = t / _sigma;
    double tNorm2 = tNorm * tNorm;
    
    // (1 - (t/sigma)^2) * exp(-t^2/(2*sigma^2))
    double value = _normConstant * (1.0 - tNorm2) * Math.exp(-0.5 * tNorm2);
    
    // Mexican Hat is real-valued
    return new Complex(value, 0.0);
  }

  /**
   * Evaluates the Fourier transform of the Mexican Hat wavelet.
   * F[psi](omega) = norm * sigma * sqrt(2*pi) * omega^2 * exp(-sigma^2*omega^2/2)
   * 
   * @param omega angular frequency
   * @return Fourier transform value at frequency omega
   */
  @Override
  public Complex fourierTransform(double omega) {
    // Fourier transform normalization includes the original norm constant
    double ftNorm = _normConstant * _sigma * Math.sqrt(2.0 * Math.PI);
    
    // omega^2 * exp(-sigma^2*omega^2/2)
    double omega2 = omega * omega;
    double value = ftNorm * omega2 * Math.exp(-0.5 * _sigma * _sigma * omega2);
    
    // Fourier transform is real-valued for Mexican Hat
    return new Complex(value, 0.0);
  }

  /**
   * Computes the admissibility constant for the Mexican Hat wavelet.
   * The Mexican Hat wavelet is admissible with C_psi = pi.
   * 
   * @return admissibility constant (pi)
   */
  @Override
  public double getAdmissibilityConstant() {
    return Math.PI;
  }

  /**
   * Returns the effective support of the Mexican Hat wavelet in time domain.
   * The wavelet has significant values within approximately +/- 5*sigma.
   * 
   * @return array of [min_t, max_t]
   */
  @Override
  public double[] getEffectiveSupport() {
    double range = 5.0 * _sigma;
    return new double[] { -range, range };
  }

  /**
   * Returns the bandwidth of the Mexican Hat wavelet in frequency domain.
   * The wavelet has its peak at omega = 1/sigma and significant values
   * in the range [0, 3/sigma].
   * 
   * @return array of [min_freq, max_freq]
   */
  @Override
  public double[] getBandwidth() {
    // Convert to ordinary frequency (Hz)
    double peakFreq = 1.0 / (2.0 * Math.PI * _sigma);
    double maxFreq = 3.0 / (2.0 * Math.PI * _sigma);
    return new double[] { 0.0, maxFreq };
  }

  /**
   * Get the width parameter.
   * 
   * @return width parameter sigma
   */
  public double getSigma() {
    return _sigma;
  }

  /**
   * Static method to create a Mexican Hat wavelet with a specific center frequency.
   * This is useful when you want to specify the wavelet by its frequency characteristics.
   * 
   * @param centerFreq desired center frequency
   * @return MexicanHatWavelet with appropriate sigma
   */
  public static MexicanHatWavelet fromCenterFrequency(double centerFreq) {
    if (centerFreq <= 0) {
      throw new IllegalArgumentException("Center frequency must be positive");
    }
    // fc = 1/(2*pi*sigma), so sigma = 1/(2*pi*fc)
    double sigma = 1.0 / (2.0 * Math.PI * centerFreq);
    return new MexicanHatWavelet(sigma);
  }
}