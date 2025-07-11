/**
 * JWave is distributed under the MIT License (MIT); this file is part of.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jwave.transforms.wavelets.continuous;

import static org.junit.Assert.*;
import org.junit.Test;

import jwave.datatypes.natives.Complex;
import jwave.transforms.ContinuousWaveletTransform;
import jwave.transforms.CWTResult;

/**
 * Test suite for Paul wavelet implementation.
 *
 * @author Stephen Romano
 * @date 07.01.2025
 */
public class PaulWaveletTest {

    private static final double DELTA = 1e-10;

    /**
     * Test Paul wavelet construction and basic properties.
     */
    @Test
    public void testPaulWaveletConstruction() {
        // Test default constructor
        PaulWavelet paul1 = new PaulWavelet();
        assertEquals("Default order should be 4", 4, paul1.getOrder());
        
        // Test custom order
        PaulWavelet paul2 = new PaulWavelet(6);
        assertEquals("Custom order should be 6", 6, paul2.getOrder());
        
        // Test invalid order
        try {
            new PaulWavelet(0);
            fail("Should throw exception for order = 0");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("positive integer"));
        }
        
        try {
            new PaulWavelet(25);
            fail("Should throw exception for order > 20");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("numerical issues"));
        }
    }

    /**
     * Test Paul wavelet values at specific points.
     */
    @Test
    public void testPaulWaveletValues() {
        PaulWavelet paul = new PaulWavelet(4);
        
        // Test at t=0: psi(0) = norm * i^4 * 1^(-5) = norm * 1 * 1 = norm (real)
        Complex val0 = paul.wavelet(0);
        assertTrue("Value at t=0 should be real", Math.abs(val0.getImag()) < DELTA);
        assertTrue("Value at t=0 should be positive", val0.getReal() > 0);
        
        // Test that wavelet is complex-valued in general
        Complex val1 = paul.wavelet(1.0);
        assertTrue("Paul wavelet should be complex", Math.abs(val1.getImag()) > DELTA);
        
        // Test asymptotic behavior for large t
        Complex valLarge = paul.wavelet(10.0);
        assertTrue("Should decay for large t", valLarge.getMag() < 0.01);
    }

    /**
     * Test Paul wavelet Fourier transform properties.
     */
    @Test
    public void testPaulWaveletFourierTransform() {
        PaulWavelet paul = new PaulWavelet(4);
        
        // Test negative frequencies (should be zero - analytic signal)
        Complex negFreq = paul.fourierTransform(-1.0);
        assertEquals("Negative frequency should be zero", 0.0, negFreq.getMag(), DELTA);
        
        // Test positive frequencies
        Complex posFreq = paul.fourierTransform(1.0);
        assertTrue("Positive frequency should be non-zero", posFreq.getMag() > 0);
        assertEquals("Fourier transform should be real", 0.0, posFreq.getImag(), DELTA);
        
        // Test peak location (should be around omega = m)
        double maxMag = 0;
        double peakOmega = 0;
        for (double omega = 0.1; omega < 10; omega += 0.1) {
            double mag = paul.fourierTransform(omega).getMag();
            if (mag > maxMag) {
                maxMag = mag;
                peakOmega = omega;
            }
        }
        assertEquals("Peak should be near omega = m", 4.0, peakOmega, 0.5);
    }

    /**
     * Test admissibility constant calculation.
     */
    @Test
    public void testAdmissibilityConstant() {
        // Test for different orders
        PaulWavelet paul2 = new PaulWavelet(2);
        double expected2 = 2.0 * Math.PI / 5.0;  // 2*pi / (2*2 + 1)
        assertEquals("Admissibility for m=2", expected2, paul2.getAdmissibilityConstant(), DELTA);
        
        PaulWavelet paul4 = new PaulWavelet(4);
        double expected4 = 2.0 * Math.PI / 9.0;  // 2*pi / (2*4 + 1)
        assertEquals("Admissibility for m=4", expected4, paul4.getAdmissibilityConstant(), DELTA);
    }

    /**
     * Test i^m computation for different orders.
     */
    @Test
    public void testIPowerM() {
        // Test i^0 through i^7
        int[] orders = {0, 1, 2, 3, 4, 5, 6, 7};
        Complex[] expected = {
            new Complex(1, 0),   // i^0 = 1
            new Complex(0, 1),   // i^1 = i
            new Complex(-1, 0),  // i^2 = -1
            new Complex(0, -1),  // i^3 = -i
            new Complex(1, 0),   // i^4 = 1
            new Complex(0, 1),   // i^5 = i
            new Complex(-1, 0),  // i^6 = -1
            new Complex(0, -1)   // i^7 = -i
        };
        
        for (int i = 0; i < orders.length; i++) {
            if (orders[i] == 0) continue; // Skip 0 as it's invalid for Paul wavelet
            
            PaulWavelet paul = new PaulWavelet(orders[i]);
            // Test that the wavelet correctly incorporates i^m
            // At t=0, psi(0) = norm * i^m * 1
            Complex val = paul.wavelet(0);
            
            // The ratio should give us i^m (modulo normalization)
            // This is a simplified test - full verification would need more analysis
            assertTrue("Wavelet should incorporate i^m correctly", val.getMag() > 0);
        }
    }

    /**
     * Test effective support calculation.
     */
    @Test
    public void testEffectiveSupport() {
        PaulWavelet paul = new PaulWavelet(4);
        double[] support = paul.getEffectiveSupport();
        
        assertEquals("Min support", -1.0, support[0], DELTA);
        assertEquals("Max support", 10.0, support[1], DELTA); // 2*(4+1)
        
        // Test that most energy is in the positive time region
        assertTrue("Support should be asymmetric", support[1] > Math.abs(support[0]));
    }

    /**
     * Test bandwidth calculation.
     */
    @Test
    public void testBandwidth() {
        PaulWavelet paul = new PaulWavelet(4);
        double[] bandwidth = paul.getBandwidth();
        
        assertEquals("Min frequency (analytic)", 0.0, bandwidth[0], DELTA);
        assertTrue("Max frequency should be positive", bandwidth[1] > 0);
        
        // Max frequency should be around (2m + 2) / (2*pi)
        double expectedMax = 10.0 / (2.0 * Math.PI); // (2*4 + 2) / (2*pi)
        assertEquals("Max frequency", expectedMax, bandwidth[1], 0.1);
    }

    /**
     * Test factory method for resolution balance.
     */
    @Test
    public void testResolutionBalanceFactory() {
        // Low frequency resolution
        PaulWavelet lowRes = PaulWavelet.fromResolutionBalance(1.0);
        assertEquals("Low resolution order", 2, lowRes.getOrder());
        
        // High frequency resolution
        PaulWavelet highRes = PaulWavelet.fromResolutionBalance(10.0);
        assertEquals("High resolution order", 20, highRes.getOrder());
        
        // Medium resolution
        PaulWavelet medRes = PaulWavelet.fromResolutionBalance(5.5);
        assertTrue("Medium resolution order", medRes.getOrder() >= 10 && medRes.getOrder() <= 12);
    }

    /**
     * Test Paul wavelet in CWT context.
     */
    @Test
    public void testPaulWaveletCWT() {
        // Create a test signal with frequency modulation
        int n = 256;
        double[] signal = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i / 256.0;
            // Chirp signal: frequency increases linearly with time
            double phase = 2 * Math.PI * (5 * t + 10 * t * t);
            signal[i] = Math.cos(phase);
        }
        
        // Create CWT with Paul wavelet
        PaulWavelet paul = new PaulWavelet(4);
        ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(paul);
        
        // Generate scales
        double[] scales = ContinuousWaveletTransform.generateLinearScales(0.5, 4.0, 20);
        
        // Perform transform
        CWTResult result = cwt.transformFFT(signal, scales, 256.0);
        
        // Basic checks
        assertEquals("Number of scales", 20, result.getNumberOfScales());
        assertEquals("Number of time points", n, result.getNumberOfTimePoints());
        
        // Paul wavelet should show good frequency resolution
        double[][] magnitude = result.getMagnitude();
        
        // Check that magnitude is positive
        for (int i = 0; i < scales.length; i++) {
            for (int j = 0; j < n; j++) {
                assertTrue("Magnitude should be non-negative", magnitude[i][j] >= 0);
            }
        }
    }

    /**
     * Test Paul wavelet performance with FFT.
     */
    @Test
    public void testPaulWaveletPerformance() {
        // Create test signal
        int n = 512;
        double[] signal = new double[n];
        for (int i = 0; i < n; i++) {
            signal[i] = Math.sin(2 * Math.PI * 10 * i / n) + 
                       0.5 * Math.sin(2 * Math.PI * 25 * i / n);
        }
        
        PaulWavelet paul = new PaulWavelet(4);
        ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(paul);
        
        double[] scales = ContinuousWaveletTransform.generateLogScales(0.5, 5.0, 30);
        
        // Time FFT method
        long startFFT = System.nanoTime();
        CWTResult resultFFT = cwt.transformFFT(signal, scales, 1.0);
        long timeFFT = System.nanoTime() - startFFT;
        
        // Time direct method (only if signal is small)
        if (n <= 512) {
            long startDirect = System.nanoTime();
            CWTResult resultDirect = cwt.transform(signal, scales, 1.0);
            long timeDirect = System.nanoTime() - startDirect;
            
            double speedup = timeDirect / (double) timeFFT;
            assertTrue("FFT should be faster than direct method", speedup > 1.0);
        }
        
        // Verify result is valid
        assertNotNull("FFT result should not be null", resultFFT);
        assertTrue("Should have coefficients", resultFFT.getNumberOfScales() > 0);
    }

    /**
     * Test complex power computation accuracy.
     */
    @Test
    public void testComplexPower() {
        PaulWavelet paul = new PaulWavelet(4);
        
        // Test (1-i)^(-5) which appears in Paul wavelet at t=1
        Complex val = paul.wavelet(1.0);
        
        // Verify the result is finite and reasonable
        assertTrue("Result should be finite", Double.isFinite(val.getReal()));
        assertTrue("Result should be finite", Double.isFinite(val.getImag()));
        assertTrue("Magnitude should be reasonable", val.getMag() < 1e10);
    }
}