/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import jwave.transforms.wavelets.Wavelet;
import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.daubechies.*;
import jwave.transforms.wavelets.symlets.*;
import jwave.transforms.wavelets.coiflet.*;
import jwave.transforms.wavelets.continuous.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Performance comparison test for incremental updates across streaming transforms.
 * 
 * This test measures the actual performance difference between FULL and INCREMENTAL
 * update strategies for the scenario of:
 * - Initial signal of 512 samples
 * - Single sample updates every second
 * 
 * @author Prophetizo
 * @date 2025-07-13
 */
public class StreamingIncrementalPerformanceTest {
    
    private static final Logger LOGGER = Logger.getLogger(StreamingIncrementalPerformanceTest.class.getName());
    
    private static final int INITIAL_SIGNAL_SIZE = 512;
    private static final int SINGLE_SAMPLE_UPDATES = 100;
    private static final int WARM_UP_ITERATIONS = 50;
    
    // Performance thresholds for assertions
    private static final double MIN_MODWT_SPEEDUP = 1.3;  // Expect at least 30% improvement
    private static final double MAX_OVERHEAD_RATIO = 1.1;  // Allow up to 10% overhead for "no improvement" cases
    
    // Test wavelets for discrete transforms
    private static final Map<String, Wavelet> TEST_WAVELETS = new LinkedHashMap<>();
    static {
        // Haar family
        TEST_WAVELETS.put("Haar1", new Haar1());
        
        // Daubechies family
        TEST_WAVELETS.put("Daubechies2", new Daubechies2());
        TEST_WAVELETS.put("Daubechies4", new Daubechies4());
        TEST_WAVELETS.put("Daubechies8", new Daubechies8());
        
        // Symlets family
        TEST_WAVELETS.put("Symlet2", new Symlet2());
        TEST_WAVELETS.put("Symlet4", new Symlet4());
        TEST_WAVELETS.put("Symlet8", new Symlet8());
        
        // Coiflets family
        TEST_WAVELETS.put("Coiflet1", new Coiflet1());
        TEST_WAVELETS.put("Coiflet2", new Coiflet2());
        TEST_WAVELETS.put("Coiflet3", new Coiflet3());
    }
    
    // Test wavelets for continuous transforms
    private static final Map<String, ContinuousWavelet> TEST_CONTINUOUS_WAVELETS = new LinkedHashMap<>();
    static {
        TEST_CONTINUOUS_WAVELETS.put("Morlet", new MorletWavelet());
        TEST_CONTINUOUS_WAVELETS.put("MexicanHat", new MexicanHatWavelet());
        TEST_CONTINUOUS_WAVELETS.put("DOG", new DOGWavelet(1, 2));
    }
    
    @Test
    public void testIncrementalPerformanceComparison() {
        // Skip if performance tests are not enabled
        assumeTrue("Performance tests not enabled", 
                  Boolean.getBoolean("enablePerformanceTests"));
        
        LOGGER.log(Level.INFO, "Running incremental performance analysis with {0} samples and {1} updates", 
                   new Object[]{INITIAL_SIGNAL_SIZE, SINGLE_SAMPLE_UPDATES});
        
        // Test with a representative wavelet (Haar1)
        // Full comprehensive test is in testAllWaveletsPerformance
        Wavelet testWavelet = new Haar1();
        
        // Test StreamingMODWT (has true incremental updates)
        double modwtSpeedup = testMODWTPerformance(testWavelet);
        assertTrue("StreamingMODWT should show significant speedup with incremental updates", 
                   modwtSpeedup >= MIN_MODWT_SPEEDUP);
        
        // Test StreamingFWT (falls back to full recomputation)
        double fwtSpeedup = testFWTPerformance(testWavelet);
        assertTrue("StreamingFWT incremental should not have significant overhead", 
                   fwtSpeedup >= 1.0 / MAX_OVERHEAD_RATIO && fwtSpeedup <= MAX_OVERHEAD_RATIO);
        
        // Test StreamingWPT (falls back to full recomputation)
        double wptSpeedup = testWPTPerformance(testWavelet);
        assertTrue("StreamingWPT incremental should not have significant overhead", 
                   wptSpeedup >= 1.0 / MAX_OVERHEAD_RATIO && wptSpeedup <= MAX_OVERHEAD_RATIO);
        
        // Test StreamingCWT (with incremental updates)
        ContinuousWavelet cwTestWavelet = new MorletWavelet();
        double cwtSpeedup = testCWTPerformance(cwTestWavelet);
        // CWT incremental updates have minimal benefit due to edge effects
        // Allow for some overhead but ensure it's not excessive
        assertTrue("StreamingCWT incremental should not have significant overhead", 
                   cwtSpeedup >= 0.9 && cwtSpeedup <= MAX_OVERHEAD_RATIO);
    }
    
    @Test
    public void testAllWaveletsPerformance() {
        // Skip if performance tests are not enabled
        assumeTrue("Performance tests not enabled", 
                  Boolean.getBoolean("enablePerformanceTests"));
        
        LOGGER.log(Level.INFO, "Running comprehensive wavelet performance analysis");
        
        // Test each discrete wavelet
        for (Map.Entry<String, Wavelet> entry : TEST_WAVELETS.entrySet()) {
            String waveletName = entry.getKey();
            Wavelet wavelet = entry.getValue();
            
            LOGGER.log(Level.INFO, "Testing wavelet: {0}", waveletName);
            
            // Test MODWT performance
            double modwtSpeedup = testMODWTPerformance(wavelet);
            LOGGER.log(Level.INFO, "{0} - MODWT Speedup: {1}x", 
                      new Object[]{waveletName, String.format("%.2f", modwtSpeedup)});
            
            // Test FWT performance
            double fwtSpeedup = testFWTPerformance(wavelet);
            LOGGER.log(Level.INFO, "{0} - FWT Speedup: {1}x", 
                      new Object[]{waveletName, String.format("%.2f", fwtSpeedup)});
            
            // Test WPT performance
            double wptSpeedup = testWPTPerformance(wavelet);
            LOGGER.log(Level.INFO, "{0} - WPT Speedup: {1}x", 
                      new Object[]{waveletName, String.format("%.2f", wptSpeedup)});
        }
        
        // Test continuous wavelets
        for (Map.Entry<String, ContinuousWavelet> entry : TEST_CONTINUOUS_WAVELETS.entrySet()) {
            String waveletName = entry.getKey();
            ContinuousWavelet wavelet = entry.getValue();
            
            LOGGER.log(Level.INFO, "Testing continuous wavelet: {0}", waveletName);
            
            double cwtSpeedup = testCWTPerformance(wavelet);
            LOGGER.log(Level.INFO, "{0} - CWT Speedup: {1}x", 
                      new Object[]{waveletName, String.format("%.2f", cwtSpeedup)});
        }
    }
    
    private double testMODWTPerformance(Wavelet wavelet) {
        LOGGER.log(Level.FINE, "Testing StreamingMODWT incremental performance with {0}", wavelet.getName());
        
        // Test with FULL strategy
        StreamingTransformConfig fullConfig = StreamingTransformConfig.builder()
            .bufferSize(INITIAL_SIGNAL_SIZE)
            .maxLevel(4)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingMODWT fullMODWT = new StreamingMODWT(wavelet, fullConfig);
        
        // Test with INCREMENTAL strategy
        StreamingTransformConfig incConfig = StreamingTransformConfig.builder()
            .bufferSize(INITIAL_SIGNAL_SIZE)
            .maxLevel(4)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingMODWT incMODWT = new StreamingMODWT(wavelet, incConfig);
        
        // Generate initial signal
        double[] initialSignal = generateSignal(INITIAL_SIGNAL_SIZE);
        
        // Warm up
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            fullMODWT.update(new double[]{Math.random()});
            incMODWT.update(new double[]{Math.random()});
        }
        
        // Initial load
        long fullInitStart = System.nanoTime();
        fullMODWT.update(initialSignal);
        long fullInitEnd = System.nanoTime();
        double fullInitTime = (fullInitEnd - fullInitStart) / 1e6;
        
        long incInitStart = System.nanoTime();
        incMODWT.update(initialSignal);
        long incInitEnd = System.nanoTime();
        double incInitTime = (incInitEnd - incInitStart) / 1e6;
        
        // Single sample updates
        long fullUpdateStart = System.nanoTime();
        for (int i = 0; i < SINGLE_SAMPLE_UPDATES; i++) {
            fullMODWT.update(new double[]{Math.random()});
        }
        long fullUpdateEnd = System.nanoTime();
        double fullAvgUpdate = (fullUpdateEnd - fullUpdateStart) / (1e6 * SINGLE_SAMPLE_UPDATES);
        
        long incUpdateStart = System.nanoTime();
        for (int i = 0; i < SINGLE_SAMPLE_UPDATES; i++) {
            incMODWT.update(new double[]{Math.random()});
        }
        long incUpdateEnd = System.nanoTime();
        double incAvgUpdate = (incUpdateEnd - incUpdateStart) / (1e6 * SINGLE_SAMPLE_UPDATES);
        
        // Calculate speedup
        double speedup = fullAvgUpdate / incAvgUpdate;
        
        LOGGER.log(Level.FINE, "MODWT Initial load - FULL: {0}ms, INCREMENTAL: {1}ms", 
                   new Object[]{String.format("%.3f", fullInitTime), String.format("%.3f", incInitTime)});
        LOGGER.log(Level.FINE, "MODWT Updates - FULL: {0}ms/update, INCREMENTAL: {1}ms/update, Speedup: {2}x", 
                   new Object[]{String.format("%.3f", fullAvgUpdate), String.format("%.3f", incAvgUpdate), 
                               String.format("%.2f", speedup)});
        
        return speedup;
    }
    
    private double testFWTPerformance(Wavelet wavelet) {
        LOGGER.log(Level.FINE, "Testing StreamingFWT incremental performance with {0}", wavelet.getName());
        
        // Test with FULL strategy
        StreamingTransformConfig fullConfig = StreamingTransformConfig.builder()
            .bufferSize(INITIAL_SIGNAL_SIZE)
            .maxLevel(4)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingFWT fullFWT = new StreamingFWT(wavelet, fullConfig);
        
        // Test with INCREMENTAL strategy
        StreamingTransformConfig incConfig = StreamingTransformConfig.builder()
            .bufferSize(INITIAL_SIGNAL_SIZE)
            .maxLevel(4)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingFWT incFWT = new StreamingFWT(wavelet, incConfig);
        
        // Generate initial signal
        double[] initialSignal = generateSignal(INITIAL_SIGNAL_SIZE);
        
        // Warm up
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            fullFWT.update(new double[]{Math.random()});
            incFWT.update(new double[]{Math.random()});
        }
        
        // Initial load
        fullFWT.update(initialSignal);
        incFWT.update(initialSignal);
        
        // Single sample updates
        long fullUpdateStart = System.nanoTime();
        for (int i = 0; i < SINGLE_SAMPLE_UPDATES; i++) {
            fullFWT.update(new double[]{Math.random()});
        }
        long fullUpdateEnd = System.nanoTime();
        double fullAvgUpdate = (fullUpdateEnd - fullUpdateStart) / (1e6 * SINGLE_SAMPLE_UPDATES);
        
        long incUpdateStart = System.nanoTime();
        for (int i = 0; i < SINGLE_SAMPLE_UPDATES; i++) {
            incFWT.update(new double[]{Math.random()});
        }
        long incUpdateEnd = System.nanoTime();
        double incAvgUpdate = (incUpdateEnd - incUpdateStart) / (1e6 * SINGLE_SAMPLE_UPDATES);
        
        double speedup = fullAvgUpdate / incAvgUpdate;
        
        LOGGER.log(Level.FINE, "FWT Updates - FULL: {0}ms/update, INCREMENTAL: {1}ms/update, Speedup: {2}x", 
                   new Object[]{String.format("%.3f", fullAvgUpdate), String.format("%.3f", incAvgUpdate), 
                               String.format("%.2f", speedup)});
        
        return speedup;
    }
    
    private double testWPTPerformance(Wavelet wavelet) {
        LOGGER.log(Level.FINE, "Testing StreamingWPT incremental performance with {0}", wavelet.getName());
        
        // Test with FULL strategy
        StreamingTransformConfig fullConfig = StreamingTransformConfig.builder()
            .bufferSize(INITIAL_SIGNAL_SIZE)
            .maxLevel(4)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingWPT fullWPT = new StreamingWPT(wavelet, fullConfig);
        
        // Test with INCREMENTAL strategy
        StreamingTransformConfig incConfig = StreamingTransformConfig.builder()
            .bufferSize(INITIAL_SIGNAL_SIZE)
            .maxLevel(4)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingWPT incWPT = new StreamingWPT(wavelet, incConfig);
        
        // Generate initial signal
        double[] initialSignal = generateSignal(INITIAL_SIGNAL_SIZE);
        
        // Warm up
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            fullWPT.update(new double[]{Math.random()});
            incWPT.update(new double[]{Math.random()});
        }
        
        // Initial load
        fullWPT.update(initialSignal);
        incWPT.update(initialSignal);
        
        // Single sample updates
        long fullUpdateStart = System.nanoTime();
        for (int i = 0; i < SINGLE_SAMPLE_UPDATES; i++) {
            fullWPT.update(new double[]{Math.random()});
        }
        long fullUpdateEnd = System.nanoTime();
        double fullAvgUpdate = (fullUpdateEnd - fullUpdateStart) / (1e6 * SINGLE_SAMPLE_UPDATES);
        
        long incUpdateStart = System.nanoTime();
        for (int i = 0; i < SINGLE_SAMPLE_UPDATES; i++) {
            incWPT.update(new double[]{Math.random()});
        }
        long incUpdateEnd = System.nanoTime();
        double incAvgUpdate = (incUpdateEnd - incUpdateStart) / (1e6 * SINGLE_SAMPLE_UPDATES);
        
        double speedup = fullAvgUpdate / incAvgUpdate;
        
        LOGGER.log(Level.FINE, "WPT Updates - FULL: {0}ms/update, INCREMENTAL: {1}ms/update, Speedup: {2}x", 
                   new Object[]{String.format("%.3f", fullAvgUpdate), String.format("%.3f", incAvgUpdate), 
                               String.format("%.2f", speedup)});
        
        return speedup;
    }
    
    private double testCWTPerformance(ContinuousWavelet wavelet) {
        LOGGER.log(Level.FINE, "Testing StreamingCWT incremental performance with {0}", wavelet.getName());
        
        // Test with FULL strategy
        StreamingTransformConfig fullConfig = StreamingTransformConfig.builder()
            .bufferSize(INITIAL_SIGNAL_SIZE)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingCWT fullCWT = new StreamingCWT(wavelet, fullConfig, 1.0, 32.0, 16, true);
        
        // Test with INCREMENTAL strategy
        StreamingTransformConfig incConfig = StreamingTransformConfig.builder()
            .bufferSize(INITIAL_SIGNAL_SIZE)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.INCREMENTAL)
            .build();
        
        StreamingCWT incCWT = new StreamingCWT(wavelet, incConfig, 1.0, 32.0, 16, true);
        
        // Generate initial signal
        double[] initialSignal = generateSignal(INITIAL_SIGNAL_SIZE);
        
        // Warm up
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            fullCWT.update(new double[]{Math.random()});
            incCWT.update(new double[]{Math.random()});
        }
        
        // Initial load
        fullCWT.update(initialSignal);
        incCWT.update(initialSignal);
        
        // Single sample updates
        long fullUpdateStart = System.nanoTime();
        for (int i = 0; i < SINGLE_SAMPLE_UPDATES; i++) {
            fullCWT.update(new double[]{Math.random()});
        }
        long fullUpdateEnd = System.nanoTime();
        double fullAvgUpdate = (fullUpdateEnd - fullUpdateStart) / (1e6 * SINGLE_SAMPLE_UPDATES);
        
        long incUpdateStart = System.nanoTime();
        for (int i = 0; i < SINGLE_SAMPLE_UPDATES; i++) {
            incCWT.update(new double[]{Math.random()});
        }
        long incUpdateEnd = System.nanoTime();
        double incAvgUpdate = (incUpdateEnd - incUpdateStart) / (1e6 * SINGLE_SAMPLE_UPDATES);
        
        // Calculate speedup
        double speedup = fullAvgUpdate / incAvgUpdate;
        
        LOGGER.log(Level.FINE, "CWT Updates - FULL: {0}ms/update, INCREMENTAL: {1}ms/update, Speedup: {2}x", 
                   new Object[]{String.format("%.3f", fullAvgUpdate), String.format("%.3f", incAvgUpdate), 
                               String.format("%.2f", speedup)});
        
        return speedup;
    }
    
    @Test
    public void testWaveletFamilyCharacteristics() {
        assumeTrue("Performance tests not enabled", 
                  Boolean.getBoolean("enablePerformanceTests"));
        
        LOGGER.log(Level.INFO, "Analyzing performance characteristics of different wavelet families");
        
        // Test representative wavelets from each family
        Map<String, Wavelet> familyRepresentatives = new LinkedHashMap<>();
        familyRepresentatives.put("Haar", new Haar1());
        familyRepresentatives.put("Daubechies", new Daubechies4());
        familyRepresentatives.put("Symlet", new Symlet4());
        familyRepresentatives.put("Coiflet", new Coiflet2());
        
        LOGGER.log(Level.INFO, "");
        LOGGER.log(Level.INFO, "=== MODWT Incremental Performance by Wavelet Family ===");
        
        for (Map.Entry<String, Wavelet> entry : familyRepresentatives.entrySet()) {
            String family = entry.getKey();
            Wavelet wavelet = entry.getValue();
            
            double speedup = testMODWTPerformance(wavelet);
            String performance = speedup >= 1.5 ? "EXCELLENT" : 
                               speedup >= 1.3 ? "GOOD" : 
                               speedup >= 1.1 ? "MODERATE" : "MINIMAL";
            
            LOGGER.log(Level.INFO, "{0} family ({1}): {2}x speedup - {3} incremental performance", 
                      new Object[]{family, wavelet.getName(), 
                                  String.format("%.2f", speedup), performance});
        }
        
        LOGGER.log(Level.INFO, "");
        LOGGER.log(Level.INFO, "Note: Longer wavelets may show different performance characteristics");
        LOGGER.log(Level.INFO, "      due to increased computation per coefficient update.");
    }
    
    @Test
    public void testLazyStrategyPerformance() {
        assumeTrue("Performance tests not enabled", 
                  Boolean.getBoolean("enablePerformanceTests"));
        
        LOGGER.log(Level.INFO, "Testing LAZY strategy performance with infrequent coefficient access");
        
        // Test scenario: 10 updates followed by 1 coefficient access
        int updatesPerAccess = 10;
        int totalCycles = 10;
        
        // Test with StreamingMODWT
        StreamingTransformConfig fullConfig = StreamingTransformConfig.builder()
            .bufferSize(INITIAL_SIGNAL_SIZE)
            .maxLevel(4)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .build();
        
        StreamingTransformConfig lazyConfig = StreamingTransformConfig.builder()
            .bufferSize(INITIAL_SIGNAL_SIZE)
            .maxLevel(4)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.LAZY)
            .build();
        
        StreamingMODWT fullMODWT = new StreamingMODWT(new Haar1(), fullConfig);
        StreamingMODWT lazyMODWT = new StreamingMODWT(new Haar1(), lazyConfig);
        
        // Initial load
        double[] initialSignal = generateSignal(INITIAL_SIGNAL_SIZE);
        fullMODWT.update(initialSignal);
        lazyMODWT.update(initialSignal);
        
        // Test FULL strategy
        long fullStart = System.nanoTime();
        for (int cycle = 0; cycle < totalCycles; cycle++) {
            // Multiple updates
            for (int i = 0; i < updatesPerAccess; i++) {
                fullMODWT.update(new double[]{Math.random()});
            }
            // Access coefficients
            fullMODWT.getCurrentCoefficients();
        }
        long fullEnd = System.nanoTime();
        double fullTotalTime = (fullEnd - fullStart) / 1e6;
        
        // Test LAZY strategy
        long lazyStart = System.nanoTime();
        for (int cycle = 0; cycle < totalCycles; cycle++) {
            // Multiple updates (these are very fast with LAZY)
            for (int i = 0; i < updatesPerAccess; i++) {
                lazyMODWT.update(new double[]{Math.random()});
            }
            // Access coefficients (triggers computation)
            lazyMODWT.getCurrentCoefficients();
        }
        long lazyEnd = System.nanoTime();
        double lazyTotalTime = (lazyEnd - lazyStart) / 1e6;
        
        double lazySavings = ((fullTotalTime - lazyTotalTime) / fullTotalTime) * 100;
        
        LOGGER.log(Level.FINE, "LAZY Strategy - Updates per access: {0}, Total cycles: {1}", 
                   new Object[]{updatesPerAccess, totalCycles});
        LOGGER.log(Level.FINE, "LAZY Strategy Results - FULL: {0}ms, LAZY: {1}ms, Savings: {2}%", 
                   new Object[]{String.format("%.3f", fullTotalTime), 
                               String.format("%.3f", lazyTotalTime),
                               String.format("%.1f", lazySavings)});
        
        // Assert that LAZY strategy provides measurable benefit
        assertTrue("LAZY strategy should provide time savings for infrequent access patterns", 
                   lazySavings > 10.0);  // Expect at least 10% savings
    }
    
    private double[] generateSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 64) + 
                       0.5 * Math.cos(4 * Math.PI * i / 64) + 
                       0.1 * Math.random();
        }
        return signal;
    }
}