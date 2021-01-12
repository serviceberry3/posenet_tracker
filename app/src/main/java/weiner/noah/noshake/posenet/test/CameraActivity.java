package weiner.noah.noshake.posenet.test;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;

import java.util.Objects;

import weiner.noah.noshake.posenet.test.R;

import weiner.noah.openglbufftesting.OpenGLRenderer;
import weiner.noah.openglbufftesting.OpenGLView;

public class CameraActivity extends AppCompatActivity {
    private String TAG = "CameraActivity";
    public MatOfPoint3f humanModelMat;
    public MatOfPoint2f humanActualMat;

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
