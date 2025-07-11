package jwave.transforms;

import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.daubechies.Daubechies2;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.transforms.wavelets.daubechies.Daubechies8;
import jwave.transforms.wavelets.daubechies.Daubechies16;
import jwave.transforms.wavelets.daubechies.Daubechies20;
import jwave.transforms.wavelets.symlets.Symlet8;
import jwave.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * Performance test comparing direct vs FFT-based convolution for MODWT.
 * 
 * @author Stephen Romano
 * @date 11.01.2025
 */
public class MODWTFFTPerformanceTest {
    
    private static final int WARMUP_RUNS = 5;
    private static final int BENCHMARK_RUNS = 10;
    private static final long RANDOM_SEED = 42L;
    
    private MODWTTransform modwtHaar;
    private MODWTTransform modwtDb4;
    private MODWTTransform modwtDb20;
    private MODWTTransform modwtSym8;
    
    @Before
    public void setUp() {
        modwtHaar = new MODWTTransform(new Haar1());
        modwtDb4 = new MODWTTransform(new Daubechies4());
        modwtDb20 = new MODWTTransform(new Daubechies20());
        modwtSym8 = new MODWTTransform(new Symlet8());
    }
    
    @Test
    public void testConvolutionPerformanceComparison() {
        TestUtils.skipIfPerformanceTestsDisabled();
        
        System.out.println("\n=== MODWT Convolution Performance: Direct vs FFT ===");
        System.out.println("Warmup runs: " + WARMUP_RUNS + ", Benchmark runs: " + BENCHMARK_RUNS);
        
        // Test different signal sizes
        int[] signalSizes = {64, 128, 256, 512, 1024, 2048, 4096, 8192};
        int[] maxLevels = {3, 3, 4, 4, 5, 5, 6, 6}; // Appropriate levels for each size
        
        System.out.println("\nHaar Wavelet (filter length = 2):");
        System.out.println("Size    | Direct (ms) | FFT (ms) | Speedup | FFT Overhead");
        System.out.println("--------|-------------|----------|---------|-------------");
        
        for (int i = 0; i < signalSizes.length; i++) {
            benchmarkWavelet(modwtHaar, signalSizes[i], maxLevels[i], "Haar");
        }
        
        System.out.println("\nDaubechies-4 Wavelet (filter length = 8):");
        System.out.println("Size    | Direct (ms) | FFT (ms) | Speedup | FFT Overhead");
        System.out.println("--------|-------------|----------|---------|-------------");
        
        for (int i = 0; i < signalSizes.length; i++) {
            benchmarkWavelet(modwtDb4, signalSizes[i], maxLevels[i], "Db4");
        }
        
        // Test with longer filter to show when FFT becomes more beneficial
        System.out.println("\nDaubechies-20 Wavelet (filter length = 40):");
        System.out.println("Size    | Direct (ms) | FFT (ms) | Speedup | FFT Overhead");
        System.out.println("--------|-------------|----------|---------|-------------");
        
        for (int i = 0; i < signalSizes.length; i++) {
            benchmarkWavelet(modwtDb20, signalSizes[i], maxLevels[i], "Db20");
        }
    }
    
    @Test
    public void testBreakEvenPoint() {
        TestUtils.skipIfPerformanceTestsDisabled();
        
        System.out.println("\n=== Finding Break-Even Point for FFT Convolution ===");
        
        // Test with different filter lengths to find when FFT becomes beneficial
        int[] filterLengths = {2, 4, 8, 16, 32, 40}; // Haar, Db2, Db4, Db8, Db16, Db20
        MODWTTransform[] transforms = {
            modwtHaar,
            new MODWTTransform(new Daubechies2()),
            modwtDb4,
            new MODWTTransform(new Daubechies8()),
            new MODWTTransform(new Daubechies16()),
            modwtDb20
        };
        
        for (int f = 0; f < filterLengths.length; f++) {
            System.out.println("\nFilter length " + filterLengths[f] + ":");
            
            // Find the signal size where FFT becomes faster
            int breakEvenSize = findBreakEvenPoint(transforms[f], filterLengths[f]);
            if (breakEvenSize > 0) {
                System.out.println("  Break-even point: N = " + breakEvenSize);
                System.out.println("  N * M = " + (breakEvenSize * filterLengths[f]));
            } else {
                System.out.println("  FFT is always slower for tested sizes");
            }
        }
    }
    
    @Test
    public void testLargeScalePerformance() {
        TestUtils.skipIfPerformanceTestsDisabled();
        
        System.out.println("\n=== Large-Scale MODWT Performance Test ===");
        
        // Test with a very large signal
        int signalSize = 32768; // 2^15
        int maxLevel = 8;
        
        double[] signal = generateRandomSignal(signalSize);
        
        // Warmup
        for (int i = 0; i < 2; i++) {
            modwtDb4.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
            modwtDb4.forwardMODWT(signal, maxLevel);
        }
        
        // Benchmark FFT
        modwtDb4.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
        long startTime = System.nanoTime();
        double[][] fftCoeffs = modwtDb4.forwardMODWT(signal, maxLevel);
        long fftTime = System.nanoTime() - startTime;
        
        // Estimate direct convolution time (too slow to run fully)
        // Run a smaller portion and extrapolate
        int smallSize = 2048;
        double[] smallSignal = new double[smallSize];
        System.arraycopy(signal, 0, smallSignal, 0, smallSize);
        
        modwtDb4.setConvolutionMethod(MODWTTransform.ConvolutionMethod.DIRECT);
        startTime = System.nanoTime();
        modwtDb4.forwardMODWT(smallSignal, maxLevel);
        long smallDirectTime = System.nanoTime() - startTime;
        
        // Extrapolate (O(N²) scaling)
        double scaleFactor = (double)(signalSize * signalSize) / (smallSize * smallSize);
        long estimatedDirectTime = (long)(smallDirectTime * scaleFactor);
        
        System.out.println("Signal size: " + signalSize);
        System.out.println("Decomposition levels: " + maxLevel);
        System.out.println("FFT-based time: " + String.format("%.2f ms", fftTime / 1_000_000.0));
        System.out.println("Estimated direct time: " + String.format("%.2f ms", estimatedDirectTime / 1_000_000.0));
        System.out.println("Estimated speedup: " + String.format("%.1fx", estimatedDirectTime / (double)fftTime));
        
        // Verify result validity
        assertNotNull("FFT coefficients should not be null", fftCoeffs);
        assertEquals("Should have correct number of levels", maxLevel + 1, fftCoeffs.length);
    }
    
    private void benchmarkWavelet(MODWTTransform modwt, int signalSize, int maxLevel, String name) {
        double[] signal = generateRandomSignal(signalSize);
        
        // Warmup
        for (int i = 0; i < WARMUP_RUNS; i++) {
            modwt.setConvolutionMethod(MODWTTransform.ConvolutionMethod.DIRECT);
            modwt.forwardMODWT(signal, maxLevel);
            modwt.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
            modwt.forwardMODWT(signal, maxLevel);
        }
        
        // Benchmark direct convolution
        modwt.setConvolutionMethod(MODWTTransform.ConvolutionMethod.DIRECT);
        long directTime = 0;
        for (int i = 0; i < BENCHMARK_RUNS; i++) {
            long start = System.nanoTime();
            modwt.forwardMODWT(signal, maxLevel);
            directTime += System.nanoTime() - start;
        }
        double avgDirectTime = directTime / (double) BENCHMARK_RUNS / 1_000_000.0; // ms
        
        // Benchmark FFT convolution
        modwt.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
        long fftTime = 0;
        for (int i = 0; i < BENCHMARK_RUNS; i++) {
            long start = System.nanoTime();
            modwt.forwardMODWT(signal, maxLevel);
            fftTime += System.nanoTime() - start;
        }
        double avgFFTTime = fftTime / (double) BENCHMARK_RUNS / 1_000_000.0; // ms
        
        // Calculate speedup
        double speedup = avgDirectTime / avgFFTTime;
        String overhead = speedup < 1.0 ? String.format("%.1f%%", (1.0 - speedup) * 100) : "-";
        
        System.out.printf("%-7d | %11.2f | %8.2f | %7.2fx | %s\n",
                         signalSize, avgDirectTime, avgFFTTime, speedup, overhead);
    }
    
    private int findBreakEvenPoint(MODWTTransform modwt, int filterLength) {
        // Binary search for break-even point
        int low = 16;
        int high = 8192;
        int breakEven = -1;
        
        while (low <= high) {
            int mid = (low + high) / 2;
            double[] signal = generateRandomSignal(mid);
            
            // Time both methods
            modwt.setConvolutionMethod(MODWTTransform.ConvolutionMethod.DIRECT);
            long directTime = timeTransform(modwt, signal, 3);
            
            modwt.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
            long fftTime = timeTransform(modwt, signal, 3);
            
            if (fftTime < directTime) {
                breakEven = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        
        return breakEven;
    }
    
    private long timeTransform(MODWTTransform modwt, double[] signal, int maxLevel) {
        // Quick timing without warmup (for binary search)
        long start = System.nanoTime();
        modwt.forwardMODWT(signal, maxLevel);
        return System.nanoTime() - start;
    }
    
    private double[] generateRandomSignal(int size) {
        Random rand = new Random(RANDOM_SEED);
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = rand.nextGaussian();
        }
        return signal;
    }
}