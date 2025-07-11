package jwave.transforms;

import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.transforms.wavelets.symlets.Symlet8;
import jwave.exceptions.JWaveException;
import org.junit.Test;

/**
 * Analysis of theoretical and practical limits for MODWT decomposition levels.
 * 
 * This test class explores:
 * 1. The relationship between signal length and maximum meaningful decomposition level
 * 2. Memory impact of high decomposition levels
 * 3. Filter length constraints based on wavelet type
 * 4. Practical recommendations for maximum levels
 * 
 * @author Stephen Romano
 */
public class MODWTLevelLimitsAnalysis {

    /**
     * Test 1: Theoretical Maximum Level Based on Signal Length
     * 
     * For DWT, the maximum level is log2(N) where N is the signal length.
     * For MODWT, the theoretical limit is similar, but we need to consider
     * the effective support of the upsampled filters.
     */
    @Test
    public void analyzeTheoreticalMaximumLevels() {
        System.out.println("=== THEORETICAL MAXIMUM LEVELS BASED ON SIGNAL LENGTH ===\n");
        
        // Test various signal lengths (powers of 2)
        int[] signalLengths = {8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536};
        
        System.out.println("Signal Length | Log2(N) | Max Meaningful Level");
        System.out.println("--------------|---------|--------------------");
        
        for (int N : signalLengths) {
            int maxLevel = (int)(Math.log(N) / Math.log(2));
            System.out.printf("%13d | %7d | %d%n", N, maxLevel, maxLevel);
        }
        
        System.out.println("\nNote: For MODWT, the theoretical maximum is log2(N), same as DWT.");
        System.out.println("However, practical limits depend on filter length and memory constraints.\n");
    }

    /**
     * Test 2: Filter Length Growth at Different Levels
     * 
     * At level j, the MODWT filter length becomes:
     * L_j = (L - 1) * 2^(j-1) + 1
     * where L is the base filter length
     */
    @Test
    public void analyzeFilterLengthGrowth() {
        System.out.println("=== FILTER LENGTH GROWTH AT DIFFERENT LEVELS ===\n");
        
        // Test with different wavelets having different filter lengths
        int[] baseFilterLengths = {2, 4, 8, 16, 20};  // Haar, Db2, Db4, Db8, Db10
        String[] waveletNames = {"Haar", "Db2", "Db4", "Db8", "Db10"};
        
        System.out.println("Level | Haar | Db2  | Db4   | Db8     | Db10");
        System.out.println("------|------|------|-------|---------|----------");
        
        for (int level = 1; level <= 15; level++) {
            System.out.printf("%5d |", level);
            
            for (int i = 0; i < baseFilterLengths.length; i++) {
                int L = baseFilterLengths[i];
                long filterLength = (long)(L - 1) * (1L << (level - 1)) + 1;
                
                if (filterLength > Integer.MAX_VALUE) {
                    System.out.printf(" OVERFLOW |");
                } else if (filterLength > 1000000) {
                    System.out.printf(" %7.1fM |", filterLength / 1000000.0);
                } else if (filterLength > 1000) {
                    System.out.printf(" %7.1fK |", filterLength / 1000.0);
                } else {
                    System.out.printf(" %8d |", (int)filterLength);
                }
            }
            System.out.println();
        }
        
        System.out.println("\nObservation: Filter lengths grow exponentially with level!");
        System.out.println("This becomes a major constraint at higher levels.\n");
    }

    /**
     * Test 3: Memory Requirements at Different Levels
     * 
     * For MODWT with N samples and J levels:
     * Memory = N * (J + 1) * 8 bytes (for double arrays)
     * Plus temporary storage for filters and intermediate calculations
     */
    @Test
    public void analyzeMemoryRequirements() {
        System.out.println("=== MEMORY REQUIREMENTS FOR MODWT COEFFICIENTS ===\n");
        
        int[] signalLengths = {1024, 4096, 16384, 65536, 262144, 1048576};
        
        System.out.println("Signal Length | Levels | Coefficient Memory | Filter Memory (Db4) | Total Memory");
        System.out.println("--------------|--------|-------------------|---------------------|-------------");
        
        for (int N : signalLengths) {
            int maxLevel = (int)(Math.log(N) / Math.log(2));
            
            // Memory for coefficients: N * (maxLevel + 1) * 8 bytes
            long coeffMemory = (long)N * (maxLevel + 1) * 8;
            
            // Memory for cached filters (assuming Db4 with length 8)
            // Two filters per level, upsampled
            long filterMemory = 0;
            for (int j = 1; j <= maxLevel; j++) {
                long filterLength = 7L * (1L << (j - 1)) + 1;  // For Db4
                filterMemory += 2 * filterLength * 8;  // 2 filters, 8 bytes per double
            }
            
            long totalMemory = coeffMemory + filterMemory;
            
            System.out.printf("%13d | %6d | %17s | %19s | %s%n",
                N, maxLevel,
                formatBytes(coeffMemory),
                formatBytes(filterMemory),
                formatBytes(totalMemory));
        }
        
        System.out.println("\nNote: This doesn't include temporary arrays for convolution operations.\n");
    }

    /**
     * Test 4: Practical Decomposition Level Recommendations
     * 
     * Based on:
     * 1. Signal length constraints (filter shouldn't exceed signal length)
     * 2. Memory constraints
     * 3. Meaningful frequency resolution
     */
    @Test
    public void analyzePracticalLevelRecommendations() {
        System.out.println("=== PRACTICAL LEVEL RECOMMENDATIONS ===\n");
        
        int[] signalLengths = {128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536};
        int[] filterLengths = {2, 4, 8, 16, 20};  // Different wavelet filter lengths
        String[] waveletNames = {"Haar", "Db2", "Db4", "Db8", "Db10"};
        
        System.out.println("Signal Length | Theoretical Max | Practical Max by Wavelet Type");
        System.out.println("              |                 | Haar | Db2 | Db4 | Db8 | Db10");
        System.out.println("--------------|-----------------|------|-----|-----|-----|------");
        
        for (int N : signalLengths) {
            int theoreticalMax = (int)(Math.log(N) / Math.log(2));
            System.out.printf("%13d | %15d |", N, theoreticalMax);
            
            for (int i = 0; i < filterLengths.length; i++) {
                int L = filterLengths[i];
                int practicalMax = findPracticalMaxLevel(N, L);
                System.out.printf(" %4d |", practicalMax);
            }
            System.out.println();
        }
        
        System.out.println("\nRecommendations:");
        System.out.println("1. For signals < 1024 samples: limit to 3-5 levels");
        System.out.println("2. For signals 1024-8192 samples: limit to 5-8 levels");
        System.out.println("3. For signals > 8192 samples: can use up to 10-12 levels");
        System.out.println("4. Consider wavelet filter length - longer filters need lower max levels");
        System.out.println("5. For real-time applications, keep levels ≤ 8 for performance\n");
    }

    /**
     * Test 5: Performance Impact of High Decomposition Levels
     */
    @Test
    public void analyzePerformanceImpact() {
        System.out.println("=== PERFORMANCE IMPACT OF DECOMPOSITION LEVELS ===\n");
        
        // Test with a moderate signal length
        int N = 4096;
        double[] signal = new double[N];
        for (int i = 0; i < N; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 64) + 0.5 * Math.sin(2 * Math.PI * i / 16);
        }
        
        MODWTTransform modwt = new MODWTTransform(new Daubechies4());
        
        System.out.println("Level | Forward Time (ms) | Inverse Time (ms) | Total Time (ms)");
        System.out.println("------|------------------|------------------|----------------");
        
        for (int level = 1; level <= 10; level++) {
            // Warm up
            modwt.forwardMODWT(signal, level);
            
            // Time forward transform
            long startForward = System.nanoTime();
            double[][] coeffs = modwt.forwardMODWT(signal, level);
            long endForward = System.nanoTime();
            double forwardTime = (endForward - startForward) / 1_000_000.0;
            
            // Time inverse transform
            long startInverse = System.nanoTime();
            modwt.inverseMODWT(coeffs);
            long endInverse = System.nanoTime();
            double inverseTime = (endInverse - startInverse) / 1_000_000.0;
            
            double totalTime = forwardTime + inverseTime;
            
            System.out.printf("%5d | %16.2f | %16.2f | %15.2f%n",
                level, forwardTime, inverseTime, totalTime);
        }
        
        System.out.println("\nObservation: Time complexity increases linearly with decomposition levels.\n");
    }

    /**
     * Test 6: Check for Level Constraints in Current Implementation
     */
    @Test
    public void analyzeCurrentImplementationLimits() {
        System.out.println("=== CURRENT IMPLEMENTATION LIMITS ===\n");
        
        // Check the hardcoded limit in upsample method
        System.out.println("1. Hardcoded limit in upsample() method: level <= 30");
        System.out.println("   - This prevents integer overflow in shift operations");
        System.out.println("   - At level 30: 2^29 = 536,870,912 zeros between coefficients");
        System.out.println("   - Filter length would be approximately 536M elements!\n");
        
        System.out.println("2. Array size limits:");
        System.out.println("   - Java arrays limited to Integer.MAX_VALUE = 2,147,483,647 elements");
        System.out.println("   - At 8 bytes per double, max array = ~17 GB\n");
        
        System.out.println("3. Practical memory limits:");
        System.out.println("   - Default JVM heap is often 1-4 GB");
        System.out.println("   - Large arrays can cause OutOfMemoryError\n");
        
        // Test the actual limit
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        double[] testSignal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        System.out.println("4. Testing actual limits with 8-sample signal:");
        try {
            // This should work (level 3 is max for 8 samples)
            modwt.forwardMODWT(testSignal, 3);
            System.out.println("   - Level 3: SUCCESS");
            
            // This should fail in forward() due to level check
            try {
                modwt.forward(testSignal, 4);
                System.out.println("   - Level 4: SUCCESS (unexpected!)");
            } catch (JWaveException e) {
                System.out.println("   - Level 4: FAILED as expected - " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("   - Unexpected error: " + e.getMessage());
        }
    }

    // Helper method to find practical maximum level
    private int findPracticalMaxLevel(int signalLength, int baseFilterLength) {
        int theoreticalMax = (int)(Math.log(signalLength) / Math.log(2));
        
        for (int level = 1; level <= theoreticalMax; level++) {
            // Calculate upsampled filter length
            long filterLength = (long)(baseFilterLength - 1) * (1L << (level - 1)) + 1;
            
            // Practical constraints:
            // 1. Filter length should not exceed signal length / 4 (for meaningful convolution)
            // 2. Filter length should not exceed reasonable memory limits (say 1M elements)
            if (filterLength > signalLength / 4 || filterLength > 1_000_000) {
                return level - 1;
            }
        }
        
        return Math.min(theoreticalMax, 12); // Cap at 12 for practical reasons
    }

    // Helper method to format bytes
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}