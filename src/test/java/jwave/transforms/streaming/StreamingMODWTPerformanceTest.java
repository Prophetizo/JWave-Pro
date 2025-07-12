/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import jwave.transforms.wavelets.haar.Haar1;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Performance tests for StreamingMODWT update strategies.
 * 
 * @author Prophetizo
 * @date 2025-07-12
 */
public class StreamingMODWTPerformanceTest {
    
    @Test
    public void compareUpdateStrategyPerformance() {
        int bufferSize = 4096;
        int maxLevel = 8;
        int numUpdates = 1000;
        int updateSize = 100;
        
        Haar1 wavelet = new Haar1();
        
        // Test FULL strategy
        StreamingTransformConfig fullConfig = StreamingTransformConfig.builder()
            .bufferSize(bufferSize)
            .maxLevel(maxLevel)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingMODWT fullTransform = new StreamingMODWT(wavelet, fullConfig);
        
        long fullStart = System.nanoTime();
        for (int i = 0; i < numUpdates; i++) {
            double[] data = generateData(updateSize, i);
            fullTransform.update(data);
        }
        long fullTime = System.nanoTime() - fullStart;
        
        // Test LAZY strategy with infrequent access
        StreamingTransformConfig lazyConfig = StreamingTransformConfig.builder()
            .bufferSize(bufferSize)
            .maxLevel(maxLevel)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.LAZY)
            .build();
        
        StreamingMODWT lazyTransform = new StreamingMODWT(wavelet, lazyConfig);
        
        long lazyStart = System.nanoTime();
        for (int i = 0; i < numUpdates; i++) {
            double[] data = generateData(updateSize, i);
            lazyTransform.update(data);
            
            // Only access coefficients every 100 updates
            if (i % 100 == 0) {
                lazyTransform.getCurrentCoefficients();
            }
        }
        long lazyTime = System.nanoTime() - lazyStart;
        
        // Print results
        System.out.println("Performance comparison for " + numUpdates + " updates:");
        System.out.println("FULL strategy: " + (fullTime / 1_000_000) + " ms");
        System.out.println("LAZY strategy: " + (lazyTime / 1_000_000) + " ms");
        System.out.println("Speed improvement: " + String.format("%.2fx", (double)fullTime / lazyTime));
        
        // LAZY should be significantly faster when coefficients are accessed infrequently
        assertTrue("LAZY should be faster than FULL for infrequent access", 
                  lazyTime < fullTime);
    }
    
    @Test
    public void measureLazyComputationOverhead() {
        int bufferSize = 2048;
        int maxLevel = 6;
        
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(bufferSize)
            .maxLevel(maxLevel)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.LAZY)
            .build();
        
        StreamingMODWT transform = new StreamingMODWT(new Haar1(), config);
        
        // Fill buffer
        double[] data = generateData(bufferSize, 0);
        transform.update(data);
        
        // Add more data (marks coefficients as dirty)
        transform.update(generateData(100, 1));
        
        // Measure first access (triggers computation)
        long firstAccessStart = System.nanoTime();
        double[][] coeffs1 = transform.getCurrentCoefficients();
        long firstAccessTime = System.nanoTime() - firstAccessStart;
        
        // Measure second access (uses cache)
        long secondAccessStart = System.nanoTime();
        double[][] coeffs2 = transform.getCurrentCoefficients();
        long secondAccessTime = System.nanoTime() - secondAccessStart;
        
        System.out.println("LAZY computation overhead:");
        System.out.println("First access (with computation): " + (firstAccessTime / 1_000) + " μs");
        System.out.println("Second access (cached): " + (secondAccessTime / 1_000) + " μs");
        System.out.println("Overhead ratio: " + String.format("%.1fx", 
                          (double)firstAccessTime / secondAccessTime));
        
        // First access should be significantly slower due to computation
        assertTrue("First access should be slower than cached access", 
                  firstAccessTime > secondAccessTime * 5);
    }
    
    private double[] generateData(int size, int seed) {
        double[] data = new double[size];
        for (int i = 0; i < size; i++) {
            data[i] = Math.sin(2 * Math.PI * (i + seed) / 50) + 
                     0.5 * Math.cos(2 * Math.PI * (i + seed) / 13);
        }
        return data;
    }
}