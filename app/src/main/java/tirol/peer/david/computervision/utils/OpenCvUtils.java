package tirol.peer.david.computervision.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;

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


    public static Bitmap getBitmapFromUri(ContentResolver contentResolver, Uri uri) {
        try {
            ParcelFileDescriptor parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();

            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    public static Bitmap getBitmapFromAbsolteUri(ContentResolver contentResolver, Uri uri) {
        File imgFile = new  File(uri.getPath());
        if(imgFile.exists()){
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            return myBitmap;
        }

        return null;
    }


    /**
     * Getting All Images Path
     *
     * @param activity
     * @return ArrayList with images Path
     */
    public static ArrayList<Uri> getAllImages(Activity activity) {
        Uri uri;
        Cursor cursor;
        int column_index_data, column_index_folder_name;
        ArrayList<Uri> listOfAllImages = new ArrayList<Uri>();
        String absolutePathOfImage = null;
        uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = { MediaStore.MediaColumns.DATA,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME };

        cursor = activity.getContentResolver().query(uri, projection, null,
                null, null);

        column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        column_index_folder_name = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
        while (cursor.moveToNext()) {
            absolutePathOfImage = cursor.getString(column_index_data);

            listOfAllImages.add(Uri.parse(absolutePathOfImage));
        }
        return listOfAllImages;
    }
}
