package jwave.transforms;

import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.transforms.wavelets.haar.Haar1;
import jwave.utils.ArrayBufferPool;
import org.junit.Test;
import org.junit.After;

import static org.junit.Assert.*;
import jwave.exceptions.JWaveException;

/**
 * Unit tests for ParallelWaveletPacketTransform.
 * 
 * @author Stephen Romano
 * @since 2.1.0
 */
public class ParallelWPTTest {
    
    private static final double DELTA = 1e-10;
    
    @After
    public void tearDown() {
        ArrayBufferPool.remove();
    }
    
    @Test
    public void testBasicForwardAndReverse() throws Exception {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        Haar1 wavelet = new Haar1();
        
        ParallelWaveletPacketTransform wpt = new ParallelWaveletPacketTransform(wavelet);
        
        // Forward transform
        double[] coeffs, reconstructed;
        try {
            coeffs = wpt.forward(signal, 2);
            assertNotNull(coeffs);
            assertEquals(signal.length, coeffs.length);
            
            // Reverse transform
            reconstructed = wpt.reverse(coeffs, 2);
        } catch (JWaveException e) {
            fail("Transform failed: " + e.getMessage());
            return;
        }
        assertNotNull(reconstructed);
        assertArrayEquals("Signal should be perfectly reconstructed", 
                         signal, reconstructed, DELTA);
        
        wpt.shutdown();
    }
    
    @Test
    public void testMultipleLevels() throws Exception {
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signal.length);
        }
        
        Daubechies4 wavelet = new Daubechies4();
        ParallelWaveletPacketTransform wpt = new ParallelWaveletPacketTransform(wavelet);
        
        // Test different decomposition levels
        for (int level = 1; level <= 4; level++) {
            try {
                double[] coeffs = wpt.forward(signal, level);
                double[] reconstructed = wpt.reverse(coeffs, level);
                
                assertArrayEquals("Level " + level + " reconstruction failed", 
                                signal, reconstructed, DELTA);
            } catch (JWaveException e) {
                fail("Transform failed at level " + level + ": " + e.getMessage());
            }
        }
        
        wpt.shutdown();
    }
    
    @Test
    public void testCustomThreadPool() throws Exception {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        Haar1 wavelet = new Haar1();
        
        // Create with custom thread pool size
        ParallelWaveletPacketTransform wpt = new ParallelWaveletPacketTransform(wavelet, 2);
        assertEquals("Thread pool should have 2 threads", 2, wpt.getParallelism());
        
        try {
            double[] coeffs = wpt.forward(signal, 2);
            double[] reconstructed = wpt.reverse(coeffs, 2);
            
            assertArrayEquals("Custom thread pool should work correctly", 
                             signal, reconstructed, DELTA);
        } catch (JWaveException e) {
            fail("Transform failed: " + e.getMessage());
        }
        
        
        wpt.shutdown();
    }
    
    @Test
    public void testConvenienceMethods() throws Exception {
        double[] signal = new double[32];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i;
        }
        
        Daubechies4 wavelet = new Daubechies4();
        ParallelWaveletPacketTransform wpt = new ParallelWaveletPacketTransform(wavelet);
        
        // Use convenience methods (auto level selection)
        try {
            double[] coeffs = wpt.forwardWPT(signal);
            double[] reconstructed = wpt.reverseWPT(coeffs);
            
            assertArrayEquals("Convenience methods should work correctly", 
                             signal, reconstructed, DELTA);
        } catch (JWaveException e) {
            fail("Convenience methods failed: " + e.getMessage());
        }
        
        wpt.shutdown();
    }
    
    @Test(expected = jwave.exceptions.JWaveFailure.class)
    public void testInvalidSignalLength() throws JWaveException {
        double[] signal = new double[17]; // Not power of 2
        Haar1 wavelet = new Haar1();
        
        ParallelWaveletPacketTransform wpt = new ParallelWaveletPacketTransform(wavelet);
        try {
            wpt.forward(signal, 2);
        } finally {
            wpt.shutdown();
        }
    }
    
    @Test(expected = jwave.exceptions.JWaveFailure.class)
    public void testInvalidLevel() throws JWaveException {
        double[] signal = new double[8];
        Haar1 wavelet = new Haar1();
        
        ParallelWaveletPacketTransform wpt = new ParallelWaveletPacketTransform(wavelet);
        try {
            wpt.forward(signal, 10); // Level too high
        } finally {
            wpt.shutdown();
        }
    }
    
    @Test
    public void testParallelVsSequentialEquivalence() throws Exception {
        double[] signal = new double[256];
        // Create interesting signal
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32) + 
                       0.5 * Math.cos(2 * Math.PI * i / 8);
        }
        
        Daubechies4 wavelet = new Daubechies4();
        
        try {
            // Sequential
            WaveletPacketTransform wptSeq = new WaveletPacketTransform(wavelet);
            double[] seqCoeffs = wptSeq.forward(signal, 4);
            
            // Parallel
            ParallelWaveletPacketTransform wptPar = new ParallelWaveletPacketTransform(wavelet);
            double[] parCoeffs = wptPar.forward(signal, 4);
            
            assertArrayEquals("Parallel and sequential should produce identical results",
                             seqCoeffs, parCoeffs, DELTA);
            
            wptPar.shutdown();
        } catch (JWaveException e) {
            fail("Transform comparison failed: " + e.getMessage());
        }
    }
}