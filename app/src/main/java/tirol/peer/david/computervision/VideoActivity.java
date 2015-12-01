package tirol.peer.david.computervision;

import android.graphics.Bitmap;
import android.graphics.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


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
                mSequence.clear();
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


    int mUpdateXt =0;
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = convertAndRotateFrame(inputFrame);
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_BGRA2GRAY);

        // Insert a clone into our sequence, otherwise every pic is the same...
        insertFrame(mGray.clone());

        mUpdateXt++;
        if(mUpdateXt % 4 == 0){
            computeAndViewXt();
            mUpdateXt = 0;
        }

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
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Mat xtMat = getXtImageFromFrame(20);
                Bitmap bmp = Bitmap.createBitmap(xtMat.width(), xtMat.height(), Bitmap.Config.RGB_565);
                Utils.matToBitmap(xtMat, bmp);
                mImageView.setImageBitmap(bmp);
            }
        });
    }


    public Mat getXtImageFromFrame(int yPosition) {
        int currentSize = mSequence.size();
        Mat xtMat = new Mat(currentSize, mSequence.get(0).cols(), mSequence.get(0).type());

        for(int i = 0; i < currentSize; i++){
            for(int j = 0; j < mSequence.get(0).cols(); j++){
                xtMat.put(i, j, new double[] {mSequence.get(i).get(yPosition, j)[0]});
            }
        }

        return xtMat;
    }
}
