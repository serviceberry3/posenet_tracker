package weiner.noah.openglbufftesting;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

//The GLSurfaceView class provides helper classes for managing EGL contexts, interthread communication, and interaction with the
// activity lifecycle. You don't need to use a GLSurfaceView to use GLES.

//For example, GLSurfaceView creates a thread for rendering and configures an EGL context there. The state is cleaned up automatically when
// the activity pauses. Most apps don't need to know anything about EGL to use GLES with GLSurfaceView.

public class OpenGLView extends GLSurfaceView {
    Context myContext;
    Activity myActivity;

    public OpenGLView(Context context) {
        super(context);
        myContext = context;
        //init();
    }

    private void init() {
        //set embedded OpenGL version
        setEGLContextClientVersion(2);

        setPreserveEGLContextOnPause(true);
        setRenderer(new OpenGLRenderer(myContext, myActivity));
    }
}
