package jwave.utils;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for TestUtils utility class.
 * 
 * @author Stephen Romano
 * @since 2.1.0
 */
public class TestUtilsTest {
    
    @Test
    public void testPerformanceResultFormatting() {
        // Test performance result formatting
        TestUtils.printPerformanceResult("TestOperation", 1_000_000_000L, 1000);
        // Output: TestOperation: 1000.000 ms for 1000 operations (1000000.00 ns/op)
        
        // This test mainly verifies no exceptions are thrown
        assertTrue("printPerformanceResult should complete without exceptions", true);
    }
    
    @Test
    public void testPerformanceComparisonFormatting() {
        // Test performance comparison formatting
        TestUtils.printPerformanceComparison("Baseline", 2_000_000_000L,
                                             "Optimized", 1_000_000_000L);
        // Should print speedup and improvement percentage
        
        // This test mainly verifies no exceptions are thrown
        assertTrue("printPerformanceComparison should complete without exceptions", true);
    }
    
    @Test
    public void testIsPerformanceTestsDisabled() {
        // Test the boolean check method
        boolean disabled = TestUtils.isPerformanceTestsDisabled();
        
        // The value depends on the system property
        String sysProp = System.getProperty(TestUtils.SKIP_PERFORMANCE_PROPERTY, "false");
        assertEquals("isPerformanceTestsDisabled should match system property",
                     Boolean.parseBoolean(sysProp), disabled);
    }
    
    @Test(expected = AssertionError.class)
    public void testPrivateConstructor() {
        // Verify that the utility class cannot be instantiated
        try {
            // Use reflection to access private constructor
            java.lang.reflect.Constructor<TestUtils> constructor = 
                TestUtils.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        } catch (Exception e) {
            // Unwrap the InvocationTargetException to get the actual AssertionError
            if (e.getCause() instanceof AssertionError) {
                throw (AssertionError) e.getCause();
            }
            throw new RuntimeException("Unexpected exception", e);
        }
    }
}