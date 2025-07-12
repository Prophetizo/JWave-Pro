/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test suite for all streaming transform tests.
 * 
 * This suite runs all unit and integration tests for the streaming
 * transform framework.
 * 
 * @author Prophetizo
 * @date 2025-07-12
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    // Core infrastructure tests
    CircularBufferTest.class,
    StreamingTransformConfigTest.class,
    
    // Abstract base class tests
    AbstractStreamingTransformTest.class,
    
    // Factory tests
    StreamingTransformFactoryTest.class,
    StreamingTransformFactoryBufferSizeTest.class,
    
    // Implementation tests
    StreamingMODWTTest.class,
    
    // Integration tests
    StreamingTransformIntegrationTest.class,
    
    // Performance tests (usually @Ignored)
    StreamingMODWTPerformanceTest.class
})
public class StreamingTransformTestSuite {
    // This class remains empty, it is used only as a holder for the above annotations
}