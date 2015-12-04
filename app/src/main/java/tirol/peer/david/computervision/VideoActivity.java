package tirol.peer.david.computervision;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.widget.ImageView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Core;
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

    private List<Mat> mFrames = new LinkedList<>();
    private CameraBridgeViewBase mOpenCvCameraView;
    private ImageView mImageView;

    private Gabor mGabor;

    private final static int NUM_OF_FRAMES = 15;
    private final static int FRAME_TO_DISPLAY = NUM_OF_FRAMES / 2;

    private boolean mFrameComputationRunning = true;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mGabor = new Gabor(new Size(NUM_OF_FRAMES, NUM_OF_FRAMES), 3, 4, Math.PI, 1);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.cameraView);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.enableView();
        mOpenCvCameraView.setMaxFrameSize(400, 400);

        mImageView = (ImageView) findViewById(R.id.xtView);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_video, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        int gaborOrientation = 0;

        switch (id){
            case R.id.action_video_gabor_direction_static:
                gaborOrientation = 0;
                break;

            case R.id.action_video_gabor_direction_left:
                gaborOrientation = 1;
                break;

            case R.id.action_video_gabor_direction_flicker:
                gaborOrientation = 2;
                break;

            case R.id.action_video_gabor_direction_right:
                gaborOrientation = 3;
                break;

        }

        double[] gaborOrientations = new double [] {
                gaborOrientation * Math.PI / 4
        };

        doMotionAnalysisWithEnergyOfGabor(gaborOrientations);

        return super.onOptionsItemSelected(item);
    }


    /*
     * Activity event handler
     */
    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    /*
     * Camera event handler
     */
    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }


    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat grayImage = convertAndRotateFrameToGray(inputFrame);
        insertFrameIfNoComputationIsRunning(grayImage);
        return grayImage;
    }


    private Mat convertAndRotateFrameToGray(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        Mat image = inputFrame.rgba();
        Mat mRgbaT = rotateFrame(image);
        return rgbToGray(mRgbaT);
    }


    private Mat rotateFrame(Mat image) {
        //Mat tmp = image.clone();
        Mat tmp = image.t();
        Core.flip(image.t(), tmp, 1);
        Imgproc.resize(tmp, tmp, image.size());
        return tmp;
    }


    private Mat rgbToGray(Mat mRgbaT) {
        Imgproc.cvtColor(mRgbaT, mRgbaT, Imgproc.COLOR_BGRA2GRAY);
        return mRgbaT;
    }


    private void insertFrameIfNoComputationIsRunning(Mat frame){
        if(!mFrameComputationRunning){
            return;
        }

        if(mFrames.size() > NUM_OF_FRAMES){
            mFrames.remove(mFrames.size() - 1);
        }

        mFrames.add(0, frame);
    }


    /**
     * This function computes the motion analysis.
     * @param orientations
     */
    private void doMotionAnalysisWithEnergyOfGabor(final double[] orientations) {
        if(frameComputationPossible()){
            return;
        }

        /*
         * Run motion analysis in own thread. So our gui keeps responsive
         */
        new Thread(new Runnable() {
            public void run() {
                mFrameComputationRunning = false;
                calculateAndDisplayMotion(orientations);
                mFrameComputationRunning = true;
            }

        }).start();
    }


    private void calculateAndDisplayMotion(double[] orientations) {
        Mat selectedFrame = mFrames.get(FRAME_TO_DISPLAY);

        for (int y = 0; y < selectedFrame.height(); y++) {
            Mat xtMat = computeXtImageOnHeight(y);

            mGabor.applyEnergyOfGabor(xtMat, orientations);

            replaceXtPixelsOfFrame(xtMat, y, FRAME_TO_DISPLAY);
            displayImage(selectedFrame);
        }
    }


    private boolean frameComputationPossible() {
        return mFrames != null && !mFrameComputationRunning;
    }


    /**
     * This function displays the image. This is possible in every thread.
     * @param image
     */
    private void displayImage(Mat image) {
        final Bitmap bmp = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bmp);

        // Post image to ui
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mImageView.setImageBitmap(bmp);
            }
        });
    }


    /**
     * Calculate xt image for (important) one! y over all frames
     * @param y
     * @return
     */
    private Mat computeXtImageOnHeight(int y) {
        int t_max = mFrames.size();
        int x_max = mFrames.get(0).cols();
        Mat xtMat = new Mat(t_max, x_max, mFrames.get(0).type());

        // Copy every frame of every t into our xt image
        for(int t = 0; t < t_max; t++){
            mFrames.get(t).row(y).copyTo(xtMat.row(t));
        }

        return xtMat;
    }


    /**
     * Replace on row of a given frame from a (possible conv.) xt image
     * ToDo.: Copy the whole row -> this should be faster
     * @param xtMat - XT image
     * @param y - y position of our dest. image
     * @param t - Frame to use
     */
    private void replaceXtPixelsOfFrame(Mat xtMat, int y, int t){
        int width = mFrames.get(t).cols();

        for(int x = 0; x < width; x++){
            mFrames.get(t).put(y, x, xtMat.get(t, x));
        }
    }
}
