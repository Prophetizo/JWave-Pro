package jwave.transforms;

import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Test cache performance for sliding window analysis - a common use case.
 */
public class MODWTSlidingWindowTest {
    
    private static final int WINDOW_SIZE = 512;
    private static final int DECOMP_LEVELS = 8;
    private static final int TOTAL_SIGNAL_LENGTH = 10000;
    private static final int SLIDE_STEP = 64; // Slide window by 64 samples
    
    @Test
    public void testSlidingWindowPerformance() {
        System.out.println("=== MODWT Sliding Window Analysis (512 samples, 8 levels) ===\n");
        
        // Generate a long signal
        double[] longSignal = TestSignalGenerator.generateMultiFrequencySignal(TOTAL_SIGNAL_LENGTH);
        int numWindows = (TOTAL_SIGNAL_LENGTH - WINDOW_SIZE) / SLIDE_STEP + 1;
        
        System.out.println("Configuration:");
        System.out.println("- Window size: " + WINDOW_SIZE);
        System.out.println("- Decomposition levels: " + DECOMP_LEVELS);
        System.out.println("- Total signal length: " + TOTAL_SIGNAL_LENGTH);
        System.out.println("- Slide step: " + SLIDE_STEP);
        System.out.println("- Number of windows: " + numWindows);
        System.out.println();
        
        // Test with Haar wavelet
        testWaveletSlidingWindow("Haar1", new MODWTTransform(new Haar1()), 
                                longSignal, numWindows);
        
        // Test with Daubechies4 wavelet
        testWaveletSlidingWindow("Daubechies4", new MODWTTransform(new Daubechies4()), 
                                longSignal, numWindows);
    }
    
    private void testWaveletSlidingWindow(String waveletName, MODWTTransform modwt, 
                                         double[] longSignal, int numWindows) {
        System.out.println("\nWavelet: " + waveletName);
        System.out.println("------------------------------------------");
        
        // Test without cache
        modwt.clearFilterCache();
        long startNoCache = System.nanoTime();
        
        for (int i = 0; i < numWindows; i++) {
            int startIdx = i * SLIDE_STEP;
            double[] window = new double[WINDOW_SIZE];
            System.arraycopy(longSignal, startIdx, window, 0, WINDOW_SIZE);
            
            // Clear cache to simulate no caching
            if (i % 10 == 0) { // Clear periodically to simulate worst case
                modwt.clearFilterCache();
            }
            
            double[][] coeffs = modwt.forwardMODWT(window, DECOMP_LEVELS);
            // Simulate some processing (e.g., feature extraction)
            double energy = calculateEnergy(coeffs[0]); // Level 1 energy
            assertTrue("Energy value should be non-negative", energy >= 0);
        }
        
        long timeNoCache = System.nanoTime() - startNoCache;
        
        // Test with cache (pre-computed)
        modwt.clearFilterCache();
        modwt.precomputeFilters(DECOMP_LEVELS);
        long startWithCache = System.nanoTime();
        
        for (int i = 0; i < numWindows; i++) {
            int startIdx = i * SLIDE_STEP;
            double[] window = new double[WINDOW_SIZE];
            System.arraycopy(longSignal, startIdx, window, 0, WINDOW_SIZE);
            
            double[][] coeffs = modwt.forwardMODWT(window, DECOMP_LEVELS);
            // Simulate some processing
        }
        
        long timeWithCache = System.nanoTime() - startWithCache;
        
        // Calculate and display results
        double totalTimeNoCache = timeNoCache / 1e6; // ms
        double totalTimeWithCache = timeWithCache / 1e6; // ms
        double avgTimeNoCache = totalTimeNoCache / numWindows;
        double avgTimeWithCache = totalTimeWithCache / numWindows;
        double improvement = ((timeNoCache - timeWithCache) / (double)timeNoCache) * 100;
        double speedup = totalTimeNoCache / totalTimeWithCache;
        
        System.out.printf("Total time without cache: %.1f ms\n", totalTimeNoCache);
        System.out.printf("Total time with cache: %.1f ms\n", totalTimeWithCache);
        System.out.printf("Average per window without cache: %.3f ms\n", avgTimeNoCache);
        System.out.printf("Average per window with cache: %.3f ms\n", avgTimeWithCache);
        System.out.printf("Performance improvement: %.1f%%\n", improvement);
        System.out.printf("Speedup factor: %.2fx\n", speedup);
        
        // Calculate theoretical savings
        double filterComputationTime = 0.016 * 2 * DECOMP_LEVELS; // ~16μs per level from previous test
        double theoreticalSavings = filterComputationTime * numWindows / 1000; // Convert to ms
        System.out.printf("Theoretical filter computation savings: %.1f ms\n", theoreticalSavings);
    }
    
    @Test
    public void testRepeatedTransformsSameSignal() {
        System.out.println("\n=== Repeated Transforms on Same Signal (512 samples, 8 levels) ===\n");
        
        double[] signal = TestSignalGenerator.generateCompositeSignal(WINDOW_SIZE);
        int numIterations = 1000;
        
        MODWTTransform modwtHaar = new MODWTTransform(new Haar1());
        MODWTTransform modwtDb4 = new MODWTTransform(new Daubechies4());
        
        // Test Haar wavelet
        System.out.println("Haar wavelet - " + numIterations + " iterations:");
        testRepeatedTransforms(modwtHaar, signal, numIterations);
        
        // Test Daubechies4 wavelet
        System.out.println("\nDaubechies4 wavelet - " + numIterations + " iterations:");
        testRepeatedTransforms(modwtDb4, signal, numIterations);
    }
    
    private void testRepeatedTransforms(MODWTTransform modwt, double[] signal, int iterations) {
        // Without cache
        modwt.clearFilterCache();
        long startNoCache = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            if (i % 100 == 0) modwt.clearFilterCache(); // Simulate cache misses
            modwt.forwardMODWT(signal, DECOMP_LEVELS);
        }
        long timeNoCache = System.nanoTime() - startNoCache;
        
        // With cache
        modwt.clearFilterCache();
        modwt.precomputeFilters(DECOMP_LEVELS);
        long startWithCache = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            modwt.forwardMODWT(signal, DECOMP_LEVELS);
        }
        long timeWithCache = System.nanoTime() - startWithCache;
        
        double avgNoCache = (timeNoCache / 1e6) / iterations;
        double avgWithCache = (timeWithCache / 1e6) / iterations;
        double improvement = ((timeNoCache - timeWithCache) / (double)timeNoCache) * 100;
        
        System.out.printf("Average time without cache: %.3f ms\n", avgNoCache);
        System.out.printf("Average time with cache: %.3f ms\n", avgWithCache);
        System.out.printf("Improvement: %.1f%%\n", improvement);
        System.out.printf("Time saved per 1000 transforms: %.1f ms\n", 
                         (avgNoCache - avgWithCache) * 1000);
    }
    
    private double calculateEnergy(double[] coeffs) {
        double energy = 0;
        for (double c : coeffs) {
            energy += c * c;
        }
        return energy;
    }
    
}