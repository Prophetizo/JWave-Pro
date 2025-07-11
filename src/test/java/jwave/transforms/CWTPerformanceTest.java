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
import org.junit.Test;

/**
 * Performance comparison test for CWT with FFT optimization.
 *
 * @author Stephen Romano
 * @date 07.01.2025
 */
public class CWTPerformanceTest {

    @Test
    public void compareDirectVsFFTPerformance() {
        System.out.println("=== CWT Performance: Direct vs FFT-based Implementation ===\n");
        
        // Test parameters
        int[] signalLengths = {256, 512, 1024, 2048, 4096};
        int nScales = 50;
        double minScale = 1.0;
        double maxScale = 100.0;
        
        MorletWavelet morlet = new MorletWavelet();
        ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(morlet);
        
        System.out.println("Signal Length | Direct Time (ms) | FFT Time (ms) | Speedup");
        System.out.println("-------------|------------------|---------------|--------");
        
        for (int length : signalLengths) {
            // Generate test signal
            double[] signal = new double[length];
            for (int i = 0; i < length; i++) {
                signal[i] = Math.sin(2 * Math.PI * 5 * i / length) + 
                           0.5 * Math.sin(2 * Math.PI * 10 * i / length);
            }
            
            // Generate scales
            double[] scales = ContinuousWaveletTransform.generateLogScales(minScale, maxScale, nScales);
            
            // Time direct method
            long startDirect = System.nanoTime();
            CWTResult directResult = cwt.transform(signal, scales);
            long timeDirect = System.nanoTime() - startDirect;
            
            // Time FFT method
            long startFFT = System.nanoTime();
            CWTResult fftResult = cwt.transformFFT(signal, scales, 1.0);
            long timeFFT = System.nanoTime() - startFFT;
            
            // Convert to milliseconds
            double directMs = timeDirect / 1_000_000.0;
            double fftMs = timeFFT / 1_000_000.0;
            double speedup = directMs / fftMs;
            
            System.out.printf("%12d | %16.2f | %13.2f | %.2fx%n",
                length, directMs, fftMs, speedup);
        }
        
        System.out.println("\nNote: FFT-based CWT uses O(n log n) algorithms,");
        System.out.println("while direct convolution is O(n²) for each scale.");
    }
    
    @Test
    public void testLargeSignalPerformance() {
        System.out.println("\n=== Large Signal CWT Performance Test ===\n");
        
        // Large signal test
        int signalLength = 16384;
        int nScales = 100;
        
        double[] signal = new double[signalLength];
        for (int i = 0; i < signalLength; i++) {
            signal[i] = Math.random() - 0.5;
        }
        
        double[] scales = ContinuousWaveletTransform.generateLogScales(1.0, 500.0, nScales);
        
        MexicanHatWavelet wavelet = new MexicanHatWavelet();
        ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(wavelet);
        
        System.out.println("Signal length: " + signalLength);
        System.out.println("Number of scales: " + nScales);
        System.out.println("Total coefficients: " + (signalLength * nScales));
        
        // Only test FFT method for large signals (direct would take too long)
        long start = System.nanoTime();
        CWTResult result = cwt.transformFFT(signal, scales, 1.0);
        long elapsed = System.nanoTime() - start;
        
        double elapsedMs = elapsed / 1_000_000.0;
        double coefficientsPerMs = (signalLength * nScales) / elapsedMs;
        
        System.out.printf("\nFFT-based CWT completed in: %.2f ms%n", elapsedMs);
        System.out.printf("Processing rate: %.0f coefficients/ms%n", coefficientsPerMs);
        System.out.println("\nWith the original O(n²) DFT, this would have taken significantly longer!");
    }
}