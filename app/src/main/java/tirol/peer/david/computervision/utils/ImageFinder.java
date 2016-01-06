package tirol.peer.david.computervision.utils;

import android.util.Log;

import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;

import java.util.List;

/**
 * Created by david on 26.12.15.
 */
public class ImageFinder {

    private Mat mRefImg;

    final private FeatureDetector mFeatureDetector = FeatureDetector.create(FeatureDetector.ORB);
    final private DescriptorExtractor mFeatureDescriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);
    final private DescriptorMatcher mMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

    private final MatOfKeyPoint mReferenceKeypoints = new MatOfKeyPoint();
    private final Mat mReferenceDescriptors = new Mat();

    private final MatOfKeyPoint mSceneKeypoints = new MatOfKeyPoint();
    private final Mat mSceneDescriptors = new Mat();

    public ImageFinder(Mat refImg){
        mRefImg = refImg;

        computeReferenceImageKeypoints();
    }


    private void computeReferenceImageKeypoints() {
        mFeatureDetector.detect(mRefImg, mReferenceKeypoints);
        mFeatureDescriptor.compute(mRefImg, mReferenceKeypoints, mReferenceDescriptors);
    }


    public boolean containsImage(Mat img){

        try {
            mFeatureDetector.detect(img, mSceneKeypoints);
            mFeatureDescriptor.compute(img, mSceneKeypoints, mSceneDescriptors);

            MatOfDMatch matches = new MatOfDMatch();
            mMatcher.match(mReferenceDescriptors, mSceneDescriptors, matches);

            return checkMatches(matches);
        } catch (Exception e){
            Log.e("tirol.peer.david.cv", e.toString());
        }

        return false;
    }


    private boolean checkMatches(MatOfDMatch mMatches) {

        // If we have not found at least n matches, we cancel immediately
        List<DMatch> matchesList = mMatches.toList();
        if (matchesList.size() < 10) {
            return false;
        }

        // Calculate the min distance, max distance and number of good keypoints
        //
        // http://www.vision.cs.chubu.ac.jp/CV-R/pdf/Rublee_iccv2011.pdf
        // On page 4 you can find, that good keypoints are smaller than 65
        double maxDist = 0.0;
        double minDist = Double.MAX_VALUE;
        int numOfGoodKeypoints = 0;
        for(DMatch match : matchesList) {
            double dist = match.distance;
            if (dist < minDist) {
                minDist = dist;
            }
            if (dist > maxDist) {
                maxDist = dist;
            }

            if(dist < 65){
                numOfGoodKeypoints++;
            }
        }

        Log.d("tirol.peer.david.cv", "MinDist = " + minDist);
        Log.d("tirol.peer.david.cv", "MaxDist = " + maxDist);
        Log.d("tirol.peer.david.cv", "GoodKeypoints = " + numOfGoodKeypoints);

        return isAMatch(minDist, numOfGoodKeypoints, matchesList.size());
    }


    /**
     * At leas 50% good matches and our min distance should not be too small (I "guessed" this value)
     * @param minDist
     * @param numOfGoodKeypoints
     * @param numOfAllMatches
     * @return true if it is a match, false otherwise
     */
    private boolean isAMatch(double minDist, int numOfGoodKeypoints, int numOfAllMatches) {
        return minDist < 20.0 && numOfGoodKeypoints > numOfAllMatches * 0.5;
    }
}
