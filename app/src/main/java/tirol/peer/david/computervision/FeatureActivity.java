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
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import tirol.peer.david.computervision.utils.OpenCvUtils;

public class FeatureActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {


    private CameraBridgeViewBase mOpenCvCameraView;
    private ImageView mImageView;

    private Mat mMatchingMat;
    private Mat mCurrentMat;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.cameraView);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.enableView();
        mOpenCvCameraView.setMaxFrameSize(400, 400);

        mImageView = (ImageView) findViewById(R.id.matchingImage);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMatchingMat = mCurrentMat.clone();
            }
        });
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
     * Camera events
     */
    @Override
    public void onCameraViewStarted(int width, int height) { }


    @Override
    public void onCameraViewStopped() { }


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mCurrentMat = OpenCvUtils.convertAndRotateFrameToGray(inputFrame);

        if(mMatchingMat == null){
            mMatchingMat = mCurrentMat.clone();
        }

        matchFeatures();

        return mCurrentMat;
    }


    private void matchFeatures() {
        FeatureDetector featureDetector = FeatureDetector.create(FeatureDetector.ORB);
        DescriptorExtractor descriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        //
        // Image 1
        //
        Mat img1 = mCurrentMat.clone();
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
        Mat descriptors1 = new Mat();
        featureDetector.detect(img1, keypoints1);
        descriptor.compute(img1, keypoints1, descriptors1);
        //Features2d.drawKeypoints(image1, keypoints1, image1);

        //
        // Image 2
        //
        Mat img2 = mMatchingMat.clone();
        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
        Mat descriptors2 = new Mat();
        featureDetector.detect(img2, keypoints2);
        descriptor.compute(img2, keypoints2, descriptors2);

        //
        // Match both images
        //
        MatOfDMatch matches = new MatOfDMatch();
        matcher.match(descriptors1, descriptors2, matches);

        // Linking
        Scalar RED = new Scalar(255,0,0);
        Scalar GREEN = new Scalar(0,255,0);

        List<DMatch> matchesList = matches.toList();
        Double max_dist = 0.0;
        Double min_dist = 100.0;

        for(int i = 0;i < matchesList.size(); i++){
            Double dist = (double) matchesList.get(i).distance;
            if (dist < min_dist)
                min_dist = dist;
            if ( dist > max_dist)
                max_dist = dist;
        }



        LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
        for(int i = 0;i < matchesList.size(); i++){
            if (matchesList.get(i).distance <= (1.5 * min_dist))
                good_matches.addLast(matchesList.get(i));
        }

        // Printing
        MatOfDMatch goodMatches = new MatOfDMatch();
        goodMatches.fromList(good_matches);

        Mat outputImg = new Mat();
        MatOfByte drawnMatches = new MatOfByte();
        Features2d.drawMatches(img1, keypoints1, img2, keypoints2, goodMatches, outputImg,
                GREEN, RED, drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);


        displayImage(outputImg);
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
}
