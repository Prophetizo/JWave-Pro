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
package jwave.examples;

import jwave.datatypes.natives.Complex;
import jwave.transforms.ContinuousWaveletTransform;
import jwave.transforms.CWTResult;
import jwave.transforms.wavelets.continuous.MeyerWavelet;
import java.util.Random;

/**
 * Example demonstrating the Meyer wavelet for Continuous Wavelet Transform.
 * 
 * The Meyer wavelet is defined in the frequency domain with compact support,
 * making it ideal for frequency-localized analysis. It is infinitely
 * differentiable and orthogonal.
 *
 * @author Stephen Romano
 * @date 09.01.2025
 */
public class MeyerWaveletExample {

  public static void main(String[] args) {
    
    System.out.println("Meyer Wavelet CWT Example");
    System.out.println("=========================\n");
    
    // Create Meyer wavelet
    MeyerWavelet meyerWavelet = new MeyerWavelet();
    
    // Display wavelet properties
    System.out.println("Meyer Wavelet Properties:");
    System.out.println("Name: " + meyerWavelet.getName());
    System.out.println("Center Frequency: " + meyerWavelet.getCenterFrequency());
    System.out.println("Admissibility Constant: " + meyerWavelet.getAdmissibilityConstant());
    
    double[] support = meyerWavelet.getEffectiveSupport();
    System.out.println("Effective Support: [" + support[0] + ", " + support[1] + "]");
    
    double[] bandwidth = meyerWavelet.getBandwidth();
    System.out.println("Frequency Bandwidth: [" + bandwidth[0] + ", " + bandwidth[1] + "] Hz");
    
    // Show wavelet values at different time points
    System.out.println("\nMeyer Wavelet Values (Real-valued):");
    System.out.println("t\tψ(t)");
    System.out.println("-----------------");
    double[] timePoints = {-10, -5, -2, -1, 0, 1, 2, 5, 10};
    for (double t : timePoints) {
      Complex value = meyerWavelet.wavelet(t);
      System.out.printf("%.1f\t%.6f\n", t, value.getReal());
    }
    
    // Show Fourier transform values (where Meyer is naturally defined)
    System.out.println("\nMeyer Wavelet Fourier Transform (complex-valued):");
    System.out.println("ω\t|Ψ(ω)|\t\tRe[Ψ(ω)]\tIm[Ψ(ω)]");
    System.out.println("---------------------------------------------");
    double[] freqPoints = {0.5, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0};
    for (double omega : freqPoints) {
      Complex value = meyerWavelet.fourierTransform(omega);
      System.out.printf("%.1f\t%.6f\t%.6f\t%.6f\n", 
                       omega, value.getMag(), value.getReal(), value.getImag());
    }
    
    // Create test signal with multiple frequency components
    System.out.println("\nGenerating Test Signal...");
    double samplingRate = 100.0; // 100 Hz
    int signalLength = 512;
    double[] signal = new double[signalLength];
    
    // Use a fixed seed for reproducible results
    Random random = new Random(42);
    
    // Create a signal with time-varying frequency content
    // Low frequency at the beginning, high frequency at the end
    for (int i = 0; i < signalLength; i++) {
      double t = i / samplingRate;
      
      if (i < signalLength / 3) {
        // Low frequency component (5 Hz)
        signal[i] = Math.sin(2 * Math.PI * 5 * t);
      } else if (i < 2 * signalLength / 3) {
        // Medium frequency component (15 Hz)
        signal[i] = Math.sin(2 * Math.PI * 15 * t);
      } else {
        // High frequency component (25 Hz)
        signal[i] = Math.sin(2 * Math.PI * 25 * t);
      }
      
      // Add some reproducible noise
      signal[i] += 0.1 * random.nextGaussian();
    }
    
    // Perform CWT with Meyer wavelet
    System.out.println("\nPerforming Continuous Wavelet Transform...");
    ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(meyerWavelet);
    
    // Generate scales for analysis
    // Meyer wavelet has good frequency resolution, so we can use fine scale steps
    double[] scales = ContinuousWaveletTransform.generateLogScales(0.5, 20.0, 30);
    
    // Perform the transform
    CWTResult result = cwt.transformFFT(signal, scales, samplingRate);
    
    // Analyze the results
    System.out.println("\nCWT Analysis Results:");
    System.out.println("Number of scales: " + result.getNumberOfScales());
    System.out.println("Number of time points: " + result.getNumberOfTimePoints());
    
    // Convert scales to frequencies for Meyer wavelet
    double[] frequencies = result.scaleToFrequency(meyerWavelet.getCenterFrequency());
    
    // Find dominant frequencies in each time segment
    System.out.println("\nTime-Frequency Analysis:");
    System.out.println("Time Segment\tDominant Frequency (Hz)\tExpected Frequency (Hz)");
    System.out.println("--------------------------------------------------------");
    
    double[][] magnitude = result.getMagnitude();
    int segmentLength = signalLength / 3;
    
    // Analyze first segment (should detect ~5 Hz)
    analyzeDominantFrequency(magnitude, frequencies, 0, segmentLength, 
                            samplingRate, "0.0 - 1.7 s", "5.0");
    
    // Analyze second segment (should detect ~15 Hz)
    analyzeDominantFrequency(magnitude, frequencies, segmentLength, 2 * segmentLength, 
                            samplingRate, "1.7 - 3.4 s", "15.0");
    
    // Analyze third segment (should detect ~25 Hz)
    analyzeDominantFrequency(magnitude, frequencies, 2 * segmentLength, signalLength, 
                            samplingRate, "3.4 - 5.1 s", "25.0");
    
    // Demonstrate Meyer wavelet's frequency localization
    System.out.println("\nMeyer Wavelet Frequency Localization:");
    System.out.println("The Meyer wavelet provides excellent frequency resolution");
    System.out.println("due to its compact support in the frequency domain.");
    System.out.println("This makes it ideal for:");
    System.out.println("- Analyzing signals with distinct frequency bands");
    System.out.println("- Detecting frequency transitions");
    System.out.println("- Orthogonal multiresolution analysis");
    
    // Show scalogram (energy distribution across scales)
    System.out.println("\nScalogram (Energy per Scale):");
    double[] scalogram = result.getScalogram();
    System.out.println("Scale\tFreq (Hz)\tEnergy");
    System.out.println("--------------------------------");
    for (int i = 0; i < Math.min(10, scales.length); i++) {
      System.out.printf("%.2f\t%.2f\t\t%.2f\n", 
                       scales[i], frequencies[i], scalogram[i]);
    }
  }
  
  /**
   * Helper method to find and display dominant frequency in a time segment
   */
  private static void analyzeDominantFrequency(double[][] magnitude, double[] frequencies,
                                              int startIdx, int endIdx, double samplingRate,
                                              String timeLabel, String expectedFreq) {
    double maxEnergy = 0;
    int maxFreqIdx = 0;
    
    // Sum energy across the time segment for each frequency
    for (int freqIdx = 0; freqIdx < frequencies.length; freqIdx++) {
      double energy = 0;
      for (int timeIdx = startIdx; timeIdx < endIdx; timeIdx++) {
        energy += magnitude[freqIdx][timeIdx] * magnitude[freqIdx][timeIdx];
      }
      
      if (energy > maxEnergy) {
        maxEnergy = energy;
        maxFreqIdx = freqIdx;
      }
    }
    
    System.out.printf("%-15s %10.2f %20s\n", 
                     timeLabel, frequencies[maxFreqIdx], expectedFreq);
  }
}