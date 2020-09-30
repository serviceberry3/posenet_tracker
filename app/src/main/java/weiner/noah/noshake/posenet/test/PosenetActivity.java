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

package weiner.noah.noshake.posenet.test;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
//kotlin.math.abs?
import weiner.noah.noshake.posenet.test.R;

import weiner.noah.noshake.posenet.test.ctojavaconnector.CircBuffer;
import weiner.noah.noshake.posenet.test.ctojavaconnector.Convolve;
import weiner.noah.noshake.posenet.test.ctojavaconnector.ImpulseResponse;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.examples.noah.lib.BodyPart;
import org.tensorflow.lite.examples.noah.lib.Device;
import org.tensorflow.lite.examples.noah.lib.KeyPoint;
import org.tensorflow.lite.examples.noah.lib.Person;
import org.tensorflow.lite.examples.noah.lib.Posenet;
import org.tensorflow.lite.examples.noah.lib.Position;

import weiner.noah.noshake.posenet.test.utils.Utils;

import weiner.noah.openglbufftesting.OpenGLRenderer;
import weiner.noah.openglbufftesting.OpenGLView;

public class PosenetActivity extends Fragment implements ActivityCompat.OnRequestPermissionsResultCallback, SensorEventListener {

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

//NAIVE IMPLEMENTATION ACCEL ARRAYS

//temporary array to store raw linear accelerometer data before low-pass filter applied
private final float[] NtempAcc = new float[3];

//acceleration array for data after filtering
private final float[] Nacc = new float[3];

//velocity array (calculated from acceleration values)
private final float[] Nvelocity = new float[3];

//position (displacement) array (calculated from dVelocity values)
private final float[] Nposition = new float[3];

//NOSHAKE SPRING IMPLEMENTATION ACCEL ARRAYS
private final float[] StempAcc = new float[3];
private final float[] Sacc = new float[3];
private final float[] accAfterFrix = new float[3];

//long to use for keeping track of thyme
private long timestamp = 0;

//the view to be stabilized
private View layoutSensor, waitingText;

//the text that can be dragged around (compare viewing of this text to how the stabilized text looks)
private TextView noShakeText;

//original vs. changed layout parameters of the draggable text
private RelativeLayout.LayoutParams originalLayoutParams;
private RelativeLayout.LayoutParams editedLayoutParams;
private int ogLeftMargin, ogTopMargin;

//the accelerometer and its manager
private Sensor accelerometer;
private SensorManager sensorManager;

//changes in x and y to be used to move the draggable text based on user's finger
private int _xDelta, _yDelta;

//time variables, and the results of H(t) and Y(t) functions
private double HofT, YofT, startTime, timeElapsed;

//the raw values that the low-pass filter is applied to
private float[] gravity = new float[3];

//working on circular buffer for the data
private float[] accelBuffer = new float[3];

private float impulseSum;

//is the device shaking??
private volatile int shaking = 0;

private int index=0, check=0, times=0;

private Thread outputPlayerThread=null;

private OpenGLRenderer myRenderer;

private OpenGLView openGLView;

public static float toMoveX, toMoveY;

float noseDeltaX, noseDeltaY;

//declare global matrix containing my model 3D coordinates of human pose, to be used for camera pose estimation
private Point3[] humanModelRaw = new Point3[5];
private List<Point3> humanModelList = new ArrayList<Point3>();
private MatOfPoint3f humanModelMat;

//declare global matrix containing the actual 2D coordinates of the human found
private Point[] humanActualRaw = new Point[5];
private List<Point> humanActualList = new ArrayList<Point>();
private MatOfPoint2f humanActualMat;

//matrices to be used for pose estimation calculation
private Mat cameraMatrix, rotationMat, translationMat;
private MatOfDouble distortionMat;

Point3[] testPt = new Point3[1];
List<Point3> testPtList = new ArrayList<Point3>();


//thread that writes data to the circular buffer
class getDataWriteBuffer implements Runnable {
        float xAccel;

        getDataWriteBuffer(float x) {
                this.xAccel = x;
        }

        @Override
        public void run() {
                Log.d("WRITER", String.format("Putting value %f", xAccel));
                CircBuffer.circular_buf_put(xAccel, 0);
        }
}

//thread that reads/aggregates the last ~1 second of data from the buffer and determines if the devices is shaking
class detectShaking implements Runnable {
        @Override
        public void run() {
                while (true) {
                        //Log.d("DBUG", "Running thread");
                        //Log.d("HEAD", String.format("Head is %d", CircBuffer.circular_buf_get_head()));
                        float aggregation = (CircBuffer.aggregate_last_n_entries(50, 0) + CircBuffer.aggregate_last_n_entries(50, 1))/2;
                        //Log.d("AVERAGE", String.format("%f", aggregation));
                        if (aggregation >= 0) {
                                if (aggregation >= NoShakeConstants.shaking_threshold) {
                                        shaking = 1;
                                }
                                else {
                                        shaking = 0;
                                }
                        }
                }
        }
}

//thread that displays "Please wait..." until the circular buffer is full so that convolution can begin
class bufferWait implements Runnable {
        @Override
        public void run() {
                while (CircBuffer.circular_buf_size(0) < NoShakeConstants.buffer_size) {
                        ;
                }
                waitingText.setVisibility(View.INVISIBLE);
        }
}

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

                humanModelMat = ((CameraActivity) Objects.requireNonNull(getActivity())).getHumanModelMat();
                humanActualMat = ((CameraActivity) getActivity()).getHumanActualMat();

                testPt[0] = new Point3(0,0,1000.0);
                testPtList.add(testPt[0]);
        }

        /*
        @Override
        public void onResume() {
                super.onResume();

                if (!OpenCVLoader.initDebug()) {
                        Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, getActivity(),
                                ((CameraActivity)getActivity()).getmLoaderCallback());
                }

                else {
                        Log.d("OpenCV", "OpenCV library found inside package. Using it!");
                        ((CameraActivity)getActivity()).getmLoaderCallback().onManagerConnected(LoaderCallbackInterface.SUCCESS);
                }
        }

         */


        @Override
        public void onStart() {
                super.onStart();
                /*
                //initiate the openGLView and create an instance with this activity
                openGLView = new OpenGLView(getActivity().getApplicationContext());

                openGLView.setEGLContextClientVersion(2);

                openGLView.setPreserveEGLContextOnPause(true);

                myRenderer = new OpenGLRenderer(getActivity().getApplicationContext(), getActivity());

                openGLView.setRenderer(myRenderer);

                //openGLView = (OpenGLView) findViewById(R.id.openGLView);

                getActivity().setContentView(openGLView);
                */

                /*
                //initiate the openGLView and create an instance with this activity
                openGLView = new OpenGLView(getActivity().getApplicationContext());

                openGLView.setEGLContextClientVersion(2);

                openGLView.setPreserveEGLContextOnPause(true);

                myRenderer = new OpenGLRenderer(getActivity().getApplicationContext(), getActivity());

                openGLView.setRenderer(myRenderer);

                getActivity().setContentView(openGLView);

                velocity[0] = velocity[1] = 0;
                 */

                showToast("Added PoseNet submodule fragment into Activity");
                openCamera();
                posenet = new Posenet(Objects.requireNonNull(this.getContext()), "posenet_model.tflite", Device.GPU);

                //populate the 3D human model
                humanModelRaw[0] = new Point3(0.0f, 0.0f, 0.0f); //nose
                humanModelRaw[1] = new Point3(-225.0f, 170.0f, -135.0f); //left eye ctr WAS -150
                humanModelRaw[2] = new Point3(225.0f, 170.0f, -135.0f); //rt eye ctr
                humanModelRaw[3] = new Point3(450.0f, -700.0f, -600.0f); //rt shoulder
                humanModelRaw[4] = new Point3(-450.0f, -700.0f, -600.0f); //left shoulder

                //push all of the model coordinates into the ArrayList version so they can be converted to a MatofPoint3f
                humanModelList.add(humanModelRaw[0]);
                humanModelList.add(humanModelRaw[1]);
                humanModelList.add(humanModelRaw[2]);
                humanModelList.add(humanModelRaw[3]);
                humanModelList.add(humanModelRaw[4]);


                humanModelMat.fromList(humanModelList);


                /*
                setContentView(R.layout.activity_main);

                layoutSensor = findViewById(R.id.layout_sensor);
                noShakeText = findViewById(R.id.movable_text); //get the NoShake sample text as a TextView so we can move it around (TextView)
                waitingText = findViewById(R.id.waiting_text);

                originalLayoutParams = (RelativeLayout.LayoutParams) noShakeText.getLayoutParams();
                ogLeftMargin = originalLayoutParams.leftMargin;
                ogTopMargin = originalLayoutParams.topMargin;


                //get pixel dimensions of screen
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int height = displayMetrics.heightPixels;
                int width = displayMetrics.widthPixels;
                 */

                /*
                //initialize a circular buffer of 211 floats
                CircBuffer.circular_buffer(NoShakeConstants.buffer_size, 0);
                CircBuffer.circular_buffer(NoShakeConstants.buffer_size, 1);

                //initialize an impulse response array also of size 211
                ImpulseResponse.impulse_resp_arr(NoShakeConstants.buffer_size, NoShakeConstants.e, NoShakeConstants.spring_const);

                //populate the H(t) impulse response array in C++ based on the selected spring constant
                ImpulseResponse.impulse_response_arr_populate();

                //store sum of filter
                impulseSum = ImpulseResponse.impulse_response_arr_get_sum();
                Log.d("DBUG", String.format("Impulse sum is %f", impulseSum));

                //instantiate a convolver which has a pointer to both the circular buffer and the impulse response array
                Convolve.convolver(CircBuffer.circular_buf_address(0), 0);
                Convolve.convolver(CircBuffer.circular_buf_address(1), 1);


                //immediately start a looping thread that constantly reads the last 50 data and sets the "shaking" flag accordingly
                detectShaking shakeListener = new detectShaking();
                new Thread(shakeListener).start();


                bufferWait waitingTextThread = new bufferWait();
                new Thread(waitingTextThread).start();


                gravity[0]=gravity[1]=gravity[2] = 0;
                accelBuffer[0]=accelBuffer[1]=accelBuffer[2] = 0;

                //set the draggable text to listen, according to onTouch function (defined below)
                //((TextView)findViewById(R.id.movable_text)).setOnTouchListener(this);

                //initialize a SensorEvent
                sensorManager = (SensorManager) Objects.requireNonNull(getActivity()).getSystemService(Context.SENSOR_SERVICE);

                //get the linear accelerometer as object from the system
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

                /*
                //check for accelerometers present
                if (checkAccelerometer() < 0) {
                        Toast.makeText(PosenetActivity.this, "No accelerometer found.", Toast.LENGTH_SHORT).show();
                }


                //set click listener for the RESET button
                ((Button)findViewById(R.id.move_button)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(MainActivity.this, "RESETTING", Toast.LENGTH_SHORT).show();
                        reset();
                    }
                });
                 */
        }

        @Override
        public void onPause() {
                closeCamera();
                //stopBackgroundThread();
                super.onPause();
                //openGLView.onPause();
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
                //Log.i("DBUG", "onImageAvailable");

                // We need to wait until we have some size from onPreviewSizeChosen
                if (previewWidth == 0 || previewHeight == 0) {
                        return;
                }

                //acquire the latest image from the the ImageReader queue
                Image image = imageReader.acquireLatestImage();
                if (image == null) {
                        return;
                }


                //get the planes from the image
                Image.Plane[] planes = image.getPlanes();


                fillBytes(planes, yuvBytes);

                Image.Plane copy = planes[0];

                //get raw bytes from incoming 2d image
                ByteBuffer byteBuffer = copy.getBuffer();

                //create new array of raw bytes of the appropriate size (remaining)
                byte[] buffer = new byte[byteBuffer.remaining()];

                //store the ByteBuffer in the raw byte array
                byteBuffer.get(buffer);

                //instantiate new Matrix object to hold the image pixels
                Mat imageGrab = new Mat();

                //put all of the bytes into the Mat
                imageGrab.put(0,0,buffer);


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

                //testing convert bitmap to OpenCV Mat
                Mat testMat = new Mat();

                org.opencv.android.Utils.bitmapToMat(rotatedBitmap, testMat);

                Log.i(TAG, String.format("Focal length found is %d", testMat.cols()));

                // Camera internals
                double focal_length = testMat.cols(); // Approximate focal length.

                Point center = new Point(testMat.cols()/2f,testMat.rows()/2f);

                Log.i(TAG, String.format("Center at %f, %f", center.x, center.y));

                //create a 3x3 camera (intrinsic params) matrix
                cameraMatrix = Mat.eye(3, 3, CvType.CV_64F);

                double[] vals = {focal_length, 0, center.x, 0, focal_length, center.y, 0, 0, 1};

                //populate the 3x3 camera matrix
                cameraMatrix.put(0, 0, vals);

                /*
                cameraMatrix.put(0, 0, 400);
                cameraMatrix.put(1, 1, 400);
                cameraMatrix.put(0, 2, 640 / 2f);
                cameraMatrix.put(1, 2, 480 / 2f);

                 */

                distortionMat = new MatOfDouble(0,0,0,0);

                //new mat objects to store rotation and translation matrices from camera coords to world coords when solvePnp runs
                rotationMat = new Mat();
                translationMat = new Mat();

                //send the final bitmap to be drawn on and output to the screen
                processImage(rotatedBitmap);
        }
}

/** Crop Bitmap to maintain aspect ratio of model input. */
private Bitmap cropBitmap(Bitmap bitmap) {
        float bitmapRatio = (float)bitmap.getHeight() / bitmap.getWidth();

        float modelInputRatio = (float)Constants.MODEL_HEIGHT / Constants.MODEL_WIDTH;

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
        paint.setTextSize(70.0f);
        paint.setStrokeWidth(8.0f);
}

private int noseFound = 0;
private float noseOriginX, noseOriginY, lastNosePosX, lastNosePosY;

/** Draw bitmap on Canvas.   */
//the Canvas class holds the draw() calls. To draw something, you need 4 basic components: A Bitmap to hold the pixels,
// a Canvas to host the draw calls (writing into the bitmap),
// a drawing primitive (e.g. Rect, Path, text, Bitmap), and a paint (to describe the colors and styles for the drawing).
private void draw(Canvas canvas, Person person, Bitmap bitmap) {
        //draw clear nothing color to the screen (needs this to clear out the old text and stuff)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        // Draw `bitmap` and `person` in square canvas.
        int screenWidth, screenHeight, left, right, top, bottom, canvasHeight, canvasWidth;

        int rightEyeFound = 0, leftEyeFound = 0;

        float xValue, yValue, xVel, yVel;

        BodyPart currentPart;
        Position leftEye = null, rightEye = null;

        //get the dimensions of our drawing canvas
        canvasHeight = canvas.getHeight();
        canvasWidth = canvas.getWidth();

        //check screen orientation: if portrait mode, set the camera preview square appropriately
        if (canvasHeight > canvasWidth) {
                screenWidth = canvasWidth;
                screenHeight = canvasWidth;
                left = 0;
                top = (canvasHeight - canvasWidth) / 2;
        }

        //otherwise if landscape mode, set the width and height of the camera preview square appropriately
        else {
                screenWidth = canvasHeight;
                screenHeight = canvasHeight;
                left = (canvasWidth - canvasHeight) / 2;
                top = 0;
        }

        //right is right edge of screen if portrait mode; otherwise it's in middle of screen
        right = left + screenWidth;
        bottom = top + screenHeight;

        //set up the Paint tool
        setPaint();

        //draw the camera preview square bitmap on the screen
        //Android fxn documentation: Draw the specified bitmap, scaling/translating automatically to fill the destination rectangle.
        //*If the source rectangle is not null, it specifies the subset of the bitmap to draw.
        //This function ignores the density associated with the bitmap. This is because the source and destination rectangle
        // coordinate spaces are in their respective densities, so must already have the appropriate scaling factor applied.
        canvas.drawBitmap(bitmap, /*src*/new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()),
                /*dest*/new Rect(left, top, right, bottom), paint);


        //Next need to calculate ratios used to scale image back up from the 257x257 passed to PoseNet to the actual display

        //divide the available screen width pixels by PoseNet's required number of width pixels to get the number of real screen pixels
        //widthwise per posenet input image "pixel"
        float widthRatio = (float) screenWidth / Constants.MODEL_WIDTH;

        //divide the available screen height pixels by PoseNet's required number of height pixels to get number of real screen pixels
        //heightwise per posenet input image "pixel"
        float heightRatio = (float) screenHeight / Constants.MODEL_HEIGHT;

        //get the keypoints list ONCE at the beginning
        List<KeyPoint> keyPoints = person.getKeyPoints();

        //Log.d(TAG, String.format("Found %d keypoints for the person", keyPoints.size()));

        // Draw key points of the peron's body parts over the camera image
        for (KeyPoint keyPoint : person.getKeyPoints()) {
                //get the body part ONCE at the beginning
                currentPart = keyPoint.getBodyPart();

                //make sure we're confident enough about where this posenet pose is to display it
                if (keyPoint.getScore() > minConfidence) {
                        Position position = keyPoint.getPosition();
                        xValue = (float) position.getX();
                        yValue = (float) position.getY();

                        //the real x value for this body part dot should be the xValue PoseNet found in its 257x257 input bitmap multiplied
                        //by the number of real Android display (or at least Canvas) pixels per Posenet input bitmap pixel
                        float adjustedX = (float) xValue * widthRatio + left; //x value adjusted for actual Android display
                        float adjustedY = (float) yValue * heightRatio + top; //y value adjusted for actual Android display

                        //I'll start by just using the person's nose to try to estimate how fast the phone is moving
                        if (currentPart == BodyPart.NOSE) {
                                //add nose to first slot of Point array for pose estimation
                                humanActualRaw[0] = new Point(adjustedX, adjustedY);

                                if (noseFound == 0) {
                                        noseFound = 1;
                                        setInitialNoseLocation(adjustedX, adjustedY);
                                }

                                else {
                                        //compute the displacement from the starting position that the nose has traveled (helper fxn)
                                        computeDisplacement(adjustedX, adjustedY);
                                }
                        }
                        else if (currentPart == BodyPart.LEFT_EYE) {
                                //add nose to first slot of Point array for pose estimation
                                humanActualRaw[1] = new Point(adjustedX, adjustedY);

                                leftEyeFound = 1;
                                leftEye = new Position(adjustedX, adjustedY);

                                //if we've also already found right eye, we have both eyes. Send data to the scale computer
                                if (rightEyeFound == 1) {
                                        computeScale(leftEye, rightEye);
                                }
                        }
                        else if (currentPart == BodyPart.RIGHT_EYE) {
                                //add nose to first slot of Point array for pose estimation
                                humanActualRaw[2] = new Point(adjustedX, adjustedY);

                                rightEyeFound = 1;
                                rightEye = new Position(adjustedX, adjustedY);

                                //if we've also already found left eye, we have both eyes. Send data to the scale computer
                                if (leftEyeFound == 1) {
                                        computeScale(leftEye, rightEye);
                                }
                        }

                        else if (currentPart == BodyPart.RIGHT_SHOULDER) {
                                //add nose to first slot of Point array for pose estimation
                                humanActualRaw[3] = new Point(adjustedX, adjustedY);
                        }
                        else if (currentPart == BodyPart.LEFT_SHOULDER) {
                                //add nose to first slot of Point array for pose estimation
                                humanActualRaw[4] = new Point(adjustedX, adjustedY);
                        }

                        //draw the point corresponding to this body joint
                        canvas.drawCircle(adjustedX, adjustedY, circleRadius, paint);
                }

                //if this point is the nose but we've lost our lock on it (confidence level is low)
                else if (currentPart == BodyPart.NOSE) {
                        //set noseFound back to 0
                        noseFound = 0;

                        //reset velocity array
                        velocity[0] = velocity[1] = 0;
                }
        }

        //draw the lines of the person's limbs
        for (Pair line : bodyJoints) {
                assert line.first != null;
                assert line.second != null;

                if ((keyPoints.get(BodyPart.getValue((BodyPart) line.first)).getScore() > minConfidence) &&
                        (keyPoints.get(BodyPart.getValue((BodyPart) line.second)).getScore() > minConfidence)) {

                        //draw a line for this "limb" using coordinates of the two BodyPart points and the scaling ratios again
                        canvas.drawLine(
                                keyPoints.get(BodyPart.getValue((BodyPart) line.first)).getPosition().getX() * widthRatio + left,
                                keyPoints.get(BodyPart.getValue((BodyPart) line.first)).getPosition().getY() * heightRatio + top,
                                keyPoints.get(BodyPart.getValue((BodyPart) line.second)).getPosition().getX() * widthRatio + left,
                                keyPoints.get(BodyPart.getValue((BodyPart) line.second)).getPosition().getY() * heightRatio + top,
                                //the paint tool we've set up
                                paint
                        );
                }
        }


        if (humanActualRaw[0]!=null && humanActualRaw[1]!=null && humanActualRaw[2]!=null && humanActualRaw[3]!=null &&
                humanActualRaw[4]!=null) {

                //clear out the ArrayList
                humanActualList.clear();

                //compute pose estimation and draw line coming out of nose
                humanActualList.add(humanActualRaw[0]);
                humanActualList.add(humanActualRaw[1]);
                humanActualList.add(humanActualRaw[2]);
                humanActualList.add(humanActualRaw[3]);
                humanActualList.add(humanActualRaw[4]);

                humanActualMat.fromList(humanActualList);

                //now should have everything we need to run solvePnP


                Calib3d.solvePnP(humanModelMat, humanActualMat, cameraMatrix, distortionMat, rotationMat, translationMat);

                //Now we'll try projecting a 3D point (0, 0, 1000.0) onto the image plane. We'll use this to draw line sticking out of
                //the nose
                MatOfPoint3f testPtMat = new MatOfPoint3f();
                testPtMat.fromList(testPtList);

                MatOfPoint2f imagePts = new MatOfPoint2f();

                Calib3d.projectPoints(testPtMat, rotationMat, translationMat, cameraMatrix, distortionMat, imagePts);

                Log.i(TAG, String.format("Resulting imagepts Mat is of size %d x %d", imagePts.rows(), imagePts.cols()));

                double[] temp_double = imagePts.get(0,0);

                Log.i(TAG, String.format("Found point %f, %f to draw line to from nose", temp_double[0], temp_double[1]));

                canvas.drawLine((float)humanActualRaw[0].x, (float)humanActualRaw[0].y, (float)temp_double[0], (float)temp_double[1], paint);

        }
        else {
                Log.e(TAG, "Not everything found");
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

        //print out the time it took to do calculation of this frame
        canvas.drawText(
                String.format("Time to run image: %.2f ms", posenet.getLastInferenceTimeNanos() * 1.0f / 1_000_000),
                (15.0f * widthRatio),
                (70.0f * heightRatio + bottom),
                paint
        );

        //print out velocity vector values
        canvas.drawText(
                String.format("Velocity(m/s) X: %.2f, Y: %.2f", velocity[0], velocity[1]),
                (15.0f * widthRatio),
                (90.0f * heightRatio + bottom),
                paint
        );

        //draw/push the Canvas bits to the screen
        surfaceHolder.unlockCanvasAndPost(canvas);
}

private void displacementOnly(Person person, Canvas canvas) {
        // Draw `bitmap` and `person` in square canvas.
        int screenWidth, screenHeight, left, right, top, bottom, canvasHeight, canvasWidth;

        int rightEyeFound = 0, leftEyeFound = 0;

        float xValue, yValue, xVel, yVel;

        BodyPart currentPart;
        Position leftEye = null, rightEye = null;

        //get the dimensions of our drawing canvas
        //canvasHeight = canvas.getHeight();
        //canvasWidth = canvas.getWidth();

        canvasHeight = 2280;
        canvasWidth = 1080;


        //check screen orientation: if portrait mode, set the camera preview square appropriately
        if (canvasHeight > canvasWidth) {
                screenWidth = canvasWidth;
                screenHeight = canvasWidth;
                left = 0;
                top = (canvasHeight - canvasWidth) / 2;
        }

        //otherwise if landscape mode, set the width and height of the camera preview square appropriately
        else {
                screenWidth = canvasHeight;
                screenHeight = canvasHeight;
                left = (canvasWidth - canvasHeight) / 2;
                top = 0;
        }

        //right is right edge of screen if portrait mode; otherwise it's in middle of screen
        right = left + screenWidth;
        bottom = top + screenHeight;


        //Next need to calculate ratios used to scale image back up from the 257x257 passed to PoseNet to the actual display

        //divide the available screen width pixels by PoseNet's required number of width pixels to get the number of real screen pixels
        //widthwise per posenet input image "pixel"
        float widthRatio = (float) screenWidth / Constants.MODEL_WIDTH;

        //divide the available screen height pixels by PoseNet's required number of height pixels to get number of real screen pixels
        //heightwise per posenet input image "pixel"
        float heightRatio = (float) screenHeight / Constants.MODEL_HEIGHT;

        //get the keypoints list ONCE at the beginning
        List<KeyPoint> keyPoints = person.getKeyPoints();

        //Log.d(TAG, String.format("Found %d keypoints for the person", keyPoints.size()));


        // Draw key points of the peron's body parts over the camera image
        for (KeyPoint keyPoint : person.getKeyPoints()) {
                //get the body part ONCE at the beginning
                currentPart = keyPoint.getBodyPart();

                //make sure we're confident enough about where this posenet pose is to display it
                if (keyPoint.getScore() > minConfidence) {
                        Position position = keyPoint.getPosition();
                        xValue = (float) position.getX();
                        yValue = (float) position.getY();

                        //the real x value for this body part dot should be the xValue PoseNet found in its 257x257 input bitmap multiplied
                        //by the number of real Android display (or at least Canvas) pixels per Posenet input bitmap pixel
                        float adjustedX = (float) xValue * widthRatio + left; //x value adjusted for actual Android display
                        float adjustedY = (float) yValue * heightRatio + top; //y value adjusted for actual Android display

                        //I'll start by just using the person's nose to try to estimate how fast the phone is moving
                        if (currentPart == BodyPart.NOSE) {
                                if (noseFound == 0) {
                                        noseFound = 1;
                                        setInitialNoseLocation(adjustedX, adjustedY);
                                }

                                else {
                                        //compute the displacement from the starting position that the nose has traveled (helper fxn)
                                        computeDisplacement(adjustedX, adjustedY);
                                }
                        }
                        else if (currentPart == BodyPart.LEFT_EYE) {
                                leftEyeFound = 1;
                                leftEye = new Position(adjustedX, adjustedY);

                                //if we've also already found right eye, we have both eyes. Send data to the scale computer
                                if (rightEyeFound == 1) {
                                        computeScale(leftEye, rightEye);
                                }
                        }
                        else if (currentPart == BodyPart.RIGHT_EYE) {
                                rightEyeFound = 1;
                                rightEye = new Position(adjustedX, adjustedY);

                                //if we've also already found left eye, we have both eyes. Send data to the scale computer
                                if (leftEyeFound == 1) {
                                        computeScale(leftEye, rightEye);
                                }
                        }

                        //draw the point
                        //canvas.drawCircle(adjustedX, adjustedY, circleRadius, paint);
                }
                //if this point is the nose but we've lost our lock on it (confidence level is low)
                else if (currentPart == BodyPart.NOSE) {
                        //set noseFound back to 0
                        noseFound = 0;

                        //reset velocity array
                        velocity[0] = velocity[1] = 0;
                }
        }

}

private void setInitialNoseLocation(float x, float y) {
        noseOriginX = lastNosePosX = x;
        noseOriginY = lastNosePosY = y;
        Log.d(TAG, String.format("Nose origin set to %f, %f", x, y));
}

private float[] velocity = new float[2];

private void computeDisplacement(float x, float y) {
        //compute vector of displacement
        noseDeltaX = (x - lastNosePosX) * mPerPixel;
        noseDeltaY = (y - lastNosePosY) * mPerPixel;

        NtempAcc[0] = noseDeltaX;
        NtempAcc[1] = noseDeltaY;
        NtempAcc[2] = 0;

        Utils.lowPassFilter(NtempAcc, Nacc, NaiveConstants.LOW_PASS_ALPHA);

        /*
        for (int i=0; i<3; i++) {
                //find position friction to be applied using last position reading
                float pFrictionToApply = NaiveConstants.POSITION_FRICTION_DEFAULT * Nposition[i];
                Nposition[i] +=  - pFrictionToApply;
        }

         */

        //move the noshake box accordingly


        //set the current nose position to the last nose position
        lastNosePosX = x;
        lastNosePosY = y;

        Log.d(TAG, String.format("X displacement %f, Y displacement %f", noseDeltaX, noseDeltaY));

        float time = posenet.getLastInferenceTimeNanos() / 1000000000f;

        //now we'll try to calculate the velocity by using time of each frame
        float velocityX = noseDeltaX/time;
        float velocityY = noseDeltaY/time;

        //set the global fields
        velocity[0] = velocityX;
        velocity[1] = velocityY;

        Log.d(TAG, String.format("X velocity %f, Y velocity %f", velocityX, velocityY));
}


//how many real-world meters each pixel in the camera image represents
private float mPerPixel;

//compute how much distance each pixel currently represents in real life, using known data about avg human pupillary distance
private float computeScale(Position leftEye, Position rightEye) {
        //I'll just use the x distance between left eye and right eye points to get distance in pixels between eyes
        //don't forget left eye is on the right and vice versa
        float distance = leftEye.getX() - rightEye.getX();

        Log.d(TAG, String.format("Pupillary distance in pixels: %f", distance));

        //now we want to find out how many real meters each pixel on the display corresponds to
        float scale = Constants.PD/distance;
        mPerPixel = scale;

        Log.d(TAG, String.format("Each pixel on the screen represents %f meters in real life in plane of peron's face", scale));

        return 0;
}

//use calculated meters per pixel and pixel displacement to calculate estimated velocity of the phone (or person, for now)
//the issue of relativity here needs to be fixed
private float computeVelocity() {
        return 0;
}

//Process image using Posenet library. The image needs to be scaled in order to fit Posenet's input dimension requirements of
//257 x 257 (defined in Constants.java), and probably needs to be cropped in order to preserve the image's aspect ratio
private void processImage(Bitmap bitmap) {
        // Crop bitmap.
        Bitmap croppedBitmap = cropBitmap(bitmap);

        // Created scaled version of bitmap for model input.
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, Constants.MODEL_WIDTH, Constants.MODEL_HEIGHT, true);

        // Perform inference.
        Person person = posenet.estimateSinglePose(scaledBitmap);

        Canvas canvas = surfaceHolder.lockCanvas();

        /*
        if (canvas == null) {
                Log.e("DEBUG", "processImage: canvas came up NULL");
                return;
        }

         */
        draw(canvas, person, scaledBitmap);

        //displacementOnly(person, canvas);
}


//Creates a new [CameraCaptureSession] for camera preview.
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

        @Override
        public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                        //layoutSensor.setVisibility(View.INVISIBLE);

                        //noShake implementation
                        //noShake钟林(event);

                        //more naive implementation
                        //naivePhysicsImplementation(event);
                }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }


        public void naivePhysicsImplementation(SensorEvent event) {
                if (timestamp != 0)
                {
                        //fill the temporary acceleration vector with the current sensor readings
                        NtempAcc[0] = Utils.rangeValue(event.values[0], -NaiveConstants.MAX_ACC, NaiveConstants.MAX_ACC);
                        NtempAcc[1] = Utils.rangeValue(event.values[1], -NaiveConstants.MAX_ACC, NaiveConstants.MAX_ACC);
                        NtempAcc[2] = Utils.rangeValue(event.values[2], -NaiveConstants.MAX_ACC, NaiveConstants.MAX_ACC);

                        //apply lowpass filter and store results in acc float array
                        Utils.lowPassFilter(NtempAcc, Nacc, NaiveConstants.LOW_PASS_ALPHA);

                        //get change in time, convert from nanoseconds to seconds
                        float dt = (event.timestamp - timestamp) * NaiveConstants.NANOSEC_TO_SEC;

                        //get velocity and position
                        for (int i = 0; i < 3; i++)
                        {
                                //find friction to be applied using last velocity reading
                                float vFrictionToApply = NaiveConstants.VELOCITY_FRICTION_DEFAULT * Nvelocity[i];
                                Nvelocity[i] += (Nacc[i] * dt) - vFrictionToApply;

                                //if resulting value is Nan or infinity, just change it to 0
                                Nvelocity[i] = Utils.fixNanOrInfinite(Nvelocity[i]);

                                //find position friction to be applied using last position reading
                                float pFrictionToApply = NaiveConstants.POSITION_FRICTION_DEFAULT * Nposition[i];
                                Nposition[i] += (Nvelocity[i] * NaiveConstants.VELOCITY_AMPL_DEFAULT * dt) - pFrictionToApply;

                                //set max limits on the position change
                                Nposition[i] = Utils.rangeValue(Nposition[i], -NaiveConstants.MAX_POS_SHIFT, NaiveConstants.MAX_POS_SHIFT);
                        }
                }

                //if timestamp is 0, we just started
                else
                {
                        Nvelocity[0] = Nvelocity[1] = Nvelocity[2] = 0f;
                        Nposition[0] = Nposition[1] = Nposition[2] = 0f;

                        //fill in the acceleration float array
                        Nacc[0] = Utils.rangeValue(event.values[0], -NaiveConstants.MAX_ACC, NaiveConstants.MAX_ACC);
                        Nacc[1] = Utils.rangeValue(event.values[1], -NaiveConstants.MAX_ACC, NaiveConstants.MAX_ACC);
                        Nacc[2] = Utils.rangeValue(event.values[2], -NaiveConstants.MAX_ACC, NaiveConstants.MAX_ACC);
                }

                //set timestamp to the current time of the sensor reading in nanoseconds
                timestamp = event.timestamp;

                //set the position of the text based on x and y axis values in position float array
                layoutSensor.setTranslationX(-Nposition[0]);
                layoutSensor.setTranslationY(Nposition[1]);
        }

        private void reset()
        {
                Nposition[0] = Nposition[1] = Nposition[2] = 0;
                Nvelocity[0] = Nvelocity[1] = Nvelocity[2] = 0;
                timestamp = 0;

                CircBuffer.circular_buffer_destroy(0);
                CircBuffer.circular_buffer_destroy(1);

                Convolve.convolver_destroy(0);
                Convolve.convolver_destroy(1);

                //initialize a circular buffer of 211 floats
                CircBuffer.circular_buffer(NoShakeConstants.buffer_size, 0);
                CircBuffer.circular_buffer(NoShakeConstants.buffer_size, 1);

                Convolve.convolver(CircBuffer.circular_buf_address(0), 0);
                Convolve.convolver(CircBuffer.circular_buf_address(1), 1);

                layoutSensor.setTranslationX(0);
                layoutSensor.setTranslationY(0);
        }

        //check to see if accelerometer is connected; print out the sensors found via Toasts
        public int checkAccelerometer() {
                List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);

                //nothing found, something's wrong, error
                if (sensors.isEmpty()) {
                        Toast.makeText(Objects.requireNonNull(getActivity()).getApplicationContext(), "No accelerometers found, try restarting device.", Toast.LENGTH_SHORT).show();
                        return -1;
                }

        /*
        Toast.makeText(MainActivity.this, String.format("%d accelerometers found", sensors.size()), Toast.LENGTH_SHORT).show();

        int index=0;
        for (Sensor thisSensor : sensors) {
            Toast.makeText(MainActivity.this, String.format("Sensor #%d: ", index++) + thisSensor.getName(), Toast.LENGTH_SHORT).show();
        }
        */
                return 0;
        }

        //implementation of LZ's NoShake version
        private void noShake钟林(SensorEvent event) {
                StempAcc[0] = Utils.rangeValue(event.values[0], -NaiveConstants.MAX_ACC, NaiveConstants.MAX_ACC);
                StempAcc[1] = Utils.rangeValue(event.values[1], -NaiveConstants.MAX_ACC, NaiveConstants.MAX_ACC);

                //apply the low pass filter to reduce noise
                Utils.lowPassFilter(StempAcc, Sacc, NoShakeConstants.low_pass_alpha);

                /*
                //to speed things up, start a separate thread to go write the acceleration data to the buffer while we finish calculations here
                getDataWriteBuffer writerThread = new getDataWriteBuffer(Sacc[0]);
                new Thread(writerThread).start();
                 */

                //try to eliminate noise by knocking low values down to 0 (also make text re-center faster)
                if (Math.abs(Sacc[0]) <= 0.5) {
                        Sacc[0] = 0;
                }
                if (Math.abs(Sacc[1]) <= 0.5) {
                        Sacc[1] = 0;
                }

                //apply some extra friction (hope is to make text return to center of screen a little faster)
                //rapid decreases will be highlighted by this
                float xFrixToApply = accAfterFrix[0] * NoShakeConstants.extra_frix_const;
                float yFrixToApply = accAfterFrix[1] * NoShakeConstants.extra_frix_const;

                accAfterFrix[0] = Sacc[0] - xFrixToApply;
                accAfterFrix[1] = Sacc[1] - yFrixToApply;

                int h = CircBuffer.circular_buf_put(accAfterFrix[0], 0);
                int l = CircBuffer.circular_buf_put(accAfterFrix[1], 1);

                //DEBUGGING
                //Log.d("TIME", String.format("%f", timeElapsed));
                //Log.d("H VALUE", String.format("%f", HofT));

                /* IF USING NORMAL ACCELEROMETER
                //use low-pass filter to affect the gravity readings slightly based on what they were before
                gravity[0] = NoShakeConstants.alpha * gravity[0] + (1 - NoShakeConstants.alpha) * event.values[0];
                gravity[1] = NoShakeConstants.alpha * gravity[1] + (1 - NoShakeConstants.alpha) * event.values[1];
                gravity[2] = NoShakeConstants.alpha * gravity[2] + (1 - NoShakeConstants.alpha) * event.values[2];
                 */

                //Log.d("Y of T", String.format("%f", YofT));

                /*
                //calculate how much the acceleration changed from what it was before
                deltaX = x - accelBuffer[0];
                deltaY = y - accelBuffer[1];
                deltaZ = z - accelBuffer[2];

                //calculate overall acceleration vector
                float accelSqRt = (x * x + y * y + z * z) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
                 */

                //update the stats on the UI to show the accelerometer readings
                //((TextView) findViewById(R.id.x_axis)).setText(String.format("X accel: %f", Sacc[0]));
                //((TextView) findViewById(R.id.y_axis)).setText(String.format("Y accel: %f", Sacc[1]));
                //((TextView) findViewById(R.id.z_axis)).setText(String.format("Z accel: %f", z));

                //this is a check to see whether the device is shaking
                //if (shaking==1) { //empirically-determined threshold in order to keep text still when not really shaking
                //convolve the circular buffer of acceleration data with the impulse response array to get Y(t) array
                float f = Convolve.convolve(0, CircBuffer.circular_buf_get_head(0));
                float y = Convolve.convolve(1, CircBuffer.circular_buf_get_head(1));

                float deltaX = 0;
                float deltaY = 0;

                for (int i=0; i < NoShakeConstants.buffer_size; i++) {
                        float impulseValue = ImpulseResponse.impulse_response_arr_get_value(i);
                        deltaX += impulseValue * Convolve.getTempXMember(i, 0);
                        deltaY += impulseValue * Convolve.getTempXMember(i, 1);
                }

                //normalize the scale of filters with arbitrary length/magnitude
                deltaX /= impulseSum;
                deltaY /= impulseSum;

                //toMoveX = (deltaX - NaiveConstants.POSITION_FRICTION_DEFAULT * deltaX) * NoShakeConstants.yFactor;
                toMoveX = NtempAcc[0] * 500;

                /*
                if (Math.abs(toMoveX) < 10) {
                        //toMoveX = 0;
                }

                 */

                //Log.d("DBUG", String.format("To move x is %f", toMoveX));
                myRenderer.toMoveX = toMoveX/1000f;
                //layoutSensor.setTranslationX(Utils.rangeValue(toMoveX, -NaiveConstants.MAX_POS_SHIFT, NaiveConstants.MAX_POS_SHIFT));

                //toMoveY = -1 * (deltaY - NaiveConstants.POSITION_FRICTION_DEFAULT * deltaY) * NoShakeConstants.yFactor;
                toMoveY = NtempAcc[1] * 500;

                /*
                if (Math.abs(toMoveY) < 10) {
                        //toMoveY = 0;
                }

                 */
                myRenderer.toMoveY = toMoveY/1000f;
                //layoutSensor.setTranslationY(toMoveY);

            /*
            //print out convolved signal array on the log
            for (int i=0; i<NoShakeConstants.buffer_size; i++) {
                Log.d("XARRAY", String.format("Index %d: %f", i, Convolve.getXMember(i, 0)));
            }


            //print out convolved signal array on the log
            for (int i=0; i<Convolve.getYSize(); i++) {
                Log.d("YARRAY", String.format("Index %d: %f", i, Convolve.getYMember(i)));
            }
            */

                //float toMoveX = Utils.rangeValue((float)(NoShakeConstants.yFactor * YofT), -NaiveConstants.MAX_POS_SHIFT, NaiveConstants.MAX_POS_SHIFT);

                //** Move the view containing the text by the calculated amount of pixels
                //layoutSensor.setTranslationX(toMoveX);

                //get current layout parameters (current position, etc) of the NoShake sample text
                //editedLayoutParams = (RelativeLayout.LayoutParams) noShakeText.getLayoutParams();

                    /* INCORRECT IMPLEMENTATION
                    //adjust the x position of the NoShake text, making sure it doesn't go off the screen
                    editedLayoutParams.leftMargin+=yFactor * YofT;

                    //adjust the y position of the NoShake text, making sure it doesn't go off the screen
                    editedLayoutParams.topMargin+=yFactor * YofT;

                    //set right and bottom margins to avoid compression
                    editedLayoutParams.rightMargin = -250;
                    editedLayoutParams.bottomMargin = -250;

                    //set new layout parameters for the view (save changes)
                    noShakeText.setLayoutParams(editedLayoutParams);

                    //refresh the view
                    noShakeText.invalidate();
                    */

                //}
                //Log.d("DBUG", String.format("From x: %f", Convolve.getXMember(5, 0)));
                //Log.d("DBUG", String.format("From y: %f", Convolve.getXMember(5, 1)));
        }
}
