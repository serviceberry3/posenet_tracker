package weiner.noah.noshake.posenet.test;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import weiner.noah.noshake.posenet.test.R;

import weiner.noah.openglbufftesting.OpenGLRenderer;
import weiner.noah.openglbufftesting.OpenGLView;

public class CameraActivity extends AppCompatActivity {
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

        //add the PoseNet submodule into the activity
        if (savedInstanceState==null) {
            /*FragmentManager*/getSupportFragmentManager()./*FragmentTransaction*/beginTransaction()
                    .replace(R.id.container, new PosenetActivity()).commit();
        }


    }
}
