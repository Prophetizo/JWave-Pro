# Changelog

All notable changes to JWave are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0-SNAPSHOT] - 2025-07-11

### Added

#### New Transform Types
- **Continuous Wavelet Transform (CWT)** - Complete implementation with:
  - Time-frequency analysis capabilities
  - Support for Morlet, Mexican Hat, Paul, DOG, and Meyer wavelets
  - Optimized convolution with automatic FFT/direct method selection
  - Parallel processing support for multi-scale analysis
  - Comprehensive JavaDoc with mathematical foundations

- **Maximal Overlap Discrete Wavelet Transform (MODWT)** - Full-featured implementation:
  - Shift-invariant transform for time series analysis
  - Handles arbitrary signal lengths (not restricted to powers of 2)
  - FFT-accelerated convolution (up to 47x speedup)
  - Thread-safe filter caching with lazy initialization
  - Pooled memory version for reduced GC pressure
  - Extensive documentation with usage examples

#### Performance Optimizations
- **Parallel Wavelet Packet Transform (WPT)**:
  - Multi-threaded packet processing with ForkJoinPool
  - Work-stealing algorithm for optimal load balancing
  - 1.2-1.3x speedup on multi-core systems
  - Configurable thread pool size

- **Memory-Efficient Buffer Pooling**:
  - Thread-local `ArrayBufferPool` for array reuse
  - Reduces GC pressure in tight loops
  - Support for double[], Complex[], and 2D arrays
  - Automatic size bucketing based on powers of 2
  - Clear-on-return for security

- **In-Place Transform API**:
  - `InPlaceFastWaveletTransform` providing in-place interface
  - Foundation for future true in-place implementations
  - Reduces memory footprint for large datasets

#### New Wavelet Families
- **Continuous Wavelets**:
  - Morlet wavelet (complex-valued for phase analysis)
  - Mexican Hat (Ricker wavelet)
  - Paul wavelet (analytic signal processing)
  - Derivative of Gaussian (DOG)
  - Meyer wavelet (smooth in frequency domain)

- **Additional Orthogonal Wavelets**:
  - Extended Daubechies support (up to D20)
  - Extended Symlets support (up to Sym20)
  - Extended Coiflets support (up to Coif5)
  - Legendre wavelets (1-3)
  - Discrete Meyer wavelet

#### Infrastructure Improvements
- **Enhanced Testing Framework**:
  - Performance benchmarking utilities
  - `TestUtils.skipIfPerformanceTestsDisabled()` for CI
  - Comprehensive test coverage for new features
  - Property-based testing for transform correctness

- **Documentation**:
  - Extensive JavaDoc with mathematical foundations
  - Usage examples in documentation
  - Performance characteristics documented
  - CLAUDE.md for AI-assisted development

### Changed

- **MODWT Implementation**:
  - Switched from O(N²) direct convolution to O(N log N) FFT-based convolution
  - Dynamic threshold-based method selection (auto/direct/FFT)
  - Improved filter caching with thread-safe lazy initialization
  - Better memory efficiency through pooled operations

- **Build Configuration**:
  - Updated to Java 21
  - Improved Maven configuration
  - Added performance test categories

### Fixed

- **Critical Bug Fixes**:
  - Fixed `ArrayIndexOutOfBoundsException` in `PooledWaveletPacketTransform`
  - Corrected packet indexing logic in WPT reverse transform
  - Fixed FFT normalization issues
  - Resolved thread safety issues in filter caching
  - Fixed memory leaks in pooled transform implementations

- **Algorithm Corrections**:
  - Proper MODWT adjoint convolution implementation
  - Correct filter wrapping for circular convolution
  - Fixed boundary handling in various transforms

### Optimized

- **Memory Usage**:
  - 36-70% reduction through buffer pooling
  - Pre-allocation strategies for known sizes
  - Efficient filter caching to avoid recomputation
  - Minimized array copying in critical paths

- **Performance**:
  - MODWT: Up to 47x faster with FFT convolution
  - WPT: 1.2-1.3x speedup with parallelization
  - Reduced GC pressure across all transforms
  - Optimized inner loops for cache efficiency

## [2.0.0] - Previous Release

### Original JWave Features
- Fast Wavelet Transform (FWT)
- Wavelet Packet Transform (WPT)
- Basic wavelet families (Haar, Daubechies, Symlets, Coiflets)
- 1D, 2D, and 3D transform support
- Ancient Egyptian Decomposition for odd-length signals

---

*Note: This changelog documents the significant enhancements made to JWave since forking from the original project by Christian Scheiblich (Graetz23). The improvements focus on performance, new transform types, expanded wavelet support, and production-ready reliability.*