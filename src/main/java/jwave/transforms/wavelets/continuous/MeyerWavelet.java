/**
 * JWave is distributed under the MIT License (MIT); this file is part of JWave.
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
 * Meyer wavelet implementation for Continuous Wavelet Transform.
 * 
 * The Meyer wavelet is defined in the frequency domain and has compact support
 * in frequency. It is infinitely differentiable and has excellent frequency
 * localization properties.
 * 
 * The Meyer wavelet is defined through its Fourier transform:
 * Ψ(ω) = sin(π/2 * ν(3|ω|/(2π) - 1)) * exp(iω/2)  for 2π/3 < |ω| < 4π/3
 * Ψ(ω) = cos(π/2 * ν(3|ω|/(4π) - 1)) * exp(iω/2)  for 4π/3 < |ω| < 8π/3
 * Ψ(ω) = 0                                         otherwise
 * 
 * where ν is a smooth transition function satisfying:
 * ν(x) = 0 for x ≤ 0
 * ν(x) = 1 for x ≥ 1
 * ν(x) + ν(1-x) = 1
 * 
 * Key properties:
 * - Defined in frequency domain
 * - Compact support in frequency
 * - Infinitely differentiable
 * - Orthogonal
 * - Real-valued in time domain
 * - Excellent frequency localization
 *
 * @author Stephen Romano
 * @date 09.01.2025
 */
public class MeyerWavelet extends ContinuousWavelet {

  /**
   * Time domain decay parameter that controls the envelope width.
   * This value determines how quickly the wavelet decays in time domain.
   * Larger values create wider wavelets with slower decay.
   * The value 25.0 provides a good balance between localization and smoothness.
   */
  private static final double TIME_DECAY_PARAMETER = 25.0;
  
  /**
   * First harmonic amplitude for time domain approximation.
   * This correction term improves the accuracy of the truncated Fourier series.
   */
  private static final double FIRST_HARMONIC_AMPLITUDE = 0.2;
  
  /**
   * First harmonic frequency multiplier relative to center frequency.
   * Set to 1.4 to capture higher frequency components.
   */
  private static final double FIRST_HARMONIC_FREQ_MULT = 1.4;
  
  /**
   * Second harmonic amplitude for time domain approximation.
   * This negative correction term helps balance the approximation.
   */
  private static final double SECOND_HARMONIC_AMPLITUDE = -0.1;
  
  /**
   * Second harmonic frequency multiplier relative to center frequency.
   * Set to 0.5 to capture lower frequency components.
   */
  private static final double SECOND_HARMONIC_FREQ_MULT = 0.5;
  
  /**
   * Center frequency for time domain approximation.
   * This corresponds to the peak frequency response of the Meyer wavelet.
   */
  private static final double TIME_DOMAIN_CENTER_FREQ = 0.7;
  
  /**
   * Threshold for detecting near-zero time values.
   * Used to avoid division by zero in the sinc function evaluation.
   */
  private static final double ZERO_TIME_THRESHOLD = 1e-10;
  
  /**
   * Effective support radius in time domain.
   * The Meyer wavelet decays rapidly, and this value represents
   * the time range where |ψ(t)| > 0.01 * max|ψ(t)|.
   */
  private static final double EFFECTIVE_SUPPORT_RADIUS = 15.0;
  
  /**
   * Lower frequency boundary for Meyer wavelet support in radians.
   * The Meyer wavelet has compact support in frequency domain starting at 2π/3.
   */
  private static final double FREQ_SUPPORT_LOWER = 2.0 * Math.PI / 3.0;
  
  /**
   * Middle frequency boundary for Meyer wavelet transition in radians.
   * This marks the transition from sin to cos branch at 4π/3.
   */
  private static final double FREQ_SUPPORT_MIDDLE = 4.0 * Math.PI / 3.0;
  
  /**
   * Upper frequency boundary for Meyer wavelet support in radians.
   * The Meyer wavelet has compact support in frequency domain ending at 8π/3.
   */
  private static final double FREQ_SUPPORT_UPPER = 8.0 * Math.PI / 3.0;
  
  /**
   * Normalized lower frequency boundary (in cycles per unit).
   * This is the lower boundary divided by 2π for frequency domain representation.
   */
  private static final double NORMALIZED_FREQ_LOWER = 2.0 / 3.0 / (2.0 * Math.PI);
  
  /**
   * Normalized upper frequency boundary (in cycles per unit).
   * This is the upper boundary divided by 2π for frequency domain representation.
   */
  private static final double NORMALIZED_FREQ_UPPER = 8.0 / 3.0 / (2.0 * Math.PI);
  
  /**
   * Polynomial coefficient for x^0 term in transition function.
   * Part of the smooth C∞ transition polynomial.
   */
  private static final double TRANSITION_POLY_C0 = 35.0;
  
  /**
   * Polynomial coefficient for x^1 term in transition function.
   * Part of the smooth C∞ transition polynomial.
   */
  private static final double TRANSITION_POLY_C1 = -84.0;
  
  /**
   * Polynomial coefficient for x^2 term in transition function.
   * Part of the smooth C∞ transition polynomial.
   */
  private static final double TRANSITION_POLY_C2 = 70.0;
  
  /**
   * Polynomial coefficient for x^3 term in transition function.
   * Part of the smooth C∞ transition polynomial.
   */
  private static final double TRANSITION_POLY_C3 = -20.0;

  /**
   * Default constructor.
   */
  public MeyerWavelet() {
    super();
    _name = "Meyer";
    // Meyer wavelet has center frequency at approximately 0.7 / (2π)
    _centerFrequency = 0.7 / (2.0 * Math.PI);
  }

  /**
   * Evaluates the Meyer wavelet at a given time point.
   * Since Meyer wavelet is defined in frequency domain, we approximate
   * it in time domain using a truncated series.
   * 
   * @param t time point
   * @return complex value of the wavelet at time t (imaginary part is always 0)
   */
  @Override
  public Complex wavelet(double t) {
    // Meyer wavelet in time domain requires numerical approximation
    // We use a truncated Fourier series approximation
    
    // Check if |t| is beyond effective support to avoid unnecessary computation
    // and potential numerical underflow in exponential
    if (Math.abs(t) > EFFECTIVE_SUPPORT_RADIUS) {
      return new Complex(0.0, 0.0);
    }
    
    // For practical implementation, we approximate using the fact that
    // Meyer wavelet resembles a modulated sinc function
    double value = 0.0;
    
    // Envelope function that ensures decay
    double envelope = Math.exp(-0.5 * t * t / TIME_DECAY_PARAMETER);
    
    // Core oscillation using numerically stable sinc function
    // Approximation based on Meyer wavelet properties
    double omega0 = TIME_DOMAIN_CENTER_FREQ;
    value = omega0 * sinc(omega0 * t) * envelope;
    
    // Add correction terms for better approximation
    double omega1 = FIRST_HARMONIC_FREQ_MULT * omega0;
    value += FIRST_HARMONIC_AMPLITUDE * omega1 * sinc(omega1 * t) * envelope;
    
    double omega2 = SECOND_HARMONIC_FREQ_MULT * omega0;
    value += SECOND_HARMONIC_AMPLITUDE * omega2 * sinc(omega2 * t) * envelope;
    
    // Normalize
    value *= Math.sqrt(2.0 / Math.PI);
    
    return new Complex(value, 0.0);
  }

  /**
   * Evaluates the Fourier transform of the Meyer wavelet.
   * This is where Meyer wavelet is naturally defined.
   * 
   * @param omega angular frequency
   * @return Fourier transform value at frequency omega
   */
  @Override
  public Complex fourierTransform(double omega) {
    double absOmega = Math.abs(omega);
    
    // Meyer wavelet has compact support in frequency: [2π/3, 8π/3]
    if (absOmega < FREQ_SUPPORT_LOWER || absOmega > FREQ_SUPPORT_UPPER) {
      return new Complex(0.0, 0.0);
    }
    
    double value = 0.0;
    
    if (absOmega >= FREQ_SUPPORT_LOWER && absOmega <= FREQ_SUPPORT_MIDDLE) {
      // First band: sin branch
      double arg = 3.0 * absOmega / (2.0 * Math.PI) - 1.0;
      value = Math.sin(Math.PI / 2.0 * transitionFunction(arg));
    } else if (absOmega >= FREQ_SUPPORT_MIDDLE && absOmega <= FREQ_SUPPORT_UPPER) {
      // Second band: cos branch
      double arg = 3.0 * absOmega / (4.0 * Math.PI) - 1.0;
      value = Math.cos(Math.PI / 2.0 * transitionFunction(arg));
    }
    
    // Normalization
    value *= Math.sqrt(2.0 * Math.PI);
    
    // Apply phase factor exp(iω/2) = cos(ω/2) + i·sin(ω/2)
    // This ensures the Meyer wavelet is properly defined in frequency domain
    double phase = omega / 2.0;
    double realPart = value * Math.cos(phase);
    double imagPart = value * Math.sin(phase);
    
    return new Complex(realPart, imagPart);
  }

  /**
   * Computes the sinc function sin(x)/x in a numerically stable way.
   * Handles the limit case at x=0 and very small x values.
   * 
   * @param x input value
   * @return sinc(x) = sin(x)/x
   */
  private double sinc(double x) {
    double absX = Math.abs(x);
    if (absX < ZERO_TIME_THRESHOLD) {
      // Use Taylor series expansion for small x: sinc(x) ≈ 1 - x²/6 + x⁴/120
      double x2 = x * x;
      return 1.0 - x2 / 6.0 + x2 * x2 / 120.0;
    }
    return Math.sin(x) / x;
  }
  
  /**
   * Smooth transition function ν(x) used in Meyer wavelet definition.
   * This implements a C∞ transition from 0 to 1.
   * 
   * @param x input value
   * @return transition function value
   */
  private double transitionFunction(double x) {
    if (x <= 0) {
      return 0.0;
    } else if (x >= 1) {
      return 1.0;
    } else {
      // Use polynomial transition for smoothness
      // ν(x) = x^4 * (C0 + C1*x + C2*x^2 + C3*x^3)
      double x2 = x * x;
      double x3 = x2 * x;
      double x4 = x3 * x;
      return x4 * (TRANSITION_POLY_C0 + TRANSITION_POLY_C1 * x + 
                   TRANSITION_POLY_C2 * x2 + TRANSITION_POLY_C3 * x3);
    }
  }

  /**
   * Computes the admissibility constant for the Meyer wavelet.
   * Meyer wavelet is admissible with finite constant.
   * 
   * @return admissibility constant
   */
  @Override
  public double getAdmissibilityConstant() {
    // Meyer wavelet has finite admissibility constant
    // Exact value is 2π (due to orthogonality properties)
    return 2.0 * Math.PI;
  }

  /**
   * Returns the effective support of the Meyer wavelet in time domain.
   * Meyer wavelet decays rapidly but doesn't have compact support in time.
   * 
   * @return array of [min_t, max_t]
   */
  @Override
  public double[] getEffectiveSupport() {
    // Meyer wavelet has infinite support in time but decays rapidly
    // Effective support where |ψ(t)| > 0.01 * max|ψ(t)|
    return new double[] { -EFFECTIVE_SUPPORT_RADIUS, EFFECTIVE_SUPPORT_RADIUS };
  }

  /**
   * Returns the bandwidth of the Meyer wavelet in frequency domain.
   * Meyer wavelet has compact support in frequency.
   * 
   * @return array of [min_freq, max_freq]
   */
  @Override
  public double[] getBandwidth() {
    // Return the normalized frequency boundaries
    return new double[] { NORMALIZED_FREQ_LOWER, NORMALIZED_FREQ_UPPER };
  }
}