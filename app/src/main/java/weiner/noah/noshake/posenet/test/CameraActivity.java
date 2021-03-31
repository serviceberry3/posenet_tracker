package weiner.noah.noshake.posenet.test;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import weiner.noah.noshake.posenet.test.R;

import weiner.noah.openglbufftesting.OpenGLRenderer;
import weiner.noah.openglbufftesting.OpenGLView;

public class CameraActivity extends AppCompatActivity {
    private String TAG = "CameraActivity";
    public MatOfPoint3f humanModelMat;
    public MatOfPoint2f humanActualMat;


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
    MatOfPoint2f prevFeatures, nextFeatures, thisFeatures, safeFeatures;
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
    long lastInferenceTimeNanos;

    //the x and y displacement between the last two frames, along with the velocity calculated
    double pointX, pointY, xVel, yVel;

//initialize some doubles to hold the average position of the corners in the previous frame vs the current frame

    //average position of the goodFeatures in previous frame
    double xAvg1 = 0, yAvg1 = 0,
    //average position of the goodFeatures in this frame
    xAvg2 = 0, yAvg2 = 0;

    private int[] breakPoints;

    //load up native C code
    static {
        System.loadLibrary("circ_buffer");
    }

    public OpenGLView openGLView;
    public OpenGLRenderer myRenderer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i("DBUG", "Created");
        super.onCreate(savedInstanceState);

        // requesting to turn the title OFF
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // making it full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        //openGLView = (OpenGLView) findViewById(R.id.openGLView);

        setContentView(R.layout.tfe_pn_activity_camera);


        //setContentView(R.layout.tfe_pn_activity_camera);
        Toast.makeText(getApplicationContext(), "Welcome to CameraActivity.kt", Toast.LENGTH_SHORT).show();

        /*
        //add the PoseNet submodule into the activity
        if (savedInstanceState==null) {
            getSupportFragmentManager().beginTransaction() //Returns: FragmentManager, FragmentTransaction
                    .replace(R.id.container, new PosenetActivity()).commit();
        }
         */
    }

    //use this OpenCV loader callback to instantiate Mat objects, otherwise we'll get an error about Mat not being found
    public BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            Log.i(TAG, "BaseLoaderCallback called!");

            if (status == LoaderCallbackInterface.SUCCESS) {//instantiate everything we need from OpenCV
                //everything succeeded
                Log.i(TAG, "OpenCV loaded successfully, everything created");

                humanModelMat = new MatOfPoint3f();

                humanActualMat = new MatOfPoint2f();


                /*
                //mCamera = new VideoCapture();
                sceneColor = new Mat();
                sceneGrayScale = new Mat();

                //everything succeeded
                Log.i(TAG, "OpenCV loaded successfully, everything created");


                mOpenCvCameraView = new ShiTomasiView(CameraBaseActivity.this, 0);

                //set the camera listener callback to the one declared in this class
                mOpenCvCameraView.setCvCameraViewListener(CameraBaseActivity.this);


                mOpenCvCameraView.setOnTouchListener(CameraBaseActivity.this);

                //to improve speed and reduce lag of the algorithms, lower the camera frame quality
                mOpenCvCameraView.setMaxFrameSize(720, 1280); //720 x 1280?

                //make the camera view visible
                mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
                mOpenCvCameraView.enableView();

                //SurfaceHolder interfaces enable apps to edit and control surfaces.
                //A SurfaceHolder is an interface the system uses to share ownership of surfaces with apps. Some clients that
                //work with surfaces want a SurfaceHolder, because APIs to get and set surface parameters are implemented through a
                //SurfaceHolder. **A SurfaceView contains a SurfaceHolder**.
                //Most components that interact with a view involve a SurfaceHolder.
                SurfaceHolder mHolder = mOpenCvCameraView.getHolder();

                //display the new instance of ShiTomasi view
                setContentView(mOpenCvCameraView);

                */
            }

            else {
                super.onManagerConnected(status);
            }

            //add the PoseNet submodule into the activity
            getSupportFragmentManager().beginTransaction() //Returns: FragmentManager, FragmentTransaction
                    .replace(R.id.container, new PosenetActivity()).commit();

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        }

        else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public MatOfPoint3f getHumanModelMat() {
        return humanModelMat;
    }

    public MatOfPoint2f getHumanActualMat() {
        return humanActualMat;
    }

    public BaseLoaderCallback getmLoaderCallback() {
        return mLoaderCallback;
    }
}
