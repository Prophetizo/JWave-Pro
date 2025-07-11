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

import jwave.transforms.wavelets.continuous.MorletWavelet;
import jwave.transforms.wavelets.continuous.MexicanHatWavelet;

/**
 * Example demonstrating the use of Continuous Wavelet Transform.
 *
 * @author Stephen Romano
 * @date 07.01.2025
 */
public class CWTExample {

  public static void main(String[] args) {
    
    // Example 1: Analyze a signal with changing frequency
    System.out.println("=== CWT Example: Chirp Signal Analysis ===\n");
    
    // Create a chirp signal (frequency increases linearly with time)
    double samplingRate = 1000.0; // 1000 Hz
    int signalLength = 1000;
    double[] chirpSignal = new double[signalLength];
    
    for (int i = 0; i < signalLength; i++) {
      double t = i / samplingRate;
      // Frequency increases from 10 Hz to 100 Hz over 1 second
      double instantFreq = 10.0 + 90.0 * t;
      double phase = 2.0 * Math.PI * (10.0 * t + 45.0 * t * t);
      chirpSignal[i] = Math.sin(phase);
    }
    
    // Create Morlet wavelet CWT
    MorletWavelet morletWavelet = new MorletWavelet(1.0, 1.0);
    ContinuousWaveletTransform morletCWT = new ContinuousWaveletTransform(morletWavelet);
    
    // Generate logarithmically spaced scales
    // Scales correspond to frequencies from ~5 Hz to ~200 Hz
    double[] scales = ContinuousWaveletTransform.generateLogScales(0.2, 20.0, 50);
    
    // Perform CWT
    System.out.println("Performing Morlet CWT on chirp signal...");
    CWTResult morletResult = morletCWT.transformFFT(chirpSignal, scales, samplingRate);
    
    // Analyze results
    double[][] magnitude = morletResult.getMagnitude();
    double[] frequencies = morletResult.scaleToFrequency(morletWavelet.getCenterFrequency());
    
    System.out.println("Number of scales: " + scales.length);
    System.out.println("Frequency range: " + String.format("%.1f Hz to %.1f Hz", 
                       frequencies[frequencies.length-1], frequencies[0]));
    
    // Find dominant frequency at different time points
    System.out.println("\nDominant frequencies at different times:");
    int[] timePoints = {100, 300, 500, 700, 900};
    for (int timeIdx : timePoints) {
      int maxFreqIdx = 0;
      double maxMag = 0;
      for (int scaleIdx = 0; scaleIdx < scales.length; scaleIdx++) {
        if (magnitude[scaleIdx][timeIdx] > maxMag) {
          maxMag = magnitude[scaleIdx][timeIdx];
          maxFreqIdx = scaleIdx;
        }
      }
      double time = timeIdx / samplingRate;
      double dominantFreq = frequencies[maxFreqIdx];
      double expectedFreq = 10.0 + 90.0 * time;
      System.out.println(String.format("  t=%.2fs: Detected=%.1f Hz, Expected=%.1f Hz", 
                         time, dominantFreq, expectedFreq));
    }
    
    // Example 2: Edge detection using Mexican Hat wavelet
    System.out.println("\n=== CWT Example: Edge Detection ===\n");
    
    // Create a signal with edges (step functions)
    double[] stepSignal = new double[500];
    for (int i = 0; i < 100; i++) stepSignal[i] = 0.0;
    for (int i = 100; i < 200; i++) stepSignal[i] = 1.0;
    for (int i = 200; i < 300; i++) stepSignal[i] = -0.5;
    for (int i = 300; i < 400; i++) stepSignal[i] = 0.5;
    for (int i = 400; i < 500; i++) stepSignal[i] = 0.0;
    
    // Create Mexican Hat wavelet CWT
    MexicanHatWavelet mexicanWavelet = new MexicanHatWavelet(2.0);
    ContinuousWaveletTransform mexicanCWT = new ContinuousWaveletTransform(mexicanWavelet);
    
    // Use fewer scales for edge detection
    double[] edgeScales = ContinuousWaveletTransform.generateLinearScales(1.0, 10.0, 10);
    
    System.out.println("Performing Mexican Hat CWT for edge detection...");
    CWTResult mexicanResult = mexicanCWT.transform(stepSignal, edgeScales, 100.0);
    
    // Find edges by looking for maxima in CWT magnitude
    double[][] edgeMagnitude = mexicanResult.getMagnitude();
    System.out.println("Edge locations (expected at samples 100, 200, 300, 400):");
    
    // Use scale index 2 for edge detection
    int scaleIdx = 2;
    System.out.print("Detected edges at samples: ");
    for (int i = 1; i < stepSignal.length - 1; i++) {
      // Simple peak detection
      if (edgeMagnitude[scaleIdx][i] > edgeMagnitude[scaleIdx][i-1] &&
          edgeMagnitude[scaleIdx][i] > edgeMagnitude[scaleIdx][i+1] &&
          edgeMagnitude[scaleIdx][i] > 0.1) {
        System.out.print(i + " ");
      }
    }
    System.out.println();
    
    // Example 3: Analyzing signal energy distribution
    System.out.println("\n=== CWT Example: Energy Analysis ===\n");
    
    // Create a signal with two distinct frequency components
    double[] multiFreqSignal = new double[1000];
    for (int i = 0; i < 500; i++) {
      double t = i / samplingRate;
      multiFreqSignal[i] = Math.sin(2.0 * Math.PI * 25.0 * t);
    }
    for (int i = 500; i < 1000; i++) {
      double t = i / samplingRate;
      multiFreqSignal[i] = Math.sin(2.0 * Math.PI * 75.0 * t);
    }
    
    // Perform CWT
    CWTResult energyResult = morletCWT.transformFFT(multiFreqSignal, scales, samplingRate);
    
    // Compute scalogram (energy at each scale)
    double[] scalogram = energyResult.getScalogram();
    double[] freqs = energyResult.scaleToFrequency(morletWavelet.getCenterFrequency());
    
    System.out.println("Energy distribution across frequencies:");
    
    // Find peaks in scalogram
    for (int i = 1; i < scalogram.length - 1; i++) {
      if (scalogram[i] > scalogram[i-1] && scalogram[i] > scalogram[i+1] && 
          scalogram[i] > 0.1 * getMax(scalogram)) {
        System.out.println(String.format("  Peak at %.1f Hz with energy %.2f", 
                           freqs[i], scalogram[i]));
      }
    }
    
    System.out.println("\n=== CWT Examples Complete ===");
  }
  
  private static double getMax(double[] array) {
    double max = array[0];
    for (double val : array) {
      if (val > max) max = val;
    }
    return max;
  }
}