package jwave.transforms;

import jwave.exceptions.JWaveException;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.transforms.wavelets.haar.Haar1;
import jwave.utils.TestUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Performance test comparing in-place transforms with standard copying transforms.
 * 
 * @author Stephen Romano
 * @since 2.1.0
 */
public class InPlaceTransformPerformanceTest {
    
    private static final double DELTA = 1e-10;
    private static final int WARMUP_RUNS = 100;
    private static final int BENCHMARK_RUNS = 1000;
    
    @Test
    public void testInPlaceCorrectness() throws JWaveException {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] signalCopy = Arrays.copyOf(signal, signal.length);
        
        Haar1 wavelet = new Haar1();
        
        // Standard transform
        FastWaveletTransform fwt = new FastWaveletTransform(wavelet);
        double[] standardResult = fwt.forward(signal);
        
        // In-place transform
        InPlaceFastWaveletTransform inPlaceFwt = new InPlaceFastWaveletTransform(wavelet);
        double[] inPlaceResult = inPlaceFwt.forwardInPlace(signalCopy);
        
        // Results should be identical
        assertArrayEquals("In-place and standard transforms should produce same results",
                         standardResult, inPlaceResult, DELTA);
        
        // In-place should return the same array reference
        assertSame("In-place should return same array reference", signalCopy, inPlaceResult);
        
        // Test reverse transform
        double[] standardReverse = fwt.reverse(standardResult);
        double[] inPlaceReverse = inPlaceFwt.reverseInPlace(inPlaceResult);
        
        assertArrayEquals("Reverse transforms should produce same results",
                         standardReverse, inPlaceReverse, DELTA);
    }
    
    @Test
    public void testInPlaceWithLevels() throws JWaveException {
        double[] signal = new double[256];
        Random random = new Random(42);
        for (int i = 0; i < signal.length; i++) {
            signal[i] = random.nextGaussian();
        }
        
        Daubechies4 wavelet = new Daubechies4();
        
        for (int level = 1; level <= 8; level++) {
            double[] signalCopy1 = Arrays.copyOf(signal, signal.length);
            double[] signalCopy2 = Arrays.copyOf(signal, signal.length);
            
            // Standard transform - returns new array
            FastWaveletTransform fwt = new FastWaveletTransform(wavelet);
            double[] standardForward = fwt.forward(signalCopy1, level);
            double[] standardReverse = fwt.reverse(standardForward, level);
            
            // In-place transform - modifies input array
            InPlaceFastWaveletTransform inPlaceFwt = new InPlaceFastWaveletTransform(wavelet);
            double[] inPlaceForward = inPlaceFwt.forwardInPlace(signalCopy2, level);
            assertSame("In-place forward should return same array", signalCopy2, inPlaceForward);
            
            // Compare forward transforms
            assertArrayEquals("Level " + level + " forward transform mismatch",
                             standardForward, inPlaceForward, DELTA);
            
            // Perform reverse transform
            double[] inPlaceReverse = inPlaceFwt.reverseInPlace(inPlaceForward, level);
            assertSame("In-place reverse should return same array", inPlaceForward, inPlaceReverse);
            
            // Compare reverse transforms
            assertArrayEquals("Level " + level + " reverse transform mismatch",
                             standardReverse, inPlaceReverse, DELTA);
        }
    }
    
    @Test
    public void testMemoryEfficiency() throws JWaveException {
        TestUtils.skipIfPerformanceTestsDisabled();
        
        System.out.println("\n=== Memory Efficiency Test ===");
        
        int[] sizes = {1024, 4096, 16384, 65536};
        Daubechies4 wavelet = new Daubechies4();
        
        System.out.println("Size    | Standard Allocations | In-Place Allocations | Reduction");
        System.out.println("--------|---------------------|---------------------|----------");
        
        for (int size : sizes) {
            double[] signal = new double[size];
            Arrays.fill(signal, 1.0);
            
            // Measure standard transform allocations
            long standardAllocations = measureAllocations(() -> {
                try {
                    FastWaveletTransform fwt = new FastWaveletTransform(wavelet);
                    double[] result = fwt.forward(Arrays.copyOf(signal, size));
                    fwt.reverse(result);
                } catch (JWaveException e) {
                    throw new RuntimeException(e);
                }
            });
            
            // Measure in-place transform allocations
            long inPlaceAllocations = measureAllocations(() -> {
                try {
                    InPlaceFastWaveletTransform fwt = new InPlaceFastWaveletTransform(wavelet);
                    double[] work = Arrays.copyOf(signal, size);
                    fwt.forwardInPlace(work);
                    fwt.reverseInPlace(work);
                } catch (JWaveException e) {
                    throw new RuntimeException(e);
                }
            });
            
            double reduction = (1.0 - (double)inPlaceAllocations / standardAllocations) * 100;
            
            System.out.printf("%-7d | %19d | %19d | %7.1f%%\n",
                             size, standardAllocations, inPlaceAllocations, reduction);
        }
    }
    
    @Test
    public void testPerformanceComparison() throws JWaveException {
        TestUtils.skipIfPerformanceTestsDisabled();
        
        System.out.println("\n=== Performance Comparison Test ===");
        
        int[] sizes = {1024, 4096, 16384, 65536};
        Daubechies4 wavelet = new Daubechies4();
        
        System.out.println("Size    | Standard (ms) | In-Place (ms) | Speedup");
        System.out.println("--------|---------------|---------------|--------");
        
        for (int size : sizes) {
            double[] signal = generateSignal(size);
            
            // Benchmark standard transform
            FastWaveletTransform standardFwt = new FastWaveletTransform(wavelet);
            double standardTime = benchmarkTransform(standardFwt, signal, false);
            
            // Benchmark in-place transform
            InPlaceFastWaveletTransform inPlaceFwt = new InPlaceFastWaveletTransform(wavelet);
            double inPlaceTime = benchmarkTransform(inPlaceFwt, signal, true);
            
            double speedup = standardTime / inPlaceTime;
            
            System.out.printf("%-7d | %13.2f | %13.2f | %6.2fx\n",
                             size, standardTime, inPlaceTime, speedup);
        }
    }
    
    private double[] generateSignal(int size) {
        double[] signal = new double[size];
        Random random = new Random(42);
        for (int i = 0; i < size; i++) {
            signal[i] = random.nextGaussian();
        }
        return signal;
    }
    
    private double benchmarkTransform(BasicTransform transform, double[] signal, boolean inPlace) 
            throws JWaveException {
        // Warmup
        for (int i = 0; i < WARMUP_RUNS; i++) {
            double[] work = Arrays.copyOf(signal, signal.length);
            if (inPlace && transform instanceof InPlaceFastWaveletTransform) {
                InPlaceFastWaveletTransform ipTransform = (InPlaceFastWaveletTransform) transform;
                ipTransform.forwardInPlace(work);
                ipTransform.reverseInPlace(work);
            } else {
                double[] coeffs = transform.forward(work);
                transform.reverse(coeffs);
            }
        }
        
        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_RUNS; i++) {
            double[] work = Arrays.copyOf(signal, signal.length);
            if (inPlace && transform instanceof InPlaceFastWaveletTransform) {
                InPlaceFastWaveletTransform ipTransform = (InPlaceFastWaveletTransform) transform;
                ipTransform.forwardInPlace(work);
                ipTransform.reverseInPlace(work);
            } else {
                double[] coeffs = transform.forward(work);
                transform.reverse(coeffs);
            }
        }
        long endTime = System.nanoTime();
        
        return (endTime - startTime) / 1_000_000.0 / BENCHMARK_RUNS;
    }
    
    private long measureAllocations(Runnable task) {
        // Force GC to get clean baseline
        System.gc();
        Runtime runtime = Runtime.getRuntime();
        long before = runtime.totalMemory() - runtime.freeMemory();
        
        // Run task multiple times
        for (int i = 0; i < 10; i++) {
            task.run();
        }
        
        long after = runtime.totalMemory() - runtime.freeMemory();
        return Math.max(0, after - before);
    }
}