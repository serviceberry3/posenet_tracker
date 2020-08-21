package weiner.noah.openglbufftesting;

import androidx.appcompat.app.AppCompatActivity;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity {

    private OpenGLView openGLView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("DBUG", "MainActivity for openGL: Created");

        super.onCreate(savedInstanceState);
        // requesting to turn the title OFF
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // making it full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //initiate the openGLView and create an instance with this activity
        openGLView = new OpenGLView(this);

        openGLView.setEGLContextClientVersion(2);

        openGLView.setPreserveEGLContextOnPause(true);

        openGLView.setRenderer(new OpenGLRenderer(this, MainActivity.this));

        //openGLView = (OpenGLView) findViewById(R.id.openGLView);

        setContentView(openGLView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        openGLView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        openGLView.onPause();
    }
}