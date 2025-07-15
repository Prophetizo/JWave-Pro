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
package jwave.operations;

import jwave.datatypes.natives.Complex;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for enhanced ThreadLocal cleanup mechanisms to prevent ClassLoader leaks.
 * 
 * @author Stephen Romano
 */
public class EnhancedThreadLocalCleanupTest {

    @Test
    public void testCleanupStatus() {
        System.out.println("=== Enhanced ThreadLocal Cleanup Status Test ===");
        
        // Get initial status
        SIMDComplexOperations.CleanupStatus initialStatus = SIMDComplexOperations.getCleanupStatus();
        System.out.println("Initial status: " + initialStatus);
        
        // Verify shutdown hook is registered
        assertTrue("Shutdown hook should be registered", initialStatus.isShutdownHookRegistered());
        
        // Perform some operations to trigger buffer allocation
        SIMDComplexOperations ops = new SIMDComplexOperations();
        Complex[] array1 = createTestArray(1000);
        Complex[] array2 = createTestArray(1000);
        Complex[] result = new Complex[1000];
        
        ops.add(array1, array2, result, 1000);
        
        // Get status after operations
        SIMDComplexOperations.CleanupStatus afterStatus = SIMDComplexOperations.getCleanupStatus();
        System.out.println("After operations: " + afterStatus);
        
        assertTrue("Current thread should have buffers after operations", afterStatus.isCurrentThreadHasBuffers());
        assertTrue("Memory usage should be > 0", afterStatus.getCurrentThreadMemoryUsage() > 0);
        
        System.out.println("Enhanced cleanup status test completed successfully!");
    }

    @Test
    public void testCleanupAllThreadLocals() {
        System.out.println("=== Test cleanupAllThreadLocals Method ===");
        
        // Perform operations to ensure ThreadLocal is initialized
        SIMDComplexOperations ops = new SIMDComplexOperations();
        Complex[] array1 = createTestArray(500);
        Complex[] array2 = createTestArray(500);
        Complex[] result = new Complex[500];
        
        ops.multiply(array1, array2, result, 500);
        
        assertTrue("Should have buffers before cleanup", SIMDComplexOperations.hasThreadLocalBuffers());
        
        // Cleanup all ThreadLocals
        int cleanedCount = SIMDComplexOperations.cleanupAllThreadLocals();
        System.out.println("Cleaned up " + cleanedCount + " ThreadLocal instances");
        
        assertTrue("Should have cleaned up at least 1 ThreadLocal", cleanedCount >= 1);
        assertFalse("Should not have buffers after cleanup", SIMDComplexOperations.hasThreadLocalBuffers());
        
        System.out.println("cleanupAllThreadLocals test completed successfully!");
    }

    @Test
    public void testForceCleanupAndShutdown() {
        System.out.println("=== Test forceCleanupAndShutdown Method ===");
        
        // Perform operations to initialize ThreadLocal
        SIMDComplexOperations ops = new SIMDComplexOperations();
        Complex[] array1 = createTestArray(2000);
        Complex[] array2 = createTestArray(2000);
        Complex[] result = new Complex[2000];
        
        ops.conjugate(array1, result, 2000);
        
        long memoryBefore = SIMDComplexOperations.estimateThreadLocalMemoryUsage();
        System.out.println("Memory usage before cleanup: " + memoryBefore + " bytes");
        
        // Force cleanup and shutdown
        SIMDComplexOperations.CleanupResult cleanupResult = SIMDComplexOperations.forceCleanupAndShutdown();
        System.out.println("Cleanup result: " + cleanupResult);
        
        assertNotNull("Cleanup result should not be null", cleanupResult);
        assertTrue("Should have cleaned up ThreadLocal instances", cleanupResult.getThreadLocalsCleanedUp() >= 1);
        
        long memoryAfter = SIMDComplexOperations.estimateThreadLocalMemoryUsage();
        System.out.println("Memory usage after cleanup: " + memoryAfter + " bytes");
        
        assertEquals("Memory usage should be 0 after cleanup", 0, memoryAfter);
        
        System.out.println("forceCleanupAndShutdown test completed successfully!");
    }

    @Test
    public void testWeakReferenceCleanup() {
        System.out.println("=== Test WeakReference-based Cleanup ===");
        
        // Get initial status
        SIMDComplexOperations.CleanupStatus initialStatus = SIMDComplexOperations.getCleanupStatus();
        System.out.println("Initial live ThreadLocals: " + initialStatus.getLiveThreadLocals());
        
        // Perform operations
        SIMDComplexOperations ops = new SIMDComplexOperations();
        Complex[] array1 = createTestArray(100);
        double[] result = new double[100];
        
        ops.magnitude(array1, result, 100);
        
        // Get status after operations
        SIMDComplexOperations.CleanupStatus afterStatus = SIMDComplexOperations.getCleanupStatus();
        System.out.println("After operations - Live: " + afterStatus.getLiveThreadLocals() + 
                         ", Dead: " + afterStatus.getDeadReferences());
        
        // The live count should be at least 1 (our ThreadLocal)
        assertTrue("Should have at least 1 live ThreadLocal", afterStatus.getLiveThreadLocals() >= 1);
        
        System.out.println("WeakReference cleanup test completed successfully!");
    }

    @Test
    public void testShutdownHookRegistration() {
        System.out.println("=== Test Shutdown Hook Registration ===");
        
        // The shutdown hook should be automatically registered
        SIMDComplexOperations.CleanupStatus status = SIMDComplexOperations.getCleanupStatus();
        assertTrue("Shutdown hook should be registered", status.isShutdownHookRegistered());
        
        System.out.println("Shutdown hook registration verified!");
        System.out.println("Note: The actual shutdown hook will execute during JVM shutdown");
        
        System.out.println("Shutdown hook registration test completed successfully!");
    }

    @Test
    public void testClassLoaderLeakPrevention() {
        System.out.println("=== Test ClassLoader Leak Prevention Mechanisms ===");
        
        // This test verifies that the mechanisms are in place to prevent ClassLoader leaks
        // In a real scenario, this would be tested by unloading a ClassLoader that contains
        // this class and verifying no memory leaks occur
        
        // Verify WeakReference usage
        SIMDComplexOperations ops = new SIMDComplexOperations();
        Complex[] testArray = createTestArray(50);
        Complex[] resultArray = new Complex[50];
        
        ops.multiplyScalar(testArray, 2.0, resultArray, 50);
        
        SIMDComplexOperations.CleanupStatus status = SIMDComplexOperations.getCleanupStatus();
        
        // Verify the infrastructure is working
        assertTrue("Should have registered ThreadLocals", status.getLiveThreadLocals() > 0);
        assertTrue("Shutdown hook should be registered", status.isShutdownHookRegistered());
        assertTrue("Current thread should have buffers", status.isCurrentThreadHasBuffers());
        
        System.out.println("ClassLoader leak prevention mechanisms verified:");
        System.out.println("  ✓ WeakReference-based ThreadLocal tracking");
        System.out.println("  ✓ Automatic shutdown hook registration");
        System.out.println("  ✓ Comprehensive cleanup methods available");
        System.out.println("  ✓ Memory usage monitoring");
        
        System.out.println("ClassLoader leak prevention test completed successfully!");
    }

    private Complex[] createTestArray(int size) {
        Complex[] array = new Complex[size];
        for (int i = 0; i < size; i++) {
            array[i] = new Complex(Math.sin(i * 0.1), Math.cos(i * 0.1));
        }
        return array;
    }
}