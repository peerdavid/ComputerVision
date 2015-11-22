package tirol.peer.david.computervision;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileDescriptor;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int LOAD_IMAGE = 1;

    private static Mat mOriginalImage = null;
    private static Mat mCurrentImage = null;


    /*
     * Link open cv c++ libs
     */
    static {
        // If you use opencv 2.4, System.loadLibrary("opencv_java")
        System.loadLibrary("opencv_java3");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadImageFromGallery();
            }
        });
    }


    private void loadImageFromGallery() {
        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(i, LOAD_IMAGE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(mOriginalImage == null){
            loadImageFromGallery();
            return true;
        }

        switch (id){
            case R.id.action_reset:
                resetCurrentImage();
                break;

            case R.id.action_gabor:
                applyGaborFilter(0);
                break;

            case R.id.action_harries:
                applyHarriesCornerDetection();
                break;

            case R.id.action_canny:
                applyCannyEdgeDetector();
                break;

            case R.id.action_gaussian:
                applyGaussianBlur();
                break;

        }

        setAndViewCurrentImage();

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode != LOAD_IMAGE || resultCode != RESULT_OK || data == null){
            return;
        }

        // Load all views and image
        Uri imageUri = data.getData();
        Bitmap fullBmp = getBitmapFromUri(imageUri);

        // Scale down for better performance
        Bitmap imageBmp = Bitmap.createScaledBitmap(fullBmp, fullBmp.getWidth() / 4, fullBmp.getHeight() / 4, false);

        // Convert to mat...
        Mat imageMat = new Mat (imageBmp.getWidth(), imageBmp.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(imageBmp, imageMat);

        // Grayscale image
        Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_BGR2GRAY);

        // This is our new original image (to reset filters etc.)
        mOriginalImage = imageMat.clone();
        resetCurrentImage();

        setAndViewCurrentImage();
    }


    /**
     * Apply gabor filter on our current image
     * @param theta orientation of our convolution kernel
     * @return
     */
    private void applyGaborFilter(double theta) {
        Size kernelSize = new Size(31,31);
        double lambda = 30;
        double sigma = 24;
        double gamma = 1;
        double psi =  0;

        // Do the gabor convolution
        Mat kernel = Imgproc.getGaborKernel(kernelSize, sigma, theta, lambda, gamma, psi, CvType.CV_32F);
        Imgproc.filter2D(mCurrentImage, mCurrentImage, -1, kernel);
    }


    private void applyCannyEdgeDetector(){
        Imgproc.Canny(mCurrentImage, mCurrentImage, 10, 100);
    }


    private void applyGaussianBlur(){
        Imgproc.GaussianBlur(mCurrentImage, mCurrentImage, new Size(5, 5), 2);
    }


    private void applyHarriesCornerDetection() {

        Mat corners = new Mat();

        // Find corners
        Mat tempDst = new Mat();
        Imgproc.cornerHarris(mCurrentImage, tempDst, 5, 3, 0.04);

        // Normalize harries output
        Mat tempDstNorm = new Mat();
        Core.normalize(tempDst, tempDstNorm, 0, 255, Core.NORM_MINMAX);
        Core.convertScaleAbs(tempDstNorm, corners);

        // Draw corners on new image
        for(int i = 0; i < tempDstNorm.cols(); i++){
            for(int j = 0; j < tempDstNorm.rows(); j++){
                double[] value = tempDstNorm.get(j, i);
                // If value is too small, its not a corner...
                if(value[0] < 130) {
                    continue;
                }

                Imgproc.circle(mCurrentImage, new Point(i,j), 10, new Scalar(100));
            }
        }
    }


    private void resetCurrentImage(){
        mCurrentImage = mOriginalImage.clone();
    }


    private void setAndViewCurrentImage(){
        ImageView imageView = (ImageView) findViewById(R.id.ImgView);

        Bitmap bmp = Bitmap.createBitmap(mCurrentImage.width(), mCurrentImage.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(mCurrentImage, bmp);
        imageView.setImageBitmap(bmp);
    }


    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();

            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
