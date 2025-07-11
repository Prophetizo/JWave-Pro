# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Maven Commands
```bash
# Build the project
mvn clean compile

# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=MODWTTransformTest
mvn test -Dtest=FastWaveletTransformTest

# Package as JAR
mvn package

# Skip tests during build
mvn package -DskipTests

# Update dependencies
mvn dependency:resolve -U
```

### Running JWave
```bash
# After building, run the command-line interface
java -cp target/jwave-2.0.0.jar jwave.JWave "Fast Wavelet Transform" "Daubechies 20"
java -cp target/jwave-2.0.0.jar jwave.JWave "Discrete Fourier Transform"
```

## Architecture Overview

### Core Design Pattern
JWave uses a Strategy/Builder pattern with a Transform facade:
- `Transform` - Main entry point for all transforms
- `BasicTransform` - Abstract base class for transform implementations
- `TransformBuilder` - Factory for creating transform instances
- `WaveletBuilder` - Factory for creating wavelet instances

### Transform Types
1. **DiscreteFourierTransform (DFT)** - Basic Fourier transform
2. **FastFourierTransform (FFT)** - Optimized Fourier using Cooley-Tukey
3. **FastWaveletTransform (FWT)** - Standard pyramid algorithm (requires 2^n length)
4. **WaveletPacketTransform (WPT)** - Full decomposition tree
5. **MODWTTransform** - Maximal Overlap DWT (handles arbitrary length, shift-invariant)
6. **ContinuousWaveletTransform (CWT)** - Time-frequency analysis (v2.0.0)
7. **AncientEgyptianDecomposition** - Wrapper for handling odd-length signals with FWT/WPT
8. **ShiftingWaveletTransform** - Alternative shift-based implementation

### Package Structure
- `jwave.transforms/` - Core transform implementations
- `jwave.transforms.wavelets/` - ~50 wavelet implementations organized by family:
  - `haar/` - Haar wavelets
  - `daubechies/` - Daubechies 2-20
  - `symlets/` - Symlets 2-20
  - `coiflet/` - Coiflets 1-5
  - `biorthogonal/` - BiOrthogonal wavelets
  - `continuous/` - CWT wavelets (Morlet, Mexican Hat, Paul, DOG, Meyer) - Added in v2.0.0
- `jwave.datatypes/` - Data structures (Block, Line, Space, Complex)
- `jwave.compressions/` - Compression utilities
- `jwave.exceptions/` - Custom exception hierarchy

### Key Implementation Details

#### Multi-dimensional Support
All transforms (except MODWT) support 1D, 2D, and 3D operations through overloaded methods:
- `forward(double[])` / `reverse(double[])` - 1D
- `forward(double[][])` / `reverse(double[][])` - 2D
- `forward(double[][][])` / `reverse(double[][][])` - 3D

#### Signal Length Requirements
- **FWT/WPT**: Require power-of-2 length (2, 4, 8, 16, ..., 1024, ...)
- **MODWT**: Handles arbitrary length signals without padding
- **AncientEgyptianDecomposition**: Wrapper that enables FWT/WPT on odd-length signals

#### MODWT Specifics
- Shift-invariant (translated signals produce translated coefficients)
- Energy preserving (variance preservation property)
- Perfect reconstruction within numerical precision
- Supports multi-level decomposition
- Currently 1D only
- Has filter caching optimization for performance

### Testing Approach
- Base test class: `jwave.Base` provides assertion helpers
- Test categories: unit tests, performance tests, thread safety tests
- Use JUnit 4 framework
- Tests located in `/src/test/java/jwave/`

### Common Development Tasks

#### Adding a New Wavelet
1. Create class in appropriate package under `jwave.transforms.wavelets/`
2. Extend `Wavelet` abstract class
3. Implement `matDecompose()` and `matReconstruct()` methods
4. Add to `WaveletBuilder` for factory access
5. Create unit test in corresponding test package

#### Adding a New Transform
1. Create class in `jwave.transforms/`
2. Extend `BasicTransform` abstract class
3. Implement forward/reverse methods for 1D, 2D, 3D
4. Add to `TransformBuilder` if needed
5. Create comprehensive unit tests

### Important Notes
- Java 21 is required (uses Java 21 features)
- No external runtime dependencies (only JUnit for testing)
- All wavelets use normalized coefficients
- Different implementations may produce different intermediate results due to basis construction, but compression rates and final results remain equivalent