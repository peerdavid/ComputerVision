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
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.LinkedList;
import java.util.List;

import tirol.peer.david.computervision.utils.Gabor;


/**
 * http://www.jayrambhia.com/blog/beginning-android-opencv/
 */
public class VideoActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private Mat mRgba;
    private Mat mGray;
    private List<Mat> mFrames = new LinkedList<>();
    private CameraBridgeViewBase mOpenCvCameraView;
    private ImageView mImageView;

    private Gabor mGabor;

    private final static int FRAME_SIZE = 15;

    private boolean mShouldRecordFrames = true;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mGabor = new Gabor(new Size(FRAME_SIZE, FRAME_SIZE), 3, 4, Math.PI, 1);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.cameraView);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.enableView();
        mOpenCvCameraView.setMaxFrameSize(400, 300);

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
        if(!mShouldRecordFrames){
            return;
        }

        if(mFrames.size() > FRAME_SIZE){
            mFrames.remove(mFrames.size() - 1);
        }

        mFrames.add(0, mat);
    }


    private void computeAndViewXt() {
        if(mFrames.isEmpty()){
            return;
        }

        new Thread(new Runnable() {
            public void run() {
                mShouldRecordFrames = false;

                Mat selectedFrame = mFrames.get(FRAME_SIZE / 2);

                for(int i = 0; i < selectedFrame.height(); i++) {
                    Mat xtMat = getXtImageForY(i);
                    mGabor.applyEnergyOfGabor(xtMat);

                    replaceXtPixelsOfFrame(xtMat, i, FRAME_SIZE / 2);

                    Mat tmp = selectedFrame.clone();
                    Core.normalize(tmp, tmp, 0, 255, Core.NORM_MINMAX, CvType.CV_8U);

                    final Bitmap bmp = Bitmap.createBitmap(tmp.width(), tmp.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(tmp, bmp);

                    // Post image to ui
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mImageView.setImageBitmap(bmp);
                        }
                    });
                }

/*
                Mat xtMat = getXtImageForY(20);
                mGabor.applyEnergyOfGabor(xtMat);

                final Bitmap bmp = Bitmap.createBitmap(xtMat.width(), xtMat.height(), Bitmap.Config.RGB_565);
                Utils.matToBitmap(xtMat, bmp);

                // Post image to ui
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mImageView.setImageBitmap(bmp);
                    }
                });*/

                mShouldRecordFrames = true;
            }
        }).start();
    }


    private Mat getXtImageForY(int yPosition) {
        int currentSize = mFrames.size();
        Mat xtMat = new Mat(currentSize, mFrames.get(0).cols(), mFrames.get(0).type());

        for(int i = 0; i < currentSize; i++){
            for(int j = 0; j < mFrames.get(0).cols(); j++){
                xtMat.put(i, j, new double[] {mFrames.get(i).get(yPosition, j)[0]});
            }
        }

        return xtMat;
    }


    private void replaceXtPixelsOfFrame(Mat xtMat, int yPosition, int frameToReplace){
        for(int j = 0; j < mFrames.get(0).cols(); j++){
            mFrames.get(frameToReplace).put(yPosition, j, xtMat.get(frameToReplace, j));
        }
    }
}
