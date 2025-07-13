/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.transforms.wavelets.haar.Haar1;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

/**
 * Performance test suite for StreamingWPT implementation.
 * 
 * Tests measure performance characteristics of different update strategies
 * and analyze the impact of packet tree decomposition on processing time.
 * 
 * These tests are marked with @Ignore to prevent them from running in CI/CD
 * as they are designed for performance analysis rather than correctness.
 * 
 * To run these tests manually:
 * mvn test -Dtest=StreamingWPTPerformanceTest -DenablePerformanceTests=true
 * 
 * @author Prophetizo
 * @date 2025-07-13
 */
public class StreamingWPTPerformanceTest {
    
    private static final int WARM_UP_ITERATIONS = 100;
    private static final int TEST_ITERATIONS = 1000;
    
    /**
     * Test update strategy performance comparison.
     */
    @Test
    @Ignore("Performance test - run manually")
    public void testUpdateStrategyPerformance() {
        // Skip if performance tests are not enabled
        assumeTrue("Performance tests not enabled", 
                  Boolean.getBoolean("enablePerformanceTests"));
        
        int bufferSize = 512;
        int maxLevel = 6;
        
        // Test configurations
        StreamingTransformConfig.UpdateStrategy[] strategies = {
            StreamingTransformConfig.UpdateStrategy.FULL,
            StreamingTransformConfig.UpdateStrategy.INCREMENTAL,
            StreamingTransformConfig.UpdateStrategy.LAZY
        };
        
        System.out.println("\nStreamingWPT Update Strategy Performance Test");
        System.out.println("Buffer size: " + bufferSize);
        System.out.println("Max level: " + maxLevel);
        System.out.println("Packets at max level: " + (1 << maxLevel));
        System.out.println("Total coefficients: " + bufferSize);
        System.out.println();
        
        for (StreamingTransformConfig.UpdateStrategy strategy : strategies) {
            double avgTime = measureStrategyPerformance(strategy, bufferSize, maxLevel);
            System.out.printf("%s: %.3f ms per update\n", strategy, avgTime);
        }
    }
    
    /**
     * Test performance impact of decomposition level.
     */
    @Test
    @Ignore("Performance test - run manually")
    public void testDecompositionLevelImpact() {
        assumeTrue("Performance tests not enabled", 
                  Boolean.getBoolean("enablePerformanceTests"));
        
        int bufferSize = 1024;
        
        System.out.println("\nStreamingWPT Decomposition Level Impact Test");
        System.out.println("Buffer size: " + bufferSize);
        System.out.println();
        
        // Test different decomposition levels
        for (int level = 1; level <= 10; level++) {
            StreamingTransformConfig config = StreamingTransformConfig.builder()
                .bufferSize(bufferSize)
                .maxLevel(level)
                .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
                .build();
            
            StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
            
            // Warm up
            double[] signal = generateTestSignal(bufferSize);
            for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
                wpt.update(signal);
            }
            
            // Measure
            long startTime = System.nanoTime();
            for (int i = 0; i < TEST_ITERATIONS; i++) {
                wpt.update(signal);
            }
            long endTime = System.nanoTime();
            
            double avgTime = (endTime - startTime) / (1e6 * TEST_ITERATIONS);
            int totalPackets = (1 << (level + 1)) - 1; // Total nodes in binary tree
            
            System.out.printf("Level %2d: %4d packets, %.3f ms per update\n", 
                            level, totalPackets, avgTime);
        }
    }
    
    /**
     * Test packet access performance.
     */
    @Test
    @Ignore("Performance test - run manually")
    public void testPacketAccessPerformance() {
        assumeTrue("Performance tests not enabled", 
                  Boolean.getBoolean("enablePerformanceTests"));
        
        int bufferSize = 512;
        int maxLevel = 8;
        
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(bufferSize)
            .maxLevel(maxLevel)
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Daubechies4(), config);
        wpt.update(generateTestSignal(bufferSize));
        
        System.out.println("\nStreamingWPT Packet Access Performance Test");
        System.out.println("Buffer size: " + bufferSize);
        System.out.println("Max level: " + maxLevel);
        System.out.println();
        
        // Test packet access at different levels
        for (int level = 0; level <= maxLevel; level++) {
            int packetCount = wpt.getPacketCount(level);
            
            // Warm up
            for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
                for (int p = 0; p < packetCount; p++) {
                    wpt.getPacket(level, p);
                }
            }
            
            // Measure
            long startTime = System.nanoTime();
            for (int i = 0; i < TEST_ITERATIONS; i++) {
                for (int p = 0; p < packetCount; p++) {
                    wpt.getPacket(level, p);
                }
            }
            long endTime = System.nanoTime();
            
            double totalTime = (endTime - startTime) / 1e6;
            double avgTimePerPacket = totalTime / (TEST_ITERATIONS * packetCount);
            
            System.out.printf("Level %d: %4d packets, %.3f μs per packet access\n", 
                            level, packetCount, avgTimePerPacket * 1000);
        }
    }
    
    /**
     * Test real-time streaming scenario.
     */
    @Test
    @Ignore("Performance test - run manually")
    public void testRealtimeStreamingScenario() {
        assumeTrue("Performance tests not enabled", 
                  Boolean.getBoolean("enablePerformanceTests"));
        
        int bufferSize = 256;
        int maxLevel = 4;
        
        System.out.println("\nStreamingWPT Real-time Streaming Scenario");
        System.out.println("Buffer size: " + bufferSize);
        System.out.println("Max level: " + maxLevel);
        System.out.println("Scenario: Initial load of " + bufferSize + " samples, then single-sample updates");
        System.out.println();
        
        StreamingTransformConfig.UpdateStrategy[] strategies = {
            StreamingTransformConfig.UpdateStrategy.FULL,
            StreamingTransformConfig.UpdateStrategy.INCREMENTAL
        };
        
        for (StreamingTransformConfig.UpdateStrategy strategy : strategies) {
            StreamingTransformConfig config = StreamingTransformConfig.builder()
                .bufferSize(bufferSize)
                .maxLevel(maxLevel)
                .updateStrategy(strategy)
                .build();
            
            StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
            
            // Initial load
            double[] initialData = generateTestSignal(bufferSize);
            long loadStart = System.nanoTime();
            wpt.update(initialData);
            long loadEnd = System.nanoTime();
            double loadTime = (loadEnd - loadStart) / 1e6;
            
            // Single sample updates
            long updateStart = System.nanoTime();
            for (int i = 0; i < TEST_ITERATIONS; i++) {
                wpt.update(new double[] { Math.random() });
            }
            long updateEnd = System.nanoTime();
            double avgUpdateTime = (updateEnd - updateStart) / (1e6 * TEST_ITERATIONS);
            
            System.out.printf("%s strategy:\n", strategy);
            System.out.printf("  Initial load: %.3f ms\n", loadTime);
            System.out.printf("  Single sample update: %.3f ms\n", avgUpdateTime);
            System.out.printf("  Throughput: %.0f samples/sec\n", 1000.0 / avgUpdateTime);
            System.out.println();
        }
    }
    
    /**
     * Compare WPT vs FWT performance.
     */
    @Test
    @Ignore("Performance test - run manually")
    public void testWPTvsFWTPerformance() {
        assumeTrue("Performance tests not enabled", 
                  Boolean.getBoolean("enablePerformanceTests"));
        
        int bufferSize = 512;
        int maxLevel = 6;
        
        System.out.println("\nWPT vs FWT Performance Comparison");
        System.out.println("Buffer size: " + bufferSize);
        System.out.println("Max level: " + maxLevel);
        System.out.println();
        
        // Create configurations
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(bufferSize)
            .maxLevel(maxLevel)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        StreamingFWT fwt = new StreamingFWT(new Haar1(), config);
        
        // Test data
        double[] signal = generateTestSignal(bufferSize);
        
        // Warm up both
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            wpt.update(signal);
            fwt.update(signal);
        }
        
        // Measure WPT
        long wptStart = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            wpt.update(signal);
        }
        long wptEnd = System.nanoTime();
        double wptTime = (wptEnd - wptStart) / (1e6 * TEST_ITERATIONS);
        
        // Measure FWT
        long fwtStart = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            fwt.update(signal);
        }
        long fwtEnd = System.nanoTime();
        double fwtTime = (fwtEnd - fwtStart) / (1e6 * TEST_ITERATIONS);
        
        System.out.printf("WPT: %.3f ms per update\n", wptTime);
        System.out.printf("FWT: %.3f ms per update\n", fwtTime);
        System.out.printf("WPT/FWT ratio: %.2fx\n", wptTime / fwtTime);
        System.out.println();
        System.out.println("Note: WPT decomposes all nodes creating " + ((1 << (maxLevel + 1)) - 1) + 
                          " packets vs FWT's " + maxLevel + " decompositions");
    }
    
    /**
     * Measure performance for a specific update strategy.
     */
    private double measureStrategyPerformance(StreamingTransformConfig.UpdateStrategy strategy,
                                            int bufferSize, int maxLevel) {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(bufferSize)
            .maxLevel(maxLevel)
            .updateStrategy(strategy)
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        
        // Generate test data
        double[] signal = generateTestSignal(bufferSize);
        
        // Warm up
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            wpt.update(signal);
        }
        
        // Measure
        long startTime = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            wpt.update(signal);
            if (strategy == StreamingTransformConfig.UpdateStrategy.LAZY) {
                // Force coefficient computation for LAZY strategy
                wpt.getCurrentCoefficients();
            }
        }
        long endTime = System.nanoTime();
        
        return (endTime - startTime) / (1e6 * TEST_ITERATIONS);
    }
    
    /**
     * Generate a test signal with mixed frequency components.
     */
    private double[] generateTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32) +
                       0.5 * Math.cos(8 * Math.PI * i / 32) +
                       0.25 * Math.sin(16 * Math.PI * i / 32);
        }
        return signal;
    }
}