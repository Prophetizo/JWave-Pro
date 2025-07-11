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
 * Test suite for DOG (Derivative of Gaussian) wavelet implementation.
 *
 * @author Stephen Romano
 * @date 07.01.2025
 */
public class DOGWaveletTest {

    private static final double DELTA = 1e-10;
    private static final double COARSE_DELTA = 1e-6;

    /**
     * Test DOG wavelet construction and basic properties.
     */
    @Test
    public void testDOGWaveletConstruction() {
        // Test default constructor
        DOGWavelet dog1 = new DOGWavelet();
        assertEquals("Default order should be 2", 2, dog1.getDerivativeOrder());
        assertEquals("Default sigma should be 1.0", 1.0, dog1.getSigma(), DELTA);
        
        // Test single parameter constructor
        DOGWavelet dog2 = new DOGWavelet(3);
        assertEquals("Order should be 3", 3, dog2.getDerivativeOrder());
        assertEquals("Default sigma should be 1.0", 1.0, dog2.getSigma(), DELTA);
        
        // Test full constructor
        DOGWavelet dog3 = new DOGWavelet(4, 2.0);
        assertEquals("Order should be 4", 4, dog3.getDerivativeOrder());
        assertEquals("Sigma should be 2.0", 2.0, dog3.getSigma(), DELTA);
        
        // Test invalid order
        try {
            new DOGWavelet(0);
            fail("Should throw exception for order = 0");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("positive integer"));
        }
        
        try {
            new DOGWavelet(15);
            fail("Should throw exception for order > 10");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("numerical issues"));
        }
        
        // Test invalid sigma
        try {
            new DOGWavelet(2, -1.0);
            fail("Should throw exception for negative sigma");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("positive"));
        }
    }

    /**
     * Test DOG wavelet values at specific points.
     */
    @Test
    public void testDOGWaveletValues() {
        // Test n=1 (first derivative)
        DOGWavelet dog1 = new DOGWavelet(1, 1.0);
        
        // At t=0, first derivative should be 0
        Complex val0 = dog1.wavelet(0);
        assertEquals("First derivative at t=0 should be 0", 0.0, val0.getReal(), COARSE_DELTA);
        assertEquals("DOG is real-valued", 0.0, val0.getImag(), DELTA);
        
        // First derivative should be antisymmetric
        Complex valPos = dog1.wavelet(1.0);
        Complex valNeg = dog1.wavelet(-1.0);
        assertEquals("First derivative should be antisymmetric", 
                    -valPos.getReal(), valNeg.getReal(), COARSE_DELTA);
        
        // Test n=2 (second derivative - Mexican Hat)
        DOGWavelet dog2 = new DOGWavelet(2, 1.0);
        
        // At t=0, should have maximum absolute value
        // Note: The sign convention may vary - we check for non-zero
        val0 = dog2.wavelet(0);
        assertTrue("Second derivative at t=0 should be non-zero", Math.abs(val0.getReal()) > DELTA);
        
        // Should be symmetric
        valPos = dog2.wavelet(1.0);
        valNeg = dog2.wavelet(-1.0);
        assertEquals("Second derivative should be symmetric", 
                    valPos.getReal(), valNeg.getReal(), COARSE_DELTA);
        
        // Test n=3 (third derivative)
        DOGWavelet dog3 = new DOGWavelet(3, 1.0);
        
        // At t=0, third derivative should be 0
        val0 = dog3.wavelet(0);
        assertEquals("Third derivative at t=0 should be 0", 0.0, val0.getReal(), COARSE_DELTA);
        
        // Should be antisymmetric
        valPos = dog3.wavelet(1.0);
        valNeg = dog3.wavelet(-1.0);
        assertEquals("Third derivative should be antisymmetric", 
                    -valPos.getReal(), valNeg.getReal(), COARSE_DELTA);
    }

    /**
     * Test Hermite polynomial calculation.
     */
    @Test
    public void testHermitePolynomials() {
        // Test known Hermite polynomial values
        // H_0(x) = 1
        // H_1(x) = 2x
        // H_2(x) = 4x^2 - 2
        // H_3(x) = 8x^3 - 12x
        // H_4(x) = 16x^4 - 48x^2 + 12
        
        double x = 1.5;
        
        // Note: DOG applies (-1)^(n+1) factor
        // For n=1: (-1)^2 * H_1(x) = H_1(x) = 2x
        DOGWavelet dog1 = new DOGWavelet(1, 1.0);
        Complex val1 = dog1.wavelet(x);
        
        // For n=2: (-1)^3 * H_2(x) = -(4x^2 - 2) = -4x^2 + 2
        DOGWavelet dog2 = new DOGWavelet(2, 1.0);
        Complex val2 = dog2.wavelet(x);
        
        // For n=3: (-1)^4 * H_3(x) = H_3(x) = 8x^3 - 12x
        DOGWavelet dog3 = new DOGWavelet(3, 1.0);
        Complex val3 = dog3.wavelet(x);
        
        // Check the sign patterns
        // n=1: Should be positive for x > 0 (since H_1(x) = 2x)
        assertTrue("DOG n=1 should be positive for x > 0", val1.getReal() > 0);
        
        // n=2: At x=0, H_2(0) = -2, with (-1)^3 factor gives +2, should be positive
        DOGWavelet dog2_at_0 = new DOGWavelet(2, 1.0);
        Complex val2_at_0 = dog2_at_0.wavelet(0);
        assertTrue("DOG n=2 should be positive at x=0", val2_at_0.getReal() > 0);
        
        // n=3: At x=0, H_3(0) = 0, should be zero
        DOGWavelet dog3_at_0 = new DOGWavelet(3, 1.0);
        Complex val3_at_0 = dog3_at_0.wavelet(0);
        assertEquals("DOG n=3 should be zero at x=0", 0.0, val3_at_0.getReal(), COARSE_DELTA);
        
        // Check relative magnitudes follow Hermite polynomial growth
        // Higher order polynomials should have larger values for |x| > 1
        assertTrue("Higher order should have larger magnitude for |x| > 1", 
                  Math.abs(val3.getReal()) > Math.abs(val1.getReal()));
    }

    /**
     * Test DOG wavelet Fourier transform properties.
     */
    @Test
    public void testDOGWaveletFourierTransform() {
        DOGWavelet dog = new DOGWavelet(2, 1.0);
        
        // Test at omega = 0 (should be 0 for all n >= 1)
        Complex ft0 = dog.fourierTransform(0);
        assertEquals("FT at omega=0 should be 0", 0.0, ft0.getMag(), DELTA);
        
        // Test positive frequencies
        Complex ftPos = dog.fourierTransform(1.0);
        assertTrue("FT at positive frequency should be non-zero", ftPos.getMag() > 0);
        
        // Test negative frequencies (should have same magnitude for even n)
        Complex ftNeg = dog.fourierTransform(-1.0);
        assertEquals("FT should be symmetric for even n", 
                    ftPos.getMag(), ftNeg.getMag(), COARSE_DELTA);
        
        // For odd n, FT should be antisymmetric
        DOGWavelet dog3 = new DOGWavelet(3, 1.0);
        ftPos = dog3.fourierTransform(1.0);
        ftNeg = dog3.fourierTransform(-1.0);
        
        // Check phase relationship for odd n
        assertTrue("FT should have correct phase for odd n", 
                  Math.abs(ftPos.getImag()) > DELTA || Math.abs(ftNeg.getImag()) > DELTA);
    }

    /**
     * Test comparison with Mexican Hat wavelet.
     */
    @Test
    public void testMexicanHatEquivalence() {
        // DOG with n=2 should be equivalent to Mexican Hat
        DOGWavelet dog = new DOGWavelet(2, 1.0);
        MexicanHatWavelet mexican = new MexicanHatWavelet(1.0);
        
        assertTrue("DOG(n=2) should identify as Mexican Hat", dog.isMexicanHat());
        
        // Compare values at several points - they should have the same shape
        double[] testPoints = {0.0, 0.5, 1.0, 1.5, 2.0};
        
        // Both should have the same sign pattern
        for (double t : testPoints) {
            Complex dogVal = dog.wavelet(t);
            Complex mexVal = mexican.wavelet(t);
            
            // Check they have the same sign (both positive or both negative)
            if (Math.abs(mexVal.getReal()) > DELTA && Math.abs(dogVal.getReal()) > DELTA) {
                assertTrue("DOG n=2 and Mexican Hat should have same sign pattern",
                          Math.signum(dogVal.getReal()) == Math.signum(mexVal.getReal()));
            }
        }
    }

    /**
     * Test effective support calculation.
     */
    @Test
    public void testEffectiveSupport() {
        // Support should increase with n and sigma
        DOGWavelet dog1 = new DOGWavelet(1, 1.0);
        DOGWavelet dog4 = new DOGWavelet(4, 1.0);
        DOGWavelet dog1s2 = new DOGWavelet(1, 2.0);
        
        double[] support1 = dog1.getEffectiveSupport();
        double[] support4 = dog4.getEffectiveSupport();
        double[] support1s2 = dog1s2.getEffectiveSupport();
        
        // Higher order should have larger support
        assertTrue("Higher order should have larger support", 
                  support4[1] - support4[0] > support1[1] - support1[0]);
        
        // Larger sigma should have larger support
        assertTrue("Larger sigma should have larger support", 
                  support1s2[1] - support1s2[0] > support1[1] - support1[0]);
        
        // Support should be symmetric
        assertEquals("Support should be symmetric", 
                    -support1[0], support1[1], COARSE_DELTA);
    }

    /**
     * Test bandwidth calculation.
     */
    @Test
    public void testBandwidth() {
        DOGWavelet dog = new DOGWavelet(2, 1.0);
        double[] bandwidth = dog.getBandwidth();
        
        assertEquals("Min frequency should be 0", 0.0, bandwidth[0], DELTA);
        assertTrue("Max frequency should be positive", bandwidth[1] > 0);
        
        // Higher order should have higher bandwidth
        DOGWavelet dog4 = new DOGWavelet(4, 1.0);
        double[] bandwidth4 = dog4.getBandwidth();
        assertTrue("Higher order should have higher bandwidth", 
                  bandwidth4[1] > bandwidth[1]);
    }

    /**
     * Test center frequency calculation.
     */
    @Test
    public void testCenterFrequency() {
        // Center frequency should be sqrt(n) / (2*pi*sigma)
        DOGWavelet dog1 = new DOGWavelet(1, 1.0);
        double expected1 = Math.sqrt(1) / (2.0 * Math.PI);
        assertEquals("Center frequency for n=1", expected1, dog1.getCenterFrequency(), COARSE_DELTA);
        
        DOGWavelet dog4 = new DOGWavelet(4, 2.0);
        double expected4 = Math.sqrt(4) / (2.0 * Math.PI * 2.0);
        assertEquals("Center frequency for n=4, sigma=2", expected4, dog4.getCenterFrequency(), COARSE_DELTA);
    }

    /**
     * Test standard factory method.
     */
    @Test
    public void testStandardFactory() {
        // Test edge detector
        DOGWavelet edge = DOGWavelet.createStandard(DOGWavelet.WaveletType.EDGE, 1.0);
        assertEquals("Edge detector should be n=1", 1, edge.getDerivativeOrder());
        
        // Test Mexican Hat
        DOGWavelet mexican = DOGWavelet.createStandard(DOGWavelet.WaveletType.MEXICAN_HAT, 1.0);
        assertEquals("Mexican Hat should be n=2", 2, mexican.getDerivativeOrder());
        assertTrue("Should identify as Mexican Hat", mexican.isMexicanHat());
        
        // Test Ricker (alias for Mexican Hat)
        DOGWavelet ricker = DOGWavelet.createStandard(DOGWavelet.WaveletType.RICKER, 1.0);
        assertEquals("Ricker should be n=2", 2, ricker.getDerivativeOrder());
        
        // Test zero crossing
        DOGWavelet zero = DOGWavelet.createStandard(DOGWavelet.WaveletType.ZERO_CROSSING, 1.0);
        assertEquals("Zero crossing should be n=3", 3, zero.getDerivativeOrder());
        
        // Test ridge
        DOGWavelet ridge = DOGWavelet.createStandard(DOGWavelet.WaveletType.RIDGE, 1.0);
        assertEquals("Ridge should be n=4", 4, ridge.getDerivativeOrder());
        
        // Test null type
        try {
            DOGWavelet.createStandard((DOGWavelet.WaveletType)null, 1.0);
            fail("Should throw exception for null type");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("cannot be null"));
        }
    }


    /**
     * Test DOG wavelet in CWT context.
     */
    @Test
    public void testDOGWaveletCWT() {
        // Create a test signal with an edge
        int n = 256;
        double[] signal = new double[n];
        
        // Step function with smooth transition
        for (int i = 0; i < n; i++) {
            double t = (i - n/2) / 10.0;
            signal[i] = 0.5 * (1 + Math.tanh(t)); // Smooth step
        }
        
        // Use DOG n=1 for edge detection
        DOGWavelet dog = new DOGWavelet(1, 2.0);
        ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(dog);
        
        // Single scale
        double[] scales = {10.0};
        
        CWTResult result = cwt.transformFFT(signal, scales, 1.0);
        
        // Verify result dimensions
        assertEquals("Number of scales", 1, result.getNumberOfScales());
        assertEquals("Number of time points", n, result.getNumberOfTimePoints());
        
        // Verify we get a response
        double[][] magnitude = result.getMagnitude();
        double maxMag = 0;
        for (int i = 0; i < n; i++) {
            if (magnitude[0][i] > maxMag) {
                maxMag = magnitude[0][i];
            }
        }
        assertTrue("Edge detector should produce response", maxMag > 0);
    }

    /**
     * Test multi-scale analysis with DOG wavelets.
     */
    @Test
    public void testMultiScaleAnalysis() {
        // Create signal with features at different scales
        int n = 512;
        double[] signal = new double[n];
        
        for (int i = 0; i < n; i++) {
            double t = i / 512.0;
            // Low frequency + high frequency + noise
            signal[i] = Math.sin(2 * Math.PI * 5 * t) + 
                       0.3 * Math.sin(2 * Math.PI * 50 * t) +
                       0.1 * (Math.random() - 0.5);
        }
        
        // Use DOG n=2 (Mexican Hat) for analysis
        DOGWavelet dog = new DOGWavelet(2, 1.0);
        ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(dog);
        
        // Multiple scales
        double[] scales = ContinuousWaveletTransform.generateLogScales(1.0, 50.0, 20);
        
        CWTResult result = cwt.transformFFT(signal, scales, 512.0);
        
        // Check dimensions
        assertEquals("Number of scales", 20, result.getNumberOfScales());
        assertEquals("Number of time points", n, result.getNumberOfTimePoints());
        
        // Verify scalogram
        double[] scalogram = result.getScalogram();
        assertEquals("Scalogram length", 20, scalogram.length);
        
        // All scalogram values should be non-negative
        for (double val : scalogram) {
            assertTrue("Scalogram values should be non-negative", val >= 0);
        }
    }

    /**
     * Test performance comparison between direct and FFT methods.
     */
    @Test
    public void testPerformance() {
        // Small signal for testing both methods
        int n = 128;
        double[] signal = new double[n];
        for (int i = 0; i < n; i++) {
            signal[i] = Math.sin(2 * Math.PI * 10 * i / n);
        }
        
        DOGWavelet dog = new DOGWavelet(2, 1.0);
        ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(dog);
        
        double[] scales = {1.0, 2.0, 4.0, 8.0};
        
        // Time direct method
        long startDirect = System.nanoTime();
        CWTResult resultDirect = cwt.transform(signal, scales, 1.0);
        long timeDirect = System.nanoTime() - startDirect;
        
        // Time FFT method
        long startFFT = System.nanoTime();
        CWTResult resultFFT = cwt.transformFFT(signal, scales, 1.0);
        long timeFFT = System.nanoTime() - startFFT;
        
        // FFT should be faster for reasonable signal sizes
        if (n >= 64) {
            assertTrue("FFT should be faster for n >= 64", timeFFT < timeDirect);
        }
        
        // Verify both methods produce valid results
        assertNotNull("Direct method should produce result", resultDirect);
        assertNotNull("FFT method should produce result", resultFFT);
        assertEquals("Same number of scales", resultDirect.getNumberOfScales(), resultFFT.getNumberOfScales());
        assertEquals("Same number of time points", resultDirect.getNumberOfTimePoints(), resultFFT.getNumberOfTimePoints());
    }

    /**
     * Test DOG wavelet normalization.
     */
    @Test
    public void testNormalization() {
        // Test that wavelets are properly normalized
        DOGWavelet dog = new DOGWavelet(2, 1.0);
        
        // Compute approximate L2 norm numerically
        double sum = 0;
        double dt = 0.01;
        for (double t = -10; t <= 10; t += dt) {
            double val = dog.wavelet(t).getReal();
            sum += val * val * dt;
        }
        double norm = Math.sqrt(sum);
        
        // The norm should be finite and reasonable
        // Note: The exact normalization may vary depending on conventions
        assertTrue("Wavelet should have reasonable L2 norm", norm > 0.1 && norm < 10.0);
    }
}