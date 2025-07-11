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
 * Paul wavelet implementation for Continuous Wavelet Transform.
 * The Paul wavelet is a complex-valued wavelet that provides excellent
 * frequency localization and is analytic (no negative frequencies).
 * 
 * The Paul wavelet of order m is defined as:
 * psi(t) = (2^m * i^m * m!) / sqrt(pi * (2m)!) * (1 - it)^(-(m+1))
 * 
 * where m is the order parameter (typically 4).
 * 
 * Key properties:
 * - Complex-valued (analytic signal)
 * - Excellent frequency resolution
 * - No negative frequency components
 * - Asymmetric in time domain
 * - Often used for phase analysis
 *
 * @author Stephen Romano
 * @date 07.01.2025
 */
public class PaulWavelet extends ContinuousWavelet {

  /**
   * Order parameter m (must be a positive integer).
   * Default value is 4, which provides a good balance.
   */
  private int _m;

  /**
   * Normalization constant computed once in constructor.
   */
  private double _normConstant;

  /**
   * i^m precomputed for efficiency.
   */
  private Complex _iPowerM;

  /**
   * Default constructor with standard order (m=4).
   */
  public PaulWavelet() {
    this(4);
  }

  /**
   * Constructor with custom order parameter.
   * 
   * @param m order parameter (must be positive integer, typically 1-20)
   */
  public PaulWavelet(int m) {
    super();
    
    if (m < 1) {
      throw new IllegalArgumentException("Order parameter m must be a positive integer");
    }
    if (m > 20) {
      throw new IllegalArgumentException("Order parameter m > 20 may cause numerical issues");
    }
    
    _name = "Paul";
    _m = m;
    
    // Compute normalization constant: (2^m * m!) / sqrt(pi * (2m)!)
    _normConstant = Math.pow(2, m) * factorial(m) / 
                    Math.sqrt(Math.PI * factorial(2 * m));
    
    // Precompute i^m
    _iPowerM = computeIPowerM(m);
    
    // Center frequency for Paul wavelet
    _centerFrequency = (m + 0.5) / (2.0 * Math.PI);
  }

  /**
   * Evaluates the Paul wavelet at a given time point.
   * psi(t) = norm * i^m * (1 - it)^(-(m+1))
   * 
   * @param t time point
   * @return complex value of the wavelet at time t
   */
  @Override
  public Complex wavelet(double t) {
    // (1 - it)
    Complex oneMinusIT = new Complex(1.0, -t);
    
    // (1 - it)^(-(m+1))
    Complex power = complexPower(oneMinusIT, -(_m + 1));
    
    // norm * i^m * (1 - it)^(-(m+1))
    return _iPowerM.mul(_normConstant).mul(power);
  }

  /**
   * Evaluates the Fourier transform of the Paul wavelet.
   * F[psi](omega) = sqrt(2*pi) * omega^m * exp(-omega) * H(omega)
   * where H(omega) is the Heaviside step function (1 for omega > 0, 0 otherwise)
   * 
   * @param omega angular frequency
   * @return Fourier transform value at frequency omega
   */
  @Override
  public Complex fourierTransform(double omega) {
    // Paul wavelet has no negative frequency components (analytic signal)
    if (omega <= 0) {
      return new Complex(0, 0);
    }
    
    // sqrt(2*pi) * omega^m * exp(-omega)
    double value = Math.sqrt(2.0 * Math.PI) * 
                   Math.pow(omega, _m) * 
                   Math.exp(-omega);
    
    return new Complex(value, 0);
  }

  /**
   * Evaluates the Fourier transform at scaled frequency.
   * Override for efficiency to avoid unnecessary scaling operations.
   * 
   * @param omega angular frequency
   * @param scale scale parameter
   * @param translation translation parameter (ignored in frequency domain)
   * @return Fourier transform value
   */
  @Override
  public Complex fourierTransform(double omega, double scale, double translation) {
    if (omega <= 0) {
      return new Complex(0, 0);
    }
    
    // For Paul wavelet: F[psi_s](omega) = sqrt(s) * F[psi](s*omega)
    double scaledOmega = scale * omega;
    double value = Math.sqrt(scale) * Math.sqrt(2.0 * Math.PI) * 
                   Math.pow(scaledOmega, _m) * 
                   Math.exp(-scaledOmega);
    
    return new Complex(value, 0);
  }

  /**
   * Computes the admissibility constant for the Paul wavelet.
   * For Paul wavelet of order m: C_psi = 2*pi / (2m + 1)
   * 
   * @return admissibility constant
   */
  @Override
  public double getAdmissibilityConstant() {
    return 2.0 * Math.PI / (2 * _m + 1);
  }

  /**
   * Returns the effective support of the Paul wavelet in time domain.
   * The Paul wavelet decays as |t|^(-(m+1)), so we use a cutoff
   * where the magnitude drops below a threshold.
   * 
   * @return array of [min_t, max_t]
   */
  @Override
  public double[] getEffectiveSupport() {
    // For Paul wavelet, significant values are asymmetric
    // Most energy is for t > -1, and decays as t^(-(m+1)) for large t
    double minT = -1.0;
    double maxT = 2.0 * (_m + 1);  // Rough estimate for 99% energy
    return new double[] { minT, maxT };
  }

  /**
   * Returns the bandwidth of the Paul wavelet in frequency domain.
   * The Paul wavelet has peak at omega = m and decays exponentially.
   * 
   * @return array of [min_freq, max_freq]
   */
  @Override
  public double[] getBandwidth() {
    // Convert angular frequency to ordinary frequency
    // Peak is at omega = m, significant values up to omega = 2m + 2
    double minFreq = 0.0;  // Analytic signal, no negative frequencies
    double maxFreq = (2 * _m + 2) / (2.0 * Math.PI);
    return new double[] { minFreq, maxFreq };
  }

  /**
   * Get the order parameter.
   * 
   * @return order parameter m
   */
  public int getOrder() {
    return _m;
  }

  /**
   * Compute factorial of n.
   * Uses double to handle larger values.
   * 
   * @param n input value
   * @return n!
   */
  private double factorial(int n) {
    double result = 1.0;
    for (int i = 2; i <= n; i++) {
      result *= i;
    }
    return result;
  }

  /**
   * Compute i^m where i is the imaginary unit.
   * i^1 = i, i^2 = -1, i^3 = -i, i^4 = 1, ...
   * 
   * @param m power
   * @return i^m as a complex number
   */
  private Complex computeIPowerM(int m) {
    int mMod4 = m % 4;
    switch (mMod4) {
      case 0: return new Complex(1, 0);   // i^0 = 1
      case 1: return new Complex(0, 1);   // i^1 = i
      case 2: return new Complex(-1, 0);  // i^2 = -1
      case 3: return new Complex(0, -1);  // i^3 = -i
      default: 
        throw new AssertionError("Unexpected modulo result: " + mMod4 + 
                                " (this should never happen for m % 4)");
    }
  }

  /**
   * Compute complex number raised to real power.
   * z^p = |z|^p * exp(i * p * arg(z))
   * 
   * @param z complex number
   * @param p real power
   * @return z^p
   */
  private Complex complexPower(Complex z, double p) {
    double mag = z.getMag();
    double arg = Math.atan2(z.getImag(), z.getReal());
    
    double newMag = Math.pow(mag, p);
    double newArg = p * arg;
    
    return new Complex(newMag * Math.cos(newArg), 
                      newMag * Math.sin(newArg));
  }

  /**
   * Static method to create a Paul wavelet with specific time-frequency balance.
   * Higher order gives better frequency resolution but poorer time resolution.
   * 
   * @param frequencyResolution desired frequency resolution (1=low, 10=high)
   * @return PaulWavelet with appropriate order
   */
  public static PaulWavelet fromResolutionBalance(double frequencyResolution) {
    if (frequencyResolution < 1 || frequencyResolution > 10) {
      throw new IllegalArgumentException("Resolution balance must be between 1 and 10");
    }
    
    // Map resolution balance (1-10) to order (2-20) with step of 2
    // Formula: order = 2 + (frequencyResolution - 1) * 2
    // This gives: 1→2, 2→4, 3→6, 4→8, 5→10, 6→12, 7→14, 8→16, 9→18, 10→20
    // All resulting orders are within constructor limit (m ≤ 20)
    int order = (int) Math.round(2 + (frequencyResolution - 1) * 2);
    return new PaulWavelet(order);
  }
}