/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.posenet;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.Handler;;
import android.os.HandlerThread;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
//kotlin.math.abs?
import org.tensorflow.lite.examples.posenet.lib.BodyPart;
import org.tensorflow.lite.examples.posenet.lib.Device;
import org.tensorflow.lite.examples.posenet.lib.KeyPoint;
import org.tensorflow.lite.examples.posenet.lib.Person;
import org.tensorflow.lite.examples.posenet.lib.Posenet;
import org.tensorflow.lite.examples.posenet.lib.Position;

import static org.tensorflow.lite.examples.posenet.Constants.MODEL_WIDTH;


public class PosenetActivity extends Fragment implements ActivityCompat.OnRequestPermissionsResultCallback {

/** List of body joints that should be connected.    */
ArrayList<Pair> bodyJoints = new ArrayList<Pair>(
        Arrays.asList(new Pair(BodyPart.LEFT_WRIST, BodyPart.LEFT_ELBOW),
                new Pair(BodyPart.LEFT_ELBOW, BodyPart.LEFT_SHOULDER),
                new Pair(BodyPart.LEFT_SHOULDER, BodyPart.RIGHT_SHOULDER),
                new Pair(BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_ELBOW),
                new Pair(BodyPart.RIGHT_ELBOW, BodyPart.RIGHT_WRIST),
                new Pair(BodyPart.LEFT_SHOULDER, BodyPart.LEFT_HIP),
                new Pair(BodyPart.LEFT_HIP, BodyPart.RIGHT_HIP),
                new Pair(BodyPart.RIGHT_HIP, BodyPart.RIGHT_SHOULDER),
                new Pair(BodyPart.LEFT_HIP, BodyPart.LEFT_KNEE),
                new Pair(BodyPart.LEFT_KNEE, BodyPart.LEFT_ANKLE),
                new Pair(BodyPart.RIGHT_HIP, BodyPart.RIGHT_KNEE),
                new Pair(BodyPart.RIGHT_KNEE, BodyPart.RIGHT_ANKLE)));

/** Threshold for confidence score. */
private double minConfidence = 0.5;

/** Radius of circle used to draw keypoints.  */
private float circleRadius = 8.0f;

/** Paint class holds the style and color information to draw geometries,text and bitmaps. */
private Paint paint = new Paint();

/** A shape for extracting frame data.   */
private int PREVIEW_WIDTH = 640;
private int PREVIEW_HEIGHT = 480;
public static final String ARG_MESSAGE = "message";
/**
 * Tag for the [Log].
 */
private String TAG = "PosenetActivity";

private String FRAGMENT_DIALOG = "dialog";

/** An object for the Posenet library.    */
private Posenet posenet;

/** ID of the current [CameraDevice].   */
private String cameraId = null; //nullable

/** A [SurfaceView] for camera preview.   */
private SurfaceView surfaceView = null; //nullable

/** A [CameraCaptureSession] for camera preview.   */
private CameraCaptureSession captureSession = null; //nullable

/** A reference to the opened [CameraDevice].    */
private CameraDevice cameraDevice = null; //nullable

/** The [android.util.Size] of camera preview.  */
private Size previewSize = null;

/** The [android.util.Size.getWidth] of camera preview. */
private int previewWidth = 0;

/** The [android.util.Size.getHeight] of camera preview.  */
private int previewHeight = 0;

/** A counter to keep count of total frames.  */
private int frameCounter = 0;

/** An IntArray to save image data in ARGB8888 format  */
private int[] rgbBytes;

/** A ByteArray to save image data in YUV format  */
private byte[][] yuvBytes = new byte[3][];  //???

/** An additional thread for running tasks that shouldn't block the UI.   */
private HandlerThread backgroundThread = null; //nullable

/** A [Handler] for running tasks in the background.    */
private Handler backgroundHandler = null; //nullable

/** An [ImageReader] that handles preview frame capture.   */
private ImageReader imageReader = null; //nullable

/** [CaptureRequest.Builder] for the camera preview   */
private CaptureRequest.Builder previewRequestBuilder = null; //nullable

/** [CaptureRequest] generated by [.previewRequestBuilder   */
private CaptureRequest previewRequest = null; //nullable

/** A [Semaphore] to prevent the app from exiting before closing the camera.    */
private Semaphore cameraOpenCloseLock = new Semaphore(1);

/** Whether the current camera device supports Flash or not.    */
private boolean flashSupported = false;

/** Orientation of the camera sensor.   */
private int sensorOrientation = 0;  //was null. Need Integer?

/** Abstract interface to someone holding a display surface.    */
private SurfaceHolder surfaceHolder; //nullable

/** [CameraDevice.StateCallback] is called when [CameraDevice] changes its state.   */
private class stateCallback extends CameraDevice.StateCallback {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
                cameraOpenCloseLock.release();
                PosenetActivity.this.cameraDevice = cameraDevice;
                createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                cameraOpenCloseLock.release();
                cameraDevice.close();
                PosenetActivity.this.cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
                onDisconnected(cameraDevice);

                //getActivity in a Fragment returns the activity the fragment is associated with
                if (PosenetActivity.this.getActivity() == null) {
                        return;
                }
                PosenetActivity.this.getActivity().finish();
        }
}

/**
 * A [CameraCaptureSession.CaptureCallback] that handles events related to JPEG capture.
 */
private class captureCallback extends CameraCaptureSession.CaptureCallback {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                super.onCaptureProgressed(session, request, partialResult);

        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);

        }
}

/**
 * Shows a [Toast] on the UI thread.
 *
 * @param text The message to show
 */
private void showToast(final String text) {
        final Activity activity = PosenetActivity.this.getActivity();
        if (activity!=null)
                activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                                Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                        }
                });
}


        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
                return inflater.inflate(R.layout.tfe_pn_activity_posenet, container, false);
                //return super.onCreateView(inflater, container, savedInstanceState);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
                //super.onViewCreated(view, savedInstanceState);
                surfaceView = view.findViewById(R.id.surfaceView);
                if (surfaceView==null) {
                        Log.e("DEBUG", "onViewCreated: surfaceView came up NULL");
                        return;
                }


                surfaceHolder = surfaceView.getHolder();
                if (surfaceHolder==null) {
                        Log.e("DEBUG", "onViewCreated: surfaceHolder came up NULL");
                }
        }

        @Override
        public void onResume() {
                super.onResume();
                startBackgroundThread();
        }

        @Override
        public void onStart() {
                super.onStart();
                showToast("Added PoseNet submodule fragment into Activity");
                openCamera();
                posenet = new Posenet(this.getContext(), "posenet_model.tflite", Device.CPU);
        }

        @Override
        public void onPause() {
                closeCamera();
                stopBackgroundThread();
                super.onPause();
        }

        @Override
        public void onDestroy() {
                super.onDestroy();
                posenet.close();
        }


private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                ConfirmationDialog confirmationDialog = new ConfirmationDialog();
                confirmationDialog.show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }

        else {
                String[] camera = {Manifest.permission.CAMERA};
                requestPermissions(camera, Constants.REQUEST_CAMERA_PERMISSION);
        }
}


        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
                if (requestCode==Constants.REQUEST_CAMERA_PERMISSION) {
                        if (allPermissionsGranted(grantResults)) {
                                ErrorDialog.newInstance(getString(R.string.tfe_pn_request_permission))
                                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
                        }
                }
                else {
                        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                }
        }

//was a lambda expression in Kotlin
        /*
        private fun allPermissionsGranted(grantResults: IntArray) = grantResults.all {
    //this returns true if all elements of grantResults match this constant
    it == PackageManager.PERMISSION_GRANTED
  }
         */
private boolean allPermissionsGranted(int[] grantResults) {
        for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        return false;
                }
        }
        return true;
}

/**
 * Sets up member variables related to camera.
 */
private void setUpCameraOutputs() {
        Activity activity = getActivity();
        CameraManager cameraManager = (CameraManager) Objects.requireNonNull(getActivity()).getSystemService(Context.CAMERA_SERVICE);

        try {
                for (String cameraId : cameraManager.getCameraIdList()) {
                        CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);

                        //don't use front facing camera in this example
                        Integer cameraDirection = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);

                        if (cameraDirection !=null && cameraDirection == CameraCharacteristics.LENS_FACING_FRONT) {
                                //skip this one because it's a front-facing camera
                                continue;
                        }

                        previewSize = new Size(PREVIEW_WIDTH, PREVIEW_HEIGHT);

                        imageReader = ImageReader.newInstance(PREVIEW_WIDTH, PREVIEW_HEIGHT, ImageFormat.YUV_420_888, 2);

                        try {
                                sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                        } catch (NullPointerException e) {
                                e.printStackTrace();
                        }

                        previewHeight = previewSize.getHeight();
                        previewWidth = previewSize.getWidth();

                        rgbBytes = new int[previewWidth * previewHeight];

                        flashSupported = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);

                        this.cameraId = cameraId;

                        //we've now found a usable back camera and finished setting up member variables, so don't need to keep iterating
                        return;

                }
        } catch (CameraAccessException e) {
                e.printStackTrace();
        } catch (NullPointerException e) {
                //NPE thrown when Camera2API is used but not supported on the device
                ErrorDialog.newInstance(getString(R.string.tfe_pn_camera_error)).show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
}


/**
 * Opens the camera specified by [PosenetActivity.cameraId].
 */
private void openCamera() {
        int permissionCamera = Objects.requireNonNull(getContext()).checkPermission(Manifest.permission.CAMERA, Process.myPid(), Process.myUid());
        if (permissionCamera != PackageManager.PERMISSION_GRANTED) {
                //still need permission to access camera, so get it now
                requestCameraPermission();
        }

        setUpCameraOutputs();

        CameraManager cameraManager = (CameraManager)Objects.requireNonNull(getActivity()).getSystemService(Context.CAMERA_SERVICE);

        try {
        // Wait for camera to open - 2.5 seconds is sufficient
        if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
        }

        cameraManager.openCamera(cameraId, new stateCallback(), backgroundHandler);


        } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException("Interrupted while trying to lock camera opening.");
        } catch (CameraAccessException e) {
                e.printStackTrace();
        }

}

/**
 * Closes the current [CameraDevice].
 */
private void closeCamera() {
        if (captureSession == null) {
                return;
        }

        try {
                cameraOpenCloseLock.acquire();
                captureSession.close();
                captureSession = null;
                cameraDevice.close();
                cameraDevice = null;
                imageReader.close();
                imageReader = null;
        }
        catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        }

        //Java finally block is always executed whether exception occurs or not and is handled or not
        //often used for important cleanup code that MUST be executed
        finally {
                cameraOpenCloseLock.release();
        }
}

/**
 * Starts a background thread and its [Handler].
 */
private void startBackgroundThread() {
        backgroundThread = new HandlerThread("imageAvailableListener");

        //start up the background thread
        backgroundThread.start();

        //create a new Handler to post work on the background thread
        backgroundHandler = new Handler(backgroundThread.getLooper());
}

/**
 * Stops the background thread and its [Handler].
 */
private void stopBackgroundThread() {
        if (backgroundThread!=null)  {
                backgroundThread.quitSafely();
        }

        try {
                if (backgroundThread!=null) {
                        //terminate the background thread by joining
                        backgroundThread.join();
                }
                backgroundThread = null;
                backgroundHandler = null;
        }
        catch (InterruptedException e) {
                Log.e(TAG, e.toString());
        }
}

/** Fill the yuvBytes with data from image planes.   */
private void fillBytes(Image.Plane[] planes, byte[][] yuvBytes) {
        // Row stride is the total number of bytes occupied in memory by a row of an image.
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; i++) {
                ByteBuffer buffer = planes[i].getBuffer();

                if (yuvBytes[i] == null) {
                        yuvBytes[i] = new byte[(buffer.capacity())];
                }

                //store the raw ByteBuffer of the plane at this location in yuvBytes 2D array
                buffer.get(yuvBytes[i]);
        }
}

/** A [OnImageAvailableListener] to receive frames as they are available.  */
private class imageAvailableListener implements OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
                // We need wait until we have some size from onPreviewSizeChosen
                if (previewWidth == 0 || previewHeight == 0) {
                        return;
                }

                //acquire the latest image from the the ImageReader queue
                Image image = imageReader.acquireLatestImage();
                if (image==null) {
                        return;
                }

                fillBytes(image.getPlanes(), yuvBytes);

                ImageUtils imageUtils = new ImageUtils();

                imageUtils.convertYUV420ToARGB8888(yuvBytes[0], yuvBytes[1], yuvBytes[2], previewWidth, previewHeight,
                        /*yRowStride=*/ image.getPlanes()[0].getRowStride(),
                        /*uvRowStride=*/ image.getPlanes()[1].getRowStride(),
                        /*uvPixelStride=*/ image.getPlanes()[1].getPixelStride(),
                        rgbBytes
                );

                // Create bitmap from int array
                Bitmap imageBitmap = Bitmap.createBitmap(rgbBytes, previewWidth, previewHeight, Bitmap.Config.ARGB_8888);

                // Create rotated version for portrait display
                Matrix rotateMatrix = new Matrix();
                rotateMatrix.postRotate(90.0f);

                Bitmap rotatedBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, previewWidth, previewHeight, rotateMatrix, true);
                image.close();

                processImage(rotatedBitmap);
        }
}

/** Crop Bitmap to maintain aspect ratio of model input.   */
private Bitmap cropBitmap(Bitmap bitmap) {
        float bitmapRatio = (float)bitmap.getHeight() / bitmap.getWidth();

        float modelInputRatio = (float)Constants.MODEL_HEIGHT / MODEL_WIDTH;

        //first set new edited bitmap equal to the passed one
        Bitmap croppedBitmap = bitmap;

        // Acceptable difference between the modelInputRatio and bitmapRatio to skip cropping.
        double maxDifference = 1e-5;

        // Checks if the bitmap has similar aspect ratio as the required model input.
        if (Math.abs(modelInputRatio - bitmapRatio) < maxDifference) {
                return croppedBitmap;
        }
        else if (modelInputRatio < bitmapRatio) {
                // New image is taller so we are height constrained.
                float cropHeight = bitmap.getHeight() - ((float)bitmap.getWidth() / modelInputRatio);

                croppedBitmap = Bitmap.createBitmap(bitmap, 0, (int)(cropHeight / 2), bitmap.getWidth(), (int)(bitmap.getHeight() - cropHeight));
        }
        else {
                float cropWidth = bitmap.getWidth() - ((float)bitmap.getHeight() * modelInputRatio);

                croppedBitmap = Bitmap.createBitmap(bitmap, (int)(cropWidth / 2), 0, (int)(bitmap.getWidth() - cropWidth), bitmap.getHeight());
        }

        return croppedBitmap;
}

/** Set the paint color and size.    */
private void setPaint() {
        paint.setColor(Color.RED);
        paint.setTextSize(80.0f);
        paint.setStrokeWidth(8.0f);
}

/** Draw bitmap on Canvas.   */
private void draw(Canvas canvas, Person person, Bitmap bitmap) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        // Draw `bitmap` and `person` in square canvas.

        int screenWidth, screenHeight, left, right, top, bottom, canvasHeight, canvasWidth;

        canvasHeight = canvas.getHeight();
        canvasWidth = canvas.getWidth();

        if (canvasHeight > canvasWidth) {
                screenWidth = canvasWidth;
                screenHeight = canvasWidth;
                left = 0;
                top = (canvasHeight - canvasWidth) / 2;
        } else {
                screenWidth = canvasHeight;
                screenHeight = canvasHeight;
                left = (canvasWidth - canvasHeight) / 2;
                top = 0;
        }

        right = left + screenWidth;
        bottom = top + screenHeight;

        //set up the Paint tool
        setPaint();

        canvas.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), new Rect(left, top, right, bottom), paint);

        float widthRatio = (float) screenWidth / MODEL_WIDTH;
        float heightRatio = (float) screenHeight / Constants.MODEL_HEIGHT;

        // Draw key points over the image.
        for (KeyPoint keyPoint : person.getKeyPoints()) {
                if (keyPoint.getScore() > minConfidence) {
                        Position position = keyPoint.getPosition();
                        float adjustedX = (float) position.getX() * widthRatio + left;
                        float adjustedY = (float) position.getY() * heightRatio + top;
                        canvas.drawCircle(adjustedX, adjustedY, circleRadius, paint);
                }
        }


        for (Pair line : bodyJoints) {
                assert line.first != null;
                List<KeyPoint> keyPoints = person.getKeyPoints();
                assert line.second != null;

                if ((keyPoints.get(BodyPart.getValue((BodyPart) line.first)).getScore() > minConfidence) &&
                        (keyPoints.get(BodyPart.getValue((BodyPart) line.second)).getScore() > minConfidence)) {
                        canvas.drawLine(
                                keyPoints.get(BodyPart.getValue((BodyPart) line.first)).getPosition().getX() * widthRatio + left,
                                keyPoints.get(BodyPart.getValue((BodyPart) line.first)).getPosition().getY() * heightRatio + top,
                                keyPoints.get(BodyPart.getValue((BodyPart) line.second)).getPosition().getX() * widthRatio + left,
                                keyPoints.get(BodyPart.getValue((BodyPart) line.second)).getPosition().getY() * heightRatio + top,
                                paint
                        );
                }
        }

        canvas.drawText(
                String.format("Score: %.2f",person.getScore()),
                (15.0f * widthRatio),
                (30.0f * heightRatio + bottom),
                paint
        );

        canvas.drawText(
                String.format("Device: %s",posenet.getDevice()),
                (15.0f * widthRatio),
                (50.0f * heightRatio + bottom),
                paint
        );

        canvas.drawText(
                String.format("Time: %.2f ms", posenet.getLastInferenceTimeNanos() * 1.0f / 1_000_000),
                (15.0f * widthRatio),
                (70.0f * heightRatio + bottom),
                paint
        );

        // Draw to the screen!
        surfaceHolder.unlockCanvasAndPost(canvas);
}

/** Process image using Posenet library.   */
private void processImage(Bitmap bitmap) {
        // Crop bitmap.
        Bitmap croppedBitmap = cropBitmap(bitmap);

        // Created scaled version of bitmap for model input.
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, MODEL_WIDTH, Constants.MODEL_HEIGHT, true);

        // Perform inference.
        Person person = posenet.estimateSinglePose(scaledBitmap);
        Canvas canvas = surfaceHolder.lockCanvas();

        if (canvas == null) {
                Log.e("DEBUG", "processImage: canvas came up NULL");
                return;
        }
        draw(canvas, person, scaledBitmap);
}

/**
 * Creates a new [CameraCaptureSession] for camera preview.
 */
private void createCameraPreviewSession() {
        try {
                // We capture images from preview in YUV format.
                imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 2);

                imageReader.setOnImageAvailableListener(new imageAvailableListener(), backgroundHandler);

                List<Surface> recordingSurfaces = new ArrayList<Surface>();


                // This is the surface we need to record images for processing.
                Surface recordingSurface = imageReader.getSurface();

                recordingSurfaces.add(recordingSurface);

                // We set up a CaptureRequest.Builder with the output Surface.
                previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                previewRequestBuilder.addTarget(recordingSurface);

                // Here, we create a CameraCaptureSession for camera preview.
                cameraDevice.createCaptureSession(
                        recordingSurfaces,
                        new CameraCaptureSession.StateCallback() {
                                @Override
                                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                        // The camera is already closed
                                        if (cameraDevice == null) return;

                                        // When the session is ready, we start displaying the preview.
                                        captureSession = cameraCaptureSession;

                                        try {
                                                // Auto focus should be continuous for camera preview.
                                                previewRequestBuilder.set(
                                                        CaptureRequest.CONTROL_AF_MODE,
                                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                                );
                                                // Flash is automatically enabled when necessary.
                                                setAutoFlash(previewRequestBuilder);

                                                // Finally, we start displaying the camera preview.
                                                previewRequest = previewRequestBuilder.build();
                                                captureSession.setRepeatingRequest(previewRequest, new captureCallback(), backgroundHandler);
                                        }
                                        catch (CameraAccessException e) {
                                                Log.e(TAG, e.toString());
                                        }
                                }
                                @Override
                                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                                        showToast("Failed");
                                }
                        }, null);
        } catch (CameraAccessException e) {
                Log.e(TAG, e.toString());
        }
}

private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (flashSupported) {
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
}

/**
 * Shows an error message dialog.
 */
private static class ErrorDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
                assert getArguments() != null;
                new AlertDialog.Builder(getActivity()).setMessage(getArguments().getString(ARG_MESSAGE))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                        Objects.requireNonNull(getActivity()).finish();
                                }
                        }).create();
                return super.onCreateDialog(savedInstanceState);
        }


        public static ErrorDialog newInstance(String message) {

                Bundle args = new Bundle();

                args.putString(ARG_MESSAGE, message);

                ErrorDialog fragment = new ErrorDialog();
                fragment.setArguments(args);
                return fragment;
        }
}

/*
companion object {
       //Conversion from screen rotation to JPEG orientation
        private val ORIENTATIONS = SparseIntArray()

        init {
                ORIENTATIONS.append(Surface.ROTATION_0, 90)
                ORIENTATIONS.append(Surface.ROTATION_90, 0)
                ORIENTATIONS.append(Surface.ROTATION_180, 270)
                ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }
}
*/


}
