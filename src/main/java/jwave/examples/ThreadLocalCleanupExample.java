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
package jwave.examples;

import jwave.operations.SIMDComplexOperations;
import jwave.datatypes.natives.Complex;

/**
 * Example demonstrating proper ThreadLocal cleanup patterns to prevent memory leaks
 * in web applications, application servers, and thread pool environments.
 * 
 * @author Stephen Romano
 */
public class ThreadLocalCleanupExample {

    public static void main(String[] args) {
        System.out.println("=== ThreadLocal Cleanup Patterns for Production Applications ===\n");
        
        demonstrateManualCleanup();
        demonstrateAutomaticCleanup();
        demonstrateWebApplicationPattern();
        demonstrateThreadPoolPattern();
        demonstrateMonitoringPattern();
    }
    
    /**
     * Manual cleanup pattern - explicit control over ThreadLocal lifecycle.
     */
    private static void demonstrateManualCleanup() {
        System.out.println("1. Manual Cleanup Pattern");
        System.out.println("=========================");
        
        SIMDComplexOperations ops = new SIMDComplexOperations();
        
        try {
            // Perform complex operations
            Complex[] array1 = createSampleArray(1000);
            Complex[] array2 = createSampleArray(1000);
            Complex[] result = new Complex[1000];
            
            ops.add(array1, array2, result, 1000);
            ops.multiply(array1, array2, result, 1000);
            
            System.out.println("  Operations completed successfully");
            System.out.println("  " + SIMDComplexOperations.getBufferStats());
            
        } finally {
            // CRITICAL: Always clean up ThreadLocal buffers
            SIMDComplexOperations.clearThreadBuffers();
            System.out.println("  ThreadLocal buffers cleaned up manually");
        }
        System.out.println();
    }
    
    /**
     * Automatic cleanup pattern using try-with-resources.
     */
    private static void demonstrateAutomaticCleanup() {
        System.out.println("2. Automatic Cleanup Pattern (Recommended)");
        System.out.println("==========================================");
        
        // Try-with-resources automatically handles cleanup
        try (SIMDComplexOperations.ThreadLocalCleaner cleaner = 
             SIMDComplexOperations.createThreadLocalCleaner()) {
            
            SIMDComplexOperations ops = new SIMDComplexOperations();
            
            Complex[] array1 = createSampleArray(2000);
            Complex[] array2 = createSampleArray(2000);
            Complex[] result = new Complex[2000];
            
            ops.subtract(array1, array2, result, 2000);
            ops.conjugate(array1, result, 2000);
            
            System.out.println("  Operations completed successfully");
            System.out.println("  " + SIMDComplexOperations.getBufferStats());
            
            // Cleanup happens automatically when leaving try block
        }
        System.out.println("  ThreadLocal buffers cleaned up automatically");
        System.out.println();
    }
    
    /**
     * Web application cleanup pattern using servlet filters.
     */
    private static void demonstrateWebApplicationPattern() {
        System.out.println("3. Web Application Pattern");
        System.out.println("==========================");
        
        System.out.println("  Example Servlet Filter:");
        System.out.println("  ```java");
        System.out.println("  @WebFilter(\"/*\")");
        System.out.println("  public class JWaveCleanupFilter implements Filter {");
        System.out.println("      @Override");
        System.out.println("      public void doFilter(ServletRequest request, ServletResponse response,");
        System.out.println("                           FilterChain chain) throws IOException, ServletException {");
        System.out.println("          try {");
        System.out.println("              chain.doFilter(request, response);");
        System.out.println("          } finally {");
        System.out.println("              // Prevent memory leaks in thread pools");
        System.out.println("              SIMDComplexOperations.clearThreadBuffers();");
        System.out.println("          }");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println("  ```");
        System.out.println();
        
        // Simulate request processing
        simulateWebRequest();
    }
    
    /**
     * Thread pool cleanup pattern.
     */
    private static void demonstrateThreadPoolPattern() {
        System.out.println("4. Thread Pool Pattern");
        System.out.println("======================");
        
        System.out.println("  Example Thread Pool Task:");
        System.out.println("  ```java");
        System.out.println("  public class ComplexProcessingTask implements Runnable {");
        System.out.println("      @Override");
        System.out.println("      public void run() {");
        System.out.println("          try (SIMDComplexOperations.ThreadLocalCleaner cleaner =");
        System.out.println("                   SIMDComplexOperations.createThreadLocalCleaner()) {");
        System.out.println("              ");
        System.out.println("              // Perform complex operations");
        System.out.println("              SIMDComplexOperations ops = new SIMDComplexOperations();");
        System.out.println("              // ... operations ...");
        System.out.println("              ");
        System.out.println("              // Automatic cleanup when task completes");
        System.out.println("          }");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println("  ```");
        System.out.println();
    }
    
    /**
     * Memory monitoring pattern for production applications.
     */
    private static void demonstrateMonitoringPattern() {
        System.out.println("5. Memory Monitoring Pattern");
        System.out.println("============================");
        
        // Allocate some buffers
        SIMDComplexOperations ops = new SIMDComplexOperations();
        Complex[] array1 = createSampleArray(3000);
        Complex[] array2 = createSampleArray(3000);
        Complex[] result = new Complex[3000];
        
        ops.multiplyScalar(array1, 2.0, result, 3000);
        
        // Monitor memory usage
        boolean hasBuffers = SIMDComplexOperations.hasThreadLocalBuffers();
        long memoryUsage = SIMDComplexOperations.estimateThreadLocalMemoryUsage();
        
        System.out.println("  Current thread buffer status:");
        System.out.println("    Has ThreadLocal buffers: " + hasBuffers);
        System.out.println("    Estimated memory usage: " + (memoryUsage / 1024) + " KB");
        System.out.println("    Detailed stats: " + SIMDComplexOperations.getDetailedBufferStats(3000));
        
        // Production monitoring recommendations
        System.out.println("  ");
        System.out.println("  Production Monitoring Recommendations:");
        System.out.println("  - Monitor hasThreadLocalBuffers() across thread pools");
        System.out.println("  - Alert if memory usage exceeds expected thresholds");
        System.out.println("  - Use JVM memory profilers to detect ThreadLocal leaks");
        System.out.println("  - Implement health checks that verify cleanup");
        
        // Clean up for demonstration
        SIMDComplexOperations.clearThreadBuffers();
        System.out.println("  - Buffers cleaned up for demonstration");
        System.out.println();
    }
    
    /**
     * Simulate web request processing with proper cleanup.
     */
    private static void simulateWebRequest() {
        System.out.println("  Simulating web request processing...");
        
        try {
            // Simulate request processing that uses complex operations
            SIMDComplexOperations ops = new SIMDComplexOperations();
            Complex[] data = createSampleArray(500);
            Complex[] result = new Complex[500];
            
            // Simulate some DSP processing in a web service
            ops.magnitude(data, new double[500], 500);
            
            System.out.println("    Request processed successfully");
            
        } finally {
            // This would typically be in a servlet filter
            SIMDComplexOperations.clearThreadBuffers();
            System.out.println("    ThreadLocal cleanup completed (prevents memory leaks)");
        }
        System.out.println();
    }
    
    private static Complex[] createSampleArray(int size) {
        Complex[] array = new Complex[size];
        for (int i = 0; i < size; i++) {
            double real = Math.sin(2 * Math.PI * i / 100.0);
            double imag = Math.cos(2 * Math.PI * i / 100.0);
            array[i] = new Complex(real, imag);
        }
        return array;
    }
}