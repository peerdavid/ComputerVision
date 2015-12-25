package tirol.peer.david.computervision.utils;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 * Created by david on 20.12.15.
 */
public class OpenCvUtils {

    public static Mat convertAndRotateFrameToGray(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        Mat image = inputFrame.rgba();
        Mat mRgbaT = rotateFrame(image);
        return rgbToGray(mRgbaT);
    }


    private static Mat rotateFrame(Mat image) {
        Mat tmp = image.clone();
        //Mat tmp = image.t();
        //Core.flip(image.t(), tmp, 1);
        //Imgproc.resize(tmp, tmp, image.size());
        return tmp;
    }


    private static Mat rgbToGray(Mat mRgbaT) {
        Imgproc.cvtColor(mRgbaT, mRgbaT, Imgproc.COLOR_BGRA2GRAY);
        return mRgbaT;
    }
}
