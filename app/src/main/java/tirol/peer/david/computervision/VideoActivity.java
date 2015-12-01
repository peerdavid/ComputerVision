package tirol.peer.david.computervision;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * http://www.jayrambhia.com/blog/beginning-android-opencv/
 */
public class VideoActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private Mat mRgba;
    private Mat mGray;
    private List<Mat> mSequence = new LinkedList<>();
    private CameraBridgeViewBase mOpenCvCameraView;
    private ImageView mImageView;

    private final static int FRAME_SIZE = 40;


    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.cameraView);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.enableView();

        mImageView = (ImageView) findViewById(R.id.xtView);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                computeAndViewXt();
            }
        });
    }


    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    @Override
    public void onResume()
    {
        super.onResume();
    }


    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
    }


    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = convertAndRotateFrame(inputFrame);
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_BGRA2GRAY);

        // Insert a clone into our sequence, otherwise every pic is the same...
        insertFrame(mGray.clone());

        // Paint a line for testing only
        Imgproc.line(mGray, new Point(0, 20), new Point(200, 20), new Scalar(80), 2);

        return mGray;
    }


    private Mat convertAndRotateFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        mRgba = inputFrame.rgba();
        Mat mRgbaT = mRgba.t();
        Core.flip(mRgba.t(), mRgbaT, 1);
        Imgproc.resize(mRgbaT, mRgbaT, mRgba.size());
        return mRgbaT;
    }


    private void insertFrame(Mat mat){
        if(mSequence.size() > FRAME_SIZE){
            mSequence.remove(mSequence.size() - 1);
        }

        mSequence.add(0, mat);
    }


    private void computeAndViewXt() {
        if(mSequence.isEmpty()){
            return;
        }

        Mat xtMat = getXtImageForY(20);
        //applyGabor(xtMat, 45);
        final Bitmap bmp = Bitmap.createBitmap(xtMat.width(), xtMat.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(xtMat, bmp);

        // Post image to ui
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mImageView.setImageBitmap(bmp);
            }
        });
    }


    private Mat getXtImageForY(int yPosition) {
        int currentSize = mSequence.size();
        Mat xtMat = new Mat(currentSize, mSequence.get(0).cols(), mSequence.get(0).type());

        for(int i = 0; i < currentSize; i++){
            for(int j = 0; j < mSequence.get(0).cols(); j++){
                xtMat.put(i, j, new double[] {mSequence.get(i).get(yPosition, j)[0]});
            }
        }

        return xtMat;
    }


    private void applyGabor(Mat mat, double theta){
        Mat even = getEvenGaborKernel(theta);
        Mat odd = getOddGaborKernel(theta);
        Imgproc.filter2D(mat, mat, -1, even);
    }


    private Mat getOddGaborKernel(double theta){
        Size kernelSize = new Size(31,31);
        double lambda = 30;
        double sigma = 24;
        double gamma = 1;
        double psi = 0 + (Math.PI / 2);

        // Do the gabor convolution
        return Imgproc.getGaborKernel(kernelSize, sigma, theta, lambda, gamma, psi, CvType.CV_32F);
    }


    private Mat getEvenGaborKernel(double theta){
        Size kernelSize = new Size(31,31);
        double lambda = 30;
        double sigma = 24;
        double gamma = 1;
        double psi =  0;

        // Do the gabor convolution
        return Imgproc.getGaborKernel(kernelSize, sigma, theta, lambda, gamma, psi, CvType.CV_32F);
    }
}
