package jwave.transforms;

import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.transforms.wavelets.daubechies.Daubechies8;
import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.symlets.Symlet8;
import jwave.utils.TestUtils;
import jwave.utils.ArrayBufferPool;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.Random;
import java.util.concurrent.ForkJoinPool;

import static org.junit.Assert.*;
import jwave.exceptions.JWaveException;

/**
 * Performance test for parallel Wavelet Packet Transform implementation.
 * 
 * <p>Tests compare sequential vs parallel performance across different:
 * <ul>
 *   <li>Signal sizes (512 to 65536 samples)</li>
 *   <li>Decomposition levels</li>
 *   <li>Wavelet types</li>
 *   <li>Thread pool sizes</li>
 * </ul>
 * 
 * @author Stephen Romano
 * @since 2.1.0
 */
public class ParallelWPTPerformanceTest {
    
    private static final int WARMUP_RUNS = 5;
    private static final int BENCHMARK_RUNS = 10;
    private static final long RANDOM_SEED = 42L;
    private static final double TOLERANCE = 1e-10;
    
    private Random random;
    
    @Before
    public void setUp() {
        random = new Random(RANDOM_SEED);
    }
    
    @After
    public void tearDown() {
        // Clean up thread-local pools
        ArrayBufferPool.remove();
    }
    
    /**
     * Test correctness: verify parallel and sequential produce identical results.
     */
    @Test
    public void testParallelCorrectness() throws Exception {
        int[] sizes = {512, 1024, 2048, 4096};
        int[] levels = {2, 3, 4, 5};
        
        Daubechies4 wavelet = new Daubechies4();
        
        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            int maxLevel = Math.min(levels[i], calcMaxLevel(size));
            
            double[] signal = generateRandomSignal(size);
            
            // Sequential transform
            WaveletPacketTransform wptSeq = new WaveletPacketTransform(wavelet);
            double[] seqForward, seqReverse, parForward, parReverse;
            ParallelWaveletPacketTransform wptPar = new ParallelWaveletPacketTransform(wavelet);
            
            try {
                seqForward = wptSeq.forward(signal, maxLevel);
                seqReverse = wptSeq.reverse(seqForward, maxLevel);
                
                // Parallel transform
                parForward = wptPar.forward(signal, maxLevel);
                parReverse = wptPar.reverse(parForward, maxLevel);
            } catch (JWaveException e) {
                wptPar.shutdown();
                throw new RuntimeException("Transform failed", e);
            }
            
            // Verify forward transform results match
            assertArrayEquals("Forward transform mismatch at size " + size, 
                            seqForward, parForward, TOLERANCE);
            
            // Verify reverse transform results match
            assertArrayEquals("Reverse transform mismatch at size " + size,
                            seqReverse, parReverse, TOLERANCE);
            
            // Verify perfect reconstruction
            assertArrayEquals("Reconstruction mismatch at size " + size,
                            signal, parReverse, TOLERANCE);
            
            wptPar.shutdown();
        }
    }
    
    /**
     * Test performance across different signal sizes and decomposition levels.
     */
    @Test
    public void testParallelPerformanceScaling() throws Exception {
        TestUtils.skipIfPerformanceTestsDisabled();
        
        System.out.println("\n=== Parallel WPT Performance Test ===");
        System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
        
        int[] sizes = {512, 1024, 2048, 4096, 8192, 16384, 32768, 65536};
        Daubechies8 wavelet = new Daubechies8();
        
        System.out.println("\nSize    | Level | Sequential (ms) | Parallel (ms) | Speedup | Efficiency");
        System.out.println("--------|-------|-----------------|---------------|---------|------------");
        
        for (int size : sizes) {
            int maxLevel = calcMaxLevel(size);
            int testLevel = Math.min(maxLevel - 2, 6); // Use reasonable level
            
            double[] signal = generateRandomSignal(size);
            
            // Benchmark sequential
            WaveletPacketTransform wptSeq = new WaveletPacketTransform(wavelet);
            double seqTime = benchmarkTransform(wptSeq, signal, testLevel);
            
            // Benchmark parallel
            ParallelWaveletPacketTransform wptPar = new ParallelWaveletPacketTransform(wavelet);
            double parTime = benchmarkTransform(wptPar, signal, testLevel);
            
            // Calculate metrics
            double speedup = seqTime / parTime;
            double efficiency = speedup / wptPar.getParallelism() * 100;
            
            System.out.printf("%-7d | %-5d | %15.2f | %13.2f | %7.2fx | %9.1f%%\n",
                            size, testLevel, seqTime, parTime, speedup, efficiency);
            
            wptPar.shutdown();
        }
    }
    
    /**
     * Test impact of different thread pool sizes.
     */
    @Test
    public void testThreadPoolScaling() throws Exception {
        TestUtils.skipIfPerformanceTestsDisabled();
        
        System.out.println("\n=== Thread Pool Scaling Test ===");
        
        int signalSize = 16384;
        int level = 6;
        double[] signal = generateRandomSignal(signalSize);
        Symlet8 wavelet = new Symlet8();
        
        // Baseline: sequential
        WaveletPacketTransform wptSeq = new WaveletPacketTransform(wavelet);
        double seqTime = benchmarkTransform(wptSeq, signal, level);
        
        System.out.println("\nThreads | Time (ms) | Speedup | Efficiency");
        System.out.println("--------|-----------|---------|------------");
        System.out.printf("%-7d | %9.2f | %7s | %10s\n", 1, seqTime, "1.00x", "100.0%");
        
        // Test different thread counts
        int maxThreads = Math.min(16, Runtime.getRuntime().availableProcessors() * 2);
        for (int threads = 2; threads <= maxThreads; threads *= 2) {
            ParallelWaveletPacketTransform wptPar = 
                new ParallelWaveletPacketTransform(wavelet, threads);
            
            double parTime = benchmarkTransform(wptPar, signal, level);
            double speedup = seqTime / parTime;
            double efficiency = speedup / threads * 100;
            
            System.out.printf("%-7d | %9.2f | %6.2fx | %9.1f%%\n",
                            threads, parTime, speedup, efficiency);
            
            wptPar.shutdown();
        }
    }
    
    /**
     * Test performance with different wavelet types.
     */
    @Test
    public void testWaveletTypePerformance() throws Exception {
        TestUtils.skipIfPerformanceTestsDisabled();
        
        System.out.println("\n=== Wavelet Type Performance Test ===");
        
        int signalSize = 8192;
        int level = 5;
        double[] signal = generateRandomSignal(signalSize);
        
        // Test different wavelets
        Object[][] wavelets = {
            {"Haar", new Haar1()},
            {"Daubechies-4", new Daubechies4()},
            {"Daubechies-8", new Daubechies8()},
            {"Symlet-8", new Symlet8()}
        };
        
        System.out.println("\nWavelet      | Sequential (ms) | Parallel (ms) | Speedup");
        System.out.println("-------------|-----------------|---------------|--------");
        
        for (Object[] waveletInfo : wavelets) {
            String name = (String) waveletInfo[0];
            jwave.transforms.wavelets.Wavelet wavelet = 
                (jwave.transforms.wavelets.Wavelet) waveletInfo[1];
            
            // Sequential
            WaveletPacketTransform wptSeq = new WaveletPacketTransform(wavelet);
            double seqTime = benchmarkTransform(wptSeq, signal, level);
            
            // Parallel
            ParallelWaveletPacketTransform wptPar = new ParallelWaveletPacketTransform(wavelet);
            double parTime = benchmarkTransform(wptPar, signal, level);
            
            double speedup = seqTime / parTime;
            
            System.out.printf("%-12s | %15.2f | %13.2f | %6.2fx\n",
                            name, seqTime, parTime, speedup);
            
            wptPar.shutdown();
        }
    }
    
    /**
     * Test the impact of parallelization threshold.
     */
    @Test
    public void testParallelizationOverhead() throws Exception {
        TestUtils.skipIfPerformanceTestsDisabled();
        
        System.out.println("\n=== Parallelization Overhead Test ===");
        System.out.println("Testing small signal sizes where parallelization may have overhead...");
        
        int[] sizes = {64, 128, 256, 512, 1024};
        Haar1 wavelet = new Haar1(); // Fast wavelet to emphasize overhead
        
        System.out.println("\nSize | Level | Sequential (ms) | Parallel (ms) | Overhead");
        System.out.println("-----|-------|-----------------|---------------|----------");
        
        for (int size : sizes) {
            int level = 2; // Fixed small level
            double[] signal = generateRandomSignal(size);
            
            // Sequential
            PooledWaveletPacketTransform wptSeq = new PooledWaveletPacketTransform(wavelet);
            double seqTime = benchmarkTransform(wptSeq, signal, level);
            
            // Parallel
            ParallelWaveletPacketTransform wptPar = new ParallelWaveletPacketTransform(wavelet);
            double parTime = benchmarkTransform(wptPar, signal, level);
            
            double overhead = (parTime / seqTime - 1) * 100;
            String overheadStr = overhead > 0 ? String.format("+%.1f%%", overhead) 
                                              : String.format("%.1f%%", overhead);
            
            System.out.printf("%-4d | %-5d | %15.3f | %13.3f | %9s\n",
                            size, level, seqTime, parTime, overheadStr);
            
            wptPar.shutdown();
        }
    }
    
    /**
     * Benchmark a transform implementation.
     */
    private double benchmarkTransform(BasicTransform transform, double[] signal, int level) throws Exception {
        // Warmup
        for (int i = 0; i < WARMUP_RUNS; i++) {
            try {
                double[] coeffs = transform.forward(signal, level);
                transform.reverse(coeffs, level);
            } catch (JWaveException e) {
                throw new RuntimeException("Warmup transform failed", e);
            }
        }
        
        // Benchmark
        long totalTime = 0;
        for (int i = 0; i < BENCHMARK_RUNS; i++) {
            long start = System.nanoTime();
            try {
                double[] coeffs = transform.forward(signal, level);
                double[] reconstructed = transform.reverse(coeffs, level);
            } catch (JWaveException e) {
                throw new RuntimeException("Transform failed", e);
            }
            totalTime += System.nanoTime() - start;
        }
        
        return totalTime / 1_000_000.0 / BENCHMARK_RUNS; // Average time in ms
    }
    
    /**
     * Generate a random signal for testing.
     */
    private double[] generateRandomSignal(int size) {
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = random.nextGaussian();
        }
        return signal;
    }
    
    /**
     * Calculate maximum decomposition level for a given signal size.
     */
    private int calcMaxLevel(int size) {
        int level = 0;
        while (size > 1) {
            size >>= 1;
            level++;
        }
        return level;
    }
    
    /**
     * Test memory efficiency of parallel implementation.
     */
    @Test
    public void testMemoryEfficiency() throws Exception {
        TestUtils.skipIfPerformanceTestsDisabled();
        
        System.out.println("\n=== Memory Efficiency Test ===");
        
        int signalSize = 16384;
        int level = 6;
        double[] signal = generateRandomSignal(signalSize);
        Daubechies4 wavelet = new Daubechies4();
        
        // Force GC before test
        System.gc();
        Thread.yield();
        
        Runtime runtime = Runtime.getRuntime();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Run parallel transform multiple times
        ParallelWaveletPacketTransform wptPar = new ParallelWaveletPacketTransform(wavelet);
        for (int i = 0; i < 100; i++) {
            try {
                double[] coeffs = wptPar.forward(signal, level);
                double[] reconstructed = wptPar.reverse(coeffs, level);
            } catch (JWaveException e) {
                throw new RuntimeException("Transform failed", e);
            }
        }
        
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = afterMemory - beforeMemory;
        
        System.out.printf("Memory used: %.2f MB\n", memoryUsed / (1024.0 * 1024.0));
        System.out.println("(Low memory usage indicates effective buffer pooling)");
        
        wptPar.shutdown();
        
        // Clean up pools
        ArrayBufferPool.remove();
    }
}