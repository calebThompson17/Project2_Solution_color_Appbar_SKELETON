package com.example.solution_color;


import android.Manifest;
import android.content.Intent;

import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Environment;
import android.provider.MediaStore;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.library.bitmap_utilities.BitMap_Helpers;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

public class MainActivity extends AppCompatActivity implements OnSharedPreferenceChangeListener {

    //these are constants and objects that I used, use them if you wish
    private static final String DEBUG_TAG = "CartoonActivity";
    private static final String ORIGINAL_FILE = "origfile.png";
    private static final String PROCESSED_FILE = "procfile.png";

    private static final int TAKE_PICTURE = 1;
    private static final double SCALE_FROM_0_TO_255 = 2.55;
    private static final int DEFAULT_COLOR_PERCENT = 3;
    private static final int DEFAULT_BW_PERCENT = 15;

    //preferences
    private int saturation = DEFAULT_COLOR_PERCENT;
    private int bwPercent = DEFAULT_BW_PERCENT;
    private String shareSubject;
    private String shareText;

    // Preference names
    private String SATURATION_NAME;
    private String BWPercent_NAME;
    private String SHARE_SUBJECT_NAME;
    private String SHARE_TEXT_NAME;

    // Shared Preference Listener
    SharedPreferences.OnSharedPreferenceChangeListener listener;

    //where images go
    private String originalImagePath;   //where orig image is
    private String processedImagePath;  //where processed image is
    private Uri outputFileUri;          //tells camera app where to store image

    //used to measure screen size
    int screenheight;
    int screenwidth;

    private ImageView myImage;

    //these guys will hog space
    Bitmap bmpOriginal;                 //original image
    Bitmap bmpThresholded;              //the black and white version of original image
    Bitmap bmpThresholdedColor;         //the colorized version of the black and white image

    //TO DO manage all the permissions you need
    private static final int PERMISSION_REQUEST_CAMERA = 3;
    private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 4;
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TO DO be sure to set up the appbar in the activity
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //dont display these
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        FloatingActionButton fab = findViewById(R.id.buttonTakePicture);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TO DO manage this, mindful of permissions
                if (verifyPermissions()) doTakePicture();
            }
        });

        //get the default image
        myImage = (ImageView) findViewById(R.id.imageView1);
        SATURATION_NAME = getString(R.string.preference_saturation_key);
        BWPercent_NAME = getString(R.string.preference_sketchiness_key);
        SHARE_SUBJECT_NAME = getString(R.string.preference_subject_key);
        SHARE_TEXT_NAME = getString(R.string.preference_text_key);

        //TO DO manage the preferences and the shared preference listenes
        SharedPreferences prefs = getDefaultSharedPreferences(this);
        listener =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                        MainActivity.this.onSharedPreferenceChanged(sharedPreferences, key);
                    }
                };
        prefs.registerOnSharedPreferenceChangeListener(listener);

        // TO DO and get the values already there getPrefValues(settings);
        //TO DO use getPrefValues(SharedPreferences settings)
        getPrefValues(prefs);

        // Fetch screen height and width,
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        screenheight = metrics.heightPixels;
        screenwidth = metrics.widthPixels;

        setUpFileSystem();
    }

    private void setImage() {
        //prefer to display processed image if available
        bmpThresholded = Camera_Helpers.loadAndScaleImage(processedImagePath, screenheight, screenwidth);
        if (bmpThresholded != null) {
            myImage.setImageBitmap(bmpThresholded);
            Log.d(DEBUG_TAG, "setImage: myImage.setImageBitmap(bmpThresholded) set");
            return;
        }

        //otherwise fall back to unprocessd photo
        bmpOriginal = Camera_Helpers.loadAndScaleImage(originalImagePath, screenheight, screenwidth);
        if (bmpOriginal != null) {
            myImage.setImageBitmap(bmpOriginal);
            Log.d(DEBUG_TAG, "setImage: myImage.setImageBitmap(bmpOriginal) set");
            return;
        }

        //worst case get from default image
        //save this for restoring
        bmpOriginal = BitMap_Helpers.copyBitmap(myImage.getDrawable());
        Log.d(DEBUG_TAG, "setImage: bmpOriginal copied");
    }

    //TO DO use this to set the following member preferences whenever preferences are changed.
    //TO DO Please ensure that this function is called by your preference change listener
    private void getPrefValues(SharedPreferences settings) {
        //TO DO should track shareSubject, shareText, saturation, bwPercent

        // save preferences
//        SharedPreferences.Editor editor = settings.edit();
//        editor.putString(SHARE_SUBJECT_NAME, shareSubject);
//        editor.putString(SHARE_TEXT_NAME, shareText);
//        editor.putInt(SATURATION_NAME, saturation);
//        editor.putInt(BWPercent_NAME, bwPercent);
//        editor.commit();  // save changes

        // get preferences
        shareSubject = settings.getString(SHARE_SUBJECT_NAME, "");
        shareText = settings.getString(SHARE_TEXT_NAME, "");
        saturation = settings.getInt(SATURATION_NAME, DEFAULT_COLOR_PERCENT);
        bwPercent = settings.getInt(BWPercent_NAME, DEFAULT_BW_PERCENT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    private void setUpFileSystem(){
        //TO DO do we have needed permissions?
        //TO DO if not then dont proceed
        if (!verifyPermissions()) return;

        //get some paths
        // Create the File where the photo should go
        File photoFile = createImageFile(ORIGINAL_FILE);
        originalImagePath = photoFile.getAbsolutePath();

        File processedfile = createImageFile(PROCESSED_FILE);
        processedImagePath=processedfile.getAbsolutePath();

        //worst case get from default image
        //save this for restoring
        if (bmpOriginal == null)
            bmpOriginal = BitMap_Helpers.copyBitmap(myImage.getDrawable());

        setImage();
    }

    //TO DO manage creating a file to store camera image in
    //TO DO where photo is stored
    private File createImageFile(final String fn) {
        //TO DO fill in
        try {
            File[] storageDir = getExternalMediaDirs();
            File imageFile = new File(storageDir[0], fn);
            if (!storageDir[0].exists()) {
                if (!storageDir[0].mkdirs()) {
                    return null;
                }
            }
            imageFile.createNewFile();
            originalImagePath = imageFile.getAbsolutePath();
            return imageFile;
        } catch (IOException e) {
            return null;
        }

//        File storageDir = getExternalMediaDirs()[0];
//        File image = new File(storageDir, fn);
//        return image;
    }

    //DUMP for students
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    // permissions

    /***
     * callback from requestPermissions
     * @param permsRequestCode  user defined code passed to requestpermissions used to identify what callback is coming in
     * @param permissions       list of permissions requested
     * @param grantResults      //results of those requests
     */
    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {
        //TODO fill in

        // Request for camera permission.
        if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission has been granted. Start camera preview Activity.
            Toast.makeText(this,"Permission granted",Toast.LENGTH_LONG).show();
            if (permsRequestCode == PERMISSION_REQUEST_CAMERA) {
//              TODO:!  Do camera stuff;
//                doTakePicture();
            }
            else if (permsRequestCode == PERMISSION_REQUEST_READ_EXTERNAL_STORAGE) {
//              TODO:!  Do read storage stuff;
            }
            else if (permsRequestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE) {
//              TODO:!  Do write storage stuff;
            }

        } else {
            // Permission request was denied.
            Toast.makeText(this,"Permission Denied",Toast.LENGTH_LONG).show();
        }
    }

    //DUMP for students
    /**
     * Verify that the specific list of permisions requested have been granted, otherwise ask for
     * these permissions.  Note this is coarse in that I assumme I need them all
     */
    private boolean verifyPermissions() {

        //TODO fill in
        boolean allGranted = true;
        if (!askForPermissionIfNeeded("android.permission.CAMERA", PERMISSION_REQUEST_CAMERA)) allGranted = false;
        if (!askForPermissionIfNeeded("android.permission.WRITE_EXTERNAL_STORAGE", PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE)) allGranted = false;
        if (!askForPermissionIfNeeded("android.permission.READ_EXTERNAL_STORAGE", PERMISSION_REQUEST_READ_EXTERNAL_STORAGE)) allGranted = false;

//        // Check for Camera Permission
//        if (ActivityCompat.checkSelfPermission(this, "android.permission.CAMERA")
//                == android.content.pm.PackageManager.PERMISSION_DENIED) {
//            Toast.makeText(this,"Permission needed",Toast.LENGTH_LONG).show();
//            ActivityCompat.requestPermissions(MainActivity.this,
//                    new String[]{Manifest.permission.CAMERA},
//                    PERMISSION_REQUEST_CAMERA);
//            if (ActivityCompat.checkSelfPermission(this, "android.permission.CAMERA")
//                    == android.content.pm.PackageManager.PERMISSION_DENIED) allGranted = false;
//        }
//        // Check for Read Storage Permission
//        if (ActivityCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE")
//                == android.content.pm.PackageManager.PERMISSION_DENIED) {
//            Toast.makeText(this,"Permission needed",Toast.LENGTH_LONG).show();
//            ActivityCompat.requestPermissions(MainActivity.this,
//                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
//                    PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
//            if (ActivityCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE")
//                    == android.content.pm.PackageManager.PERMISSION_DENIED) allGranted = false;
//        }
//        // Check for Write Storage Permission
//        if (ActivityCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE")
//                == android.content.pm.PackageManager.PERMISSION_DENIED) {
//            Toast.makeText(this,"Permission needed",Toast.LENGTH_LONG).show();
//            ActivityCompat.requestPermissions(MainActivity.this,
//                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
//                    PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
//            if (ActivityCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE")
//                    == android.content.pm.PackageManager.PERMISSION_DENIED) allGranted = false;
//        }

        //and return false until they are granted
        return allGranted;
    }

    // Self-built method
    private boolean askForPermissionIfNeeded(String permission, int permissionIdentifier) {
        if (ActivityCompat.checkSelfPermission(this, permission)
                == android.content.pm.PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this,"Permission needed",Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{permission},
                    permissionIdentifier);
        }
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    //take a picture and store it on external storage
    public void doTakePicture() {
        //TO DO verify that app has permission to use camera
        if (!verifyPermissions()) return;

        //TO DO manage launching intent to take a picture
        // create intent to take picture with camera and specify storage
        // location so we can easily get it
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (intent.resolveActivity(getPackageManager()) != null){
            // Create the File where the photo should go
            File photoFile = createImageFile(PROCESSED_FILE);

            // Continue only if the File was successfully created
            //  see https://developer.android.com/reference/androidx/core/content/FileProvider
            if (photoFile != null) {
                outputFileUri = FileProvider.getUriForFile(this,
                        "com.example.solution_color.fileprovider",
                        photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                startActivityForResult(intent, TAKE_PICTURE);
            }
        }
        else
            Toast.makeText(this,"UhOhhhh....No camera mate", Toast.LENGTH_SHORT).show();
    }

    //TODO manage return from camera and other activities
    // TO DO handle edge cases as well (no pic taken)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //TO DO get photo
        if (requestCode == TAKE_PICTURE){
            if (resultCode == RESULT_OK) {
                // Get the dimensions of the View
                int targetW = myImage.getWidth();
                int targetH = myImage.getHeight();

                // Get the dimensions of the bitmap
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                bmOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(originalImagePath, bmOptions);
                int photoW = bmOptions.outWidth;
                int photoH = bmOptions.outHeight;

                // Determine how much to scale down the image
                int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

                // Decode the image file into a Bitmap sized to fill the View
                bmOptions.inJustDecodeBounds = false;
                bmOptions.inSampleSize = scaleFactor;
                bmOptions.inPurgeable = true;

                bmpOriginal = BitmapFactory.decodeFile(originalImagePath, bmOptions);

                //TO DO set the myImage equal to the camera image returned
                myImage.setImageBitmap(bmpOriginal);

                //TO DO tell scanner to pic up this unaltered image
                scanSavedMediaFile(originalImagePath);
            }

                //tell scanner to pic up this image
//                scanSavedMediaFile(mCurrentPhotoPath);
        }
        //TODO save anything needed for later

    }

    /**
     * delete original and processed images, then rescan media paths to pick up that they are gone.
     */
    private void doReset() {
        //TO DO verify that app has permission to use file system
        //do we have needed permissions?
        if (!verifyPermissions()) {
            return;
        }
        //delete the files
        Camera_Helpers.delSavedImage(originalImagePath);
        Camera_Helpers.delSavedImage(processedImagePath);
        bmpThresholded = null;
        bmpOriginal = null;

        myImage.setImageResource(R.drawable.gutters);
        myImage.setScaleType(ImageView.ScaleType.FIT_CENTER);//what the hell? why both
        myImage.setScaleType(ImageView.ScaleType.FIT_XY);

        //worst case get from default image
        //save this for restoring
        bmpOriginal = BitMap_Helpers.copyBitmap(myImage.getDrawable());

        //TO DO make media scanner pick up that images are gone

    }

    public void doSketch() {
        //TO DO verify that app has permission to use file system
        //do we have needed permissions?
        if (!verifyPermissions()) {
            return;
        }

        //sketchify the image
        if (bmpOriginal == null){
            Log.e(DEBUG_TAG, "doSketch: bmpOriginal = null");
            return;
        }
        bmpThresholded = BitMap_Helpers.thresholdBmp(bmpOriginal, bwPercent);

        //set image
        myImage.setImageBitmap(bmpThresholded);

        //save to file for possible email
        Camera_Helpers.saveProcessedImage(bmpThresholded, processedImagePath);
        scanSavedMediaFile(processedImagePath);
    }

    public void doColorize() {
        //TO DO verify that app has permission to use file system
        //do we have needed permissions?
        if (!verifyPermissions()) {
            return;
        }

        //colorize the image
        if (bmpOriginal == null){
            Log.e(DEBUG_TAG, "doColorize: bmpOriginal = null");
            return;
        }
        //if not thresholded yet then do nothing
        if (bmpThresholded == null){
            Log.e(DEBUG_TAG, "doColorize: bmpThresholded not thresholded yet");
            return;
        }

        //otherwise color the bitmap
        bmpThresholdedColor = BitMap_Helpers.colorBmp(bmpOriginal, saturation);

        //takes the thresholded image and overlays it over the color one
        //so edges are well defined
        BitMap_Helpers.merge(bmpThresholdedColor, bmpThresholded);

        //set background to new image
        myImage.setImageBitmap(bmpThresholdedColor);

        //save to file for possible email
        Camera_Helpers.saveProcessedImage(bmpThresholdedColor, processedImagePath);
        scanSavedMediaFile(processedImagePath);
    }

    public void doShare() {
        //TO DO verify that app has permission to use file system
        //do we have needed permissions?
        if (!verifyPermissions()) {
            return;
        }

        //TODO share the processed image with appropriate subject, text and file URI
        //TODO the subject and text should come from the preferences set in the Settings Activity
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_EMAIL, getString(R.string.from_email_address));
        intent.putExtra(Intent.EXTRA_SUBJECT, shareSubject);
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        Uri fileUri;
        if (outputFileUri != null) {
            fileUri = outputFileUri;
        }
        else {
//            fileUri = Uri.fromFile(new File(originalImagePath));
            fileUri = FileProvider.getUriForFile(this,
                    "com.example.solution_color.fileprovider",
                    createImageFile(originalImagePath));
        }
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(fileUri.toString()));
        startActivity(Intent.createChooser(intent, "Share"));

    }

    //TO DO set this up
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //TO DO handle all of the appbar button clicks
        if (item.getItemId() == R.id.menu_revert) doReset();
        else if (item.getItemId() == R.id.menu_edit) doSketch();
        else if (item.getItemId() == R.id.menu_view) doColorize();
        else if (item.getItemId() == R.id.menu_share) doShare();
        else if (item.getItemId() == R.id.menu_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
//        switch (item.getItemId()) {
//            case R.id.menu_revert:
//                doReset();
//                break;
//            case R.id.menu_edit:
//                doSketch();
//                break;
//            case R.id.menu_view:
//                doColorize();
//                break;
//            case R.id.menu_share:
//                doShare();
//                break;
//            case R.id.menu_settings:
//                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
//                startActivity(intent);
//                break;
//        }
        return true;
    }

    //TO DO set up pref changes
    @Override
    public void onSharedPreferenceChanged(SharedPreferences settings, String key) {
        //TODO reload prefs at this point

        if (key.equals(SHARE_SUBJECT_NAME)) shareSubject = settings.getString(SHARE_SUBJECT_NAME, getString(R.string.shareTitle));
        else if (key.equals(SHARE_TEXT_NAME)) shareText = settings.getString(SHARE_TEXT_NAME, "");
        else if (key.equals(SATURATION_NAME)) saturation = settings.getInt(SATURATION_NAME, DEFAULT_COLOR_PERCENT);
        else if (key.equals(BWPercent_NAME)) bwPercent = settings.getInt(BWPercent_NAME, DEFAULT_BW_PERCENT);
        else Toast.makeText(this,"NO Preferences updated",Toast.LENGTH_LONG).show();

//        // save preferences
//        switch (key) {
//            case SHARE_SUBJECT_NAME:
//                shareSubject = settings.getString(SHARE_SUBJECT_NAME, "");
//                break;
//            case SHARE_TEXT_NAME:
//                shareText = settings.getString(SHARE_TEXT_NAME, "");
//                break;
//            case SATURATION_NAME:
//                saturation = settings.getInt(SATURATION_NAME, DEFAULT_COLOR_PERCENT);
//                break;
//            case BWPercent_NAME:
//                bwPercent = settings.getInt(BWPercent_NAME, DEFAULT_BW_PERCENT);
//                break;
//        }
    }

    /**
     * Notifies the OS to index the new image, so it shows up in Gallery.
     * see https://www.programcreek.com/java-api-examples/index.php?api=android.media.MediaScannerConnection
     */
    private void scanSavedMediaFile( final String path) {
        // silly array hack so closure can reference scannerConnection[0] before it's created
        final MediaScannerConnection[] scannerConnection = new MediaScannerConnection[1];
        try {
            MediaScannerConnection.MediaScannerConnectionClient scannerClient = new MediaScannerConnection.MediaScannerConnectionClient() {
                public void onMediaScannerConnected() {
                    scannerConnection[0].scanFile(path, null);
                }

                @Override
                public void onScanCompleted(String path, Uri uri) {

                }

            };
            scannerConnection[0] = new MediaScannerConnection(this, scannerClient);
            scannerConnection[0].connect();
        } catch (Exception ignored) {
        }
    }
}

