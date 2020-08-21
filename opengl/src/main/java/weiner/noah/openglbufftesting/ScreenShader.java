package weiner.noah.openglbufftesting;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

public class ScreenShader {
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint textPaint;
    private Drawable background;
    private FloatBuffer vertexBuffer;   // buffer holding the vertices
    private ShortBuffer drawListBuffer;
    private final int mProgram;
    private int positionHandle;
    private int colorHandle;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;

    private float vertices[] = {
            -1f, 1f, 0.0f,   //top left
            -1f, -1f, 0.0f,  //bottom left
            1f, -1f, 0.0f,   //bottom right
            1f, 1f, 0.0f   //top right
    };

    private final int vertexCount = vertices.length / COORDS_PER_VERTEX;

    //how much memory space each vertex takes up
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    private short drawOrder[] = {0, 1, 2, 0, 2, 3}; //order to draw vertices

    //buffer holding the texture
    private FloatBuffer textureBuffer;

    //S,T (or X,Y) texture coordinate data.
    //Since images have Y axis pointing downward (vals increase as you move down the image) while OpenGL has Y axis pting upward,
    //we adjust for that here by flipping the Y axis. Tex coords are same for every face.
    private float[] texture = {  //note the Y axis is flipped to compensate for fact that in graphics images, Y axis pts in opposite dir of OpenGL's Y axis
            //mapping coordinates for vertices
            1f, 0f, //bottom right
            1f, 1f, //top right
            0f, 1f, //top left
            0f, 0f  //bottom right
    };

    //used to pass in the texture
    private int mTextureUniformHandle;

    //used to pass in model texture coordinate info
    private int mTextureCoordinateHandle;

    //size of texture coordinate data (# elements per coord = just x and y)
    private final int mTextureCoordinateDataSize = 2;

    //handle to texture data in shader program
    private int mTextureDataHandle;

    //handle to location of alpha var in shader
    private int alphaLocation;

    //the texture pointer array, where openGL will store names of textures we'll use in our app
    private int[] textures = new int[1];

    // Set color with red, green, blue and alpha (opacity) values
    //float[] color = {0.63671875f, 0.76953125f, 0.22265625f, 1.0f};

    float[] color = {0.7f, 0.3f, 0.3f, 1.0f};

    public ScreenShader() {
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

        // a float has 4 bytes so we allocate for each coordinate 4 bytes
        ByteBuffer vertexByteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);

        vertexByteBuffer.order(ByteOrder.nativeOrder());

        // allocates the memory from the byte buffer
        vertexBuffer = vertexByteBuffer.asFloatBuffer();

        // fill the vertexBuffer with the vertices
        vertexBuffer.put(vertices);

        // set the cursor position to the beginning of the buffer
        vertexBuffer.position(0);


        //initialize byte buffer for the draw list
        vertexByteBuffer = ByteBuffer.allocateDirect(drawOrder.length * 2);
        vertexByteBuffer.order(ByteOrder.nativeOrder());
        drawListBuffer = vertexByteBuffer.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);


        vertexByteBuffer = ByteBuffer.allocateDirect(texture.length * 4);
        vertexByteBuffer.order(ByteOrder.nativeOrder());
        textureBuffer = vertexByteBuffer.asFloatBuffer();
        textureBuffer.put(texture);
        textureBuffer.position(0);
    }


    //draw method for square with gl context
    public void draw(float[] mvpMatrix) {
        //add the program to the OpenGL ES environment
        GLES20.glUseProgram(mProgram);

        //get the vertex shader's vPosition member
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        //enable openGL to read from FloatBuffer that contains the square's vertices' coords and to understand that there's a square there
        GLES20.glEnableVertexAttribArray(positionHandle);

        //point to our vertex buffer--tells openGL renderer from where to take the vertices and of what type they are
        //tell OpenGL to use the vertexBuffer to extract the vertices from
        //@param size = 3 represents number of vertices in the buffer
        //@param what type of data the buffer holds
        //@param the offset in the array used for the vertices (in this case they follow each other, no extra data stored)
        //@param the buffer containing the vertices
        //prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);


        //GLES20.glClearColor(0.0f, 0.0f,0.0f,0.5f);

        //clear the color buffer (bitmaps) -- clear screen and depth buffer
        //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        //get fragment shader's vColor member
        colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        //set color for triangle -- values of RGB floats are between 0 and 1 inclusif
        GLES20.glUniform4fv(colorHandle, 1, color, 0);

        GLES20.glDisable(GLES20.GL_CULL_FACE);

        //enable alpha blending
        GLES20.glEnable (GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        //get alpha variable location
        alphaLocation = GLES20.glGetUniformLocation(mProgram, "alpha");
        //set alpha to 0.5
        GLES20.glUniform1f(alphaLocation, 1f);

        //get shape's transformation matrix
        vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

        //pass projection and view transformation to the shader's uMVPMatrix variable
        GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0);

        //set the face rotation
        GLES20.glFrontFace(GL10.GL_CW);

        mTextureDataHandle = OpenGLRenderer.textureBuffer[0];

        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture");

        //queries our linked shader program for the attribute variable NAME and returns the index of the generic vertex attribute that's bound to that attribute var
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate");

        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

        //define an array of generic vertex attribute data. Index of the generic vertex attribute to be modified is mTextureCooordinateHandle, and the pointer to first generic vertex attrib in array is textureBuff
        //specifies location and data format of the array of generic vertex attribs at index index to use when rendering
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false, 0, textureBuffer);


        /*PROCESS:
        1. Set active texture unit
        2. Bind a texture to this unit
        3. Assign this unit to a texture uniform in the fragment shader
         */

        //set active texture unit to texture unit 0 -- textures need to be bound to texture units before they can be used in rendering
        //texture unit is what reads in texture and actually passes it through shader so can be displayed on screen
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        //bind the previously generated texture to the first texture unit
        GLES20.glBindTexture(GL10.GL_TEXTURE_2D, mTextureDataHandle);

        //tell texture uniform sampler to use this texture in shader by binding to texture unit 0
        //tell openGL we want to bind first texture unit to mTextureUniformHandle, which refers to "u_Texture" in fragment shader
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        //draw the vertices as a triangle strip
        //tells OpenGL to draw triangle strips found in buffer provided, starting with first element. Also "count" is how many vertices there are
        //GLES20.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices.length / COORDS_PER_VERTEX);

        //draw triangle -- google version
        //GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        //disable vertex array (disable client state before leaving)
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    //read in a graphics file (or make one) and load it into openGL
    public void loadGLTexture(GL10 gl, Context context) {
        //loading texture -- loads Android bitmap. It's best if the bitmap is square, because that helps a lot with scaling. Make sure bitmaps for textures are squares;
        //if not, make sure width and height are pwrs of 2
        //Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.android);

        bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_4444);

        canvas = new Canvas(bitmap);
        bitmap.eraseColor(0);

        background = context.getResources().getDrawable(R.drawable.ic_launcher_background);

        background.setBounds(0, 0, 256, 256);

        background.draw(canvas);

        //create a new paint object
        textPaint = new Paint();

        //sets the size of the text to display
        textPaint.setTextSize(32);

        //set antialiasing bit in the flags, which smooths out edges of what is being drawn
        textPaint.setAntiAlias(true);

        //set the color of the paint
        textPaint.setARGB(0xff, 0x00, 0x00, 0xdd);

        canvas.drawText("NOSHAKE TEST", 14, 135, textPaint); //WAS x:16, y:112

        //generate one texture ptr/names for textures (actually generates an int)
        GLES20.glGenTextures(1, textures, 0);

        //and bind it to our array -- binds texture with newly generated name. Meaning, anything using textures in this subroutine will use the bound texture.
        //Basically activates the texture. If we had had multiple textures and multiples squares for them, would have had to bind (activate) the appropriate textures
        //for each square just before they were used
        //tells OpenGL that subsequent OpenGL calls should affect this texture
        GLES20.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

        //create nearest filtered texture -- tells openGL what types of filters to use when it needs to shrink or expand texture to cover the square
        GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST); //GL_NEAREST is quickest and roughest form of filtering. Picks nearest textel at each pt in screen
        GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        //use Android GLUtils to specify a 2d texture image from our bitmap. Creates the image (texture) internally in its native format based on our bitmap
        //Load the bitmap into the bound texture
        //@param: target. Want a regular 2D bitmap
        //@param: level. Specifies image to use at each level. Not using mip-mapping here so put 0 (default lvl)
        //@param: bitmap
        //@param: border. Not using it.
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

        //clean up -- free up memory used by original bitmap
        bitmap.recycle(); //bitmaps contain data that resides in native memory, takes a few cycles to be garbage collected if you don't do it
    }

    private final String vertexShaderCode =
            //this matrix member var provides a hook to manipulate the coords of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +   //a constant across all vertices, representing combined model/view/projection matrix. Used to project verts onto screen.
                    "attribute vec4 vPosition;" + //per-vertex position information we will pass in
                    "attribute vec4 a_Color;" +  //per-vertex color information we will pass in

                    "attribute vec2 a_TexCoordinate;" +  //per-vertex texture coordinate information we'll pass in (array with two components that'll take in tex coord info as input)
                    //this will be per-vertex, like the position, color, and normal data
                    "varying vec2 v_TexCoordinate;" +   //this will be passed thru fragment shader via linear interpolation across the surface of the triangle

                    "varying vec4 v_Color;" + //this will be passed into fragment shader. Interpolates values across the triangle and passes it on to frag shader.
                    //when it gets to the fragment shader, it will hold an interpolated value for each pixel

                    "void main() {" + //the entry point for our vertex shader
                    //the matrix must be included as modifier of gl_Position
                    //NOTE: the uMVPMatrix factor MUST BE FIRST in order for matrix multiplication product to be correct
                    "gl_Position = /*uMVPMatrix * */ vPosition;" + //gl_Position is special var used to store final position.
                    "v_TexCoordinate = a_TexCoordinate;" +    //pass through the texture coordinate
                    "}";                                      //multiply the vertex by the matrix to get the final point in normalized screen coords

    //use to access and set the view transformation
    private int vPMatrixHandle;

    private final String fragmentShaderCode =
            "uniform float alpha;" +     //alpha constant to control transparency
                    "precision mediump float;" +    //how much precision GPU uses when calculating floats. Don't need as high of precision in fragment shader.
                    "uniform vec4 vColor;" + //this is color from the vertex shader interpolated across triangle per fragment
                    "uniform sampler2D u_Texture;" +  //the input texture--reps actual texture data (as opposed to texture coords)
                    "varying vec2 v_TexCoordinate;" + //interpolated texture coordinate per fragment. Passed in interpolated texture coords from vertex shader

                    "void main() {" +        //entry point to code
                    "gl_FragColor = vColor * alpha * texture2D(u_Texture, v_TexCoordinate);" +   //pass the color directly through the pipeline
                    "}";                                                                    //multiply the color by texture val to get final output color
    //call texture2D(texture, textureCoordinate) to read in val of texture at current coordinate
}
