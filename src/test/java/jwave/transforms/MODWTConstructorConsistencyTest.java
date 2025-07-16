/**
 * JWave is distributed under the MIT License (MIT); this file is part of.
 *
 * Copyright (c) 2025 Prophetizo
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
package jwave.transforms;

import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.exceptions.JWaveException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for MODWT constructor consistency and dependency injection patterns.
 * 
 * @author Stephen Romano
 */
public class MODWTConstructorConsistencyTest {

    @Test
    public void testBasicConstructorsUseStandardImplementations() throws JWaveException {
        
        Daubechies4 wavelet = new Daubechies4();
        
        // Test basic constructor
        MODWTTransform modwt1 = new MODWTTransform(wavelet);
        
        // Test constructor with FFT threshold
        MODWTTransform modwt2 = new MODWTTransform(wavelet, 4096);
        
        // Verify both use FastFourierTransform (not OptimizedFastFourierTransform)
        // We can't directly access private fields, but we can test behavior consistency
        
        // Both should produce identical results for the same input
        double[] signal = createTestSignal();
        double[] result1 = modwt1.forward(signal, 2);
        double[] result2 = modwt2.forward(signal, 2);
        
        // Results should be very close (might have slight differences due to FFT threshold)
        assertEquals("Basic constructors should produce consistent results", 
            result1.length, result2.length);
        
        for (int i = 0; i < result1.length; i++) {
            assertEquals("Coefficient " + i + " should be close", 
                result1[i], result2[i], 1e-10);
        }
        
    }

    @Test
    public void testConstructorVariants() throws JWaveException {
        
        Daubechies4 wavelet = new Daubechies4();
        double[] signal = createTestSignal();
        
        // Test basic constructor
        MODWTTransform modwt1 = new MODWTTransform(wavelet);
        
        // Test constructor with FFT threshold
        MODWTTransform modwt2 = new MODWTTransform(wavelet, 8192);
        
        // All should work and produce valid results
        double[] result1 = modwt1.forward(signal, 2);
        double[] result2 = modwt2.forward(signal, 2);
        
        assertNotNull("Result 1 should not be null", result1);
        assertNotNull("Result 2 should not be null", result2);
        
        assertEquals("All results should have same length", result1.length, result2.length);
        
    }

    @Test
    public void testConvolutionMethodSettings() throws JWaveException {
        
        Daubechies4 wavelet = new Daubechies4();
        double[] signal = createTestSignal();
        
        MODWTTransform modwt = new MODWTTransform(wavelet);
        
        // Test AUTO mode (default)
        assertEquals("Default should be AUTO", MODWTTransform.ConvolutionMethod.AUTO, modwt.getConvolutionMethod());
        
        // Test DIRECT mode
        modwt.setConvolutionMethod(MODWTTransform.ConvolutionMethod.DIRECT);
        assertEquals("Should be DIRECT", MODWTTransform.ConvolutionMethod.DIRECT, modwt.getConvolutionMethod());
        double[] resultDirect = modwt.forward(signal, 2);
        
        // Test FFT mode
        modwt.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
        assertEquals("Should be FFT", MODWTTransform.ConvolutionMethod.FFT, modwt.getConvolutionMethod());
        double[] resultFFT = modwt.forward(signal, 2);
        
        assertNotNull("Direct result should not be null", resultDirect);
        assertNotNull("FFT result should not be null", resultFFT);
        assertEquals("Results should have same length", resultDirect.length, resultFFT.length);
        
    }

    @Test
    public void testMaxDecompositionLevel() throws JWaveException {
        
        // Test the static method for max decomposition level
        int maxLevel = MODWTTransform.getMaxDecompositionLevel();
        assertTrue("Max decomposition level should be positive", maxLevel > 0);
        assertTrue("Max decomposition level should be reasonable (< 20)", maxLevel < 20);
        
        Daubechies4 wavelet = new Daubechies4();
        MODWTTransform modwt = new MODWTTransform(wavelet);
        
        // Test that we can precompute filters up to max level
        try {
            modwt.precomputeFilters(maxLevel);
        } catch (Exception e) {
            fail("Should be able to precompute filters up to max level: " + e.getMessage());
        }
        
        // Test that exceeding max level throws exception
        try {
            modwt.precomputeFilters(maxLevel + 1);
            fail("Should throw exception for level > max");
        } catch (IllegalArgumentException e) {
            // Expected - correctly throws exception for level > max
        }
    }

    private double[] createTestSignal() {
        double[] signal = new double[8];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(i * 0.5) + 0.5 * Math.cos(i * 0.3);
        }
        return signal;
    }
}