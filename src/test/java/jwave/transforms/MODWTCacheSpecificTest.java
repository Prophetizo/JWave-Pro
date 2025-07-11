package jwave.transforms;

import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.transforms.wavelets.daubechies.Daubechies8;
import jwave.transforms.wavelets.symlets.Symlet8;
import org.junit.Test;

/**
 * Specific performance test for 8 decomposition levels with 512-length signals.
 */
public class MODWTCacheSpecificTest {
    
    private static final int SIGNAL_LENGTH = 512;
    private static final int DECOMP_LEVELS = 8;
    private static final int WARMUP_ITERATIONS = 10;
    private static final int TEST_ITERATIONS = 100;
    
    @Test
    public void testCachePerformanceFor512Length8Levels() {
        System.out.println("=== MODWT Cache Performance: 512 samples, 8 levels ===\n");
        
        // Test different wavelets
        testWaveletPerformance("Haar1", new MODWTTransform(new Haar1()));
        testWaveletPerformance("Daubechies4", new MODWTTransform(new Daubechies4()));
        testWaveletPerformance("Daubechies8", new MODWTTransform(new Daubechies8()));
        testWaveletPerformance("Symlet8", new MODWTTransform(new Symlet8()));
        
        System.out.println("\n=== Cache Memory Analysis ===");
        analyzeCacheMemory();
        
        System.out.println("\n=== Filter Computation Overhead ===");
        analyzeFilterComputationOverhead();
    }
    
    private void testWaveletPerformance(String waveletName, MODWTTransform modwt) {
        double[] signal = TestSignalGenerator.generateCompositeSignal(SIGNAL_LENGTH);
        
        System.out.println("\nWavelet: " + waveletName);
        System.out.println("Metric\t\t\tNo Cache\tWith Cache\tImprovement");
        System.out.println("------\t\t\t--------\t----------\t-----------");
        
        // Warm-up
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            modwt.forwardMODWT(signal, DECOMP_LEVELS);
        }
        
        // Test without cache (clear before each transform)
        long startNoCache = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            modwt.clearFilterCache();
            double[][] coeffs = modwt.forwardMODWT(signal, DECOMP_LEVELS);
            modwt.inverseMODWT(coeffs);
        }
        long timeNoCache = System.nanoTime() - startNoCache;
        
        // Test with cache (pre-compute once)
        modwt.clearFilterCache();
        modwt.precomputeFilters(DECOMP_LEVELS);
        long startWithCache = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            double[][] coeffs = modwt.forwardMODWT(signal, DECOMP_LEVELS);
            modwt.inverseMODWT(coeffs);
        }
        long timeWithCache = System.nanoTime() - startWithCache;
        
        // Calculate metrics
        double avgNoCache = (timeNoCache / 1e6) / TEST_ITERATIONS;
        double avgWithCache = (timeWithCache / 1e6) / TEST_ITERATIONS;
        double improvement = ((timeNoCache - timeWithCache) / (double)timeNoCache) * 100;
        
        System.out.printf("Forward+Inverse (ms)\t%.3f\t\t%.3f\t\t%.1f%%\n", 
                         avgNoCache, avgWithCache, improvement);
        
        // Test just forward transform
        startNoCache = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            modwt.clearFilterCache();
            modwt.forwardMODWT(signal, DECOMP_LEVELS);
        }
        timeNoCache = System.nanoTime() - startNoCache;
        
        modwt.clearFilterCache();
        modwt.precomputeFilters(DECOMP_LEVELS);
        startWithCache = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            modwt.forwardMODWT(signal, DECOMP_LEVELS);
        }
        timeWithCache = System.nanoTime() - startWithCache;
        
        avgNoCache = (timeNoCache / 1e6) / TEST_ITERATIONS;
        avgWithCache = (timeWithCache / 1e6) / TEST_ITERATIONS;
        improvement = ((timeNoCache - timeWithCache) / (double)timeNoCache) * 100;
        
        System.out.printf("Forward only (ms)\t%.3f\t\t%.3f\t\t%.1f%%\n", 
                         avgNoCache, avgWithCache, improvement);
        
        // Calculate filter computation savings
        int filterComputations = 2 * DECOMP_LEVELS; // G and H filters for each level
        System.out.printf("Filter computations saved per transform: %d\n", filterComputations);
    }
    
    private void analyzeCacheMemory() {
        MODWTTransform modwt = new MODWTTransform(new Daubechies8());
        
        // Calculate theoretical memory usage
        int filterLength = 16; // Daubechies8 has 16 coefficients
        long totalMemory = 0;
        
        System.out.println("\nFilter sizes by level:");
        for (int level = 1; level <= DECOMP_LEVELS; level++) {
            int upsampledLength = filterLength + (filterLength - 1) * ((1 << (level - 1)) - 1);
            long levelMemory = 2 * upsampledLength * 8; // 2 filters, 8 bytes per double
            totalMemory += levelMemory;
            System.out.printf("Level %d: %d coefficients per filter, %d bytes\n", 
                            level, upsampledLength, levelMemory);
        }
        
        System.out.printf("\nTotal cache memory for 8 levels: %.1f KB\n", totalMemory / 1024.0);
        
        // Actual measurement
        System.gc();
        Runtime rt = Runtime.getRuntime();
        long memBefore = rt.totalMemory() - rt.freeMemory();
        
        modwt.precomputeFilters(DECOMP_LEVELS);
        
        System.gc();
        long memAfter = rt.totalMemory() - rt.freeMemory();
        long actualUsage = memAfter - memBefore;
        
        System.out.printf("Measured cache memory usage: %d KB\n", actualUsage / 1024);
    }
    
    private void analyzeFilterComputationOverhead() {
        System.out.println("\nFilter upsampling overhead analysis:");
        
        double[] baseFilter = new double[8]; // Typical filter size
        for (int i = 0; i < baseFilter.length; i++) {
            baseFilter[i] = Math.random();
        }
        
        // Time filter upsampling for each level
        for (int level = 1; level <= DECOMP_LEVELS; level++) {
            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                upsampleFilter(baseFilter, level);
            }
            long elapsed = System.nanoTime() - start;
            double avgTime = elapsed / 1000.0 / 1000.0; // microseconds
            
            int outputLength = baseFilter.length + (baseFilter.length - 1) * ((1 << (level - 1)) - 1);
            System.out.printf("Level %d upsampling: %.3f μs per filter (output size: %d)\n", 
                            level, avgTime, outputLength);
        }
        
        // Total overhead per transform
        double totalOverhead = 0;
        for (int level = 1; level <= DECOMP_LEVELS; level++) {
            totalOverhead += 2; // Rough estimate in microseconds
        }
        System.out.printf("\nTotal upsampling overhead per transform: ~%.1f μs\n", totalOverhead);
        System.out.println("This overhead is eliminated when using the cache.");
    }
    
    private double[] upsampleFilter(double[] filter, int level) {
        if (level <= 1) return filter;
        int gap = (1 << (level - 1)) - 1;
        int newLength = filter.length + (filter.length - 1) * gap;
        double[] upsampled = new double[newLength];
        for (int i = 0; i < filter.length; i++) {
            upsampled[i * (gap + 1)] = filter[i];
        }
        return upsampled;
    }
    
}