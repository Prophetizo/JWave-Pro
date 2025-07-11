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
 * DOG (Derivative of Gaussian) wavelet implementation for Continuous Wavelet Transform.
 * This wavelet is the n-th derivative of a Gaussian function.
 * 
 * The DOG wavelet of order n is defined as:
 * psi(t) = (-1)^(n+1) * (d^n/dt^n) * exp(-t^2/(2*sigma^2))
 * 
 * where n is the derivative order and sigma is the width parameter.
 * 
 * Special cases:
 * - n=1: First derivative of Gaussian (edge detection)
 * - n=2: Second derivative of Gaussian (Mexican Hat/Ricker wavelet)
 * - n=3: Third derivative (zero-crossing detection)
 * - n=4: Fourth derivative (ridge detection)
 * 
 * Key properties:
 * - Real-valued
 * - n zero crossings
 * - Excellent time localization
 * - Good for detecting singularities of order n
 * - Symmetric for even n, antisymmetric for odd n
 *
 * @author Stephen Romano
 * @date 07.01.2025
 */
public class DOGWavelet extends ContinuousWavelet {

  /**
   * Standard types of DOG wavelets with their derivative orders.
   */
  public enum WaveletType {
    EDGE(1, "Edge detection"),
    MEXICAN_HAT(2, "Mexican Hat / Ricker wavelet"),
    RICKER(2, "Ricker wavelet (alias for Mexican Hat)"),
    ZERO_CROSSING(3, "Zero-crossing detection"),
    RIDGE(4, "Ridge detection");
    
    private final int order;
    private final String description;
    
    WaveletType(int order, String description) {
      this.order = order;
      this.description = description;
    }
    
    public int getOrder() {
      return order;
    }
    
    public String getDescription() {
      return description;
    }
  }

  /**
   * Base support factor in standard deviations.
   * 3 sigma captures approximately 99.7% of the Gaussian envelope energy.
   */
  private static final double BASE_SUPPORT_FACTOR = 3.0;

  /**
   * Derivative order (must be positive integer).
   */
  private int _n;

  /**
   * Width parameter (standard deviation of the Gaussian).
   */
  private double _sigma;

  /**
   * Normalization constant for unit L2 norm.
   */
  private double _normConstant;

  /**
   * Hermite polynomial coefficients for efficient evaluation.
   * H_n(x) coefficients where psi(t) = norm * H_n(t/sigma) * exp(-t^2/(2*sigma^2))
   */
  private double[] _hermiteCoeffs;

  /**
   * Default constructor with second derivative (Mexican Hat) and sigma=1.
   */
  public DOGWavelet() {
    this(2, 1.0);
  }

  /**
   * Constructor with custom derivative order and default width.
   * 
   * @param n derivative order (must be positive integer, typically 1-6)
   */
  public DOGWavelet(int n) {
    this(n, 1.0);
  }

  /**
   * Constructor with custom derivative order and width parameter.
   * 
   * @param n derivative order (must be positive integer, typically 1-6)
   * @param sigma width parameter (must be positive)
   */
  public DOGWavelet(int n, double sigma) {
    super();
    
    if (n < 1) {
      throw new IllegalArgumentException("Derivative order n must be a positive integer");
    }
    if (n > 10) {
      throw new IllegalArgumentException("Derivative order n > 10 may cause numerical issues");
    }
    if (sigma <= 0) {
      throw new IllegalArgumentException("Width parameter sigma must be positive");
    }
    
    _name = "DOG (n=" + n + ")";
    _n = n;
    _sigma = sigma;
    
    // Compute Hermite polynomial coefficients
    _hermiteCoeffs = computeHermiteCoefficients(n);
    
    // Compute normalization constant for unit L2 norm
    _normConstant = computeNormalizationConstant(n, sigma);
    
    // Center frequency depends on derivative order and width
    // For DOG wavelet: f_c ≈ sqrt(n) / (2*pi*sigma)
    _centerFrequency = Math.sqrt(n) / (2.0 * Math.PI * sigma);
  }

  /**
   * Evaluates the DOG wavelet at a given time point.
   * psi(t) = norm * H_n(t/sigma) * exp(-t^2/(2*sigma^2))
   * where H_n is the Hermite polynomial of order n.
   * 
   * @param t time point
   * @return complex value of the wavelet at time t (imaginary part is always 0)
   */
  @Override
  public Complex wavelet(double t) {
    double x = t / _sigma;
    double gaussian = Math.exp(-0.5 * x * x);
    
    // Evaluate Hermite polynomial using Horner's method
    double hermite = evaluateHermitePolynomial(x);
    
    // Apply normalization
    double value = _normConstant * hermite * gaussian;
    
    return new Complex(value, 0.0);
  }

  /**
   * Evaluates the Fourier transform of the DOG wavelet.
   * F[psi](omega) = i^n * omega^n * sqrt(2*pi) * sigma^(n+1) * exp(-sigma^2*omega^2/2)
   * 
   * @param omega angular frequency
   * @return Fourier transform value at frequency omega
   */
  @Override
  public Complex fourierTransform(double omega) {
    // Fourier transform magnitude
    double magnitude = Math.sqrt(2.0 * Math.PI) * Math.pow(_sigma, _n + 1) * 
                      Math.pow(Math.abs(omega), _n) * 
                      Math.exp(-0.5 * _sigma * _sigma * omega * omega);
    
    // Apply normalization
    magnitude *= _normConstant;
    
    // Phase depends on derivative order: i^n
    // For real result, we need to consider the sign
    double real = 0, imag = 0;
    int nMod4 = _n % 4;
    
    switch (nMod4) {
      case 0: // i^n = 1
        real = magnitude;
        break;
      case 1: // i^n = i
        imag = magnitude * Math.signum(omega);
        break;
      case 2: // i^n = -1
        real = -magnitude;
        break;
      case 3: // i^n = -i
        imag = -magnitude * Math.signum(omega);
        break;
    }
    
    return new Complex(real, imag);
  }

  /**
   * Computes the admissibility constant for the DOG wavelet.
   * The DOG wavelet is always admissible for n >= 1.
   * 
   * @return admissibility constant
   */
  @Override
  public double getAdmissibilityConstant() {
    // For DOG wavelet: C_psi = 2*pi * integral of |F[psi](omega)|^2 / |omega|
    // This integral converges for all n >= 1
    // Exact value depends on n, but we return a standard approximation
    return 2.0 * Math.PI;
  }

  /**
   * Returns the effective support of the DOG wavelet in time domain.
   * The wavelet has significant values within approximately +/- (3 + n/2)*sigma.
   * 
   * The base factor of 3.0 represents approximately 3 standard deviations
   * where the Gaussian envelope has decayed to ~0.01 of its peak value.
   * The additional n/2 term accounts for the increased spread due to 
   * higher order derivatives.
   * 
   * @return array of [min_t, max_t]
   */
  @Override
  public double[] getEffectiveSupport() {
    // Base support: BASE_SUPPORT_FACTOR standard deviations captures ~99.7% of Gaussian energy
    // Additional support: n/2 accounts for derivative order spreading
    double range = (BASE_SUPPORT_FACTOR + _n / 2.0) * _sigma;
    return new double[] { -range, range };
  }

  /**
   * Returns the bandwidth of the DOG wavelet in frequency domain.
   * 
   * @return array of [min_freq, max_freq]
   */
  @Override
  public double[] getBandwidth() {
    // Bandwidth increases with derivative order
    double maxFreq = (1.0 + _n / 2.0) / (2.0 * Math.PI * _sigma);
    return new double[] { 0.0, maxFreq };
  }

  /**
   * Get the derivative order.
   * 
   * @return derivative order n
   */
  public int getDerivativeOrder() {
    return _n;
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
   * Compute Hermite polynomial coefficients for order n.
   * Uses the physicists' Hermite polynomials.
   * 
   * @param n order
   * @return coefficient array
   */
  private double[] computeHermiteCoefficients(int n) {
    // Hermite polynomials satisfy: H_n(x) = 2x*H_{n-1}(x) - 2(n-1)*H_{n-2}(x)
    // We'll compute coefficients for H_n(x) = sum(coeffs[k] * x^k)
    
    double[][] coeffs = new double[n + 1][];
    
    // H_0(x) = 1
    coeffs[0] = new double[] { 1.0 };
    
    if (n > 0) {
      // H_1(x) = 2x
      coeffs[1] = new double[] { 0.0, 2.0 };
    }
    
    // Compute higher order polynomials using recurrence
    for (int k = 2; k <= n; k++) {
      coeffs[k] = new double[k + 1];
      
      // H_k(x) = 2x*H_{k-1}(x) - 2(k-1)*H_{k-2}(x)
      // Multiply H_{k-1} by 2x (shift coefficients up)
      for (int i = 1; i <= k; i++) {
        if (i - 1 < coeffs[k - 1].length) {
          coeffs[k][i] += 2.0 * coeffs[k - 1][i - 1];
        }
      }
      
      // Subtract 2(k-1)*H_{k-2}
      for (int i = 0; i <= k - 2; i++) {
        coeffs[k][i] -= 2.0 * (k - 1) * coeffs[k - 2][i];
      }
    }
    
    // Apply (-1)^(n+1) factor for DOG wavelet sign convention
    // For n=1: (-1)^2 = 1
    // For n=2: (-1)^3 = -1  
    // For n=3: (-1)^4 = 1
    // For n=4: (-1)^5 = -1
    double sign = ((n + 1) % 2 == 0) ? 1.0 : -1.0;
    for (int i = 0; i < coeffs[n].length; i++) {
      coeffs[n][i] *= sign;
    }
    
    return coeffs[n];
  }

  /**
   * Evaluate Hermite polynomial at x using stored coefficients.
   * 
   * @param x evaluation point
   * @return H_n(x)
   */
  private double evaluateHermitePolynomial(double x) {
    double result = 0.0;
    
    for (int i = _hermiteCoeffs.length - 1; i >= 0; i--) {
      result = result * x + _hermiteCoeffs[i];
    }
    
    return result;
  }

  /**
   * Compute normalization constant for unit L2 norm.
   * 
   * @param n derivative order
   * @param sigma width parameter
   * @return normalization constant
   */
  private double computeNormalizationConstant(int n, double sigma) {
    // For DOG wavelet, the L2 norm depends on the derivative order
    // The normalization involves sqrt((2n-1)!! / (2^n * sqrt(pi) * sigma^(2n+1)))
    // where (2n-1)!! is the double factorial
    
    // General formula using double factorial
    double doubleFact = doubleFactorial(2 * n - 1);
    double normConst = Math.sqrt(doubleFact / (Math.pow(2, n) * Math.sqrt(Math.PI) * Math.pow(sigma, 2*n + 1)));
    
    return normConst;
  }
  
  /**
   * Compute double factorial (n!!)
   * n!! = n * (n-2) * (n-4) * ... * 1 or 2
   * 
   * @param n input value
   * @return n!!
   */
  private double doubleFactorial(int n) {
    double result = 1.0;
    for (int i = n; i > 0; i -= 2) {
      result *= i;
    }
    return result;
  }


  /**
   * Check if this DOG wavelet is equivalent to Mexican Hat.
   * 
   * @return true if n=2
   */
  public boolean isMexicanHat() {
    return _n == 2;
  }

  /**
   * Static factory method to create common DOG wavelets.
   * 
   * @param type type of DOG wavelet
   * @param sigma width parameter
   * @return DOGWavelet instance
   */
  public static DOGWavelet createStandard(WaveletType type, double sigma) {
    if (type == null) {
      throw new IllegalArgumentException("DOG wavelet type cannot be null");
    }
    return new DOGWavelet(type.getOrder(), sigma);
  }
}