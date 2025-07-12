# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Compile the project
mvn clean compile

# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=MODWTTransformTest

# Run performance benchmarks (disabled by default in CI)
mvn test -Dtest=*PerformanceTest -DenablePerformanceTests=true

# Package as JAR
mvn package

# Skip tests during packaging
mvn package -DskipTests

# Run the command-line interface
java -cp target/jwave-pro-2.0.0-SNAPSHOT.jar jwave.JWave "Fast Wavelet Transform" "Haar 1"
```

## Architecture Overview

JWave-Pro is a high-performance wavelet transform library with the following key architectural patterns:

1. **Transform Hierarchy**: All transforms extend `BasicTransform` and are accessed through the `Transform` facade class
2. **Wavelet Hierarchy**: All wavelets extend `Wavelet` base class, organized by families (orthogonal, biorthogonal, continuous)
3. **Memory Management**: `ArrayBufferPool` provides thread-local buffer pooling to reduce GC pressure
4. **Parallel Processing**: `ParallelWaveletPacketTransform` uses ForkJoinPool for multi-core utilization

### Key Components

- **Transform Types**: Located in `/src/main/java/jwave/transforms/`
  - FastWaveletTransform (FWT) - Standard DWT, requires power-of-2 lengths
  - WaveletPacketTransform (WPT) - Full decomposition tree
  - MODWTTransform - Shift-invariant, handles arbitrary lengths
  - ContinuousWaveletTransform (CWT) - Time-frequency analysis
  - AncientEgyptianDecomposition - Wrapper for odd-length signals

- **Wavelets**: Located in `/src/main/java/jwave/transforms/wavelets/`
  - 50+ wavelets organized by family (haar/, daubechies/, symlets/, etc.)
  - Each wavelet provides forward/reverse filter coefficients

- **Data Types**: Located in `/src/main/java/jwave/datatypes/`
  - Complex number support for CWT
  - Block, Line, Space for multi-dimensional data

## Development Guidelines

### Streaming Transform Architecture
The streaming package (`/src/main/java/jwave/transforms/streaming/`) provides infrastructure for incremental transforms:
- `CircularBuffer` - Fixed-size circular buffer for efficient data management
- `StreamingTransform` - Interface for all streaming transforms
- `AbstractStreamingTransform` - Base class with common functionality
- `StreamingTransformListener` - Event-driven updates
- `StreamingTransformConfig` - Configuration with update strategies

### Adding New Transforms
1. Extend `BasicTransform` abstract class
2. Implement `forward()` and `reverse()` methods
3. Add corresponding test class extending `JWaveTestCase`
4. Consider adding parallel/pooled variants for performance

### Adding New Wavelets
1. Extend appropriate base class (`Wavelet` for orthogonal, `BiOrthogonalWavelet` for biorthogonal)
2. Implement `_buildOrthonormalSpace()` to provide filter coefficients
3. Add to `WaveletBuilder` for easy instantiation
4. Add comprehensive unit tests including reconstruction verification

### Signal Length Requirements
- **FWT/WPT**: Must be power of 2 (2, 4, 8, 16, ...)
- **MODWT**: Any length supported
- **CWT**: Any length supported
- **For odd lengths**: Use MODWT or wrap with AncientEgyptianDecomposition

### Testing Approach
- All transforms must pass perfect reconstruction tests
- Cross-validate with Apache Commons Math where applicable
- Performance tests are opt-in via `-DenablePerformanceTests=true`
- Thread safety tests verify concurrent usage