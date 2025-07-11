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

import jwave.datatypes.natives.Complex;
import jwave.exceptions.JWaveException;
import jwave.transforms.wavelets.continuous.ContinuousWavelet;
import jwave.utils.MathUtils;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.stream.IntStream;

/**
 * Continuous Wavelet Transform (CWT) implementation.
 * 
 * The CWT provides a time-scale representation of a signal by convolving
 * it with scaled and translated versions of a mother wavelet. Unlike the
 * discrete wavelet transform, CWT can use any scale and translation values,
 * providing a highly redundant but information-rich representation.
 *
 * @author Stephen Romano
 * @date 07.01.2025
 */
public class ContinuousWaveletTransform extends BasicTransform {

  /**
   * The continuous wavelet to use for the transform.
   */
  private ContinuousWavelet _wavelet;

  /**
   * Parallelization thresholds for different signal sizes.
   * These constants define when to use parallel processing based on problem size.
   */
  private static final int TINY_SIGNAL_LENGTH = 64;      // Signals smaller than this are never parallelized
  private static final int SMALL_SIGNAL_LENGTH = 256;    // Small signals need more scales to benefit from parallelization
  private static final int SCALES_THRESHOLD_SMALL = 16;  // Minimum scales for small signals
  private static final int SCALES_THRESHOLD_LARGE = 8;   // Minimum scales for larger signals
  
  /**
   * Block size for cache-friendly processing.
   * This should fit comfortably in L1/L2 cache.
   */
  private static final int CACHE_BLOCK_SIZE = 64;
  
  /**
   * Minimum number of scales per fork/join task.
   * Below this threshold, tasks compute directly rather than forking.
   */
  private static final int FORK_JOIN_THRESHOLD = 4;

  /**
   * Padding type for boundary handling.
   */
  public enum PaddingType {
    ZERO,      // Zero padding
    SYMMETRIC, // Symmetric extension
    PERIODIC,  // Periodic extension
    CONSTANT   // Constant extension using edge values
  }

  /**
   * The padding type to use.
   */
  private PaddingType _paddingType;

  /**
   * Constructor with a continuous wavelet.
   * 
   * @param wavelet the continuous wavelet to use
   */
  public ContinuousWaveletTransform(ContinuousWavelet wavelet) {
    this(wavelet, PaddingType.SYMMETRIC);
  }

  /**
   * Constructor with a continuous wavelet and padding type.
   * 
   * @param wavelet the continuous wavelet to use
   * @param paddingType the padding type for boundary handling
   */
  public ContinuousWaveletTransform(ContinuousWavelet wavelet, PaddingType paddingType) {
    super();
    _name = "Continuous Wavelet Transform (" + wavelet.getName() + ")";
    _wavelet = wavelet;
    _paddingType = paddingType;
  }

  /**
   * Forward transform is not implemented for CWT in the standard way.
   * Use the transform methods with scale parameters instead.
   * 
   * @param arrTime input signal
   * @return throws exception
   * @throws JWaveException always thrown
   */
  @Override
  public double[] forward(double[] arrTime) throws JWaveException {
    throw new JWaveException("CWT requires scale parameters. Use transform() method instead.");
  }

  /**
   * Reverse transform is not implemented for CWT in the standard way.
   * CWT inverse requires the scale parameters and admissibility constant.
   * 
   * @param arrFreq frequency domain signal
   * @return throws exception
   * @throws JWaveException always thrown
   */
  @Override
  public double[] reverse(double[] arrFreq) throws JWaveException {
    throw new JWaveException("CWT inverse requires scale parameters and is not fully implemented.");
  }

  /**
   * Perform the continuous wavelet transform on a signal.
   * 
   * @param signal input signal
   * @param scales array of scale values to use
   * @return CWTResult containing the transform coefficients
   */
  public CWTResult transform(double[] signal, double[] scales) {
    return transform(signal, scales, 1.0);
  }

  /**
   * Perform the continuous wavelet transform on a signal with specified sampling rate.
   * 
   * @param signal input signal
   * @param scales array of scale values to use
   * @param samplingRate sampling rate of the signal (Hz)
   * @return CWTResult containing the transform coefficients
   */
  public CWTResult transform(double[] signal, double[] scales, double samplingRate) {
    int signalLength = signal.length;
    int nScales = scales.length;
    
    // Create time axis and initialize coefficient matrix
    double[] timeAxis = createTimeAxis(signalLength, samplingRate);
    Complex[][] coefficients = initializeCoefficients(nScales, signalLength);
    
    // Perform CWT for each scale
    for (int scaleIdx = 0; scaleIdx < nScales; scaleIdx++) {
      double scale = scales[scaleIdx];
      
      // For each time point in the signal
      for (int timeIdx = 0; timeIdx < signalLength; timeIdx++) {
        coefficients[scaleIdx][timeIdx] = computeCoefficient(signal, timeIdx, scale, samplingRate);
      }
    }
    
    return new CWTResult(coefficients, scales, timeAxis, samplingRate, _wavelet.getName());
  }

  /**
   * Perform FFT-based continuous wavelet transform for better performance.
   * This method is much faster for long signals.
   * 
   * @param signal input signal
   * @param scales array of scale values to use
   * @param samplingRate sampling rate of the signal (Hz)
   * @return CWTResult containing the transform coefficients
   */
  public CWTResult transformFFT(double[] signal, double[] scales, double samplingRate) {
    int signalLength = signal.length;
    int nScales = scales.length;
    
    // Pad signal to next power of 2 for FFT efficiency
    int paddedLength = MathUtils.nextPowerOfTwo(signalLength);
    double[] paddedSignal = padSignal(signal, paddedLength);
    
    // Compute FFT of the signal
    Complex[] signalFFT = computeFFT(paddedSignal);
    
    // Create frequency axis
    double[] omega = createFrequencyAxis(paddedLength, samplingRate);
    
    // Create time axis and initialize coefficient matrix
    double[] timeAxis = createTimeAxis(signalLength, samplingRate);
    Complex[][] coefficients = initializeCoefficients(nScales, signalLength);
    
    // Perform CWT for each scale using FFT
    for (int scaleIdx = 0; scaleIdx < nScales; scaleIdx++) {
      double scale = scales[scaleIdx];
      
      // Compute wavelet FFT at this scale
      Complex[] waveletFFT = new Complex[paddedLength];
      for (int i = 0; i < paddedLength; i++) {
        waveletFFT[i] = _wavelet.fourierTransform(omega[i], scale, 0);
        // Take complex conjugate for convolution
        waveletFFT[i] = waveletFFT[i].conjugate();
      }
      
      // Multiply in frequency domain
      Complex[] product = new Complex[paddedLength];
      for (int i = 0; i < paddedLength; i++) {
        product[i] = signalFFT[i].mul(waveletFFT[i]);
      }
      
      // Inverse FFT
      Complex[] result = computeIFFT(product);
      
      // Extract relevant part and store
      for (int timeIdx = 0; timeIdx < signalLength; timeIdx++) {
        coefficients[scaleIdx][timeIdx] = result[timeIdx];
      }
    }
    
    return new CWTResult(coefficients, scales, timeAxis, samplingRate, _wavelet.getName());
  }

  /**
   * Compute a single CWT coefficient using direct convolution.
   * 
   * @param signal input signal
   * @param timeIdx time index
   * @param scale scale value
   * @param samplingRate sampling rate
   * @return complex coefficient
   */
  private Complex computeCoefficient(double[] signal, int timeIdx, double scale, double samplingRate) {
    Complex sum = new Complex(0, 0);
    double dt = 1.0 / samplingRate;
    
    // Get effective support of the wavelet
    double[] support = _wavelet.getEffectiveSupport();
    int minIdx = Math.max(0, timeIdx + (int)(support[0] * scale * samplingRate));
    int maxIdx = Math.min(signal.length - 1, timeIdx + (int)(support[1] * scale * samplingRate));
    
    // Perform convolution
    for (int i = minIdx; i <= maxIdx; i++) {
      double t = (i - timeIdx) * dt;
      Complex waveletValue = _wavelet.wavelet(t, scale, 0);
      // Take complex conjugate for convolution
      waveletValue = waveletValue.conjugate();
      sum = sum.add(waveletValue.mul(signal[i]));
    }
    
    // Multiply by dt for numerical integration
    return sum.mul(dt);
  }

  /**
   * Pad signal according to the specified padding type.
   * 
   * @param signal input signal
   * @param targetLength target length after padding
   * @return padded signal
   */
  private double[] padSignal(double[] signal, int targetLength) {
    double[] padded = new double[targetLength];
    int signalLength = signal.length;
    
    // Copy original signal
    System.arraycopy(signal, 0, padded, 0, signalLength);
    
    // Apply padding
    switch (_paddingType) {
      case ZERO:
        // Already zero-padded
        break;
        
      case SYMMETRIC:
        for (int i = signalLength; i < targetLength; i++) {
          int mirrorIdx = 2 * signalLength - i - 2;
          if (mirrorIdx >= 0 && mirrorIdx < signalLength) {
            padded[i] = signal[mirrorIdx];
          }
        }
        break;
        
      case PERIODIC:
        for (int i = signalLength; i < targetLength; i++) {
          padded[i] = signal[i % signalLength];
        }
        break;
        
      case CONSTANT:
        double lastValue = signal[signalLength - 1];
        for (int i = signalLength; i < targetLength; i++) {
          padded[i] = lastValue;
        }
        break;
    }
    
    return padded;
  }


  /**
   * Compute FFT of a real signal using JWave's Fast Fourier Transform.
   * 
   * Uses O(n log n) algorithms:
   * - Cooley-Tukey for power-of-2 lengths
   * - Bluestein's chirp z-transform for arbitrary lengths
   * 
   * @param signal real input signal
   * @return complex FFT result
   */
  private Complex[] computeFFT(double[] signal) {
    int n = signal.length;
    
    // Convert to complex
    Complex[] complexSignal = new Complex[n];
    for (int i = 0; i < n; i++) {
      complexSignal[i] = new Complex(signal[i], 0);
    }
    
    // Use JWave's Fast Fourier Transform (O(n log n) complexity)
    FastFourierTransform fft = new FastFourierTransform();
    return fft.forward(complexSignal);
  }

  /**
   * Compute inverse FFT using JWave's Fast Fourier Transform.
   * 
   * Uses O(n log n) algorithms for all input sizes.
   * 
   * @param spectrum complex frequency domain signal
   * @return complex time domain result
   */
  private Complex[] computeIFFT(Complex[] spectrum) {
    // Use JWave's Fast Fourier Transform (O(n log n) complexity)
    FastFourierTransform fft = new FastFourierTransform();
    return fft.reverse(spectrum);
  }

  /**
   * Generate logarithmically spaced scales.
   * 
   * @param minScale minimum scale
   * @param maxScale maximum scale
   * @param numScales number of scales
   * @return array of scale values
   */
  public static double[] generateLogScales(double minScale, double maxScale, int numScales) {
    if (minScale <= 0 || maxScale <= 0) {
      throw new IllegalArgumentException("Scales must be positive");
    }
    if (minScale >= maxScale) {
      throw new IllegalArgumentException("minScale must be less than maxScale");
    }
    if (numScales < 2) {
      throw new IllegalArgumentException("Need at least 2 scales");
    }
    
    double[] scales = new double[numScales];
    double logMin = Math.log(minScale);
    double logMax = Math.log(maxScale);
    double logStep = (logMax - logMin) / (numScales - 1);
    
    for (int i = 0; i < numScales; i++) {
      scales[i] = Math.exp(logMin + i * logStep);
    }
    
    return scales;
  }

  /**
   * Generate linearly spaced scales.
   * 
   * @param minScale minimum scale
   * @param maxScale maximum scale
   * @param numScales number of scales
   * @return array of scale values
   */
  public static double[] generateLinearScales(double minScale, double maxScale, int numScales) {
    if (minScale <= 0 || maxScale <= 0) {
      throw new IllegalArgumentException("Scales must be positive");
    }
    if (minScale >= maxScale) {
      throw new IllegalArgumentException("minScale must be less than maxScale");
    }
    if (numScales < 2) {
      throw new IllegalArgumentException("Need at least 2 scales");
    }
    
    double[] scales = new double[numScales];
    double step = (maxScale - minScale) / (numScales - 1);
    
    for (int i = 0; i < numScales; i++) {
      scales[i] = minScale + i * step;
    }
    
    return scales;
  }

  /**
   * Get the wavelet used in this transform.
   * 
   * @return the continuous wavelet
   */
  public ContinuousWavelet getContinuousWavelet() {
    return _wavelet;
  }

  /**
   * Create time axis array for the given signal length and sampling rate.
   * 
   * @param signalLength length of the signal
   * @param samplingRate sampling rate in Hz
   * @return time axis array
   */
  private double[] createTimeAxis(int signalLength, double samplingRate) {
    double[] timeAxis = new double[signalLength];
    double dt = 1.0 / samplingRate;
    for (int i = 0; i < signalLength; i++) {
      timeAxis[i] = i * dt;
    }
    return timeAxis;
  }

  /**
   * Initialize coefficient matrix for CWT results.
   * 
   * @param nScales number of scales
   * @param signalLength length of the signal
   * @return initialized coefficient matrix
   */
  private Complex[][] initializeCoefficients(int nScales, int signalLength) {
    return new Complex[nScales][signalLength];
  }

  /**
   * Create frequency axis array for FFT-based methods.
   * 
   * @param paddedLength padded length for FFT
   * @param samplingRate sampling rate in Hz
   * @return frequency axis array (omega)
   */
  private double[] createFrequencyAxis(int paddedLength, double samplingRate) {
    double[] omega = new double[paddedLength];
    for (int i = 0; i < paddedLength; i++) {
      omega[i] = 2.0 * Math.PI * i * samplingRate / paddedLength;
      if (i > paddedLength / 2) {
        omega[i] -= 2.0 * Math.PI * samplingRate;
      }
    }
    return omega;
  }

  /**
   * Perform parallel continuous wavelet transform on a signal.
   * This method parallelizes across scales for improved performance.
   * 
   * @param signal input signal
   * @param scales array of scale values to use
   * @param samplingRate sampling rate of the signal (Hz)
   * @return CWTResult containing the transform coefficients
   */
  public CWTResult transformParallel(double[] signal, double[] scales, double samplingRate) {
    int signalLength = signal.length;
    int nScales = scales.length;
    
    // Check if we should use parallel processing
    if (!shouldUseParallel(nScales, signalLength)) {
      // Fall back to sequential for small scale counts
      return transform(signal, scales, samplingRate);
    }
    
    // Create time axis and initialize coefficient matrix
    double[] timeAxis = createTimeAxis(signalLength, samplingRate);
    Complex[][] coefficients = initializeCoefficients(nScales, signalLength);
    
    // Use blocked parallel processing for better cache performance
    // Process in blocks to improve cache locality
    IntStream.range(0, (signalLength + CACHE_BLOCK_SIZE - 1) / CACHE_BLOCK_SIZE).parallel().forEach(blockIdx -> {
      int startTime = blockIdx * CACHE_BLOCK_SIZE;
      int endTime = Math.min(startTime + CACHE_BLOCK_SIZE, signalLength);
      
      // Process all scales for this time block (better cache locality)
      for (int scaleIdx = 0; scaleIdx < nScales; scaleIdx++) {
        double scale = scales[scaleIdx];
        for (int timeIdx = startTime; timeIdx < endTime; timeIdx++) {
          coefficients[scaleIdx][timeIdx] = computeCoefficient(signal, timeIdx, scale, samplingRate);
        }
      }
    });
    
    return new CWTResult(coefficients, scales, timeAxis, samplingRate, _wavelet.getName());
  }

  /**
   * Perform parallel FFT-based continuous wavelet transform for better performance.
   * This method parallelizes across scales and uses FFT for convolution.
   * 
   * @param signal input signal
   * @param scales array of scale values to use
   * @param samplingRate sampling rate of the signal (Hz)
   * @return CWTResult containing the transform coefficients
   */
  public CWTResult transformFFTParallel(double[] signal, double[] scales, double samplingRate) {
    int signalLength = signal.length;
    int nScales = scales.length;
    
    // Check if we should use parallel processing
    if (!shouldUseParallel(nScales, signalLength)) {
      // Fall back to sequential for small scale counts
      return transformFFT(signal, scales, samplingRate);
    }
    
    // Pad signal to next power of 2 for FFT efficiency
    int paddedLength = MathUtils.nextPowerOfTwo(signalLength);
    double[] paddedSignal = padSignal(signal, paddedLength);
    
    // Compute FFT of the signal (done once, shared across all scales)
    Complex[] signalFFT = computeFFT(paddedSignal);
    
    // Create frequency axis
    double[] omega = createFrequencyAxis(paddedLength, samplingRate);
    
    // Create time axis and initialize coefficient matrix
    double[] timeAxis = createTimeAxis(signalLength, samplingRate);
    Complex[][] coefficients = initializeCoefficients(nScales, signalLength);
    
    // Parallel processing across scales
    // For FFT-based method, parallelizing over scales is more efficient
    // since each scale requires a full FFT/IFFT operation
    IntStream.range(0, nScales).parallel().forEach(scaleIdx -> {
      double scale = scales[scaleIdx];
      
      // Compute wavelet FFT at this scale
      Complex[] waveletFFT = new Complex[paddedLength];
      for (int i = 0; i < paddedLength; i++) {
        waveletFFT[i] = _wavelet.fourierTransform(omega[i], scale, 0);
        // Take complex conjugate for convolution
        waveletFFT[i] = waveletFFT[i].conjugate();
      }
      
      // Multiply in frequency domain
      Complex[] product = new Complex[paddedLength];
      for (int i = 0; i < paddedLength; i++) {
        product[i] = signalFFT[i].mul(waveletFFT[i]);
      }
      
      // Inverse FFT
      Complex[] result = computeIFFT(product);
      
      // Extract relevant part and store
      for (int timeIdx = 0; timeIdx < signalLength; timeIdx++) {
        coefficients[scaleIdx][timeIdx] = result[timeIdx];
      }
    });
    
    return new CWTResult(coefficients, scales, timeAxis, samplingRate, _wavelet.getName());
  }

  /**
   * Perform parallel continuous wavelet transform using ForkJoinPool for fine-grained control.
   * This method allows custom parallelism level and is useful for very large transforms.
   * 
   * @param signal input signal
   * @param scales array of scale values to use
   * @param samplingRate sampling rate of the signal (Hz)
   * @param parallelism the parallelism level (0 for default)
   * @return CWTResult containing the transform coefficients
   */
  public CWTResult transformParallelCustom(double[] signal, double[] scales, double samplingRate, int parallelism) {
    int signalLength = signal.length;
    int nScales = scales.length;
    
    // Check if we should use parallel processing
    if (!shouldUseParallel(nScales, signalLength)) {
      // Fall back to sequential for small scale counts
      return transform(signal, scales, samplingRate);
    }
    
    // Create time axis and initialize coefficient matrix
    double[] timeAxis = createTimeAxis(signalLength, samplingRate);
    Complex[][] coefficients = initializeCoefficients(nScales, signalLength);
    
    // Create custom ForkJoinPool if parallelism specified
    ForkJoinPool pool = parallelism > 0 ? 
        new ForkJoinPool(parallelism) : 
        ForkJoinPool.commonPool();
    
    try {
      // Use CWTTask with the custom pool for proper parallelism control
      CWTTask mainTask = new CWTTask(signal, scales, coefficients, samplingRate, 0, nScales);
      pool.invoke(mainTask);
    } finally {
      if (parallelism > 0) {
        pool.shutdown();
      }
    }
    
    return new CWTResult(coefficients, scales, timeAxis, samplingRate, _wavelet.getName());
  }

  /**
   * Determine the threshold for using parallel processing.
   * 
   * @param nScales number of scales
   * @param signalLength length of signal
   * @return threshold value
   */
  private int getParallelThreshold(int nScales, int signalLength) {
    // Use parallel processing if we have enough scales to benefit
    // and the signal is not too small
    if (signalLength < TINY_SIGNAL_LENGTH) {
      return Integer.MAX_VALUE; // Never parallelize very small signals
    } else if (signalLength < SMALL_SIGNAL_LENGTH) {
      return SCALES_THRESHOLD_SMALL; // Small signals need more scales to benefit
    } else {
      return SCALES_THRESHOLD_LARGE; // For larger signals, parallelize with fewer scales
    }
  }

  /**
   * Check if parallel processing should be used based on problem size.
   * 
   * @param nScales number of scales
   * @param signalLength length of signal
   * @return true if parallel processing should be used
   */
  private boolean shouldUseParallel(int nScales, int signalLength) {
    int threshold = getParallelThreshold(nScales, signalLength);
    return nScales >= threshold;
  }

  /**
   * Recursive action for parallel CWT computation using Fork/Join framework.
   * This provides more control over task granularity.
   */
  private class CWTTask extends RecursiveAction {
    private final double[] signal;
    private final double[] scales;
    private final Complex[][] coefficients;
    private final double samplingRate;
    private final int startScale;
    private final int endScale;
    
    CWTTask(double[] signal, double[] scales, Complex[][] coefficients, 
            double samplingRate, int startScale, int endScale) {
      this.signal = signal;
      this.scales = scales;
      this.coefficients = coefficients;
      this.samplingRate = samplingRate;
      this.startScale = startScale;
      this.endScale = endScale;
    }
    
    @Override
    protected void compute() {
      if (endScale - startScale <= FORK_JOIN_THRESHOLD) {
        // Compute directly
        for (int scaleIdx = startScale; scaleIdx < endScale; scaleIdx++) {
          double scale = scales[scaleIdx];
          for (int timeIdx = 0; timeIdx < signal.length; timeIdx++) {
            coefficients[scaleIdx][timeIdx] = computeCoefficient(signal, timeIdx, scale, samplingRate);
          }
        }
      } else {
        // Fork into subtasks
        int mid = startScale + (endScale - startScale) / 2;
        CWTTask leftTask = new CWTTask(signal, scales, coefficients, samplingRate, startScale, mid);
        CWTTask rightTask = new CWTTask(signal, scales, coefficients, samplingRate, mid, endScale);
        
        leftTask.fork();
        rightTask.compute();
        leftTask.join();
      }
    }
  }
}