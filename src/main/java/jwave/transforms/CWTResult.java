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

/**
 * Container class for Continuous Wavelet Transform results.
 * Stores the 2D time-scale representation along with metadata.
 *
 * @author Stephen Romano
 * @date 07.01.2025
 */
public class CWTResult {

  /**
   * The complex-valued CWT coefficients.
   * First dimension is scale index, second dimension is time index.
   */
  private Complex[][] _coefficients;

  /**
   * The scale values used in the transform.
   */
  private double[] _scales;

  /**
   * The time values corresponding to the signal samples.
   */
  private double[] _timeAxis;

  /**
   * Sampling rate of the original signal (Hz).
   */
  private double _samplingRate;

  /**
   * Name of the wavelet used.
   */
  private String _waveletName;

  /**
   * Constructor for CWT result.
   * 
   * @param coefficients 2D array of complex CWT coefficients [scale][time]
   * @param scales array of scale values used
   * @param timeAxis array of time values
   * @param samplingRate sampling rate of the original signal
   * @param waveletName name of the wavelet used
   */
  public CWTResult(Complex[][] coefficients, double[] scales, double[] timeAxis,
                   double samplingRate, String waveletName) {
    _coefficients = coefficients;
    _scales = scales;
    _timeAxis = timeAxis;
    _samplingRate = samplingRate;
    _waveletName = waveletName;
  }

  /**
   * Get the complex CWT coefficients.
   * 
   * @return 2D array of complex coefficients [scale][time]
   */
  public Complex[][] getCoefficients() {
    return _coefficients;
  }

  /**
   * Get the magnitude (absolute value) of the CWT coefficients.
   * This is often used for visualization and analysis.
   * 
   * @return 2D array of magnitudes [scale][time]
   */
  public double[][] getMagnitude() {
    int nScales = _coefficients.length;
    int nTime = _coefficients[0].length;
    double[][] magnitude = new double[nScales][nTime];
    
    for (int i = 0; i < nScales; i++) {
      for (int j = 0; j < nTime; j++) {
        magnitude[i][j] = _coefficients[i][j].getMag();
      }
    }
    
    return magnitude;
  }

  /**
   * Get the phase of the CWT coefficients in radians.
   * 
   * @return 2D array of phase values in radians [scale][time]
   */
  public double[][] getPhase() {
    int nScales = _coefficients.length;
    int nTime = _coefficients[0].length;
    double[][] phase = new double[nScales][nTime];
    
    for (int i = 0; i < nScales; i++) {
      for (int j = 0; j < nTime; j++) {
        // Convert from degrees (returned by getPhi()) to radians
        phase[i][j] = _coefficients[i][j].getPhi() * Math.PI / 180.0;
      }
    }
    
    return phase;
  }

  /**
   * Get the real part of the CWT coefficients.
   * 
   * @return 2D array of real parts [scale][time]
   */
  public double[][] getReal() {
    int nScales = _coefficients.length;
    int nTime = _coefficients[0].length;
    double[][] real = new double[nScales][nTime];
    
    for (int i = 0; i < nScales; i++) {
      for (int j = 0; j < nTime; j++) {
        real[i][j] = _coefficients[i][j].getReal();
      }
    }
    
    return real;
  }

  /**
   * Get the imaginary part of the CWT coefficients.
   * 
   * @return 2D array of imaginary parts [scale][time]
   */
  public double[][] getImaginary() {
    int nScales = _coefficients.length;
    int nTime = _coefficients[0].length;
    double[][] imag = new double[nScales][nTime];
    
    for (int i = 0; i < nScales; i++) {
      for (int j = 0; j < nTime; j++) {
        imag[i][j] = _coefficients[i][j].getImag();
      }
    }
    
    return imag;
  }

  /**
   * Get the scale values used in the transform.
   * 
   * @return array of scale values
   */
  public double[] getScales() {
    return _scales;
  }

  /**
   * Get the time axis values.
   * 
   * @return array of time values
   */
  public double[] getTimeAxis() {
    return _timeAxis;
  }

  /**
   * Convert scales to frequencies using the wavelet's center frequency.
   * frequency = centerFreq * samplingRate / scale
   * 
   * @param centerFreq center frequency of the wavelet
   * @return array of frequencies corresponding to scales
   */
  public double[] scaleToFrequency(double centerFreq) {
    double[] frequencies = new double[_scales.length];
    for (int i = 0; i < _scales.length; i++) {
      frequencies[i] = centerFreq * _samplingRate / _scales[i];
    }
    return frequencies;
  }

  /**
   * Get coefficients at a specific scale.
   * 
   * @param scaleIndex index of the scale
   * @return array of complex coefficients at the given scale
   */
  public Complex[] getCoefficientsAtScale(int scaleIndex) {
    if (scaleIndex < 0 || scaleIndex >= _coefficients.length) {
      throw new IndexOutOfBoundsException("Scale index out of bounds");
    }
    return _coefficients[scaleIndex];
  }

  /**
   * Get coefficients at a specific time point.
   * 
   * @param timeIndex index of the time point
   * @return array of complex coefficients at the given time
   */
  public Complex[] getCoefficientsAtTime(int timeIndex) {
    if (timeIndex < 0 || timeIndex >= _coefficients[0].length) {
      throw new IndexOutOfBoundsException("Time index out of bounds");
    }
    
    Complex[] timeSlice = new Complex[_coefficients.length];
    for (int i = 0; i < _coefficients.length; i++) {
      timeSlice[i] = _coefficients[i][timeIndex];
    }
    return timeSlice;
  }

  /**
   * Get the sampling rate of the original signal.
   * 
   * @return sampling rate in Hz
   */
  public double getSamplingRate() {
    return _samplingRate;
  }

  /**
   * Get the name of the wavelet used.
   * 
   * @return wavelet name
   */
  public String getWaveletName() {
    return _waveletName;
  }

  /**
   * Get the number of scales.
   * 
   * @return number of scales
   */
  public int getNumberOfScales() {
    return _scales.length;
  }

  /**
   * Get the number of time points.
   * 
   * @return number of time points
   */
  public int getNumberOfTimePoints() {
    return _timeAxis.length;
  }

  /**
   * Compute the energy at each scale (scalogram).
   * Energy is the sum of squared magnitudes across time.
   * 
   * @return array of energy values for each scale
   */
  public double[] getScalogram() {
    int nScales = _coefficients.length;
    int nTime = _coefficients[0].length;
    double[] scalogram = new double[nScales];
    
    for (int i = 0; i < nScales; i++) {
      double energy = 0.0;
      for (int j = 0; j < nTime; j++) {
        double mag = _coefficients[i][j].getMag();
        energy += mag * mag;
      }
      scalogram[i] = energy;
    }
    
    return scalogram;
  }
}