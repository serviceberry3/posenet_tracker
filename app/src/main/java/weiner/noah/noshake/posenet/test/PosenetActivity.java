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
import android.os.Environment;
import android.os.Handler;;
import android.os.HandlerThread;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.os.SystemClock;
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

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
//kotlin.math.abs?

import weiner.noah.noshake.posenet.test.ctojavaconnector.CircBuffer;
import weiner.noah.noshake.posenet.test.ctojavaconnector.Convolve;
import weiner.noah.noshake.posenet.test.ctojavaconnector.ImpulseResponse;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;
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
private Paint redPaint = new Paint();
private Paint bluePaint = new Paint();
private Paint greenPaint = new Paint();
private Paint whitePaint = new Paint();

/** A shape for extracting frame data.   */
private int PREVIEW_WIDTH = 640;
private int PREVIEW_HEIGHT = 480;
public static final String ARG_MESSAGE = "message";


//Macros for 'looking' variable
private final int LOOKING_LEFT = 0;
private final int LOOKING_RIGHT = 1;

/**
 * Tag for the [Log].
 */
private String TAG = "PosenetActivity";

private String FRAGMENT_DIALOG = "dialog";

//Whether to use front- or rear-facing camera
private final boolean USE_FRONT_CAM = true;

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

//canvas that displays relevant info on the screen
private Canvas infoCanvas;

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

private Person person = null;

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
private double HofT, YofT, timeElapsed;

//the raw values that the low-pass filter is applied to
private float[] gravity = new float[3];

//working on circular buffer for the data
private float[] accelBuffer = new float[3];

private float impulseSum;

//is the device shaking??
private volatile int shaking = 0;

int cnt = 0;
double averager = 0;

private int index = 0, check = 0, times = 0;

private Thread outputPlayerThread=null;

private OpenGLRenderer myRenderer;

private OpenGLView openGLView;

public static float toMoveX, toMoveY;

float noseDeltaX, noseDeltaY;

//declare global matrix containing my model 3D coordinates of human pose, to be used for camera pose estimation
private Point3[] humanModelRaw = new Point3[6];
private List<Point3> humanModelList = new ArrayList<Point3>();
private MatOfPoint3f humanModelMat;

//declare global matrix containing the actual 2D coordinates of the human found
private Point[] humanActualRaw = new Point[6];

//used for bounding box points
private Point[] boundingBox = new Point[4];

private List<Point> humanActualList = new ArrayList<Point>();
private MatOfPoint2f humanActualMat;

//matrices to be used for pose estimation calculation
private Mat cameraMatrix, rotationMat, translationMat;
private MatOfDouble distortionMat;

Point3[] testPts = new Point3[3];
List<Point3> testPtList = new ArrayList<Point3>();

List<Point> joints = new ArrayList<>();

private final boolean SHOULD_SAVE_IMG = false;
private final boolean USE_LK = true;

//which direction the person is looking (split at exactly perp to camera)
private int looking;

//declare floats for computing actual 2D dist found between nose and eyes and shoulders
//this lets us deduce whether the person is looking left or rt (we need to swap axes)
private float distToLeftEyeX, distToRightEyeX, distToLeftShouldX, distToRtShouldX;

//float for finding center of human bust (pt between shoulders) in 2D coordinates, used as "origin" for drawing
private float torsoCtrX, torsoCtrY;

private Point torsoCenter;


//BEGIN COPIED FROM TOMASIKANADE

//Mat class represents an n-dimensional dense numerical single-channel or multi-channel array. Can be used to store real or
//complex-valued vectors and matrices, grayscale or color images, voxel volumes, vector fields, point clouds, tensors, histograms

//use 2 Mats to store the camera image, one in color (RGB), and one black and white
private Mat sceneGrayScale, sceneColor, mGray, mPrevGray, result, image;


private CameraBridgeViewBase mOpenCvCameraView;

private int discards, numPts;

private double[]means;

//break the key features into two groups based on displacement
private final int numGroups = 2;

//Values needed for the corner detection algorithm Most likely have to tweak them to suit needs. Could also
//let the application find out the best values by itself.
private final static double qualityLevel = 0.25; //.35
private final static double minDistance = 10;
private final static int blockSize = 8;
private final static boolean useHarrisDetector = false;
private final double k = 0.0;
private final static int maxCorners = 100;
private final static Scalar circleColor = new Scalar(255, 255, 0);

Point[] goodFeaturesPrev = null, goodFeaturesNext = null;

//initialize two matrices of points
MatOfPoint2f prevFeatures, nextFeatures, thisFeatures, savedFeatures;
MatOfPoint features;

MatOfByte status;
MatOfFloat err;
List<Point> points;


KeyFeature[] cornerList, cornerListSorted;

List<Point> prevList, nextList, cornersFoundGoingBackList, forwardBackErrorList;
List<Byte> byteStatus;
ArrayList<Point> nextListCorrected;

//define a color for drawing
Scalar color = new Scalar(255, 0, 0);

//the time it took to run both algorithms on the frame and get the data back; used to calculate a rough velocity of the device
long lastInferenceTimeNanos = 0;

//time it took to run a frame, whether using Posenet or LK
long startTime = 0, endTime = 0, timeToRunFrame = 0;

//the x and y displacement between the last two frames, along with the velocity calculated
double pointX, pointY, xVel, yVel;

//initialize some doubles to hold the average position of the corners in the previous frame vs the current frame

//average position of the goodFeatures in previous frame
double xAvg1 = 0, yAvg1 = 0,
//average position of the goodFeatures in this frame
xAvg2 = 0, yAvg2 = 0;

private int[] breakPoints;


//END COPIED FROM TOMASIKANADE



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
        if (activity != null)
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
        if (surfaceView == null) {
                Log.e("DEBUG", "onViewCreated: surfaceView came up NULL");
                return;
        }


        surfaceHolder = surfaceView.getHolder();
        if (surfaceHolder == null) {
                Log.e("DEBUG", "onViewCreated: surfaceHolder came up NULL");
        }

        //get the Mats created AFTER OpenCV was loaded successfully
        humanModelMat = ((CameraActivity) Objects.requireNonNull(getActivity())).getHumanModelMat();
        humanActualMat = ((CameraActivity) getActivity()).getHumanActualMat();


        //these are the 3d pts we'd like to draw: essentially projecting a 3D axis magnitude 1000 onto 2D image in middle of person's chest
        testPts[0] = new Point3(1000.0,-1087.5,-918.75);
        testPts[1] = new Point3(0, 2087.5, -918.75);
        testPts[2] = new Point3(0,-1087.5,81.25);

        testPtList.add(testPts[0]);
        testPtList.add(testPts[1]);
        testPtList.add(testPts[2]);
}

private void resetVars() {
        //curImage = new Mat();
        //prevImage = new Mat();
        //flowMat = new Mat();

        image = new Mat(4000, 4000, CvType.CV_8UC1);
        features = new MatOfPoint();
        prevFeatures = new MatOfPoint2f();
        points = new ArrayList<>();
        //cornerList = new ArrayList<>();
        nextFeatures = new MatOfPoint2f();
        thisFeatures = new MatOfPoint2f();
        savedFeatures = new MatOfPoint2f();
        status = new MatOfByte();
        err = new MatOfFloat();
        //mergeSort = new MergeSort();
        discards = 0;

        //create two 257x257 OpenCV Mats to hold consecutive frames on which to run Lucas-Kanade
        mGray = new Mat(Constants.MODEL_HEIGHT, Constants.MODEL_WIDTH, CvType.CV_8UC1);
        mPrevGray = new Mat(mGray.rows(), mGray.cols(), CvType.CV_8UC1);
}

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

        sceneColor = new Mat();
        sceneGrayScale = new Mat();

        resetVars();

        showToast("Added PoseNet submodule fragment into Activity");
        openCamera();

        posenet = new Posenet(Objects.requireNonNull(this.getContext()), "posenet_model.tflite", Device.GPU);

        /*
        //populate the 3D human model
        humanModelRaw[0] = new Point3(0.0f, 0.0f, 0.0f); //nose
        humanModelRaw[1] = new Point3(0.0f, 0.0f, 0.0f); //nose again
        humanModelRaw[2] = new Point3(-215.0f, 170.0f, -135.0f); //left eye ctr WAS -150
        humanModelRaw[3] = new Point3(215.0f, 170.0f, -135.0f); //rt eye ctr

        //humanModelRaw[3] = new Point3(450.0f, -700.0f, -600.0f); //rt shoulder
        //humanModelRaw[4] = new Point3(-450.0f, -700.0f, -600.0f); //left shoulder
         */


        //from real measured coords
        humanModelRaw[0] = new Point3(0.0f, 0.0f, 0.0f); //nose
        humanModelRaw[1] = new Point3(0.0f, 0.0f, 0.0f); //nose
        humanModelRaw[2] = new Point3(-225.0f, 318.75f, -262.5f); //left eye ctr
        humanModelRaw[3] = new Point3(225.0f, 318.75f, -262.5f); //right eye ctr WAS -150
        humanModelRaw[4] = new Point3(-871.875f, -1087.5f, -918.75f); //rt shoulder 450, -700, -600
        humanModelRaw[5] = new Point3(871.875f, -1087.5f, -918.75f); //left shoulder -450, -700, -600

        //push all of the model coordinates into the ArrayList version so they can be converted to a MatofPoint3f
        humanModelList.add(humanModelRaw[0]);
        humanModelList.add(humanModelRaw[1]);
        humanModelList.add(humanModelRaw[2]);
        humanModelList.add(humanModelRaw[3]);
        humanModelList.add(humanModelRaw[4]);
        humanModelList.add(humanModelRaw[5]);

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

                        if (USE_FRONT_CAM && cameraDirection != null && cameraDirection == CameraCharacteristics.LENS_FACING_BACK) {
                                //skip this one because it's a back-facing camera, we wanna use the front-facing
                                continue;
                        }
                        else if (!USE_FRONT_CAM && cameraDirection != null && cameraDirection == CameraCharacteristics.LENS_FACING_FRONT) {
                                //skip this one because it's front-facing cam, we wanna use the rear-facing
                                continue;
                        }

                        previewSize = new Size(PREVIEW_WIDTH, PREVIEW_HEIGHT);

                        imageReader = ImageReader.newInstance(PREVIEW_WIDTH, PREVIEW_HEIGHT, ImageFormat.YUV_420_888, 2);

                        try {
                                //get current orientation of camera sensor
                                sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                        }
                        catch (NullPointerException e) {
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

        //make sure we have camera permission
        if (permissionCamera != PackageManager.PERMISSION_GRANTED) {
                //still need permission to access camera, so get it now
                requestCameraPermission();
        }

        //find and set up the camera
        setUpCameraOutputs();

        CameraManager cameraManager = (CameraManager)Objects.requireNonNull(getActivity()).getSystemService(Context.CAMERA_SERVICE);

        try {
                // Wait for camera to open - 2.5 seconds is sufficient
                if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                        throw new RuntimeException("Time out waiting to lock camera opening.");
                }

                if (backgroundHandler == null) {
                        Log.i(TAG, "Calling cameraManager.openCamera() with null backgroundHandler");
                }
                cameraManager.openCamera(cameraId, new stateCallback(), backgroundHandler);
        }
        catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
        catch (CameraAccessException e) {
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

                //create new byte array in yuvBytes the size of this plane
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

                //We need to wait until we have some size from onPreviewSizeChosen
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

                //put all planes data into 2D byte array called yuvBytes
                fillBytes(planes, yuvBytes);

                //get first plane
                Image.Plane copy = planes[0];

                //get raw bytes from incoming 2d image
                ByteBuffer byteBuffer = copy.getBuffer();

                //create new array of raw bytes of the appropriate size (remaining)
                byte[] buffer = new byte[byteBuffer.remaining()];

                //store the ByteBuffer in the raw byte array (the pixels from first plane of image)
                byteBuffer.get(buffer);

                //instantiate new Matrix object to hold the image pixels
                Mat imageGrab = new Mat();

                //put all of the bytes into the Mat
                imageGrab.put(0,0, buffer);


                ImageUtils imageUtils = new ImageUtils();

                //convert the three planes into single int array called rgbBytes
                imageUtils.convertYUV420ToARGB8888(yuvBytes[0], yuvBytes[1], yuvBytes[2], previewWidth, previewHeight,
                        /*yRowStride=*/ image.getPlanes()[0].getRowStride(),
                        /*uvRowStride=*/ image.getPlanes()[1].getRowStride(),
                        /*uvPixelStride=*/ image.getPlanes()[1].getPixelStride(),
                        rgbBytes //an int[]
                );

                // Create bitmap from int array
                Bitmap imageBitmap = Bitmap.createBitmap(rgbBytes, previewWidth, previewHeight, Bitmap.Config.ARGB_8888);

                /*
                // Create rotated version (FOR PORTRAIT DISPLAY)
                Matrix rotateMatrix = new Matrix();
                rotateMatrix.postRotate(90.0f);

                Bitmap rotatedBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, previewWidth, previewHeight, rotateMatrix, true);*/
                image.close();

                //testing convert bitmap to OpenCV Mat
                Mat testMat = new Mat();

                org.opencv.android.Utils.bitmapToMat(imageBitmap, testMat);

                Log.i(TAG, String.format("Focal length found is %d", testMat.cols()));

                //set up the intrinsic camera matrix and initialize the world-to-camera translation and rotation matrices
                makeCameraMat();

                startTime = SystemClock.elapsedRealtimeNanos();

                //send the final bitmap to be drawn on and output to the screen, only running Posenet every 5 frames (or if USE_LK bool is false)
                processImage(imageBitmap, ((frameCounter % Constants.RUN_PNET_EVERY_N_FRAMES == 0) || !USE_LK ? 1 : 0));

                timeToRunFrame = (SystemClock.elapsedRealtimeNanos() - startTime) / 1000000;


                frameCounter++;
        }
}



private void makeCameraMat() {
        //Camera internals
        double focal_length_x = 526.69; // Approximate focal length, found from OpenCV chessboard calibration
        double focal_length_y = 540.36;

        //center of image plane
        Point center = new Point(313.07,238.39);

        //Log.i(TAG, String.format("Center at %f, %f", center.x, center.y));

        //create a 3x3 camera (intrinsic params) matrix
        cameraMatrix = Mat.eye(3, 3, CvType.CV_64F);

        double[] vals = {focal_length_x, 0, center.x, 0, focal_length_y, center.y, 0, 0, 1};

        //populate the 3x3 camera matrix
        cameraMatrix.put(0, 0, vals);

        /*
        cameraMatrix.put(0, 0, 400);
        cameraMatrix.put(1, 1, 400);
        cameraMatrix.put(0, 2, 640 / 2f);
        cameraMatrix.put(1, 2, 480 / 2f);
         */

        //distortionMat = new MatOfDouble(0,0,0,0);

        /*
        cameraMatrix.put(0, 0, 400);
        cameraMatrix.put(1, 1, 400);
        cameraMatrix.put(0, 2, 640 / 2f);
        cameraMatrix.put(1, 2, 480 / 2f);
         */

        //assume no camera distortion
        distortionMat = new MatOfDouble(new Mat(4, 1, CvType.CV_64FC1));
        distortionMat.put(0,0,0);
        distortionMat.put(1,0,0);
        distortionMat.put(2,0,0);
        distortionMat.put(3,0,0);

        //new mat objects to store rotation and translation matrices from camera coords to world coords when solvePnp runs
        rotationMat = new Mat(1, 3, CvType.CV_64FC1);
        translationMat = new Mat(1, 3, CvType.CV_64FC1);

        //Hack! initialize transition and rotation matrixes to improve estimation
        translationMat.put(0,0,-100);
        translationMat.put(0,0,100);
        translationMat.put(0,0,1000);

        if (distToLeftEyeX < distToRightEyeX) {
                //looking at left
                rotationMat.put(0,0,-1.0);
                rotationMat.put(1,0,-0.75);
                rotationMat.put(2,0,-3.0);
                looking = LOOKING_LEFT;
        }
        else {
                //looking at right
                rotationMat.put(0,0,1.0);
                rotationMat.put(1,0,-0.75);
                rotationMat.put(2,0,-3.0);
                looking = LOOKING_RIGHT;
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

        Mat croppedImage = new Mat();

        org.opencv.android.Utils.bitmapToMat(croppedBitmap, croppedImage);

        /*
        if (capture == 0) {
                Log.i(TAG, "Writing cropped image");
                Imgcodecs.imwrite("/data/data/weiner.noah.noshake.posenet.test/testCaptureCropped.jpg", croppedImage);
        }*/

        return croppedBitmap;
}

/** Set color and size for the paints. */
private void setPaint() {
        redPaint.setColor(Color.RED);
        redPaint.setTextSize(70.0f);
        redPaint.setStrokeWidth(8.0f);

        bluePaint.setColor(Color.BLUE);
        bluePaint.setTextSize(70.0f);
        bluePaint.setStrokeWidth(8.0f);

        greenPaint.setColor(Color.GREEN);
        greenPaint.setTextSize(70.0f);
        greenPaint.setStrokeWidth(8.0f);

        whitePaint.setColor(Color.WHITE);
        whitePaint.setTextSize(70.0f);
        whitePaint.setStrokeWidth(8.0f);
        whitePaint.setStyle(Paint.Style.STROKE);
}

private int noseFound = 0;
private float noseOriginX, noseOriginY, lastNosePosX, lastNosePosY;

/** Draw bitmap on Canvas. */
//the Canvas class holds the draw() calls. To draw something, you need 4 basic components: A Bitmap to hold the pixels,
// a Canvas to host the draw calls (writing into the bitmap),
// a drawing primitive (e.g. Rect, Path, text, Bitmap), and a paint (to describe the colors and styles for the drawing).
private void draw(Canvas canvas, Person person, Bitmap bitmap) { //NOTE: the Bitmap passed here is 257x257 pixels, good for Posenet model
        //save canvas into a global
        infoCanvas = canvas;

        //draw clear nothing color to the screen (needs this to clear out the old text and stuff)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        //Draw `bitmap` and `person` in square canvas.
        int screenWidth, screenHeight, left, right, top, bottom, canvasHeight, canvasWidth;

        int rightEyeFound = 0, leftEyeFound = 0;

        float xValue, yValue, xVel, yVel;
        float dist = 0;

        BodyPart currentPart;
        Position leftEye = null, rightEye = null;

        //get the dimensions of our drawing canvas
        canvasHeight = canvas.getHeight();
        canvasWidth = canvas.getWidth();

        //should be 1080 x 2148 (full screen besides navigation bar)
        Log.i(TAG, String.format("Canvas width and height are %d and %d", canvasWidth, canvasHeight));

        //check screen orientation: if portrait mode, set the camera preview square appropriately
        if (canvasHeight > canvasWidth) {
                screenWidth = canvasWidth;
                screenHeight = canvasWidth; //screenwidth x screenHeight should now be 1080 x 1080
                left = 0;

                //we can find the top of the camera preview square by finding width of the padding on top and bottom of the square
                //the total amt of padding will be the canvasHeight (2148) minus the heigt of camera preview box, then divide by 2 to get
                //amt we need to go down from top of screen to find top of camera preview square
                top = (canvasHeight - canvasWidth) / 2; //should be 534
        }

        //otherwise if landscape mode, set the width and height of the camera preview square appropriately
        else {
                screenWidth = canvasHeight;
                screenHeight = canvasHeight;
                left = (canvasWidth - canvasHeight) / 2;
                top = 0;
        }

        Log.i(TAG, "Left is " + left);

        //right is right edge of screen if portrait mode; otherwise it's in middle of screen
        right = left + screenWidth; //should be 1080

        //find bottom of the camera preview square
        bottom = top + screenHeight; //should be 534 + 1080 = 1614

        //set up the Paint tool
        setPaint();

        int bmWidth = bitmap.getWidth(); //should be 257
        int bmHeight = bitmap.getHeight(); //should be 257

        Log.i(TAG, String.format("Bitmap width and height are %d and %d", bmWidth, bmHeight)); //should be 257x257

        //WHAT IS PT OF THIS??
        int newRectWidth = right - left;
        int newRectHeight = bottom - top;

        double scaleDownRatioVert = newRectHeight / 2280f;
        double scaleDownRatioHoriz = newRectWidth / 1080f;

        Log.i(TAG, String.format("New rect width and height are %d and %d", newRectWidth, newRectHeight));
        Log.i(TAG, String.format("Scaledown ratios are %f and %f", scaleDownRatioHoriz, scaleDownRatioVert));

        //draw the camera preview square bitmap on the screen
        //Android fxn documentation: Draw the specified bitmap, scaling/translating automatically to fill the destination rectangle.
        //*If the source rectangle is not null, it specifies the subset of the bitmap to draw.
        //This function ignores the density associated with the bitmap. This is because the source and destination rectangle
        // coordinate spaces are in their respective densities, so must already have the appropriate scaling factor applied.
        canvas.drawBitmap(bitmap, /*src*/new Rect(0, 0, bmWidth, bmHeight), //in other words draw whole bitmap
                /*dest*/new Rect(left, top, right, bottom), redPaint);


        //Next need to calculate ratios used to scale image back up from the 257x257 passed to PoseNet to the actual display

        //divide the available screen width pixels by PoseNet's required number of width pixels to get the number of real screen pixels
        //widthwise per posenet input image "pixel"
        float widthRatio = (float) screenWidth / Constants.MODEL_WIDTH; //should be 1080/257

        //divide the available screen height pixels by PoseNet's required number of height pixels to get number of real screen pixels
        //heightwise per posenet input image "pixel"
        float heightRatio = (float) screenHeight / Constants.MODEL_HEIGHT; //should be 1080/257

        Log.i(TAG, "Widthratio is " + widthRatio + ", heightRatio is " + heightRatio);

        //get the keypoints list ONCE at the beginning
        List<KeyPoint> keyPoints = person.getKeyPoints();

        //Log.d(TAG, String.format("Found %d keypoints for the person", keyPoints.size()));

        //Draw key points of the person's body parts over the camera image
        for (KeyPoint keyPoint : keyPoints) {
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
                                humanActualRaw[0] = new Point(xValue, yValue);
                                humanActualRaw[1] = new Point(xValue, yValue);

                                /*
                                if (noseFound == 0) {
                                        noseFound = 1;
                                        setInitialNoseLocation(adjustedX, adjustedY);
                                }

                                else {
                                        //compute the displacement from the starting position that the nose has traveled (helper fxn)
                                        computeDisplacement(adjustedX, adjustedY);
                                }
                                */
                        }
                        else if (currentPart == BodyPart.LEFT_EYE) {
                                //add nose to first slot of Point array for pose estimation
                                humanActualRaw[2] = new Point(xValue, yValue);

                                //add x val of left eye to bbox array
                                boundingBox[1] = new Point(adjustedX, adjustedY);


                                leftEyeFound = 1;
                                leftEye = new Position(adjustedX, adjustedY);

                                //if we've also already found right eye, we have both eyes. Send data to the scale computer
                                if (rightEyeFound == 1) {
                                        dist = computeScale(leftEye, rightEye);
                                }
                        }
                        else if (currentPart == BodyPart.RIGHT_EYE) {
                                //add nose to first slot of Point array for pose estimation
                                humanActualRaw[3] = new Point(xValue, yValue);

                                //add x val of rt eye to bbox array
                                boundingBox[0] = new Point(adjustedX, adjustedY);

                                rightEyeFound = 1;
                                rightEye = new Position(adjustedX, adjustedY);

                                //if we've also already found left eye, we have both eyes. Send data to the scale computer
                                if (leftEyeFound == 1) {
                                        dist = computeScale(leftEye, rightEye);
                                }

                        }

                        else if (currentPart == BodyPart.RIGHT_SHOULDER) {
                                //add rt shoulder to fifth slot of Point array for pose estimation
                                humanActualRaw[4] = new Point(xValue, yValue);

                                boundingBox[2] = new Point(adjustedX, adjustedY);
                        }
                        else if (currentPart == BodyPart.LEFT_SHOULDER) {
                                //add left shoulder to sixth slot of Point array for pose estimation
                                humanActualRaw[5] = new Point(xValue, yValue);

                                boundingBox[3] = new Point(adjustedX, adjustedY);
                        }

                        //draw the point corresponding to this body joint
                        canvas.drawCircle(adjustedX, adjustedY, circleRadius, redPaint);
                }

                /*
                //if this point is the nose but we've lost our lock on it (confidence level is low)
                else if (currentPart == BodyPart.NOSE) {
                        //set noseFound back to 0
                        noseFound = 0;

                        //reset velocity array
                        velocity[0] = velocity[1] = 0;
                }
                */
        }

        /*
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
        */

        if (humanActualRaw[2] != null && humanActualRaw[3] != null
                && humanActualRaw[4] != null && humanActualRaw[5] != null ){

                double dist_rt_shoulder_eye = humanActualRaw[3].x - humanActualRaw[4].x;
                double dist_left_shoulder_eye = humanActualRaw[5].x - humanActualRaw[2].x;

                double ratio = dist_rt_shoulder_eye / dist_left_shoulder_eye;



                averager += ratio;
                cnt++;

                if (cnt == 100) {
                        Log.i(TAG, "Dist ratio rt over left average is " + (averager / 100));
                        cnt = 0;
                        averager = 0;
                }
        }


        //ONLY EVERY THIRD FRAME, maybe?
        //check that all of the keypoints for a human body bust area were found
        if (humanActualRaw[0] != null && humanActualRaw[1] != null && humanActualRaw[2] != null && humanActualRaw[3] != null
                && humanActualRaw[4] != null && humanActualRaw[5] != null
                //&& frameCounter==3
        )
        {
                //DRAW BOUNDING BOX
                //top is aligned with uppermost eye
                double bbox_top = Math.max(humanActualRaw[3].y, humanActualRaw[2].y);

                //left is aligned w right shoulder
                double bbox_left = humanActualRaw[4].x;

                //rt is aligned w left shoulder
                double bbox_rt = humanActualRaw[5].x;

                //bottom is at lowermost shoulder
                double bbox_bot = Math.min(humanActualRaw[4].y, humanActualRaw[5].y);


                canvas.drawRect(new Rect((int)boundingBox[2].x, (int)Math.min(boundingBox[0].y, boundingBox[1].y),
                        (int)boundingBox[3].x, (int)Math.max(boundingBox[2].y, boundingBox[3].y)), whitePaint);


                distToLeftEyeX = (float)Math.abs(humanActualRaw[2].x - humanActualRaw[0].x);
                distToRightEyeX = (float)Math.abs(humanActualRaw[3].x - humanActualRaw[0].x);

                distToLeftEyeX = (float)Math.abs(humanActualRaw[5].x - humanActualRaw[0].x);
                distToRightEyeX = (float)Math.abs(humanActualRaw[4].x - humanActualRaw[0].x);


                //correction for axis flipping
                if (distToLeftEyeX > distToRightEyeX) {
                        //person looking towards left, swap left eye and rt eye for actual
                        Point temp = humanActualRaw[2];
                        humanActualRaw[2] = humanActualRaw[3];
                        humanActualRaw[3] = temp;
                }

                //correction for axis flipping
                if (distToLeftShouldX < distToRtShouldX) {
                        //person looking towards left, swap left eye and rt eye for actual
                        Point temp = humanActualRaw[5];
                        humanActualRaw[5] = humanActualRaw[4];
                        humanActualRaw[4] = temp;
                }


                //HARDCODED, FIXME

                //find chest pt (midpt between shoulders)
                torsoCtrX = (float) (humanActualRaw[4].x + humanActualRaw[5].x) / 2;
                torsoCtrY = (float) (humanActualRaw[4].y + humanActualRaw[5].y) / 2;


                torsoCenter = new Point(torsoCtrX, torsoCtrY);
                //torsoCenter = new Point((rt_should.x + left_should.x)/2, (rt_should.y + left_should.y)/2);
                //humanActualRaw[0] = torsoCenter;

                //draw the point corresponding to the chest
                canvas.drawCircle(torsoCtrX, torsoCtrY, circleRadius, redPaint);

                //clear out the ArrayList
                humanActualList.clear();

                //compute pose estimation and draw line coming out of person's chest

                //add the pts of interest to a list
                humanActualList.add(humanActualRaw[0]);
                humanActualList.add(humanActualRaw[1]);
                humanActualList.add(humanActualRaw[2]);
                humanActualList.add(humanActualRaw[3]);
                humanActualList.add(humanActualRaw[4]);
                humanActualList.add(humanActualRaw[5]);

                Log.i(TAG, String.format("Human actual: [%f,%f], [%f,%f], [%f,%f], [%f,%f], [%f,%f], [%f, %f]",
                        humanActualList.get(0).x,
                        humanActualList.get(0).y,
                        humanActualList.get(1).x,
                        humanActualList.get(1).y,
                        humanActualList.get(2).x,
                        humanActualList.get(2).y,
                        humanActualList.get(3).x,
                        humanActualList.get(3).y,
                        humanActualList.get(4).x,
                        humanActualList.get(4).y,
                        humanActualList.get(5).x,
                        humanActualList.get(5).y));

                humanActualMat.fromList(humanActualList);

                //now should have everything we need to run solvePnP

                //solve for translation and rotation matrices based on a model of 3d pts for human bust area
                Calib3d.solvePnP(humanModelMat, humanActualMat, cameraMatrix, distortionMat, rotationMat, translationMat);

                //Now we'll try projecting our 3D axes onto the image plane
                MatOfPoint3f testPtMat = new MatOfPoint3f();
                testPtMat.fromList(testPtList);

                //the 2d pts that correspond to the 3d pts above. Will be filled upon return of projectPoints()
                MatOfPoint2f imagePts = new MatOfPoint2f();

                //project our basic x-y-z axis from the world coordinate system onto the camera coord system using rot and trans mats we solved
                Calib3d.projectPoints(testPtMat, rotationMat, translationMat, cameraMatrix, distortionMat, imagePts);

                //imagePts now contains 3 2D coordinates which correspond to ends of the 3D axes

                Log.i(TAG, String.format("Resulting imagepts Mat is of size %d x %d", imagePts.rows(), imagePts.cols()));

                //extract the 3 2D coordinates for drawing the 3D axes
                double[] x_ax = imagePts.get(0,0);
                double[] y_ax = imagePts.get(1,0);
                double[] z_ax = imagePts.get(2,0);

                //we need to change the points found so that they map correctly into the square Canvas on the screen (basically we're
                //scaling up the pts from the original 257x257 bitmap to the 1080x1080 image preview box we now have on screen
                x_ax[0] = x_ax[0] * widthRatio + left;
                x_ax[1] = x_ax[1] * heightRatio + top;

                y_ax[0] = y_ax[0] * widthRatio + left;
                y_ax[1] = y_ax[1] * heightRatio + top;

                z_ax[0] = z_ax[0] * widthRatio + left;
                z_ax[1] = z_ax[1] * heightRatio + top;

                torsoCenter.x = torsoCenter.x * widthRatio + left;
                torsoCenter.y = torsoCenter.y * heightRatio + top;

                Log.i(TAG, String.format("Found point %f, %f for x axis", x_ax[0], x_ax[1]));
                Log.i(TAG, String.format("Found point %f, %f for y axis", y_ax[0], y_ax[1]));
                Log.i(TAG, String.format("Found point %f, %f for z axis", z_ax[0], z_ax[1]));


                //filter out the weird bogus data I was getting
                if (!(x_ax[0] > 2500 || x_ax[1] > 1400 || y_ax[0] > 1500 || y_ax[1] < -1000 || z_ax[0] > 1500 || z_ax[1] < -900
                //check for illogical axes layout
                || (looking == LOOKING_LEFT && z_ax[0] < y_ax[0]) || (looking == LOOKING_RIGHT && z_ax[0] > y_ax[0]))) {

                        //draw the projected 3D axes onto the canvas
                        canvas.drawLine((float) torsoCenter.x, (float) torsoCenter.y,
                                (float) x_ax[0], (float) x_ax[1], bluePaint);
                        canvas.drawLine((float) torsoCenter.x, (float) torsoCenter.y,
                                (float) y_ax[0], (float) y_ax[1], greenPaint);
                        canvas.drawLine((float) torsoCenter.x, (float) torsoCenter.y,
                                (float) z_ax[0], (float) z_ax[1], redPaint);

                        //estimate angles for yaw and pitch of the human's upper body

                        //Mat eulerAngles = new Mat();

                        //THIS FXN DOESN'T WORK RIGHT NOW
                        //getEulerAngles(eulerAngles);

                        Log.i(TAG, "z ax[0] is " + z_ax[0]);
                        Log.i(TAG, "torsoCenter.x is ");
                        //we know length of z axis to be 81.25. Let's find length of 'opposite' side of the rt triangle so that we can use sine to find angle
                        float lenOpposite = (float) z_ax[0] - (float)torsoCenter.x;
                        Log.i(TAG, "Len opposite is " + lenOpposite);

                        float humAngle = getHumAnglesTrig(lenOpposite, 135f); //81.25?

                        Log.i(TAG, "Human angle is " + humAngle + " degrees");

                        //Log.i(TAG, String.format("Euler angles mat is of size %d x %d", eulerAngles.rows(), eulerAngles.cols()));

                        //pitch, yaw, roll
                        //double[] angles = eulerAngles.get(0,0);

                        double[] pitch = rotationMat.get(0,0);
                        double[] yaw = rotationMat.get(1,0);

                        //Log.i(TAG, String.format("Len of angles is %d", angles.length));

                        canvas.drawText(
                                String.format("Horiz angle of human: %.2f°", humAngle),
                                100,
                                500,
                                bluePaint
                        );


                        /*
                        //print out pitch value (rotation about x axis)
                        canvas.drawText(
                                String.format("Pitch of human: %f", pitch[0] * 180/3.14),
                                (15.0f * widthRatio),
                                (110.0f * heightRatio + bottom),
                                bluePaint
                        );


                        //print out yaw value (rotation about y axis)
                        canvas.drawText(
                                String.format("Yaw of human: %f", yaw[0] * 180/3.14),
                                (15.0f * widthRatio),
                                (130.0f * heightRatio + bottom),
                                greenPaint
                        );*/
                }
                else {
                        Log.i(TAG,"Triggered");
                }
        }

        //reset contents of the arrays

        humanActualRaw[0] = humanActualRaw[1] = humanActualRaw[2] = humanActualRaw[3] = humanActualRaw[4] = null;


        //print out details about the PoseNet computation done
        canvas.drawText(
                String.format("Score (fit): %.2f",person.getScore()),
                100,
                100,
                redPaint
        );

        canvas.drawText(
                String.format("Device: %s", posenet.getDevice()),
                100,
                200,
                redPaint
        );

        //print out the time it took to do calculation of this frame
        canvas.drawText(
                String.format("Time to run frame: %.2f ms", posenet.getLastInferenceTimeNanos() * 1.0f / 1_000_000),
                100,
                300,
                redPaint
        );

        Log.i(TAG, "Dist to hum is " + dist + "m");


        //print out the time it took to do calculation of this frame
        canvas.drawText(
                String.format("Dist to hum: %.2fm", dist),
                100,
                400,
                redPaint
        );

        /*
        //print out velocity vector values
        canvas.drawText(
                String.format("Velocity(m/s) X: %.2f, Y: %.2f", velocity[0], velocity[1]),
                (15.0f * widthRatio),
                (90.0f * heightRatio + bottom),
                paint
        );
         */

        //draw/push the Canvas bits to the screen - FINISHED THE CYCLE
        surfaceHolder.unlockCanvasAndPost(canvas);

        //increment framecounter, if at 4 set to 0
        frameCounter++;
        if (frameCounter == 4) {
                frameCounter = 0;
        }
}

private float getHumAnglesTrig(float opp, float hyp) {
        Log.i(TAG, "opp/hyp is " + (opp/hyp));

        float ratio = opp/hyp;

        if (ratio <= -1)
                return -90f;
        else if (ratio >= 1)
                return 90f;

        return (float) Math.toDegrees(Math.asin(ratio));
}

//DON'T USE FOR NOW; THERE'S SOMETHING WRONG
/*
void getEulerAngles(Mat eulerAngles) {
        //create several blank Mats for the decomposeProjectionMatrix fxn
        Mat cameraMatrix = new Mat(), rotMatrix = new Mat(), transVect = new Mat(),rotMatrixX = new Mat(), rotMatrixY = new Mat(), rotMatrixZ = new Mat();

        int needed = (int)rotationMat.total() * rotationMat.channels();

        Log.i(TAG, String.format("Rotation mat has %d rows, %d cols, needed is %d", rotationMat.rows(), rotationMat.cols(), needed));

        double[] projMatrix = new double[needed];

        //fill projMatrix with the rotation matrix we found using solvePnP
        rotationMat.get(0,0, projMatrix);

        ByteBuffer byteBuffer = ByteBuffer.allocate(needed * 8);

        Log.i(TAG, "Length of projMatrix is " + projMatrix.length);
        for (double thisDouble : projMatrix) {
                Log.i(TAG, "Put into buffer");
                byteBuffer.putDouble(thisDouble);
        }

        Calib3d.decomposeProjectionMatrix(new Mat(3,1, CvType.CV_64FC1, byteBuffer),
                cameraMatrix,
                rotMatrix,
                transVect,
                rotMatrixX,
                rotMatrixY,
                rotMatrixZ,
                eulerAngles);
}*/

private void displacementOnly(Person person, Canvas canvas) {
        //Draw `bitmap` and `person` in square canvas.
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


private float calculateDistanceToHuman(float pixelDistance) {
        //Triangle simularity
        //D = (W * F) / P

        //find distance to human in meters
        return Constants.PD * Constants.focalLenExp / pixelDistance;
}

private void drawDataToCanv(Canvas canv, String data, float x, float y, Paint paint) {
        infoCanvas.drawText(
                data,
                x,
                y,
                redPaint
        );
}


//how many real-world meters each pixel in the camera image represents
private float mPerPixel;

//compute how much distance each pixel currently represents in real life, using known data about avg human pupillary distance
private float computeScale(Position leftEye, Position rightEye) {
        //I'll just use the x distance between left eye and right eye points to get distance in pixels between eyes
        //don't forget left eye is on the right and vice versa
        float pixelDistance = leftEye.getX() - rightEye.getX();

        Log.d(TAG, String.format("Pupillary distance in pixels: %f", pixelDistance));

        //now we want to find out how many real meters each pixel on the display corresponds to
        float scale = Constants.PD / pixelDistance;
        mPerPixel = scale;

        Log.d(TAG, String.format("Each pixel on the screen represents %f meters in real life in plane of peron's face", scale));

        //find experimental distance from camera to human and display it on screen

        return calculateDistanceToHuman(pixelDistance);
}

//use calculated meters per pixel and pixel displacement to calculate estimated velocity of the phone (or person, for now)
//the issue of relativity here needs to be fixed
private float computeVelocity() {
        return 0;
}


//Process image using Posenet library. The image needs to be scaled in order to fit Posenet's input dimension requirements of
//257 x 257 (defined in Constants.java), and probably needs to be cropped in order to preserve the image's aspect ratio
private void processImage(Bitmap bitmap, int shouldRunPosenet) {
        Mat unscaledImage = new Mat();
        org.opencv.android.Utils.bitmapToMat(bitmap, unscaledImage);

        //Crop bitmap
        Bitmap croppedBitmap = cropBitmap(bitmap);

        Mat scaledImage = new Mat();

        Log.i(TAG, String.valueOf(croppedBitmap.getConfig()));

        // Created scaled version of bitmap for model input (scales it to 257 x 257)
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, Constants.MODEL_WIDTH, Constants.MODEL_HEIGHT, true);

        //get an OpenCV mat from the bitmap
        org.opencv.android.Utils.bitmapToMat(scaledBitmap, scaledImage);

        //Perform inference, only every 5 frames
        person = (shouldRunPosenet == 1) ? posenet.estimateSinglePose(scaledBitmap) : null;

        //get the canvas that we'll be drawing on
        Canvas canvas = surfaceHolder.lockCanvas();

        if (USE_LK) drawLKVersion(canvas, bitmap, person, scaledImage, shouldRunPosenet); else draw(canvas, person, scaledBitmap);
}

private void drawLKVersion(Canvas canvas, Bitmap bitmap, Person person, Mat scaledImage, int jointsAreFresh) {
        discards = 0;

        //start the clock when this frame comes in. we'll get a split at the next frame and use elapsed time for velocity calculation
        long frameStartTime = SystemClock.elapsedRealtimeNanos();

        sceneColor = scaledImage;

        //convert the color image matrix to a grayscale one to improve memory usage and processing time (1 bpp instead of 3)
        //also converting to grayscale makes the contrast between features clearer
        Imgproc.cvtColor(sceneColor, sceneGrayScale, Imgproc.COLOR_BGRA2GRAY);

        //we need sceneGrayScale to be 257x257, just like the scaledBitmap we're passing to Posenet in processImage()


        //if user called for image saving
        if (SHOULD_SAVE_IMG) {
                //save the final rotated 257 x 257 bitmap that's being inputted to Posenet
                //also save final 257 x 257 Mat that's being inputted to Lucas-Kanade
                Log.i(TAG, "Writing images to storage");

                String storageLoc = "/data/data/weiner.noah.noshake.posenet.test/";

                //save Mat version
                Imgcodecs.imwrite(storageLoc + "testCaptureMat.jpg", sceneGrayScale);

                //save Bitmap version
                File file = new File(storageLoc, "testCaptureBmap.jpg");

                Log.i(TAG, "" + file);

                if (file.exists())
                        file.delete();
                try {
                        FileOutputStream out = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                        out.flush();
                        out.close();
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }


        //need to put the human joints in savedFeatures

        //check if we have new joints (if we ran posenet on this frame)
        if (person != null) {
                //start by converting human joints to MatofPoint2f
                List<KeyPoint> keyPoints = person.getKeyPoints();

                List<Point> keyPtsAsOpenCvPts = new ArrayList<Point>();

                //convert keypoint list into list of OpenCV Point objects
                for (KeyPoint keyPoint : keyPoints) {
                        //NOTE****: these points are for 257x257 image
                        Position thisPos = keyPoint.getPosition();

                        //create OpenCV-style Point obj from the Posenet KeyPoint obj
                        Point newOpencvPt = new Point(thisPos.getX(), thisPos.getY());

                        //add this Point to list of joint Points
                        keyPtsAsOpenCvPts.add(newOpencvPt);
                }

                //get the joints for this current mat and store them in thisFeatures
                thisFeatures.fromList(keyPtsAsOpenCvPts);
        }
        //otherwise we need to find the current points using the last frame's point, along with Lucas-Kanade sparse flow algo
        else {
                //retrieve the corners from the previous mat (save calculating them again)
                savedFeatures.copyTo(prevFeatures);

                //else we want thisFeatures to be populated with the Lucas-Kanade points found from tracking prevFeatures into this frame
                result = sparseFlow(sceneGrayScale);
                //now thisFeatures will be populated with LK pts

                if (result == null) {
                        Log.e(TAG, "ERROR: sparseFlow() returned NULL");
                        return;
                }

        }
        //save the current feature points for next frame
        thisFeatures.copyTo(savedFeatures);

        //get the time it took do do all calculations
        lastInferenceTimeNanos = SystemClock.elapsedRealtimeNanos() - frameStartTime;

        //Log.i(TAG, String.format("Time frame took: %d ns", lastInferenceTimeNanos));

        float timeInSec = lastInferenceTimeNanos * 1f / 1000000000;

        //Log.i(TAG, String.format("Time frame took: %f s", timeInSec));

        /*xVel = pointX / timeInSec;
        yVel = pointY / timeInSec;

        //create and rotate the text to display velocity
        Mat textImg = Mat.zeros(result.rows(), result.cols(), result.type());*/

        /*Imgproc.putText(result, String.format("Velocity(m/s) y: %f, x: %f", xVel, yVel), new Point(100, 100), Core.FONT_HERSHEY_SIMPLEX,
                0.5,
                new Scalar(255, 255, 255),
                0);
         */

        //rotate the text so it's facing the right way
        //rotate(textImg, -45, textImg);

        //Imgproc.cvtColor(textImg, textImg, Imgproc.COLOR_RGB2BGRA);

        //return textImg;

        //result = result + textImg;

        /*Log.i(TAG, String.format("The result mat has %d channels, textImg has %d channels", result.channels(), textImg.channels()));
        Log.i(TAG, String.format("The result mat has %d columns %d rows, textImg has %d col %d row",
                result.cols(),
                result.rows(),
                textImg.cols(),
                textImg.rows()));
         */

        //Imgproc.cvtColor(textImg, textImg, Imgproc.COLOR_BGRA2GRAY);

        //Core.add(result, textImg, result);


        //save canvas into a global
        infoCanvas = canvas;

        //draw clear nothing color to the screen (needs this to clear out the old text and stuff)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        //Draw `bitmap` and `person` in square canvas.
        int screenWidth, screenHeight, left, right, top, bottom, canvasHeight, canvasWidth;

        int rightEyeFound = 0, leftEyeFound = 0;

        float xValue, yValue, xVel, yVel;
        float dist = 0;

        BodyPart currentPart;
        Position leftEye = null, rightEye = null;

        //get the dimensions of our drawing canvas
        canvasHeight = canvas.getHeight();
        canvasWidth = canvas.getWidth();

        //should be 1080 x 2148 (full screen besides navigation bar)
        Log.i(TAG, String.format("Canvas width and height are %d and %d", canvasWidth, canvasHeight));

        //check screen orientation: if portrait mode, set the camera preview square appropriately
        if (canvasHeight > canvasWidth) {
                screenWidth = canvasWidth;
                screenHeight = canvasWidth; //screenwidth x screenHeight should now be 1080 x 1080
                left = 0;

                //we can find the top of the camera preview square by finding width of the padding on top and bottom of the square
                //the total amt of padding will be the canvasHeight (2148) minus the heigt of camera preview box, then divide by 2 to get
                //amt we need to go down from top of screen to find top of camera preview square
                top = (canvasHeight - canvasWidth) / 2; //should be 534
        }

        //otherwise if landscape mode, set the width and height of the camera preview square appropriately
        else {
                screenWidth = canvasHeight;
                screenHeight = canvasHeight;
                left = (canvasWidth - canvasHeight) / 2;
                top = 0;
        }

        Log.i(TAG, "Left is " + left);

        //right is right edge of screen if portrait mode; otherwise it's in middle of screen
        right = left + screenWidth; //should be 1080

        //find bottom of the camera preview square
        bottom = top + screenHeight; //should be 534 + 1080 = 1614

        //set up the Paint tool
        setPaint();

        int bmWidth = bitmap.getWidth(); //should be 257
        int bmHeight = bitmap.getHeight(); //should be 257

        Log.i(TAG, String.format("Bitmap width and height are %d and %d", bmWidth, bmHeight)); //should be 257x257

        //WHAT IS PT OF THIS??
        int newRectWidth = right - left;
        int newRectHeight = bottom - top;

        double scaleDownRatioVert = newRectHeight / 2280f;
        double scaleDownRatioHoriz = newRectWidth / 1080f;

        Log.i(TAG, String.format("New rect width and height are %d and %d", newRectWidth, newRectHeight));
        Log.i(TAG, String.format("Scaledown ratios are %f and %f", scaleDownRatioHoriz, scaleDownRatioVert));

        //draw the camera preview square bitmap on the screen
        //Android fxn documentation: Draw the specified bitmap, scaling/translating automatically to fill the destination rectangle.
        //If the source rectangle is not null, it specifies the subset of the bitmap to draw.
        //This function ignores the density associated with the bitmap. This is because the source and destination rectangle
        //coordinate spaces are in their respective densities, so must already have the appropriate scaling factor applied.
        canvas.drawBitmap(bitmap, new Rect(0, 0, bmWidth, bmHeight), //in other words draw whole bitmap
                new Rect(left, top, right, bottom), redPaint);


        //Next need to calculate ratios used to scale image back up from the 257x257 passed to PoseNet to the actual display

        //divide the available screen width pixels by PoseNet's required number of width pixels to get the number of real screen pixels
        //widthwise per posenet input image "pixel"
        float widthRatio = (float) screenWidth / Constants.MODEL_WIDTH; //should be 1080/257

        //divide the available screen height pixels by PoseNet's required number of height pixels to get number of real screen pixels
        //heightwise per posenet input image "pixel"
        float heightRatio = (float) screenHeight / Constants.MODEL_HEIGHT; //should be 1080/257

        Log.i(TAG, "Widthratio is " + widthRatio + ", heightRatio is " + heightRatio);



        //get joint points found for this frame
        joints = thisFeatures.toList();

        //Log.d(TAG, String.format("Found %d keypoints for the person", keyPoints.size()));

        //Draw key points of the person's body parts over the camera image
        for (Point joint : joints) {

                        xValue = (float) joint.x;
                        yValue = (float) joint.y;

                        //the real x value for this body part dot should be the xValue PoseNet found in its 257x257 input bitmap multiplied
                        //by the number of real Android display (or at least Canvas) pixels per Posenet input bitmap pixel
                        float adjustedX = (float) xValue * widthRatio + left; //x value adjusted for actual Android display
                        float adjustedY = (float) yValue * heightRatio + top; //y value adjusted for actual Android display

                        //draw the point corresponding to this body joint
                        canvas.drawCircle(adjustedX, adjustedY, circleRadius, redPaint);
        }

        /*
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
        }*/


        canvas.drawText(
                String.format("Device: %s", posenet.getDevice()),
                100,
                200,
                redPaint
        );

        //print out the time it took to do calculation of this frame
        canvas.drawText(
                String.format("Time to run frame: %.2f ms", (float)timeToRunFrame),
                100,
                300,
                redPaint
        );

        Log.i(TAG, "Dist to hum is " + dist + "m");


        //print out the time it took to do calculation of this frame
        canvas.drawText(
                String.format("Dist to hum: %.2fm", dist),
                100,
                400,
                redPaint
        );


        //print out velocity vector values
        canvas.drawText(
                String.format("Velocity(m/s) X: %.2f, Y: %.2f", velocity[0], velocity[1]),
                (15.0f * widthRatio),
                (90.0f * heightRatio + bottom),
                redPaint
        );


        //draw/push the Canvas bits to the screen - FINISHED THE CYCLE
        surfaceHolder.unlockCanvasAndPost(canvas);
}

//This is a Lucas-Kanade processor for a given Mat
public Mat sparseFlow(Mat inputFrame) {
        //get the CURRENT grayscale Mat from the input camera frame
        mGray = inputFrame;

        //do transposition
        //Mat mGrayT = mGray.t();

        //flip a 2D array around vertical, horizontal, or both axes
        //in this case we flip the mGrayT array around the y-axis, where
        // dst[i][j] = src[src.rows-i-1],[src.columns-j-1] so the top left slot of dest matrix = bottom rt of source matrix, etc.
        //so flipcode < 0 can be used, for example, for simultaneous horizontal and vertical flipping of an image w/ the subsequent shift
        // and absolute difference calculation to check for a central symmetry
        //Core.flip(mGray.t() /*isn't this just the same as saying mGrayT?*/, mGrayT, 1); //flipcode 1 means flip around y-axis

        //resize mGrayT image, the output image size being set to mGray.size()
        //Imgproc.resize(mGrayT, mGrayT, mGray.size());

        //if features is empty, that means we don't have any points to work with yet
        /*
        if (features.toArray().length == 0) {
            Log.i(TAG, "features is empty, now populating with Shi-Tomasi points");

            int rowStep = 50, colStep = 100;

            //create a new array of 12 Points (an x and a y)
            Point points[] = new Point[12];

            //create the points
            int k = 0;

            //what's the significance of these points? Are they just random location to track?
            for (int i = 3; i <= 6; i++) {
                for (int j = 2; j <= 4; j++) {
                    //create a point with x value of current j*100 and y value current i*50
                    //will result in (200, 150), (300, 150), (400, 150), (200, 200), (300, 200), (400, 200), (200, 250), (300, 250), ...
                    points[k] = new Point(j * colStep, i * rowStep);
                    k++;
                }
            }

            //the MatofPoint class in OpenCV is a 2D array of points. We can add all of our Shi-Tomasi points into our MatofPoint instance
            //by calling fromArray() on the array of Points
            features.fromArray(pointsToTrack); //the MatofPoint '''features''' is now populated with the Shi-Tomasi points

            //set prevFeatures equal to features (since we only have one newly created MatofPoint), so equal to list of points created above
            prevFeatures.fromList(features.toList());
        }
         */

        //set nextFeatures equal to prevFeatures. I think we have to do this as safety thing in case some of nextFeatures can't be populated by
        //the algorithm, since we iterate through all of nextFeatures after it runs to extract the deltaX and deltaY
        //nextFeatures.fromArray(prevFeatures.toArray());

        //run the LK algorithm forwards and backwards
        lucasKanadeForwardBackward();


        /*
        //throw out points that don't match between last frame and this frame
        throwOutWanderingPts();

        //get the displacements of the tracked points and fill in cornerList[] appropriately (cornerList is final list of all KeyFeatures being considered for drawing)
        populateDisplacements();

        //get the number of goodFeatures there were initially
        int listSize = prevList.size();

        //make copy of cornerList and remove the KeyFeatures with NULL pts (that failed forward-backward test). This copy will be used for sorting, Jenks, etc. Then

        //copy cornerList, remove invalid KeyFeatures from the copy, and sort
        getSortedCornerList();

        //separate the points into Jenks Natural Breaks groups
        doJenksSeparation();

        if (breakPoints == null) {
                return null;
        }

        means = new double[numGroups];
        //Log.i(TAG, String.format("Length of means: %d", means.length));

        int currStart = 0;

        if (breakPoints.length > 1) {
                //once we get the breakpoint, I want to compare the means of the two clusters found to see if they really differ much. If they
                //don't differ much, the grouping can be ignored
                for (int i = 0; i < numGroups; i++) {
                        //if this is the first iteration, start index is 0, end index is first int from breakPoint array

                        //get the mean for this grouping and store it in means array
                        means[i] = Jenks.Breaks.mean(cornerListSorted, currStart, breakPoints[i]);
                }
        }

        //print out the means of the two groups found
        for (int i = 0; i < numGroups; i++) {
                Log.i(TAG, String.format("Mean of group %d is %f", i, means[i]));
        }

        //treating the different motion groups separately, remove outliers from the groups
        removeOutliersWithinClusters();

        //draw the displacement lines on the image
        drawDisplacementLines();


        //if there's an area in the image that's moving faster than the rest, let's draw a rectangle around ti
        drawMotionRectangle();


        //finish calculating the X and Y averages of all points of interest for both the previous frame and this frame
        xAvg1 /= listSize;
        xAvg2 /= listSize;
        yAvg1 /= listSize;
        yAvg2 /= listSize;

        //get the average shift in x and y in pixels between last frame and this frame
        //DON'T FORGET the origin is top right, not bottom left
        pointX = xAvg1 - xAvg2; //x displacement
        pointY = yAvg1 - yAvg2; //y displacement*/

        //we need some way to get the time between frames

        /*
        //if our List of Points (the global one) is not empty (this is our first run of sparseFlw), calculate the cumulative total shift in x and y
        if (!points.isEmpty()) {
                //get the most recent x and y shift values
                Point lastPoint = points.get(points.size() - 1);

                //add the cumulative running x and y shift totals to the ones for just this frame change
                pointX += lastPoint.x;
                pointY += lastPoint.y;
        }

        //if list empty, just add deltaX and deltaY totals as first pt in the List. If it wasn't empty add the new cumulative totals to end of list
        points.add(new Point(pointX, pointY));*/

        //hold onto this Mat because we'll use it as the previous frame to calc optical flow next time
        //capture the current mGrayT (Mat of all pixels from cam), clone it into mPrevGray for later use (clone() COPIES all pixels in memory)
        mPrevGray = mGray.clone();

        //thisFeatures.copyTo(prevFeatures);

        //return the final Mat
        return mGray;
}

/**
 * Run the Lucas-Kanade algorithm on the previous frame and this frame, both tracking last frame's Shi-Tomasi pts forward to this frame and tracking
 * those found LK pts backward to the last frame to minimize error due to object obstruction, noise, etc.
 */
public void lucasKanadeForwardBackward() {
        Log.i(TAG, "mPrevGray is " + mPrevGray.rows() + " rows, " + mPrevGray.cols() + " cols, while mGray is " + mGray.rows() + "rows, " + mGray.cols() + " cols");



        /**
         * Run the Lucas-Kanade algo with the following passed for the parameters:
         *
         * We pass the previous gray Mat as the first 8-bit image, the current gray Mat as second image, last frame's Shi-Tomasi pts as prevFeatures,
         * and a MatofPoint nextFeatures which by default is the same as prevFeatures but should be modified
         *
         * NOTE: on first call of sparseFlow(), mPrevGray will = mGray, prevFeatures will hold goodFeatures of current frame, thisFeatures will be empty
         *
         * @param prevImg – first 8-bit input image or pyramid constructed by buildOpticalFlowPyramid()
         * @param nextImg - second input image which represents the current frame
         * @param prevPts – vector of 2D points for which the flow needs to be found; point coordinates must be single-precision floats.
         *       I think this is where we feed in Shi-Tomasi points
         * @param nextPts – output vector of 2D points (w/single-precision float coords) containing calculated new
         *       positions of input features in the 2nd image; when OPTFLOW_USE_INITIAL_FLOW flag is passed, vector must have same size as input
         * @param status – output status vector (of unsigned chars); each element of the vector is set to 1 if the flow for the corresponding
         *       features has been found, otherwise, it is set to 0.
         * @param err – output vector of errors; each element of the vector is set to an error for the corresponding feature, type of the error
         *             measure can be set in flags parameter; if the flow wasn’t found then the error is not defined (use the status parameter to find
         *             such cases).
         */
        Video.calcOpticalFlowPyrLK(mPrevGray, mGray, prevFeatures, thisFeatures, status, err); //Features we track are the ones from goodFeaturesToTrack()

        //Now thisFeatures will contain the corners from last frame that were supposedly tracked into this frame

        //Create new matrix of (x,y) float coords to store the result of running LK algorithm backwards
        MatOfPoint2f cornersFoundGoingBackwards = new MatOfPoint2f();

        //To reduce error and noise, we'll also run the algorithm backwards, treating the points found by LK in second frame as the first/original set of pts
        //and the first frame as the next frame. Then we'll compare the backtracked LK-generated points in the first frame with those originally generated in the
        //first frame by Shi-Tomasi. If there's a discrepancy for one of the points, that means that point probably was obstructed, etc. in the second frame,
        //which caused LK to find a different point for it. Thus the backtracked LK point found will differ greatly from the original S-T point. In this case, the
        //forward trajectory and displacement from last frame to this frame is invalid for that point and should be thrown out.
        Video.calcOpticalFlowPyrLK(mGray, mPrevGray, thisFeatures, cornersFoundGoingBackwards, status, err);

        //Create new matrix of (x, y) float coords to store difference between points found
        MatOfPoint2f difference = new MatOfPoint2f();

        Log.i(TAG, String.format("Prevfeatures has %d columns, %d rows. Cornersfoundback has %d col, %d row", prevFeatures.cols(),
                prevFeatures.rows(),
                cornersFoundGoingBackwards.cols(),
                cornersFoundGoingBackwards.rows()));

        //Subtract [the Mat containing features found in last frame by directly running Shi-Tomasi] from [Mat containing features found in last frame
        //by running LK backwards on LK-tracked points found in this frame] to get the forward-backward error. This error will help us determine whether we should
        //trust the supposed points tracked by LK into this frame, because if a pt was found in this frame but not backtracked to the previous frame then it should
        //probably be thrown out
        Core.subtract(prevFeatures, cornersFoundGoingBackwards, difference);

        Log.i(TAG, String.format("Difference initially has %d columns, %d rows.", difference.cols(), difference.rows()));

        //Convert the difference matrix into a matrix with just two rows for easy iteration
        //difference.reshape(-1, 2);

        Log.i(TAG, String.format("Difference has %d columns, %d rows.", difference.cols(), difference.rows()));

        Log.i(TAG, String.format("Value from difference is %f, %f", difference.toList().get(0).x, difference.toList().get(0).y));

        //Create (x,y) List of pts for the goodFeatures from previous frame
        prevList = prevFeatures.toList();

        //Create (x,y) List of pts for the current goodFeatures traced/found by FORWARD Lucas-Kanade algorithm
        nextList = thisFeatures.toList();

        //Create (x,y) List of pts for the goodFeatures found by BACKWARD Lucas-Kanade algorithm
        cornersFoundGoingBackList = cornersFoundGoingBackwards.toList();

        //Create (x,y) List of pts for difference between the actual Shi-Tomasi corner pts in the last frame and the ones supposedly found by LK in this frame
        forwardBackErrorList = difference.toList();

        //Now everything has been prepared for further processing; namely, the points have been prepared for forward-backward error removal/filtering.
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
