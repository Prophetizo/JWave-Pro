package jwave.utils;

import static org.junit.Assume.assumeFalse;

/**
 * Shared utility methods for JWave test classes.
 * 
 * <p>This class provides common functionality used across multiple test files,
 * reducing code duplication and ensuring consistency.</p>
 * 
 * @author Stephen Romano
 * @since 2.1.0
 */
public final class TestUtils {
    
    /**
     * System property to control whether performance tests should be skipped.
     * Set to "true" to skip performance tests (useful in CI environments).
     */
    public static final String SKIP_PERFORMANCE_PROPERTY = "jwave.test.skipPerformance";
    
    /**
     * Cached value of the skip performance tests property.
     */
    private static final boolean SKIP_PERFORMANCE_TESTS = 
        Boolean.parseBoolean(System.getProperty(SKIP_PERFORMANCE_PROPERTY, "false"));
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private TestUtils() {
        throw new AssertionError("TestUtils should not be instantiated");
    }
    
    /**
     * Checks if performance tests should be skipped and uses JUnit's assumeFalse
     * to skip the test if necessary.
     * 
     * <p>Call this method at the beginning of any performance test method:</p>
     * <pre>{@code
     * @Test
     * public void testPerformance() {
     *     TestUtils.skipIfPerformanceTestsDisabled();
     *     // ... performance test code ...
     * }
     * }</pre>
     * 
     * <p>To skip performance tests, run Maven with:</p>
     * <pre>{@code
     * mvn test -Djwave.test.skipPerformance=true
     * }</pre>
     */
    public static void skipIfPerformanceTestsDisabled() {
        assumeFalse("Skipping performance test (jwave.test.skipPerformance=true)", 
                    SKIP_PERFORMANCE_TESTS);
    }
    
    /**
     * Checks if performance tests are disabled.
     * 
     * <p>Use this method when you need to conditionally execute code based on
     * whether performance tests are enabled, but don't want to skip the entire test.</p>
     * 
     * @return true if performance tests should be skipped, false otherwise
     */
    public static boolean isPerformanceTestsDisabled() {
        return SKIP_PERFORMANCE_TESTS;
    }
    
    /**
     * Prints a performance test result in a consistent format.
     * 
     * @param testName The name of the performance test
     * @param elapsedNanos The elapsed time in nanoseconds
     * @param iterations The number of iterations performed
     */
    public static void printPerformanceResult(String testName, long elapsedNanos, int iterations) {
        double elapsedMs = elapsedNanos / 1e6;
        double nsPerOperation = (double) elapsedNanos / iterations;
        
        System.out.printf("%s: %.3f ms for %d operations (%.2f ns/op)%n", 
                          testName, elapsedMs, iterations, nsPerOperation);
    }
    
    /**
     * Prints a performance comparison result.
     * 
     * @param baselineName The name of the baseline implementation
     * @param baselineNanos The baseline elapsed time in nanoseconds
     * @param optimizedName The name of the optimized implementation
     * @param optimizedNanos The optimized elapsed time in nanoseconds
     */
    public static void printPerformanceComparison(String baselineName, long baselineNanos,
                                                   String optimizedName, long optimizedNanos) {
        double baselineMs = baselineNanos / 1e6;
        double optimizedMs = optimizedNanos / 1e6;
        double speedup = (double) baselineNanos / optimizedNanos;
        double improvement = ((double) baselineNanos - optimizedNanos) / baselineNanos * 100;
        
        System.out.printf("%s: %.3f ms%n", baselineName, baselineMs);
        System.out.printf("%s: %.3f ms%n", optimizedName, optimizedMs);
        System.out.printf("Speedup: %.2fx%n", speedup);
        System.out.printf("Improvement: %.1f%%%n", improvement);
    }
}