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

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import jwave.datatypes.natives.Complex;
import jwave.transforms.wavelets.continuous.MorletWavelet;
import jwave.transforms.wavelets.continuous.MexicanHatWavelet;
import jwave.transforms.CWTResult;
import jwave.utils.TestUtils;
import java.util.Random;

/**
 * Performance and correctness tests for parallel CWT implementation.
 * 
 * Tests verify that:
 * 1. Parallel implementation produces identical results to sequential
 * 2. Performance improves with parallelization
 * 3. Thread safety is maintained
 *
 * @author Stephen Romano
 * @date 10.01.2025
 */
public class CWTParallelPerformanceTest {

  /**
   * Tolerance for floating-point comparisons.
   * Set to 1e-8 to account for small variations due to:
   * - Non-associativity of floating-point operations
   * - Different operation ordering in parallel execution
   * - Accumulated rounding errors
   */
  private static final double DELTA = 1e-8;
  private static final int WARMUP_RUNS = 3;
  private static final int BENCHMARK_RUNS = 5;
  private static final long RANDOM_SEED = 42L; // Fixed seed for reproducibility
  
  // Test signal parameters
  private double[] testSignal;
  private double[] scales;
  private double samplingRate = 1000.0; // 1 kHz
  private MorletWavelet morletWavelet;
  private MexicanHatWavelet mexicanHatWavelet;
  
  @Before
  public void setUp() {
    // Create a complex test signal with multiple frequency components
    int signalLength = 4096; // Reasonable size for performance testing
    testSignal = new double[signalLength];
    
    // Use seeded random for reproducible noise
    Random random = new Random(RANDOM_SEED);
    
    // Mix of frequencies: 50 Hz, 120 Hz, 250 Hz with varying amplitudes
    for (int i = 0; i < signalLength; i++) {
      double t = i / samplingRate;
      testSignal[i] = 1.0 * Math.sin(2 * Math.PI * 50 * t) +
                      0.5 * Math.sin(2 * Math.PI * 120 * t) +
                      0.3 * Math.sin(2 * Math.PI * 250 * t) +
                      0.1 * random.nextGaussian(); // Add reproducible Gaussian noise
    }
    
    // Generate scales for frequency range 10-500 Hz
    scales = ContinuousWaveletTransform.generateLogScales(2, 100, 50);
    
    // Initialize wavelets
    morletWavelet = new MorletWavelet(1.0, 1.0);
    mexicanHatWavelet = new MexicanHatWavelet(1.0);
  }
  
  @After
  public void tearDown() {
    testSignal = null;
    scales = null;
    morletWavelet = null;
    mexicanHatWavelet = null;
  }
  
  /**
   * Test that sequential and parallel implementations produce identical results
   * for the direct transform method.
   */
  @Test
  public void testParallelCorrectnessDirectTransform() {
    ContinuousWaveletTransform cwtSequential = new ContinuousWaveletTransform(morletWavelet);
    ContinuousWaveletTransform cwtParallel = new ContinuousWaveletTransform(morletWavelet);
    
    // Run sequential transform
    CWTResult sequentialResult = cwtSequential.transform(testSignal, scales, samplingRate);
    
    // Run parallel transform (will be implemented)
    CWTResult parallelResult = cwtParallel.transformParallel(testSignal, scales, samplingRate);
    
    // Verify dimensions match
    assertEquals("Number of scales should match", 
                 sequentialResult.getNumberOfScales(), 
                 parallelResult.getNumberOfScales());
    assertEquals("Number of time points should match", 
                 sequentialResult.getNumberOfTimePoints(), 
                 parallelResult.getNumberOfTimePoints());
    
    // Verify coefficients are identical
    Complex[][] seqCoeffs = sequentialResult.getCoefficients();
    Complex[][] parCoeffs = parallelResult.getCoefficients();
    
    for (int i = 0; i < scales.length; i++) {
      for (int j = 0; j < testSignal.length; j++) {
        assertEquals("Real part should match at scale " + i + ", time " + j,
                     seqCoeffs[i][j].getReal(), 
                     parCoeffs[i][j].getReal(), 
                     DELTA);
        assertEquals("Imaginary part should match at scale " + i + ", time " + j,
                     seqCoeffs[i][j].getImag(), 
                     parCoeffs[i][j].getImag(), 
                     DELTA);
      }
    }
  }
  
  /**
   * Test that sequential and parallel implementations produce identical results
   * for the FFT-based transform method.
   */
  @Test
  public void testParallelCorrectnessFFTTransform() {
    ContinuousWaveletTransform cwtSequential = new ContinuousWaveletTransform(morletWavelet);
    ContinuousWaveletTransform cwtParallel = new ContinuousWaveletTransform(morletWavelet);
    
    // Run sequential FFT transform
    CWTResult sequentialResult = cwtSequential.transformFFT(testSignal, scales, samplingRate);
    
    // Run parallel FFT transform (will be implemented)
    CWTResult parallelResult = cwtParallel.transformFFTParallel(testSignal, scales, samplingRate);
    
    // Verify dimensions match
    assertEquals("Number of scales should match", 
                 sequentialResult.getNumberOfScales(), 
                 parallelResult.getNumberOfScales());
    assertEquals("Number of time points should match", 
                 sequentialResult.getNumberOfTimePoints(), 
                 parallelResult.getNumberOfTimePoints());
    
    // Verify coefficients are identical
    Complex[][] seqCoeffs = sequentialResult.getCoefficients();
    Complex[][] parCoeffs = parallelResult.getCoefficients();
    
    for (int i = 0; i < scales.length; i++) {
      for (int j = 0; j < testSignal.length; j++) {
        assertEquals("FFT: Real part should match at scale " + i + ", time " + j,
                     seqCoeffs[i][j].getReal(), 
                     parCoeffs[i][j].getReal(), 
                     DELTA);
        assertEquals("FFT: Imaginary part should match at scale " + i + ", time " + j,
                     seqCoeffs[i][j].getImag(), 
                     parCoeffs[i][j].getImag(), 
                     DELTA);
      }
    }
  }
  
  /**
   * Benchmark performance improvement of parallel direct transform.
   */
  @Test
  public void benchmarkParallelDirectTransform() {
    TestUtils.skipIfPerformanceTestsDisabled();
    ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(morletWavelet);
    
    // Warmup
    for (int i = 0; i < WARMUP_RUNS; i++) {
      cwt.transform(testSignal, scales, samplingRate);
      cwt.transformParallel(testSignal, scales, samplingRate);
    }
    
    // Benchmark sequential
    long sequentialTime = 0;
    for (int i = 0; i < BENCHMARK_RUNS; i++) {
      long start = System.nanoTime();
      cwt.transform(testSignal, scales, samplingRate);
      sequentialTime += System.nanoTime() - start;
    }
    double avgSequentialTime = sequentialTime / (double) BENCHMARK_RUNS / 1_000_000.0; // ms
    
    // Benchmark parallel
    long parallelTime = 0;
    for (int i = 0; i < BENCHMARK_RUNS; i++) {
      long start = System.nanoTime();
      cwt.transformParallel(testSignal, scales, samplingRate);
      parallelTime += System.nanoTime() - start;
    }
    double avgParallelTime = parallelTime / (double) BENCHMARK_RUNS / 1_000_000.0; // ms
    
    // Calculate speedup
    double speedup = avgSequentialTime / avgParallelTime;
    
    // Print results
    System.out.println("\n=== Direct Transform Performance ===");
    System.out.println("Signal length: " + testSignal.length);
    System.out.println("Number of scales: " + scales.length);
    System.out.println("Sequential time: " + String.format("%.2f", avgSequentialTime) + " ms");
    System.out.println("Parallel time: " + String.format("%.2f", avgParallelTime) + " ms");
    System.out.println("Speedup: " + String.format("%.2fx", speedup));
    System.out.println("Efficiency: " + String.format("%.1f%%", 
                       (speedup / Runtime.getRuntime().availableProcessors()) * 100));
    
    // Assert performance based on available cores
    int availableCores = Runtime.getRuntime().availableProcessors();
    if (availableCores > 1) {
      // On multi-core systems, parallel should at least not be slower
      // We use a relaxed threshold to account for CI environments and system load
      double minSpeedup = 1.0; // Just ensure it's not slower
      assertTrue("Parallel version should not be slower than sequential (speedup: " + 
                 String.format("%.2f", speedup) + "x on " + availableCores + " cores)", 
                 speedup >= minSpeedup);
      
      // Log warning if speedup is suspiciously low on multi-core system
      if (speedup < 1.2 && availableCores >= 4) {
        System.out.println("WARNING: Low speedup (" + String.format("%.2f", speedup) + 
                         "x) detected on " + availableCores + "-core system. " +
                         "This might indicate system load or CPU throttling.");
      }
    } else {
      // On single-core systems, parallel version might be slightly slower due to overhead
      System.out.println("Running on single-core system, skipping speedup assertion");
    }
  }
  
  /**
   * Benchmark performance improvement of parallel FFT transform.
   */
  @Test
  public void benchmarkParallelFFTTransform() {
    TestUtils.skipIfPerformanceTestsDisabled();
    ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(morletWavelet);
    
    // Warmup
    for (int i = 0; i < WARMUP_RUNS; i++) {
      cwt.transformFFT(testSignal, scales, samplingRate);
      cwt.transformFFTParallel(testSignal, scales, samplingRate);
    }
    
    // Benchmark sequential
    long sequentialTime = 0;
    for (int i = 0; i < BENCHMARK_RUNS; i++) {
      long start = System.nanoTime();
      cwt.transformFFT(testSignal, scales, samplingRate);
      sequentialTime += System.nanoTime() - start;
    }
    double avgSequentialTime = sequentialTime / (double) BENCHMARK_RUNS / 1_000_000.0; // ms
    
    // Benchmark parallel
    long parallelTime = 0;
    for (int i = 0; i < BENCHMARK_RUNS; i++) {
      long start = System.nanoTime();
      cwt.transformFFTParallel(testSignal, scales, samplingRate);
      parallelTime += System.nanoTime() - start;
    }
    double avgParallelTime = parallelTime / (double) BENCHMARK_RUNS / 1_000_000.0; // ms
    
    // Calculate speedup
    double speedup = avgSequentialTime / avgParallelTime;
    
    // Print results
    System.out.println("\n=== FFT Transform Performance ===");
    System.out.println("Signal length: " + testSignal.length);
    System.out.println("Number of scales: " + scales.length);
    System.out.println("Sequential time: " + String.format("%.2f", avgSequentialTime) + " ms");
    System.out.println("Parallel time: " + String.format("%.2f", avgParallelTime) + " ms");
    System.out.println("Speedup: " + String.format("%.2fx", speedup));
    System.out.println("Efficiency: " + String.format("%.1f%%", 
                       (speedup / Runtime.getRuntime().availableProcessors()) * 100));
    
    // Assert performance based on available cores
    int availableCores = Runtime.getRuntime().availableProcessors();
    if (availableCores > 1) {
      // On multi-core systems, parallel should at least not be slower
      // We use a relaxed threshold to account for CI environments and system load
      double minSpeedup = 1.0; // Just ensure it's not slower
      assertTrue("Parallel FFT version should not be slower than sequential (speedup: " + 
                 String.format("%.2f", speedup) + "x on " + availableCores + " cores)", 
                 speedup >= minSpeedup);
      
      // Log warning if speedup is suspiciously low on multi-core system
      if (speedup < 1.2 && availableCores >= 4) {
        System.out.println("WARNING: Low speedup (" + String.format("%.2f", speedup) + 
                         "x) detected on " + availableCores + "-core system. " +
                         "This might indicate system load or CPU throttling.");
      }
    } else {
      // On single-core systems, parallel version might be slightly slower due to overhead
      System.out.println("Running on single-core system, skipping speedup assertion");
    }
  }
  
  /**
   * Test scalability with different numbers of scales.
   */
  @Test
  public void testScalability() {
    TestUtils.skipIfPerformanceTestsDisabled();
    ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(mexicanHatWavelet);
    
    int[] scaleNumbers = {10, 25, 50, 100};
    System.out.println("\n=== Scalability Test ===");
    System.out.println("Cores available: " + Runtime.getRuntime().availableProcessors());
    
    for (int numScales : scaleNumbers) {
      double[] testScales = ContinuousWaveletTransform.generateLinearScales(1, 50, numScales);
      
      // Time sequential
      long start = System.nanoTime();
      cwt.transform(testSignal, testScales, samplingRate);
      double seqTime = (System.nanoTime() - start) / 1_000_000.0;
      
      // Time parallel
      start = System.nanoTime();
      cwt.transformParallel(testSignal, testScales, samplingRate);
      double parTime = (System.nanoTime() - start) / 1_000_000.0;
      
      double speedup = seqTime / parTime;
      System.out.println(String.format("Scales: %3d, Sequential: %6.1f ms, Parallel: %6.1f ms, Speedup: %.2fx",
                                       numScales, seqTime, parTime, speedup));
    }
  }
  
  /**
   * Test thread safety by running multiple parallel transforms concurrently.
   */
  @Test
  public void testThreadSafety() throws InterruptedException {
    final int numThreads = 4;
    final ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(morletWavelet);
    
    // Get reference result
    final CWTResult referenceResult = cwt.transform(testSignal, scales, samplingRate);
    
    // Run multiple transforms concurrently
    Thread[] threads = new Thread[numThreads];
    final boolean[] correctResults = new boolean[numThreads];
    
    for (int t = 0; t < numThreads; t++) {
      final int threadId = t;
      threads[t] = new Thread(() -> {
        CWTResult result = cwt.transformParallel(testSignal, scales, samplingRate);
        
        // Verify against reference
        Complex[][] refCoeffs = referenceResult.getCoefficients();
        Complex[][] resCoeffs = result.getCoefficients();
        
        boolean correct = true;
        for (int i = 0; i < scales.length && correct; i++) {
          for (int j = 0; j < testSignal.length && correct; j++) {
            if (Math.abs(refCoeffs[i][j].getReal() - resCoeffs[i][j].getReal()) > DELTA ||
                Math.abs(refCoeffs[i][j].getImag() - resCoeffs[i][j].getImag()) > DELTA) {
              correct = false;
            }
          }
        }
        correctResults[threadId] = correct;
      });
      threads[t].start();
    }
    
    // Wait for all threads to complete
    for (Thread thread : threads) {
      thread.join();
    }
    
    // Verify all threads got correct results
    for (int i = 0; i < numThreads; i++) {
      assertTrue("Thread " + i + " should get correct results", correctResults[i]);
    }
  }
  
  /**
   * Test parallel performance with different wavelet types.
   */
  @Test
  public void testDifferentWavelets() {
    TestUtils.skipIfPerformanceTestsDisabled();
    System.out.println("\n=== Performance with Different Wavelets ===");
    
    // Test with Morlet
    ContinuousWaveletTransform cwtMorlet = new ContinuousWaveletTransform(morletWavelet);
    long start = System.nanoTime();
    CWTResult morletResult = cwtMorlet.transformParallel(testSignal, scales, samplingRate);
    double morletTime = (System.nanoTime() - start) / 1_000_000.0;
    
    // Test with Mexican Hat
    ContinuousWaveletTransform cwtMexican = new ContinuousWaveletTransform(mexicanHatWavelet);
    start = System.nanoTime();
    CWTResult mexicanResult = cwtMexican.transformParallel(testSignal, scales, samplingRate);
    double mexicanTime = (System.nanoTime() - start) / 1_000_000.0;
    
    System.out.println("Morlet wavelet: " + String.format("%.2f", morletTime) + " ms");
    System.out.println("Mexican Hat wavelet: " + String.format("%.2f", mexicanTime) + " ms");
    
    // Verify results are valid and reasonable
    assertNotNull("Morlet result should not be null", morletResult);
    assertNotNull("Mexican Hat result should not be null", mexicanResult);
    
    // Check dimensions match expected values
    assertEquals("Morlet result should have correct number of scales", 
                 scales.length, morletResult.getNumberOfScales());
    assertEquals("Mexican Hat result should have correct number of scales", 
                 scales.length, mexicanResult.getNumberOfScales());
    assertEquals("Morlet result should have correct signal length", 
                 testSignal.length, morletResult.getNumberOfTimePoints());
    assertEquals("Mexican Hat result should have correct signal length", 
                 testSignal.length, mexicanResult.getNumberOfTimePoints());
    
    // Verify timing is reasonable (not too fast, not too slow)
    double minExpectedTime = 1.0; // At least 1ms for a non-trivial computation
    double maxExpectedTime = 60000.0; // Should complete within 1 minute
    assertTrue("Morlet transform time should be reasonable (" + morletTime + " ms)", 
               morletTime >= minExpectedTime && morletTime <= maxExpectedTime);
    assertTrue("Mexican Hat transform time should be reasonable (" + mexicanTime + " ms)", 
               mexicanTime >= minExpectedTime && mexicanTime <= maxExpectedTime);
    
    // Performance comparison - Mexican Hat is simpler and should typically be faster
    // but we allow some margin for variability
    System.out.println("Performance ratio (Morlet/Mexican Hat): " + 
                       String.format("%.2f", morletTime / mexicanTime));
  }
}