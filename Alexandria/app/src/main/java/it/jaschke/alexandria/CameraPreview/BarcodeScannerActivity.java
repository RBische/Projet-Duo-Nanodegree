package it.jaschke.alexandria.CameraPreview;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.TextView;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.io.IOException;

import it.jaschke.alexandria.R;


public class BarcodeScannerActivity extends ActionBarActivity {
    public static final String SCANNER_EXTRA = "ScannerExtra";
    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;
    TextView scanText;
    ImageScanner scanner;
    private boolean previewing = true;

    static {
        System.loadLibrary("iconv");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcode_scanner);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        autoFocusHandler = new Handler();
        mCamera = getCameraInstance();

        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        FrameLayout preview = (FrameLayout)findViewById(R.id.cameraPreview);
        preview.addView(mPreview);

        scanText = (TextView)findViewById(R.id.scanText);
        scanText.setText(getString(R.string.scan_flashcode));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onPause() {
        super.onPause();
        releaseCamera();
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e){
        }
        return c;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing)
                mCamera.autoFocus(autoFocusCB);
        }
    };

    Camera.PreviewCallback previewCb = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();

            Image barcode = new Image(size.width, size.height, "Y800");
            barcode.setData(data);

            int result = scanner.scanImage(barcode);

            if (result != 0) {
                previewing = false;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                SymbolSet syms = scanner.getResults();
                for (Symbol sym : syms) {

                    Intent intent = new Intent();
                    String datas = sym.getData();
                    intent.putExtra(SCANNER_EXTRA, datas);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        }
    };

    // Mimic continuous auto-focusing
    Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };
    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;
        private Camera.PreviewCallback previewCallback;
        private Camera.AutoFocusCallback autoFocusCallback;

        public CameraPreview(Context context, Camera camera,
                             Camera.PreviewCallback previewCb,
                             Camera.AutoFocusCallback autoFocusCb) {
            super(context);
            mCamera = camera;
            previewCallback = previewCb;
            autoFocusCallback = autoFocusCb;

        /*
         * Set camera to continuous focus if supported, otherwise use
         * software auto-focus. Only works for API level >=9.
         */
        /*
        Camera.Parameters parameters = camera.getParameters();
        for (String f : parameters.getSupportedFocusModes()) {
            if (f == Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) {
                mCamera.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                autoFocusCallback = null;
                break;
            }
        }
        */

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);

            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                Log.d("DBG", "Error setting camera preview: " + e.getMessage());
            } catch (RuntimeException e){
                Log.d("DBG", "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // Camera preview released in activity
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        /*
         * If your preview can change or rotate, take care of those events here.
         * Make sure to stop the preview before resizing or reformatting it.
         */
            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            try {
                // Hard code camera surface rotation 90 degs to match Activity view in portrait
                mCamera.setDisplayOrientation(90);

                mCamera.setPreviewDisplay(mHolder);
                mCamera.setPreviewCallback(previewCallback);
                mCamera.startPreview();
                mCamera.autoFocus(autoFocusCallback);
            } catch (Exception e){
                Log.d("DBG", "Error starting camera preview: " + e.getMessage());
            }
        }
    }

}

