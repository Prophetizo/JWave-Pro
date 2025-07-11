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
 * Base class for continuous wavelets used in Continuous Wavelet Transform (CWT).
 * Unlike discrete wavelets, continuous wavelets can be evaluated at any scale
 * and translation, and often produce complex-valued outputs.
 *
 * @author Stephen Romano
 * @date 07.01.2025
 */
public abstract class ContinuousWavelet {

  /**
   * The name of the continuous wavelet.
   */
  protected String _name;

  /**
   * The center frequency of the wavelet (for frequency analysis).
   */
  protected double _centerFrequency;

  /**
   * Constructor setting the name of the wavelet.
   */
  public ContinuousWavelet() {
    _name = "ContinuousWavelet";
    _centerFrequency = 1.0;
  }

  /**
   * Returns the name of the wavelet.
   * 
   * @return name of the wavelet
   */
  public String getName() {
    return _name;
  }

  /**
   * Returns the center frequency of the wavelet.
   * 
   * @return center frequency
   */
  public double getCenterFrequency() {
    return _centerFrequency;
  }

  /**
   * Evaluates the wavelet function at a given time point.
   * This is the core method that each continuous wavelet must implement.
   * 
   * @param t time point
   * @return complex value of the wavelet at time t
   */
  public abstract Complex wavelet(double t);

  /**
   * Evaluates the scaled and translated wavelet function.
   * psi_{a,b}(t) = (1/sqrt(a)) * psi((t-b)/a)
   * 
   * @param t time point
   * @param scale scale parameter (a > 0)
   * @param translation translation parameter (b)
   * @return complex value of the scaled and translated wavelet
   */
  public Complex wavelet(double t, double scale, double translation) {
    if (scale <= 0) {
      throw new IllegalArgumentException("Scale must be positive");
    }
    
    // Apply scaling and translation: psi_{a,b}(t) = (1/sqrt(a)) * psi((t-b)/a)
    double scaledTime = (t - translation) / scale;
    Complex value = wavelet(scaledTime);
    
    // Apply normalization factor 1/sqrt(a)
    double normFactor = 1.0 / Math.sqrt(scale);
    return value.mul(normFactor);
  }

  /**
   * Evaluates the Fourier transform of the wavelet at a given frequency.
   * This is useful for FFT-based CWT implementations.
   * 
   * @param omega angular frequency
   * @return Fourier transform of the wavelet at frequency omega
   */
  public abstract Complex fourierTransform(double omega);

  /**
   * Evaluates the Fourier transform of the scaled wavelet.
   * Using the property: F[psi_{a,b}](omega) = sqrt(a) * exp(-i*omega*b) * F[psi](a*omega)
   * 
   * @param omega angular frequency
   * @param scale scale parameter (a > 0)
   * @param translation translation parameter (b)
   * @return Fourier transform of the scaled and translated wavelet
   */
  public Complex fourierTransform(double omega, double scale, double translation) {
    if (scale <= 0) {
      throw new IllegalArgumentException("Scale must be positive");
    }
    
    // F[psi_{a,b}](omega) = sqrt(a) * exp(-i*omega*b) * F[psi](a*omega)
    Complex ft = fourierTransform(scale * omega);
    
    // Apply scale factor sqrt(a)
    ft = ft.mul(Math.sqrt(scale));
    
    // Apply translation phase shift exp(-i*omega*b)
    if (translation != 0) {
      double phase = -omega * translation;
      Complex phaseShift = new Complex(Math.cos(phase), Math.sin(phase));
      ft = ft.mul(phaseShift);
    }
    
    return ft;
  }

  /**
   * Computes the admissibility constant for the wavelet.
   * For a wavelet to be admissible: C_psi = integral(|F[psi](omega)|^2 / |omega|) < infinity
   * This is required for the inverse CWT to exist.
   * 
   * @return admissibility constant (should be finite for valid wavelets)
   */
  public abstract double getAdmissibilityConstant();

  /**
   * Returns the effective support of the wavelet in time domain.
   * This helps determine the range of computation needed.
   * 
   * @return array of [min_t, max_t] where the wavelet has significant values
   */
  public abstract double[] getEffectiveSupport();

  /**
   * Returns the bandwidth of the wavelet in frequency domain.
   * Useful for determining frequency resolution.
   * 
   * @return array of [min_freq, max_freq] where the wavelet has significant values
   */
  public abstract double[] getBandwidth();
}