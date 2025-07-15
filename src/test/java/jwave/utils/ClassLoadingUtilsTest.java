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
package jwave.utils;

import jwave.exceptions.OptimizedImplementationException;
import jwave.transforms.FastFourierTransform;
import jwave.transforms.wavelets.WaveletOperations;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for ClassLoadingUtils functionality.
 * 
 * @author Stephen Romano
 */
public class ClassLoadingUtilsTest {

    @Test
    public void testLoadExistingClass() throws OptimizedImplementationException {
        // Test loading a standard implementation that we know exists
        FastFourierTransform fft = ClassLoadingUtils.loadOptimizedImplementation(
            "jwave.transforms.FastFourierTransform", FastFourierTransform.class);
        
        assertNotNull("Loaded FFT should not be null", fft);
        assertTrue("Should be instance of FastFourierTransform", fft instanceof FastFourierTransform);
    }

    @Test
    public void testLoadNonExistentClass() {
        try {
            ClassLoadingUtils.loadOptimizedImplementation(
                "jwave.nonexistent.NonExistentClass", Object.class);
            fail("Should have thrown OptimizedImplementationException");
        } catch (OptimizedImplementationException e) {
            assertEquals("Should be CLASS_NOT_FOUND failure type", 
                OptimizedImplementationException.FailureType.CLASS_NOT_FOUND, e.getFailureType());
            assertTrue("Error message should mention class not found", 
                e.getMessage().contains("not found"));
        }
    }

    @Test
    public void testIncompatibleInterface() {
        try {
            // Try to load Object as a FastFourierTransform (Object has accessible constructor but wrong type)
            ClassLoadingUtils.loadOptimizedImplementation(
                "java.lang.Object", FastFourierTransform.class);
            fail("Should have thrown OptimizedImplementationException");
        } catch (OptimizedImplementationException e) {
            assertEquals("Should be INCOMPATIBLE_INTERFACE failure type", 
                OptimizedImplementationException.FailureType.INCOMPATIBLE_INTERFACE, e.getFailureType());
            assertTrue("Error message should mention interface compatibility", 
                e.getMessage().contains("does not extend"));
        }
    }

    @Test
    public void testLoadWithContext() {
        String[] expectedClasses = {
            "jwave.transforms.FastFourierTransform",
            "jwave.transforms.wavelets.StandardWaveletOperations"
        };
        
        try {
            FastFourierTransform fft = ClassLoadingUtils.loadOptimizedImplementationWithContext(
                expectedClasses[0], FastFourierTransform.class, expectedClasses);
            
            assertNotNull("Loaded FFT should not be null", fft);
            assertTrue("Should be instance of FastFourierTransform", fft instanceof FastFourierTransform);
        } catch (OptimizedImplementationException e) {
            fail("Should not throw exception for valid class: " + e.getMessage());
        }
    }

    @Test
    public void testLoadWithContextErrorMessage() {
        String[] expectedClasses = {
            "jwave.nonexistent.NonExistentClass1",
            "jwave.nonexistent.NonExistentClass2"
        };
        
        try {
            ClassLoadingUtils.loadOptimizedImplementationWithContext(
                expectedClasses[0], Object.class, expectedClasses);
            fail("Should have thrown OptimizedImplementationException");
        } catch (OptimizedImplementationException e) {
            assertEquals("Should be CLASS_NOT_FOUND failure type", 
                OptimizedImplementationException.FailureType.CLASS_NOT_FOUND, e.getFailureType());
            assertTrue("Error message should include all expected classes", 
                e.getMessage().contains("NonExistentClass1") && 
                e.getMessage().contains("NonExistentClass2"));
            assertNotNull("Should have missing classes array", e.getMissingClasses());
            assertEquals("Should have 2 missing classes", 2, e.getMissingClasses().length);
        }
    }
}