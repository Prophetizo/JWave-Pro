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

/**
 * Utility class for safe class loading and instantiation of optimized implementations.
 * 
 * <p>This class provides robust methods for loading and instantiating classes at runtime,
 * with comprehensive error handling and detailed failure reporting. It's specifically
 * designed for loading optimized implementations with fallback capabilities.</p>
 * 
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Type-safe class loading with interface validation</li>
 *   <li>Detailed error reporting with specific failure types</li>
 *   <li>Consistent exception handling across all loading scenarios</li>
 *   <li>Support for both single and multiple class loading</li>
 * </ul>
 * 
 * @author Stephen Romano
 * @see OptimizedImplementationException
 */
public final class ClassLoadingUtils {
    
    private ClassLoadingUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Loads and instantiates an optimized implementation class with type safety.
     * 
     * <p>This method provides a robust way to load optimized implementations at runtime
     * with comprehensive error handling and type validation.</p>
     * 
     * <p><b>Loading Process:</b></p>
     * <ol>
     *   <li>Load class using Class.forName()</li>
     *   <li>Verify class implements/extends the expected type</li>
     *   <li>Instantiate using default constructor</li>
     *   <li>Cast to expected type safely</li>
     * </ol>
     * 
     * <p><b>Example Usage:</b></p>
     * <pre>{@code
     * try {
     *     FastFourierTransform fft = ClassLoadingUtils.loadOptimizedImplementation(
     *         "jwave.transforms.OptimizedFastFourierTransform", 
     *         FastFourierTransform.class
     *     );
     *     // Use the optimized FFT
     * } catch (OptimizedImplementationException e) {
     *     // Fall back to standard implementation
     *     FastFourierTransform fft = new FastFourierTransform();
     * }
     * }</pre>
     * 
     * @param <T> the expected type of the implementation
     * @param className the fully qualified class name to load
     * @param expectedType the class/interface the loaded class must implement/extend
     * @return instance of the loaded and instantiated class
     * @throws OptimizedImplementationException if class loading, validation, or instantiation fails
     */
    @SuppressWarnings("unchecked")
    public static <T> T loadOptimizedImplementation(String className, Class<T> expectedType) 
            throws OptimizedImplementationException {
        
        try {
            // Load the class
            Class<?> loadedClass = Class.forName(className);
            
            // Verify type compatibility
            if (!expectedType.isAssignableFrom(loadedClass)) {
                String expectedTypeName = expectedType.isInterface() ? "implement" : "extend";
                throw new OptimizedImplementationException(
                    OptimizedImplementationException.FailureType.INCOMPATIBLE_INTERFACE,
                    String.format("Class %s does not %s %s", className, expectedTypeName, expectedType.getName())
                );
            }
            
            // Instantiate the class
            return (T) loadedClass.getDeclaredConstructor().newInstance();
            
        } catch (ClassNotFoundException e) {
            throw new OptimizedImplementationException(
                OptimizedImplementationException.FailureType.CLASS_NOT_FOUND,
                "Optimized implementation class not found: " + className, e);
        } catch (NoSuchMethodException e) {
            throw new OptimizedImplementationException(
                OptimizedImplementationException.FailureType.MISSING_CONSTRUCTOR,
                "Optimized implementation missing default constructor: " + className, e);
        } catch (InstantiationException e) {
            throw new OptimizedImplementationException(
                OptimizedImplementationException.FailureType.INSTANTIATION_FAILED,
                "Cannot instantiate optimized implementation: " + className, e);
        } catch (IllegalAccessException e) {
            throw new OptimizedImplementationException(
                OptimizedImplementationException.FailureType.ACCESS_DENIED,
                "Cannot access optimized implementation constructor: " + className, e);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new OptimizedImplementationException(
                OptimizedImplementationException.FailureType.INITIALIZATION_FAILED,
                "Optimized implementation constructor failed: " + className, e);
        } catch (ClassCastException e) {
            throw new OptimizedImplementationException(
                OptimizedImplementationException.FailureType.INCOMPATIBLE_INTERFACE,
                "Class " + className + " cannot be cast to " + expectedType.getName(), e);
        } catch (OptimizedImplementationException e) {
            // Re-throw our specific exceptions without wrapping
            throw e;
        } catch (Exception e) {
            throw new OptimizedImplementationException(
                OptimizedImplementationException.FailureType.UNKNOWN,
                "Unexpected error loading optimized implementation: " + className, e);
        }
    }
    
    /**
     * Loads multiple optimized implementation classes and provides detailed error reporting.
     * 
     * <p>This method is designed for scenarios where multiple related optimized implementations
     * need to be loaded together. It provides enhanced error messages that include information
     * about all expected classes.</p>
     * 
     * <p><b>Enhanced Error Handling:</b></p>
     * <ul>
     *   <li>Lists all expected class names in error messages</li>
     *   <li>Provides context about which specific class failed to load</li>
     *   <li>Uses OptimizedImplementationException with missing classes array</li>
     * </ul>
     * 
     * <p><b>Example Usage:</b></p>
     * <pre>{@code
     * String[] classNames = {
     *     "jwave.transforms.OptimizedFastFourierTransform",
     *     "jwave.transforms.wavelets.OptimizedWaveletOperations"
     * };
     * 
     * try {
     *     FastFourierTransform fft = ClassLoadingUtils.loadOptimizedImplementationWithContext(
     *         classNames[0], FastFourierTransform.class, classNames
     *     );
     *     WaveletOperations ops = ClassLoadingUtils.loadOptimizedImplementationWithContext(
     *         classNames[1], WaveletOperations.class, classNames
     *     );
     * } catch (OptimizedImplementationException e) {
     *     // Error message includes context about all expected classes
     * }
     * }</pre>
     * 
     * @param <T> the expected type of the implementation
     * @param className the fully qualified class name to load
     * @param expectedType the class/interface the loaded class must implement/extend
     * @param allExpectedClasses array of all related class names for error context
     * @return instance of the loaded and instantiated class
     * @throws OptimizedImplementationException if class loading, validation, or instantiation fails
     */
    public static <T> T loadOptimizedImplementationWithContext(String className, Class<T> expectedType, 
            String[] allExpectedClasses) throws OptimizedImplementationException {
        
        try {
            return loadOptimizedImplementation(className, expectedType);
            
        } catch (OptimizedImplementationException e) {
            // Enhanced error handling for missing classes
            if (e.getFailureType() == OptimizedImplementationException.FailureType.CLASS_NOT_FOUND) {
                throw new OptimizedImplementationException(
                    "Optimized implementation classes not found in classpath. " +
                    "Missing class: " + className + ". " +
                    "Expected classes: " + java.util.Arrays.toString(allExpectedClasses),
                    allExpectedClasses, e.getCause());
            }
            
            // Re-throw other types of exceptions as-is
            throw e;
        }
    }
}