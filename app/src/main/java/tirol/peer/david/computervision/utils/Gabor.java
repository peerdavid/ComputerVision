package tirol.peer.david.computervision.utils;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by david on 01.12.15.
 */
public class Gabor {

    private class GaborKernel{
        public Mat Even;
        public Mat Odd;
    }

    private Map<Double, GaborKernel> mGaborKernel;
    private Size mKernelSize = new Size(31,31);
    private double mLambda = 30;
    private double mSigma = 24;
    private double mGamma = 1;
    private double mPsi = 0;


    public Gabor(){
        mGaborKernel = new HashMap<>();
    }


    public Gabor(Size kernelSize, double lambda, double sigma, double gamma, double psi){
        this();

        mKernelSize = kernelSize;
        mLambda = lambda;
        mSigma = sigma;
        mGamma = gamma;
        mPsi = psi;
    }

    /**
     * Apply gabor filter on our current image
     * @param theta orientation of our convolution kernel
     * @return
     */
    public void applyGaborFilter(Mat image, double theta) {
        image.convertTo(image, CvType.CV_32F);

        // Do the gabor convolution
        Mat kernel = Imgproc.getGaborKernel(mKernelSize, mSigma, theta, mLambda, mGamma, mPsi, CvType.CV_32F);
        Imgproc.filter2D(image, image, -1, kernel);

        image.convertTo(image, CvType.CV_8UC1);
    }


    public void applyEnergyOfGabor(Mat image){
        double[] orientations = new double [] {
                0 * Math.PI / 4,
                1 * Math.PI / 4,
                2 * Math.PI / 4,
                3 * Math.PI / 4,
        };

        applyEnergyOfGabor(image, orientations);
    }


    public void applyEnergyOfGabor(Mat image, double[] orientations){
        image.convertTo(image, CvType.CV_32F);

        Mat energyOfGabor = new Mat(image.rows(), image.cols(), CvType.CV_32F);
        Mat tmp = new Mat(image.rows(), image.cols(), CvType.CV_32F);

        for(double theta : orientations){

            GaborKernel kernel = mGaborKernel.get(theta);

            // Cache kernel for a better performance
            if(kernel == null){
                kernel = calculateGaborKernel(theta);
                mGaborKernel.put(theta, kernel);
            }

            Mat evenF = new Mat();
            Mat oddF = new Mat();
            Imgproc.filter2D(image, evenF, CvType.CV_32F, kernel.Even);
            Imgproc.filter2D(image, oddF, CvType.CV_32F, kernel.Odd);

            Core.pow(evenF, 2, evenF);
            Core.pow(oddF, 2, oddF);
            Core.add(evenF, oddF, tmp);
            Core.sqrt(tmp, tmp);
            Core.add(energyOfGabor, tmp, energyOfGabor);

            Core.normalize(energyOfGabor, energyOfGabor, 0, 255, Core.NORM_MINMAX);
        }

        energyOfGabor.convertTo(image, CvType.CV_8UC1);
    }


    private GaborKernel calculateGaborKernel(double theta){
        GaborKernel kernel = new GaborKernel();

        kernel.Odd = Imgproc.getGaborKernel(mKernelSize, mSigma, theta, mLambda, mGamma, mPsi + (Math.PI / 2), CvType.CV_32F);
        kernel.Even = Imgproc.getGaborKernel(mKernelSize, mSigma, theta, mLambda, mGamma, mPsi, CvType.CV_32F);

        return kernel;
    }
}
