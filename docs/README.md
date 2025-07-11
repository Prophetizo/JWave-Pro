# JWave Documentation

This directory contains technical documentation, implementation guides, and development resources for the JWave wavelet library.

## Documentation Overview

### Development Guides

- **[CLAUDE.md](CLAUDE.md)** - Guidance for Claude Code (claude.ai/code) when working with the JWave repository. Contains build commands, architecture overview, and common development tasks.

### Transform Documentation

#### Continuous Wavelet Transform (CWT)

- **[CWT_README.md](CWT_README.md)** - Comprehensive guide to the Continuous Wavelet Transform implementation, including theory, available wavelets, usage examples, and performance considerations.

#### MODWT Documentation

The Maximal Overlap Discrete Wavelet Transform (MODWT) implementation is thoroughly documented across several files:

- **[MODWT_README.md](MODWT_README.md)** - User-facing documentation explaining MODWT theory, advantages, and usage examples.

- **[MODWT_IMPLEMENTATION_SUMMARY.md](MODWT_IMPLEMENTATION_SUMMARY.md)** - Summary of the MODWT implementation work completed, including test results and performance metrics.

- **[MODWT_Level_Limits_Analysis.md](MODWT_Level_Limits_Analysis.md)** - Comprehensive analysis of decomposition level limits, memory usage, and performance characteristics with recommendations for different signal lengths.

- **[MODWT_FILTER_CACHE_IMPLEMENTATION_GUIDE.md](MODWT_FILTER_CACHE_IMPLEMENTATION_GUIDE.md)** - Technical deep-dive into the filter caching optimization, including design decisions and implementation details.

### Future Development

- **[FINANCIAL_ROADMAP.md](FINANCIAL_ROADMAP.md)** - **PROPOSED FEATURES (Not Yet Implemented)** - Comprehensive plan for adding financial-specific wavelet analysis capabilities, including specialized wavelets for market microstructure analysis and GPU acceleration.

## Contributing Documentation

When adding new documentation:

1. Use descriptive filenames that indicate the content
2. Add a clear header explaining the document's purpose
3. Update this README.md to include your new documentation
4. For implementation guides, include code examples and test cases
5. For proposals, clearly mark them as "PROPOSED" or "DRAFT"

## Documentation Standards

- Use Markdown formatting
- Include code examples with proper syntax highlighting
- Add diagrams where they help explain complex concepts
- Keep technical documentation separate from user guides
- Date-stamp major updates in implementation documents