package jwave.transforms;

import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.utils.ArrayBufferPool;
import org.junit.Test;
import org.junit.Before;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * Demonstrates the more efficient API that avoids the final array copy.
 * 
 * @author Stephen Romano
 */
public class PooledMODWTEfficientAPITest {
    
    private static final int SIGNAL_SIZE = 1024;
    private double[] testSignal;
    private PooledMODWTTransform modwt;
    
    @Before
    public void setUp() {
        Random rand = new Random(42);
        testSignal = new double[SIGNAL_SIZE];
        for (int i = 0; i < SIGNAL_SIZE; i++) {
            testSignal[i] = rand.nextGaussian();
        }
        
        modwt = new PooledMODWTTransform(new Daubechies4());
    }
    
    @Test
    public void testEfficientAPIWithProvidedBuffer() {
        // Pre-allocate output buffer
        double[] outputBuffer = new double[SIGNAL_SIZE];
        
        // Get the filter
        double[] filter = modwt.getWavelet().getWaveletDeComposition();
        
        // Use the efficient API that writes directly into our buffer
        double[] result = modwt.performConvolutionInto(testSignal, filter, false, outputBuffer);
        
        // Verify that the same buffer was returned
        assertSame("Should return the same buffer", outputBuffer, result);
        
        // Verify correctness by comparing with standard API
        double[] expected = modwt.performConvolution(testSignal, filter, false);
        assertArrayEquals("Results should match", expected, result, 1e-10);
    }
    
    @Test
    public void testEfficientAPIPerformance() {
        // This test demonstrates the performance difference
        // In a real application, you would reuse these buffers across multiple operations
        
        double[] filter = modwt.getWavelet().getWaveletDeComposition();
        double[] outputBuffer = new double[SIGNAL_SIZE];
        
        int iterations = 10000;
        
        // Warm up
        for (int i = 0; i < 100; i++) {
            modwt.performConvolutionInto(testSignal, filter, false, outputBuffer);
        }
        
        // Test standard API (with final copy)
        long standardTime = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            modwt.performConvolution(testSignal, filter, false);
            standardTime += System.nanoTime() - start;
        }
        
        // Test efficient API (no final copy)
        long efficientTime = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            modwt.performConvolutionInto(testSignal, filter, false, outputBuffer);
            efficientTime += System.nanoTime() - start;
        }
        
        // The efficient API should be at least as fast, often faster
        System.out.printf("Standard API: %.3f ms total\n", standardTime / 1e6);
        System.out.printf("Efficient API: %.3f ms total\n", efficientTime / 1e6);
        System.out.printf("Improvement: %.1f%%\n", 
            100.0 * (1.0 - (double)efficientTime / standardTime));
        
        // Clean up
        ArrayBufferPool.remove();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testEfficientAPIWithSmallBuffer() {
        double[] filter = modwt.getWavelet().getWaveletDeComposition();
        double[] tooSmallBuffer = new double[SIGNAL_SIZE / 2];
        
        // Should throw exception for too small buffer
        modwt.performConvolutionInto(testSignal, filter, false, tooSmallBuffer);
    }
}