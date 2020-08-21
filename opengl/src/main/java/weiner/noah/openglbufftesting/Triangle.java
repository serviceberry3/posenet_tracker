package weiner.noah.openglbufftesting;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

public class Triangle {
    private FloatBuffer vertexBuffer;
    private final int mProgram;
    private int positionHandle;
    private int colorHandle;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;


    static float[] triangleCoords = {   // in counterclockwise order:
            0.0f, 0.622008459f, 0.0f, // top
            -0.5f, -0.311004243f, 0.0f, // bottom left
            0.5f, -0.311004243f, 0.0f  // bottom right
    };

    // Set color with red, green, blue and alpha (opacity) values
    float[] color = {0.63671875f, 0.76953125f, 0.22265625f, 1.0f};

    private final int vertexCount = triangleCoords.length / COORDS_PER_VERTEX;

    //how much memory space each vertex takes up
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex


    public Triangle() {
        //load the vertex shader
        int vertexShader = OpenGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);

        //load the fragment shader
        int fragmentShader = OpenGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        //create empty openGL ES Program
        mProgram = GLES20.glCreateProgram();

        //add the vertex shader to program
        GLES20.glAttachShader(mProgram, vertexShader);

        //add fragment shader to program
        GLES20.glAttachShader(mProgram, fragmentShader);

        //link the program, create OpenGL ES program executable
        GLES20.glLinkProgram(mProgram);

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                triangleCoords.length * 4);

        // use the device hardware's native byte order (C order)
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        vertexBuffer.put(triangleCoords);

        // set the buffer to read the first coordinate (set cursor position to beginning of buffer)
        vertexBuffer.position(0);
    }

    private final String vertexShaderCode =
            //this matrix member var provides a hook to manipulate the coords of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "void main() {" +
                    //the matrix must be included as modifier of gl_Position
                    //NOTE: the uMVPMatrix factor MUST BE FIRST in order for matrix multiplication product to be correct
                "gl_Position = uMVPMatrix * vPosition;" +
            "}";

    //use to access and set the view transformation
    private int vPMatrixHandle;

    private final String fragmentShaderCode =
            "precision mediump float;" +    //how much precision GPU uses when calculating floats
            "uniform vec4 vColor;" +
            "void main() {" +
                "gl_FragColor = vColor;" +
            "}";

    //actually draw the triangle on the display
    public void draw(float[] mvpMatrix) { //pass in calculated transformation matrix  //WAS (float[] mvpMatrix)
        //add the program to the OpenGL ES environment
        GLES20.glUseProgram(mProgram);

        //get the vertex shader's vPosition member
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        //enable openGL to read from FloatBuffer that contains the triangle's vertices' coords and to understand that there's a triangle there
        GLES20.glEnableVertexAttribArray(positionHandle);

        //point to our vertex buffer
        //tell OpenGL to use the vertexBuffer to extract the vertices from
        //@param size = 3 represents number of vertices in the buffer
        //@param what type of data the buffer holds
        //@param the offset in the array used for the vertices (in this case they follow each other, no extra data stored)
        //@param the buffer containing the vertices
        //prepare the triangle coordinate data/pass in the position information
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        //GLES20.glClearColor(0.0f, 0.0f,0.0f,0.5f);

        // clear the color buffer (bitmaps) -- clear screen and depth buffer
       GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        //get fragment shader's vColor member
        colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        //set color for triangle -- values of RGB floats are between 0 and 1 inclusif
        GLES20.glUniform4fv(colorHandle, 1, color, 0);

        //get shape's transformation matrix
        vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

        //pass projection and view transformation to the shader.
        GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0);

        //draw the vertices as a triangle strip
        //tells OpenGL to draw triangle strips found in buffer provided, starting with first element. Also "count" is how many vertices there are
        //GLES20.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, triangleCoords.length / COORDS_PER_VERTEX);

        //draw triangle -- google version
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        //disable vertex array (disable client state before leaving)
        GLES20.glDisableVertexAttribArray(positionHandle);
    }
}


