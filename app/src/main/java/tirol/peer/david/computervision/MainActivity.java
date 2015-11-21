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
import android.widget.EditText;
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
import java.security.InvalidParameterException;

public class MainActivity extends AppCompatActivity {

    private static final int IMAGE_GAUSS = 1;
    private static final int DETECT_CORNERS = 2;

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
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, IMAGE_GAUSS);
            }
        });

        FloatingActionButton fab2 = (FloatingActionButton) findViewById(R.id.fab2);
        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, DETECT_CORNERS);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode != RESULT_OK || data == null){
            return;
        }

        // Load all views and image
        ImageView imageView = (ImageView) findViewById(R.id.ImgView);
        Uri imageUri = data.getData();
        Bitmap fullBmp = getBitmapFromUri(imageUri);
        Bitmap imageBmp = Bitmap.createScaledBitmap(fullBmp, fullBmp.getWidth() / 4, fullBmp.getHeight() / 4, false);

        // Convert to mat...
        Mat imageMat = new Mat (imageBmp.getWidth(), imageBmp.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(imageBmp, imageMat);
        Mat result;

        // Convert image to grayscale
        Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_BGR2GRAY);

        // Do some cv stuff
        switch (requestCode) {
            case IMAGE_GAUSS:
                EditText textEdit = (EditText) findViewById(R.id.EditText);
                double sigma = Double.valueOf(textEdit.getText().toString());
                result = gaborFilter(imageMat, sigma);
                break;

            case DETECT_CORNERS:
                result = cornerDetection(imageMat);
                break;

            default:
                throw new InvalidParameterException("Unknown request code");
        }

        // Show result image
        Utils.matToBitmap(result, imageBmp);
        imageView.setImageBitmap(imageBmp);
    }


    /**
     * Create a gabor kernel and conv. with the image
     * @param img
     * @param theta
     * @return
     */
    private Mat gaborFilter(Mat img, double theta) {
        // Gabor filter settings
        Size kernelSize = new Size(31,31);
        double lambda = 30;
        double sigma = 24;
        double gamma = 1;
        double psi =  0;

        // Do the convolution
        Mat kernel = Imgproc.getGaborKernel(kernelSize, sigma, theta, lambda, gamma, psi, CvType.CV_32F);
        Mat gabor = new Mat (img.width(), img.height(), CvType.CV_8UC1);
        Imgproc.filter2D(img, gabor, -1, kernel);

        return gabor;
    }


    /**
     * Feature detection with harris corner detector
     * @param img
     * @return
     */
    private Mat cornerDetection(Mat img) {

        Mat corners = new Mat();

        // Apply gaussian blur to smooth all corners, lines etc.
        Imgproc.GaussianBlur(img, img, new Size(5, 5), 2);

        // Find corners
        Mat tempDst = new Mat();
        Imgproc.cornerHarris(img, tempDst, 5, 3, 0.04);

        // Normalize harries output
        Mat tempDstNorm = new Mat();
        Core.normalize(tempDst, tempDstNorm, 0, 255, Core.NORM_MINMAX);
        Core.convertScaleAbs(tempDstNorm, corners);

        // Draw corners on new image
        for(int i = 0; i < tempDstNorm.cols(); i++){
            for(int j = 0; j < tempDstNorm.rows(); j++){
                double[] value = tempDstNorm.get(j, i);
                if(value[0] > 80){
                    Imgproc.circle(corners, new Point(i,j), 10, new Scalar(250, 4));
                }
            }
        }

        return corners;
    }


    /**
     * Helper to read bitmap from gallery
     * @param uri
     * @return
     */
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
