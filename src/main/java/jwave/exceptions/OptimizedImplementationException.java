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

/**
 * Exception thrown when optimized implementations cannot be loaded or instantiated.
 * 
 * <p>This exception is used to indicate specific problems with optimized implementations
 * such as missing classes, incompatible interfaces, or instantiation failures.</p>
 * 
 * <p><b>Common Scenarios:</b></p>
 * <ul>
 *   <li>Optimized classes not found in classpath</li>
 *   <li>Optimized classes don't implement expected interfaces</li>
 *   <li>Optimized classes missing default constructors</li>
 *   <li>Optimized classes fail during initialization</li>
 * </ul>
 * 
 * @author Stephen Romano
 * @see jwave.transforms.MODWTTransform#createOptimized(jwave.transforms.wavelets.Wavelet)
 */
public class OptimizedImplementationException extends Exception {
    
    /**
     * The type of optimization failure that occurred.
     */
    public enum FailureType {
        /** Required optimized classes not found in classpath */
        CLASS_NOT_FOUND,
        
        /** Optimized classes don't implement expected interfaces */
        INCOMPATIBLE_INTERFACE,
        
        /** Optimized classes missing required constructors */
        MISSING_CONSTRUCTOR,
        
        /** Cannot access optimized class constructors */
        ACCESS_DENIED,
        
        /** Error during optimized class instantiation */
        INSTANTIATION_FAILED,
        
        /** Error during optimized class initialization */
        INITIALIZATION_FAILED,
        
        /** Unexpected error during optimization attempt */
        UNKNOWN
    }
    
    private final FailureType failureType;
    private final String[] missingClasses;
    
    /**
     * Constructs an OptimizedImplementationException with the specified failure type and message.
     * 
     * @param failureType the type of optimization failure
     * @param message the detail message
     */
    public OptimizedImplementationException(FailureType failureType, String message) {
        super(message);
        this.failureType = failureType;
        this.missingClasses = null;
    }
    
    /**
     * Constructs an OptimizedImplementationException with the specified failure type, message, and cause.
     * 
     * @param failureType the type of optimization failure
     * @param message the detail message
     * @param cause the underlying cause
     */
    public OptimizedImplementationException(FailureType failureType, String message, Throwable cause) {
        super(message, cause);
        this.failureType = failureType;
        this.missingClasses = null;
    }
    
    /**
     * Constructs an OptimizedImplementationException for missing classes.
     * 
     * @param message the detail message
     * @param missingClasses the names of the missing classes
     * @param cause the underlying cause
     */
    public OptimizedImplementationException(String message, String[] missingClasses, Throwable cause) {
        super(message, cause);
        this.failureType = FailureType.CLASS_NOT_FOUND;
        this.missingClasses = missingClasses != null ? missingClasses.clone() : null;
    }
    
    /**
     * Gets the type of optimization failure.
     * 
     * @return the failure type
     */
    public FailureType getFailureType() {
        return failureType;
    }
    
    /**
     * Gets the names of missing classes (if applicable).
     * 
     * @return array of missing class names, or null if not applicable
     */
    public String[] getMissingClasses() {
        return missingClasses != null ? missingClasses.clone() : null;
    }
    
    /**
     * Checks if this exception indicates that fallback to standard implementations is recommended.
     * 
     * @return true if fallback is recommended, false otherwise
     */
    public boolean isFallbackRecommended() {
        return failureType == FailureType.CLASS_NOT_FOUND || 
               failureType == FailureType.INCOMPATIBLE_INTERFACE;
    }
}