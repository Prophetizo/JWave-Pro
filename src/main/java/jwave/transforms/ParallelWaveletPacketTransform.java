package jwave.transforms;

import jwave.transforms.wavelets.Wavelet;
import jwave.utils.ArrayBufferPool;
import jwave.exceptions.JWaveException;
import jwave.exceptions.JWaveFailure;
import java.util.Arrays;
import java.util.concurrent.*;

/**
 * Parallel implementation of Wavelet Packet Transform (WPT) that uses multiple threads
 * to process packets concurrently. This implementation provides significant speedup
 * for multi-level decompositions on multi-core systems.
 * 
 * <p>The parallelization strategy processes independent packets at each level
 * concurrently. Since packets at the same level don't depend on each other,
 * they can be computed in parallel.</p>
 * 
 * <p><b>Performance Characteristics:</b></p>
 * <ul>
 *   <li>Best speedup achieved for large signals with multiple decomposition levels</li>
 *   <li>Overhead of thread management may reduce benefits for small signals</li>
 *   <li>Uses ForkJoinPool for efficient work-stealing parallelism</li>
 *   <li>Combines with buffer pooling to minimize allocation overhead</li>
 * </ul>
 * 
 * @author Stephen Romano
 * @since 2.1.0
 */
public class ParallelWaveletPacketTransform extends PooledWaveletPacketTransform {
    
    /**
     * Minimum packet size threshold for parallel processing.
     * Below this size, the overhead of parallelization exceeds benefits.
     */
    private static final int MIN_PARALLEL_PACKET_SIZE = 64;
    
    /**
     * Minimum number of packets to justify parallel processing.
     * With batching, we can efficiently handle more packets.
     */
    private static final int MIN_PARALLEL_PACKETS = 8;
    
    /**
     * The thread pool used for parallel execution.
     * Uses ForkJoinPool for efficient work-stealing.
     */
    private final ForkJoinPool threadPool;
    
    /**
     * Whether to use the common pool or a custom pool.
     */
    private final boolean useCommonPool;
    
    /**
     * Constructor using the common ForkJoinPool.
     * 
     * @param wavelet The wavelet to use for the transform
     */
    public ParallelWaveletPacketTransform(Wavelet wavelet) {
        super(wavelet);
        this.threadPool = ForkJoinPool.commonPool();
        this.useCommonPool = true;
    }
    
    /**
     * Constructor with custom thread pool size.
     * 
     * @param wavelet The wavelet to use for the transform
     * @param parallelism The parallelism level (number of threads)
     */
    public ParallelWaveletPacketTransform(Wavelet wavelet, int parallelism) {
        super(wavelet);
        this.threadPool = new ForkJoinPool(parallelism);
        this.useCommonPool = false;
    }
    
    @Override
    public double[] forward(double[] arrTime, int level) throws JWaveException {
        if (!isBinary(arrTime.length))
            throw new JWaveFailure("ParallelWaveletPacketTransform#forward - array length is not 2^p: got " + arrTime.length);
            
        int maxLevel = calcExponent(arrTime.length);
        if (level < 0 || level > maxLevel)
            throw new JWaveFailure("ParallelWaveletPacketTransform#forward - invalid level");
            
        int length = arrTime.length;
        double[] arrHilb = Arrays.copyOf(arrTime, length);
        
        int k = length;
        int h = length;
        int transformWavelength = _wavelet.getTransformWavelength();
        int l = 0;
        
        while (h >= transformWavelength && l < level) {
            int packets = k / h; // Number of packets at this level
            
            // Decide whether to use parallel processing
            if (shouldUseParallel(h, packets)) {
                processLevelParallel(arrHilb, h, packets, true);
            } else {
                processLevelSequential(arrHilb, h, packets, true);
            }
            
            h = h >> 1;
            l++;
        }
        
        return arrHilb;
    }
    
    @Override
    public double[] reverse(double[] arrHilb, int level) throws JWaveException {
        if (!isBinary(arrHilb.length))
            throw new JWaveFailure("ParallelWaveletPacketTransform#reverse - array length is not 2^p");
            
        int maxLevel = calcExponent(arrHilb.length);
        if (level < 0 || level > maxLevel)
            throw new JWaveFailure("ParallelWaveletPacketTransform#reverse - invalid level: " + level);
            
        int length = arrHilb.length;
        double[] arrTime = Arrays.copyOf(arrHilb, length);
        
        int transformWavelength = _wavelet.getTransformWavelength();
        int k = arrTime.length;
        int h = transformWavelength;
        
        int steps = calcExponent(length);
        for (int l = level; l < steps; l++)
            h = h << 1;
            
        while (h <= arrTime.length && h >= transformWavelength) {
            int packets = k / h;
            
            // Decide whether to use parallel processing
            if (shouldUseParallel(h, packets)) {
                processLevelParallel(arrTime, h, packets, false);
            } else {
                processLevelSequential(arrTime, h, packets, false);
            }
            
            h = h << 1;
        }
        
        return arrTime;
    }
    
    /**
     * Determines whether parallel processing should be used based on packet size and count.
     * 
     * @param packetSize Size of each packet
     * @param packetCount Number of packets
     * @return true if parallel processing should be used
     */
    private boolean shouldUseParallel(int packetSize, int packetCount) {
        return packetSize >= MIN_PARALLEL_PACKET_SIZE && 
               packetCount >= MIN_PARALLEL_PACKETS;
    }
    
    /**
     * Process a level sequentially (fallback for small problems).
     */
    private void processLevelSequential(double[] data, int h, int packets, boolean forward) {
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        double[] iBuf = pool.borrowDoubleArray(h);
        
        try {
            for (int p = 0; p < packets; p++) {
                int offset = p * h;
                
                // Copy data to buffer
                System.arraycopy(data, offset, iBuf, 0, h);
                
                // Apply transform
                double[] oBuf = forward ? _wavelet.forward(iBuf, h) : _wavelet.reverse(iBuf, h);
                
                // Copy result back
                System.arraycopy(oBuf, 0, data, offset, h);
            }
        } finally {
            pool.returnDoubleArray(iBuf);
        }
    }
    
    /**
     * Process a level in parallel using ForkJoinPool with batching.
     */
    private void processLevelParallel(double[] data, int h, int packets, boolean forward) {
        // Use ForkJoinTask for efficient work distribution
        WPTLevelTask rootTask = new WPTLevelTask(data, h, 0, packets, forward);
        threadPool.invoke(rootTask);
    }
    
    /**
     * RecursiveAction for processing WPT packets with automatic work-stealing.
     */
    private class WPTLevelTask extends RecursiveAction {
        private static final int PACKETS_PER_TASK_THRESHOLD = 16; // Process up to 16 packets per task
        
        private final double[] data;
        private final int packetSize;
        private final int startPacket;
        private final int endPacket;
        private final boolean forward;
        
        WPTLevelTask(double[] data, int packetSize, int startPacket, int endPacket, boolean forward) {
            this.data = data;
            this.packetSize = packetSize;
            this.startPacket = startPacket;
            this.endPacket = endPacket;
            this.forward = forward;
        }
        
        @Override
        protected void compute() {
            int packetCount = endPacket - startPacket;
            
            // If small enough, process directly
            if (packetCount <= PACKETS_PER_TASK_THRESHOLD) {
                for (int p = startPacket; p < endPacket; p++) {
                    processPacket(data, packetSize, p, forward);
                }
            } else {
                // Split the work
                int mid = startPacket + packetCount / 2;
                WPTLevelTask leftTask = new WPTLevelTask(data, packetSize, startPacket, mid, forward);
                WPTLevelTask rightTask = new WPTLevelTask(data, packetSize, mid, endPacket, forward);
                
                // Fork-join execution
                invokeAll(leftTask, rightTask);
            }
        }
    }
    
    /**
     * Process a single packet. This method is thread-safe as each thread
     * works on a different region of the array and uses thread-local buffer pools.
     * 
     * @param data The data array
     * @param h Size of the packet
     * @param packetIndex Index of the packet to process
     * @param forward true for forward transform, false for reverse
     */
    private void processPacket(double[] data, int h, int packetIndex, boolean forward) {
        ArrayBufferPool pool = ArrayBufferPool.getInstance(); // Thread-local pool
        double[] iBuf = pool.borrowDoubleArray(h);
        
        try {
            int offset = packetIndex * h;
            
            // Copy data to buffer
            System.arraycopy(data, offset, iBuf, 0, h);
            
            // Apply transform
            double[] oBuf = forward ? _wavelet.forward(iBuf, h) : _wavelet.reverse(iBuf, h);
            
            // Copy result back
            System.arraycopy(oBuf, 0, data, offset, h);
            
        } finally {
            pool.returnDoubleArray(iBuf);
        }
    }
    
    /**
     * Performs a parallel forward transform with automatic level selection.
     * This is a convenience method that uses maximum decomposition level.
     * 
     * @param arrTime The input signal
     * @return The transformed coefficients
     */
    public double[] forwardWPT(double[] arrTime) throws JWaveException {
        int maxLevel = calcExponent(arrTime.length);
        return forward(arrTime, maxLevel);
    }
    
    /**
     * Performs a parallel reverse transform from a given level.
     * This is a convenience method matching forwardWPT.
     * 
     * @param arrHilb The wavelet coefficients
     * @return The reconstructed signal
     */
    public double[] reverseWPT(double[] arrHilb) throws JWaveException {
        int maxLevel = calcExponent(arrHilb.length);
        return reverse(arrHilb, maxLevel);
    }
    
    /**
     * Shutdown the thread pool if using a custom pool.
     * Should be called when the transform is no longer needed.
     */
    public void shutdown() {
        if (!useCommonPool) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Get the parallelism level of the thread pool.
     * 
     * @return The number of threads in the pool
     */
    public int getParallelism() {
        return threadPool.getParallelism();
    }
}