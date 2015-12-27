package tirol.peer.david.computervision.utils;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 26.12.15.
 */
public class ImageFinder {

    private Mat mRefImg;

    final private FeatureDetector mFeatureDetector = FeatureDetector.create(FeatureDetector.ORB);
    final private DescriptorExtractor mFeatureDescriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);
    final private DescriptorMatcher mMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);

    private final MatOfKeyPoint mReferenceKeypoints = new MatOfKeyPoint();
    private final Mat mReferenceDescriptors = new Mat();

    private final MatOfKeyPoint mSceneKeypoints = new MatOfKeyPoint();
    private final Mat mSceneDescriptors = new Mat();

    private final Mat mSceneCorners = new Mat(4, 1, CvType.CV_32FC2);

    public ImageFinder(Mat refImg){
        mRefImg = refImg;

        computeReferenceImageKeypoints();
    }


    private void computeReferenceImageKeypoints() {
        mFeatureDetector.detect(mRefImg, mReferenceKeypoints);
        mFeatureDescriptor.compute(mRefImg, mReferenceKeypoints, mReferenceDescriptors);
    }


    public boolean containsImage(Mat img){

        mFeatureDetector.detect(img, mSceneKeypoints);
        mFeatureDescriptor.compute(img, mSceneKeypoints, mSceneDescriptors);

        MatOfDMatch matches = new MatOfDMatch();
        mMatcher.match(mReferenceDescriptors, mSceneDescriptors, matches);

        return findSceneCorners(matches);
    }


    private boolean findSceneCorners(MatOfDMatch mMatches) {
        List<DMatch> matchesList = mMatches.toList();
        if (matchesList.size() < 4) {
            return false;
        }

        //List<KeyPoint> referenceKeypointsList = mReferenceKeypoints.toList();
        //List<KeyPoint> sceneKeypointsList = mSceneKeypoints.toList();

        // Calculate the max and min distances between keypoints.
        double maxDist = 0.0;
        double minDist = Double.MAX_VALUE;
        for(DMatch match : matchesList) {
            double dist = match.distance;
            if (dist < minDist) {
                minDist = dist;
            }
            if (dist > maxDist) {
                maxDist = dist;
            }
        }

        if (minDist > 50.0) {
            // The target is completely lost.
            // Discard any previously found corners.
            mSceneCorners.create(0, 0, mSceneCorners.type());
            return false;

        } else if (minDist > 25.0) {
            // The target is lost but maybe it is still close.
            // Keep any previously found corners.
            return false;
        }


        // Identify "good" keypoints based on match distance.
        // ArrayList<Point> goodReferencePointsList =
        //         new ArrayList<Point>();
        // ArrayList<Point> goodScenePointsList =
        //         new ArrayList<Point>();
        // double maxGoodMatchDist = 1.75 * minDist;
        // for(DMatch match : matchesList) {
        //     if (match.distance < maxGoodMatchDist) {
        //         goodReferencePointsList.add(
        //                 referenceKeypointsList.get(match.trainIdx).pt);
        //         goodScenePointsList.add(
        //                 sceneKeypointsList.get(match.queryIdx).pt);
        //     }
        // }

        return true;

    }
}
