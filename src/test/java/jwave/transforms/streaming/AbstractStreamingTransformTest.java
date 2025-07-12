/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import jwave.transforms.BasicTransform;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unit tests for AbstractStreamingTransform listener functionality.
 * 
 * @author Prophetizo
 * @date 2025-07-12
 */
public class AbstractStreamingTransformTest {
    
    private StubStreamingTransform stubTransform;
    private TestListener listener1;
    private TestListener listener2;
    
    @Before
    public void setUp() {
        // Create a mock BasicTransform
        BasicTransform mockTransform = new BasicTransform() {
            @Override
            public double[] forward(double[] signal) {
                return signal;
            }
            
            @Override
            public double[] reverse(double[] coeffs) {
                return coeffs;
            }
        };
        
        stubTransform = new StubStreamingTransform(mockTransform);
        listener1 = new TestListener("listener1");
        listener2 = new TestListener("listener2");
    }
    
    @Test
    public void testAddListener() {
        assertEquals(0, stubTransform.getListenerCount());
        
        stubTransform.addListener(listener1);
        assertEquals(1, stubTransform.getListenerCount());
        
        stubTransform.addListener(listener2);
        assertEquals(2, stubTransform.getListenerCount());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testAddNullListener() {
        stubTransform.addListener(null);
    }
    
    @Test
    public void testRemoveListener() {
        stubTransform.addListener(listener1);
        stubTransform.addListener(listener2);
        assertEquals(2, stubTransform.getListenerCount());
        
        assertTrue(stubTransform.removeListener(listener1));
        assertEquals(1, stubTransform.getListenerCount());
        
        assertFalse(stubTransform.removeListener(listener1)); // Already removed
        assertEquals(1, stubTransform.getListenerCount());
        
        assertTrue(stubTransform.removeListener(listener2));
        assertEquals(0, stubTransform.getListenerCount());
    }
    
    @Test
    public void testClearListeners() {
        stubTransform.addListener(listener1);
        stubTransform.addListener(listener2);
        assertEquals(2, stubTransform.getListenerCount());
        
        stubTransform.clearListeners();
        assertEquals(0, stubTransform.getListenerCount());
    }
    
    @Test
    public void testCoefficientsUpdatedNotification() {
        stubTransform.initialize(16, 2);
        stubTransform.addListener(listener1);
        stubTransform.addListener(listener2);
        
        double[] newSamples = {1.0, 2.0, 3.0};
        double[] result = stubTransform.update(newSamples);
        
        // Both listeners should be notified
        assertEquals(1, listener1.coefficientsUpdateCount.get());
        assertEquals(3, listener1.lastNewSamplesCount);
        assertArrayEquals(result, listener1.lastCoefficients, 0.0);
        
        assertEquals(1, listener2.coefficientsUpdateCount.get());
        assertEquals(3, listener2.lastNewSamplesCount);
        assertArrayEquals(result, listener2.lastCoefficients, 0.0);
    }
    
    @Test
    public void testBufferFullNotification() {
        stubTransform.initialize(4, 2); // Small buffer
        stubTransform.addListener(listener1);
        stubTransform.addListener(listener2);
        
        // Fill buffer
        assertFalse(listener1.bufferFullCalled.get());
        assertFalse(listener2.bufferFullCalled.get());
        
        stubTransform.update(new double[] {1.0, 2.0});
        assertFalse(listener1.bufferFullCalled.get());
        
        stubTransform.update(new double[] {3.0, 4.0}); // Buffer becomes full
        assertTrue(listener1.bufferFullCalled.get());
        assertTrue(listener2.bufferFullCalled.get());
        
        // Reset the flags
        listener1.bufferFullCalled.set(false);
        listener2.bufferFullCalled.set(false);
        
        // Further updates shouldn't trigger buffer full again
        stubTransform.update(new double[] {5.0});
        assertFalse(listener1.bufferFullCalled.get());
        assertFalse(listener2.bufferFullCalled.get());
    }
    
    @Test
    public void testResetNotification() {
        stubTransform.initialize(16, 2);
        stubTransform.addListener(listener1);
        stubTransform.addListener(listener2);
        
        assertFalse(listener1.resetCalled.get());
        assertFalse(listener2.resetCalled.get());
        
        stubTransform.reset();
        
        assertTrue(listener1.resetCalled.get());
        assertTrue(listener2.resetCalled.get());
    }
    
    @Test
    public void testErrorNotification() {
        stubTransform.initialize(16, 2);
        stubTransform.addListener(listener1);
        stubTransform.addListener(listener2);
        
        Exception testError = new RuntimeException("Test error");
        stubTransform.notifyError(testError, true);
        
        assertEquals(testError, listener1.lastError);
        assertTrue(listener1.lastRecoverable);
        assertEquals(1, listener1.errorCount.get());
        
        assertEquals(testError, listener2.lastError);
        assertTrue(listener2.lastRecoverable);
        assertEquals(1, listener2.errorCount.get());
    }
    
    @Test
    public void testListenerExceptionHandling() {
        stubTransform.initialize(16, 2);
        
        // Add a faulty listener that throws exceptions
        FaultyListener faultyListener = new FaultyListener();
        stubTransform.addListener(faultyListener);
        stubTransform.addListener(listener1); // Normal listener
        
        // Update should complete despite faulty listener
        double[] result = stubTransform.update(new double[] {1.0});
        assertNotNull(result);
        
        // Normal listener should still be notified
        assertEquals(1, listener1.coefficientsUpdateCount.get());
        
        // Faulty listener should have received error notification
        assertEquals(1, faultyListener.errorCount.get());
        assertTrue(faultyListener.lastError instanceof RuntimeException);
    }
    
    @Test
    public void testMultipleListenersConcurrentModification() {
        stubTransform.initialize(16, 2);
        
        // Create a listener that removes itself when notified
        SelfRemovingListener selfRemovingListener = new SelfRemovingListener(stubTransform);
        
        stubTransform.addListener(selfRemovingListener);
        stubTransform.addListener(listener1);
        
        // This should not throw ConcurrentModificationException
        stubTransform.update(new double[] {1.0});
        
        // Self-removing listener should be gone
        assertEquals(1, stubTransform.getListenerCount());
        
        // Normal listener should still be notified
        assertEquals(1, listener1.coefficientsUpdateCount.get());
    }
    
    @Test
    public void testNotificationsBeforeInitialization() {
        // Should throw IllegalStateException when trying to update before initialization
        stubTransform.addListener(listener1);
        
        try {
            stubTransform.update(new double[] {1.0});
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }
        
        // Listener should not have been notified
        assertEquals(0, listener1.coefficientsUpdateCount.get());
    }
    
    /**
     * Stub implementation for testing AbstractStreamingTransform
     */
    private static class StubStreamingTransform extends AbstractStreamingTransform<double[]> {
        private double[] cachedCoefficients = new double[0];
        
        public StubStreamingTransform(BasicTransform transform) {
            super(transform);
        }
        
        @Override
        protected void initializeTransformState() {
            // Simple initialization
            cachedCoefficients = new double[bufferSize];
        }
        
        @Override
        protected double[] performUpdate(double[] newSamples) {
            // Just return the buffer contents as "coefficients"
            cachedCoefficients = buffer.toArray();
            return cachedCoefficients.clone();
        }
        
        @Override
        protected double[] getCachedCoefficients() {
            return cachedCoefficients.clone();
        }
        
        @Override
        protected void resetTransformState() {
            cachedCoefficients = new double[bufferSize];
        }
    }
    
    /**
     * Test listener that records all notifications
     */
    private static class TestListener implements StreamingTransformListener<double[]> {
        final String name;
        final AtomicInteger coefficientsUpdateCount = new AtomicInteger(0);
        final AtomicBoolean bufferFullCalled = new AtomicBoolean(false);
        final AtomicBoolean resetCalled = new AtomicBoolean(false);
        final AtomicInteger errorCount = new AtomicInteger(0);
        
        double[] lastCoefficients;
        int lastNewSamplesCount;
        Exception lastError;
        boolean lastRecoverable;
        
        TestListener(String name) {
            this.name = name;
        }
        
        @Override
        public void onCoefficientsUpdated(double[] coefficients, int newSamplesCount) {
            coefficientsUpdateCount.incrementAndGet();
            lastCoefficients = coefficients;
            lastNewSamplesCount = newSamplesCount;
        }
        
        @Override
        public void onBufferFull() {
            bufferFullCalled.set(true);
        }
        
        @Override
        public void onReset() {
            resetCalled.set(true);
        }
        
        @Override
        public void onError(Exception error, boolean recoverable) {
            errorCount.incrementAndGet();
            lastError = error;
            lastRecoverable = recoverable;
        }
    }
    
    /**
     * Listener that throws exceptions to test error handling
     */
    private static class FaultyListener extends TestListener {
        FaultyListener() {
            super("faulty");
        }
        
        @Override
        public void onCoefficientsUpdated(double[] coefficients, int newSamplesCount) {
            throw new RuntimeException("Faulty listener exception");
        }
    }
    
    /**
     * Listener that removes itself when notified
     */
    private static class SelfRemovingListener extends TestListener {
        private final StubStreamingTransform transform;
        
        SelfRemovingListener(StubStreamingTransform transform) {
            super("self-removing");
            this.transform = transform;
        }
        
        @Override
        public void onCoefficientsUpdated(double[] coefficients, int newSamplesCount) {
            super.onCoefficientsUpdated(coefficients, newSamplesCount);
            transform.removeListener(this);
        }
    }
}