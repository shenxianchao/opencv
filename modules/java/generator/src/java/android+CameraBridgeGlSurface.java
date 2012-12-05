package org.opencv.android;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.opencv.core.Mat;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

public class CameraBridgeGlSurface extends GLSurfaceView {
    private static final int MAX_UNSPECIFIED = -1;

    protected int mFrameWidth;
    protected int mFrameHeight;

    protected int mMaxHeight;
    protected int mMaxWidth;

    private Bitmap mCacheBitmap;

    private Renderer mRenderer = new Renderer() {

        private int[] textures = new int[1];

        private FloatBuffer vertexBuffer;
        private float vertices[] = {
                      -1.0f, -1.0f,  0.0f,        // V1 - bottom left
                      -1.0f,  1.0f,  0.0f,        // V2 - top left
                       1.0f, -1.0f,  0.0f,        // V3 - bottom right
                       1.0f,  1.0f,  0.0f         // V4 - top right
                };
        private FloatBuffer textureBuffer;  // buffer holding the texture coordinates

        private float texture[] = {
        // Mapping coordinates for the vertices
                0.0f, 1.0f,     // top left     (V2)
                0.0f, 0.0f,     // bottom left  (V1)
                1.0f, 1.0f,     // top right    (V4)
                1.0f, 0.0f      // bottom right (V3)
                };

        @Override
        public void onDrawFrame(GL10 gl) {

            Log.i(TAG, "Draw frame called");
            synchronized(this) {
                gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

                gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
                gl.glEnable(GL10.GL_TEXTURE_2D);

                gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
                GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, mCacheBitmap, 0);

                // Reset the Modelview Matrix
                gl.glLoadIdentity();
                // Drawing
                gl.glTranslatef(0.0f, 0.0f, -5.0f);     // move 5 units INTO the screen
                                                        // is the same as moving the camera 5 units awa

                // Texture is ready - need to draw it
                gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

                // Point to our buffers
                gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
                gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
                // Set the face rotation
                gl.glFrontFace(GL10.GL_CW);
                // Point to our vertex buffer
                gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
                gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);
                // Draw the vertices as triangle strip
                gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices.length / 3);
                //Disable the client state before leaving
                gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
                gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
            }
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int w, int h) {
            gl.glViewport(0, 0, w, h);
            gl.glGenTextures(1, textures, 0);
            // ...and bind it to our array
            gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

            // create nearest filtered texture
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);


            gl.glMatrixMode(GL10.GL_PROJECTION);    //Select The Projection Matrix
            gl.glLoadIdentity();                    //Reset The Projection Matrix
            //Calculate The Aspect Ratio Of The Window
            GLU.gluPerspective(gl, 45.0f, (float)w / (float)h, 0.1f, 100.0f);
            gl.glMatrixMode(GL10.GL_MODELVIEW);     //Select The Modelview Matrix
            gl.glLoadIdentity();                    //Reset The Modelview Matrix
        }

        @Override
        public void onSurfaceCreated(GL10 arg0, EGLConfig arg1) {
            /* Do required initialization for openGL */
               ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
               byteBuffer.order(ByteOrder.nativeOrder());
                // allocates the memory from the byte buffer
                vertexBuffer = byteBuffer.asFloatBuffer();

                // fill the vertexBuffer with the vertices
                vertexBuffer.put(vertices);

                // set the cursor position to the beginning of the buffer
                vertexBuffer.position(0);

                byteBuffer = ByteBuffer.allocateDirect(texture.length * 4);
                byteBuffer.order(ByteOrder.nativeOrder());
                textureBuffer = byteBuffer.asFloatBuffer();
                textureBuffer.put(texture);
                textureBuffer.position(0);

        }

    };


    public CameraBridgeGlSurface(Context context, AttributeSet attrs) {
        super(context,attrs);
        mMaxWidth = MAX_UNSPECIFIED;
        mMaxHeight = MAX_UNSPECIFIED;

        setRenderer(mRenderer);

    }

    public interface CvCameraViewListener {
        /**
         * This method is invoked when camera preview has started. After this method is invoked
         * the frames will start to be delivered to client via the onCameraFrame() callback.
         * @param width -  the width of the frames that will be delivered
         * @param height - the height of the frames that will be delivered
         */
        public void onCameraViewStarted(int width, int height);

        /**
         * This method is invoked when camera preview has been stopped for some reason.
         * No frames will be delivered via onCameraFrame() callback after this method is called.
         */
        public void onCameraViewStopped();

        /**
         * This method is invoked when delivery of the frame needs to be done.
         * The returned values - is a modified frame which needs to be displayed on the screen.
         * TODO: pass the parameters specifying the format of the frame (BPP, YUV or RGB and etc)
         */
        public Mat onCameraFrame(Mat inputFrame);

    }

    public class FrameSize {
        public int width;
        public int height;

        public FrameSize(int w, int h) {
            width = w;
            height = h;
        }
    }


    private static final int STOPPED = 0;
    private static final int STARTED = 1;
    private  static final String TAG = "SampleCvBase";

    private CvCameraViewListener mListener;
    private int mState = STOPPED;

    private boolean mEnabled;
    private boolean mSurfaceExist;



    private Object mSyncObject = new Object();

    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        synchronized(mSyncObject) {
            if (!mSurfaceExist) {
                mSurfaceExist = true;
                checkCurrentState();
            } else {
                /** Surface changed. We need to stop camera and restart with new parameters */
                /* Pretend that old surface has been destroyed */
                mSurfaceExist = false;
                checkCurrentState();
                /* Now use new surface. Say we have it now */
                mSurfaceExist = true;
                checkCurrentState();
            }
        }
        super.surfaceChanged(arg0, arg1, arg2, arg3);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        /* Do nothing. Wait until surfaceChanged delivered */
        super.surfaceCreated(holder);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);

        synchronized(mSyncObject) {
            mSurfaceExist = false;
            checkCurrentState();
        }
    }


    /**
     * This method is provided for clients, so they can enable the camera connection.
     * The actuall onCameraViewStarted callback will be delivered only after both this method is called and surface is available
     */
    public void enableView() {
        synchronized(mSyncObject) {
            mEnabled = true;
            checkCurrentState();
        }
        /* This will start OpenGL render thread */
        super.onResume();
    }

    /**
     * This method is provided for clients, so they can disable camera connection and stop
     * the delivery of frames eventhough the surfaceview itself is not destroyed and still stays on the scren
     */
    public void disableView() {
        /* This will stop openGL render thread */
        super.onPause();

        synchronized(mSyncObject) {
            mEnabled = false;
            checkCurrentState();
        }
    }


    public void setCvCameraViewListener(CvCameraViewListener listener) {
        mListener = listener;
    }

    /**
     * This method sets the maximum size that camera frame is allowed to be. When selecting
     * size - the biggest size which less or equal the size set will be selected.
     * As an example - we set setMaxFrameSize(200,200) and we have 176x152 and 320x240 sizes. The
     * preview frame will be selected with 176x152 size.
     * This method is usefull when need to restrict the size of preview frame for some reason (for example for video recording)
     * @param maxWidth - the maximum width allowed for camera frame.
     * @param maxHeight - the maxumum height allowed for camera frame
     */
    public void setMaxFrameSize(int maxWidth, int maxHeight) {
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
    }

    /**
     * Called when mSyncObject lock is held
     */
    private void checkCurrentState() {
        int targetState;

        if (mEnabled && mSurfaceExist) {
            targetState = STARTED;
        } else {
            targetState = STOPPED;
        }

        if (targetState != mState) {
            /* The state change detected. Need to exit the current state and enter target state */
            processExitState(mState);
            mState = targetState;
            processEnterState(mState);
        }
    }

    private void processEnterState(int state) {
        switch(state) {
        case STARTED:
            onEnterStartedState();
            if (mListener != null) {
                mListener.onCameraViewStarted(mFrameWidth, mFrameHeight);
            }
            break;
        case STOPPED:
            onEnterStoppedState();
            if (mListener != null) {
                mListener.onCameraViewStopped();
            }
            break;
        };
    }


    private void processExitState(int state) {
        switch(state) {
        case STARTED:
            onExitStartedState();
            break;
        case STOPPED:
            onExitStoppedState();
            break;
        };
    }

    private void onEnterStoppedState() {
        /* nothing to do */
    }

    private void onExitStoppedState() {
        /* nothing to do */
    }

    private void onEnterStartedState() {

        connectCamera(getWidth(), getHeight());
        /* Now create cache Bitmap */
        mCacheBitmap = Bitmap.createBitmap(mFrameWidth, mFrameHeight, Bitmap.Config.ARGB_8888);

    }

    private void onExitStartedState() {

        disconnectCamera();
        if (mCacheBitmap != null) {
            mCacheBitmap.recycle();
        }
    }


    /**
     * This method shall be called by the subclasses when they have valid
     * object and want it to be delivered to external client (via callback) and
     * then displayed on the screen.
     * @param frame - the current frame to be delivered
     */
    protected void deliverAndDrawFrame(Mat frame) {
        Mat modified;

            if (mListener != null) {
                modified = mListener.onCameraFrame(frame);
            } else {
                modified = frame;
            }

            /* We need to send the bitmap into the renderer queue  -  update Cache bitmap */
            synchronized(mRenderer) {
                /* We have syncrhonization on mRenderer object so that renderer never draw partially prepare bitmap */
                if (modified != null) {
                    Utils.matToBitmap(modified, mCacheBitmap);
                }
            }
            // Ready to process next bitmap

    }

    /**
     * This method is invoked shall perform concrete operation to initialize the camera.
     * CONTRACT: as a result of this method variables mFrameWidth and mFrameHeight MUST be
     * initialized with the size of the Camera frames that will be delivered to external processor.
     * @param width - the width of this SurfaceView
     * @param height - the height of this SurfaceView
     */
    protected void connectCamera(int width, int height){

    }

    /**
     * Disconnects and release the particular camera object beeing connected to this surface view.
     * Called when syncObject lock is held
     */
    protected  void disconnectCamera(){

    }


    public interface ListItemAccessor {
        public int getWidth(Object obj);
        public int getHeight(Object obj);
    };


    /**
     * This helper method can be called by subclasses to select camera preview size.
     * It goes over the list of the supported preview sizes and selects the maximum one which
     * fits both values set via setMaxFrameSize() and surface frame allocated for this view
     * @param supportedSizes
     * @param surfaceWidth
     * @param surfaceHeight
     * @return
     */
    protected FrameSize calculateCameraFrameSize(List<?> supportedSizes, ListItemAccessor accessor, int surfaceWidth, int surfaceHeight) {
        int calcWidth = 0;
        int calcHeight = 0;

        int maxAllowedWidth = (mMaxWidth != MAX_UNSPECIFIED && mMaxWidth < surfaceWidth)? mMaxWidth : surfaceWidth;
        int maxAllowedHeight = (mMaxHeight != MAX_UNSPECIFIED && mMaxHeight < surfaceHeight)? mMaxHeight : surfaceHeight;

        for (Object size : supportedSizes) {
            int width = accessor.getWidth(size);
            int height = accessor.getHeight(size);

            if (width <= maxAllowedWidth && height <= maxAllowedHeight) {
                if (width >= calcWidth && height >= calcHeight) {
                    calcWidth = (int) width;
                    calcHeight = (int) height;
                }
            }
        }
        return new FrameSize(calcWidth, calcHeight);
    }



}
