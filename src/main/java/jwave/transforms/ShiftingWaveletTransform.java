/**
 * Shifting Wavelet Transform shifts a wavelet of smallest wavelength over the
 * input array, then by the double wavelength, .., and so on.
 *
 * @date 15.02.2016 23:12:55
 * <p>
 * ShiftingWaveletTransform.java
 */
package jwave.transforms;

import jwave.exceptions.JWaveException;
import jwave.transforms.wavelets.Wavelet;

/**
 * Shifting Wavelet Transform shifts a wavelet of smallest wavelength over the
 * input array, then by the double wavelength, .., and so on.
 *
 *
 * @date 15.02.2016 23:12:55
 */
public class ShiftingWaveletTransform extends WaveletTransform {

    /**
     * Constructor taking Wavelet object for performing the transform.
     *
     *
     * @date 15.02.2016 23:12:55
     */
    public ShiftingWaveletTransform(Wavelet wavelet) {
        super(wavelet);
    } // ShiftingWaveletTransform

    /**
     * Forward method that uses strictly the abilities of an orthogonal transform.
     *
     *
     * @date 15.02.2016 23:12:55 (non-Javadoc)
     * @see jwave.transforms.BasicTransform#forward(double[])
     */
    @Override
    public double[] forward(double[] arrTime) throws JWaveException {

        int length = arrTime.length;

        int div = 2;
        int odd = length % div; // if odd == 1 => steps * 2 + odd else steps * 2

        double[] arrHilb = new double[length];
        System.arraycopy(arrTime, 0, arrHilb, 0, length);

        while (div <= length) {

            int splits = length / div; // cuts the digits == round down to full

            // doing smallest wavelength of div by no of steps

            for (int s = 0; s < splits; s++) {

                double[] arrDiv = new double[div];
                double[] arrRes = null;

                System.arraycopy(arrHilb, s * div + 0, arrDiv, 0, div);

                arrRes = _wavelet.forward(arrDiv, div);

                System.arraycopy(arrRes, 0, arrHilb, s * div + 0, div);

            } // s

            div *= 2;

        } // while

        if (odd == 1)
            arrHilb[length - 1] = arrTime[length - 1];

        return arrHilb;

    } // forward

    /**
     * Reverse method that uses strictly the abilities of an orthogonal transform.
     *
     *
     * @date 15.02.2016 23:12:55 (non-Javadoc)
     * @see jwave.transforms.BasicTransform#reverse(double[])
     */
    @Override
    public double[] reverse(double[] arrHilb) throws JWaveException {

        int length = arrHilb.length;

        int div = 0;
        if (length % 2 == 0)
            div = length;
        else {
            div = length / 2; // 2 = 4.5 => 4
            div *= 2; // 4 * 2 = 8
        }

        int odd = length % div; // if odd == 1 => steps * 2 + odd else steps * 2

        double[] arrTime = new double[length];
        System.arraycopy(arrHilb, 0, arrTime, 0, length);

        while (div >= 2) {

            int splits = length / div; // cuts the digits == round down to full

            // doing smallest wavelength of div by no of steps

            for (int s = 0; s < splits; s++) {

                double[] arrDiv = new double[div];
                double[] arrRes = null;

                System.arraycopy(arrTime, s * div + 0, arrDiv, 0, div);

                arrRes = _wavelet.reverse(arrDiv, div);

                System.arraycopy(arrRes, 0, arrTime, s * div + 0, div);

            } // s

            div /= 2;

        } // while

        if (odd == 1)
            arrTime[length - 1] = arrHilb[length - 1];

        return arrTime;

    } // reverse

} // class
