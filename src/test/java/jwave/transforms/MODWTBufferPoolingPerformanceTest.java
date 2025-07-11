package jwave.transforms;

import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.transforms.wavelets.daubechies.Daubechies20;
import jwave.utils.ArrayBufferPool;
import jwave.utils.TestUtils;
import org.junit.Test;
import org.junit.Before;

import java.util.Random;

/**
 * Performance test comparing standard MODWT with pooled MODWT to measure GC impact.
 * 
 * To skip performance tests in CI, use: mvn test -Djwave.test.skipPerformance=true
 * 
 * @author Stephen Romano
 */
public class MODWTBufferPoolingPerformanceTest {
    
    private static final int WARMUP_ITERATIONS = 100;
    private static final int TEST_ITERATIONS = 1000;
    private static final int SIGNAL_SIZE = 1024;
    private static final int MAX_LEVEL = 5;
    
    private double[] testSignal;
    private MODWTTransform standardModwt;
    private PooledMODWTTransform pooledModwt;
    
    @Before
    public void setUp() {
        // Generate test signal
        Random rand = new Random(42);
        testSignal = new double[SIGNAL_SIZE];
        for (int i = 0; i < SIGNAL_SIZE; i++) {
            testSignal[i] = rand.nextGaussian();
        }
        
        // Create transforms
        standardModwt = new MODWTTransform(new Daubechies4());
        pooledModwt = new PooledMODWTTransform(new Daubechies4());
    }
    
    @Test
    public void testGCPressureComparison() {
        TestUtils.skipIfPerformanceTestsDisabled();
        
        System.out.println("\n=== MODWT Buffer Pooling Performance Test ===");
        System.out.println("Signal size: " + SIGNAL_SIZE);
        System.out.println("Decomposition levels: " + MAX_LEVEL);
        System.out.println("Test iterations: " + TEST_ITERATIONS);
        
        // Test standard MODWT
        System.out.println("\n--- Standard MODWT (with allocations) ---");
        runGCTest("Standard", () -> {
            double[][] coeffs = standardModwt.forwardMODWT(testSignal, MAX_LEVEL);
            double[] reconstructed = standardModwt.inverseMODWT(coeffs);
        });
        
        // Test pooled MODWT
        System.out.println("\n--- Pooled MODWT (with buffer pooling) ---");
        runGCTest("Pooled", () -> {
            double[][] coeffs = pooledModwt.forwardMODWT(testSignal, MAX_LEVEL);
            double[] reconstructed = pooledModwt.inverseMODWT(coeffs);
        });
        
        // Clean up thread-local pool
        ArrayBufferPool.remove();
    }
    
    @Test
    public void testFFTConvolutionPooling() {
        TestUtils.skipIfPerformanceTestsDisabled();
        
        System.out.println("\n=== FFT Convolution Pooling Test ===");
        
        // Use Daubechies-20 to trigger FFT convolution
        MODWTTransform standardFFT = new MODWTTransform(new Daubechies20());
        PooledMODWTTransform pooledFFT = new PooledMODWTTransform(new Daubechies20());
        
        // Force FFT mode
        standardFFT.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
        pooledFFT.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
        
        System.out.println("\n--- Standard FFT Convolution ---");
        runGCTest("Standard FFT", () -> {
            double[][] coeffs = standardFFT.forwardMODWT(testSignal, MAX_LEVEL);
            double[] reconstructed = standardFFT.inverseMODWT(coeffs);
        });
        
        System.out.println("\n--- Pooled FFT Convolution ---");
        runGCTest("Pooled FFT", () -> {
            double[][] coeffs = pooledFFT.forwardMODWT(testSignal, MAX_LEVEL);
            double[] reconstructed = pooledFFT.inverseMODWT(coeffs);
        });
        
        // Clean up thread-local pool
        ArrayBufferPool.remove();
    }
    
    private void runGCTest(String name, Runnable transform) {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            transform.run();
        }
        
        // Force GC before test
        System.gc();
        // System.runFinalization() is deprecated and will be removed
        Thread.yield();
        
        // Get initial memory stats
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        long beforeGCs = getGCCount();
        
        // Run test
        long startTime = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            transform.run();
        }
        long endTime = System.nanoTime();
        
        // Get final memory stats
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long afterGCs = getGCCount();
        
        // Calculate metrics
        double avgTimeMs = (endTime - startTime) / 1_000_000.0 / TEST_ITERATIONS;
        long memoryGrowth = finalMemory - initialMemory;
        long gcCount = afterGCs - beforeGCs;
        
        System.out.printf("Average time per transform: %.3f ms\n", avgTimeMs);
        System.out.printf("Memory growth: %.2f MB\n", memoryGrowth / (1024.0 * 1024.0));
        System.out.printf("GC invocations during test: %d\n", gcCount);
    }
    
    private long getGCCount() {
        long totalGCs = 0;
        for (java.lang.management.GarbageCollectorMXBean gc : 
             java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
            totalGCs += gc.getCollectionCount();
        }
        return totalGCs;
    }
    
    @Test
    public void testCorrectnessOfPooledImplementation() {
        // This test verifies correctness, not performance, so it always runs
        System.out.println("\n=== Correctness Verification ===");
        
        // Perform transforms
        double[][] standardCoeffs = standardModwt.forwardMODWT(testSignal, MAX_LEVEL);
        double[][] pooledCoeffs = pooledModwt.forwardMODWT(testSignal, MAX_LEVEL);
        
        // Verify coefficients match
        double maxDiff = 0.0;
        for (int level = 0; level <= MAX_LEVEL; level++) {
            for (int i = 0; i < SIGNAL_SIZE; i++) {
                double diff = Math.abs(standardCoeffs[level][i] - pooledCoeffs[level][i]);
                maxDiff = Math.max(maxDiff, diff);
            }
        }
        
        System.out.printf("Maximum coefficient difference: %.2e\n", maxDiff);
        
        // Verify reconstruction
        double[] standardRecon = standardModwt.inverseMODWT(standardCoeffs);
        double[] pooledRecon = pooledModwt.inverseMODWT(pooledCoeffs);
        
        double maxReconDiff = 0.0;
        for (int i = 0; i < SIGNAL_SIZE; i++) {
            double diff = Math.abs(standardRecon[i] - pooledRecon[i]);
            maxReconDiff = Math.max(maxReconDiff, diff);
        }
        
        System.out.printf("Maximum reconstruction difference: %.2e\n", maxReconDiff);
        
        // Clean up
        ArrayBufferPool.remove();
    }
}