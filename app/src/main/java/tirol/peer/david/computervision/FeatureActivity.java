package tirol.peer.david.computervision;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import tirol.peer.david.computervision.utils.ImageFinder;
import tirol.peer.david.computervision.utils.OpenCvUtils;

public class FeatureActivity extends AppCompatActivity {

    private final static int LOAD_REFERENCE_IMAGE = 1;

    private ImageView mSceneView;
    private ImageView mReferenceView;
    private TextView mText;

    private Mat mReferenceImage;
    private Handler mHandler = new Handler();

    private ImageFinder mImageFinder;

    private List<Uri> mSceneImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSceneView = (ImageView) findViewById(R.id.imgScene);
        mReferenceView = (ImageView) findViewById(R.id.imgReference);
        mText = (TextView) findViewById(R.id.txtView);

        FloatingActionButton fabReference = (FloatingActionButton) findViewById(R.id.fabReference);
        fabReference.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadRefImageFromGallery();
            }
        });


        FloatingActionButton fabSearch = (FloatingActionButton) findViewById(R.id.fabSearch);
        fabSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchRefImageInGallery();
            }
        });
    }


    private void loadRefImageFromGallery() {
        mSceneImages = new ArrayList<>();

        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(i, LOAD_REFERENCE_IMAGE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode != LOAD_REFERENCE_IMAGE || resultCode != RESULT_OK || data == null){
            return;
        }

        // Load all views and image
        Uri imageUri = data.getData();
        Bitmap imageBmp = OpenCvUtils.getBitmapFromUri(getContentResolver(), imageUri);

        // Convert to mat...
        Mat imageMat = new Mat (imageBmp.getWidth(), imageBmp.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(imageBmp, imageMat);

        // Grayscale image
        Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_BGR2GRAY);

        // This is our new original image (to reset filters etc.)
        mReferenceImage = imageMat.clone();

        mImageFinder = new ImageFinder(mReferenceImage);

        setImage(mReferenceImage, mReferenceView);
    }


    private void searchRefImageInGallery() {

        final Activity self = this;

        new Thread(new Runnable() {
            @Override
            public void run() {

                // Search in not searched images or restart search
                if(mSceneImages.size() == 0) {
                    mSceneImages = OpenCvUtils.getAllImages(self);
                }

                for(Uri image : new ArrayList<>(mSceneImages)){
                    Bitmap imageBmp = OpenCvUtils.getBitmapFromAbsolteUri(self.getContentResolver(), image);
                    Mat imageMat = new Mat (imageBmp.getWidth(), imageBmp.getHeight(), CvType.CV_8UC1);
                    Utils.bitmapToMat(imageBmp, imageMat);

                    // Grayscale image
                    Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_BGR2GRAY);

                    // Inform user
                    setText(image.getPath());
                    setImage(imageMat, mSceneView);

                    // Searched it, so remove from our scene images
                    mSceneImages.remove(image);

                    if(mImageFinder.containsImage(imageMat)){
                        setText("!!! Match !!!");
                        return;
                    }
                }

                setText("Searched in all images of your phone...");

                mReferenceImage = null;
                mSceneImages = new ArrayList<>();
                clearImageViews();
            }
        }).start();

    }


    /**
     * This function displays the image. This is possible in every thread.
     * @param image
     */
    private void setImage(Mat image, final ImageView view) {
        final Bitmap bmp = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bmp);

        // Post image to ui
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                view.setImageBitmap(bmp);
            }
        });
    }


    private void setText(final String text) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mText.setText(text);
            }
        });
    }


    private void clearImageViews() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mReferenceView.setImageResource(0);
                mSceneView.setImageResource(0);
            }
        });
    }
}
