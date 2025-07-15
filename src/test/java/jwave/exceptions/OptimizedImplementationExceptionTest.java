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
package jwave.exceptions;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for OptimizedImplementationException functionality.
 * 
 * @author Stephen Romano
 */
public class OptimizedImplementationExceptionTest {

    @Test
    public void testFailureTypeConstructor() {
        OptimizedImplementationException ex = new OptimizedImplementationException(
            OptimizedImplementationException.FailureType.CLASS_NOT_FOUND,
            "Test message"
        );
        
        assertEquals(OptimizedImplementationException.FailureType.CLASS_NOT_FOUND, ex.getFailureType());
        assertEquals("Test message", ex.getMessage());
        assertNull(ex.getMissingClasses());
        assertTrue(ex.isFallbackRecommended());
    }

    @Test
    public void testFailureTypeWithCauseConstructor() {
        RuntimeException cause = new RuntimeException("Underlying cause");
        OptimizedImplementationException ex = new OptimizedImplementationException(
            OptimizedImplementationException.FailureType.INSTANTIATION_FAILED,
            "Test message",
            cause
        );
        
        assertEquals(OptimizedImplementationException.FailureType.INSTANTIATION_FAILED, ex.getFailureType());
        assertEquals("Test message", ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertNull(ex.getMissingClasses());
        assertFalse(ex.isFallbackRecommended());
    }

    @Test
    public void testMissingClassesConstructor() {
        String[] missingClasses = {"Class1", "Class2"};
        ClassNotFoundException cause = new ClassNotFoundException("Class1");
        
        OptimizedImplementationException ex = new OptimizedImplementationException(
            "Classes not found",
            missingClasses,
            cause
        );
        
        assertEquals(OptimizedImplementationException.FailureType.CLASS_NOT_FOUND, ex.getFailureType());
        assertEquals("Classes not found", ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertArrayEquals(missingClasses, ex.getMissingClasses());
        assertTrue(ex.isFallbackRecommended());
    }

    @Test
    public void testFallbackRecommendations() {
        // Test failure types that recommend fallback
        assertTrue(new OptimizedImplementationException(
            OptimizedImplementationException.FailureType.CLASS_NOT_FOUND, "msg").isFallbackRecommended());
        assertTrue(new OptimizedImplementationException(
            OptimizedImplementationException.FailureType.INCOMPATIBLE_INTERFACE, "msg").isFallbackRecommended());
        
        // Test failure types that don't recommend fallback
        assertFalse(new OptimizedImplementationException(
            OptimizedImplementationException.FailureType.INSTANTIATION_FAILED, "msg").isFallbackRecommended());
        assertFalse(new OptimizedImplementationException(
            OptimizedImplementationException.FailureType.ACCESS_DENIED, "msg").isFallbackRecommended());
        assertFalse(new OptimizedImplementationException(
            OptimizedImplementationException.FailureType.MISSING_CONSTRUCTOR, "msg").isFallbackRecommended());
        assertFalse(new OptimizedImplementationException(
            OptimizedImplementationException.FailureType.INITIALIZATION_FAILED, "msg").isFallbackRecommended());
        assertFalse(new OptimizedImplementationException(
            OptimizedImplementationException.FailureType.UNKNOWN, "msg").isFallbackRecommended());
    }

    @Test
    public void testMissingClassesCloning() {
        String[] originalClasses = {"Class1", "Class2"};
        OptimizedImplementationException ex = new OptimizedImplementationException(
            "Test", originalClasses, null);
        
        String[] returnedClasses = ex.getMissingClasses();
        assertArrayEquals(originalClasses, returnedClasses);
        
        // Verify that modifying the returned array doesn't affect the original
        returnedClasses[0] = "Modified";
        assertNotEquals("Modified", ex.getMissingClasses()[0]);
    }

    @Test
    public void testNullMissingClasses() {
        OptimizedImplementationException ex = new OptimizedImplementationException(
            "Test", null, null);
        
        assertNull(ex.getMissingClasses());
    }
}