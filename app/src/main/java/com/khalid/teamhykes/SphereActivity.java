package com.khalid.teamhykes;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageSwitcher;
import android.widget.SimpleExpandableListAdapter;

import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Logger;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.BitmapHelper;
import com.threed.jpct.util.MemoryHelper;

import java.lang.reflect.Field;
import java.util.Random;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

public class SphereActivity extends AppCompatActivity {
    public static SphereActivity master = null;
    private GLSurfaceView mGLView;
    private CelestialSphereRenderer mRenderer = null;
    private boolean g12 = true;
    private float mXRotation = 0;
    private float mYRotation = 0;
    private float mXPosition = -1;
    private float mYPosition = -1;
    private int count = 0;
    private FrameBuffer mFrameBuffer;
    private World mWorld = null;
    private RGBColor mPainting = new RGBColor(50, 50, 100);
    private Object3D mSphere = null;
    private Light mLight = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.log("onCreate");

        if (master != null) {
            copy(master);
        }

        super.onCreate(savedInstanceState);

        mGLView = new GLSurfaceView(getApplication());
        if (g12) {
            mGLView.setEGLContextClientVersion(2);
        } else {
            mGLView.setEGLConfigChooser(
                    new GLSurfaceView.EGLConfigChooser() {
                        @Override
                        public EGLConfig chooseConfig(EGL10 egl10, EGLDisplay eglDisplay) {
                            int[] attributes = new int[]{EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_NONE};
                            EGLConfig[] configs = new EGLConfig[1];
                            int[] result = new int[1];
                            egl10.eglChooseConfig(eglDisplay, attributes, configs, 1, result);
                            return configs[0];
                        }
                    }
            );
        }

        mRenderer = new CelestialSphereRenderer();
        mGLView.setRenderer(mRenderer);
        setContentView(mGLView);
    }



    @Override
    protected void onPause() {
        super.onPause();
        mGLView.onPause();
    }


    private void copy(Object src) {
        try {
            Logger.log("Copying data from master Activity!");
            Field[] fs = src.getClass().getDeclaredFields();
            for (Field f : fs) {
                f.setAccessible(true);
                f.set(this, f.get(src));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean onTouchEvent(MotionEvent me) {
        if (me.getAction() == MotionEvent.ACTION_DOWN) {
            mXPosition = me.getX();
            mYPosition = me.getY();
            return true;
        }

        if (me.getAction() == MotionEvent.ACTION_UP) {
            mXPosition = -1;
            mYPosition = -1;
            mXRotation = 0;
            mYRotation = 0;
            return true;
        }

        if (me.getAction() == MotionEvent.ACTION_MOVE) {
            float xd = me.getX() - mXPosition;
            float yd = me.getY() - mYPosition;
            mXPosition = me.getX();
            mYPosition = me.getY();
            mYRotation = xd / -100f;
            mXRotation = yd / -100f;
            return true;
        }

        try {
            Thread.sleep(15);
        } catch (Exception e) {
            // No need for this...
        }

        return super.onTouchEvent(me);
    }

    protected boolean isFullscreenOpaque() {
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLView.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.exit(0);
    }


    public static SphereActivity getApp() {
        return master;
    }

    private class CelestialSphereRenderer implements GLSurfaceView.Renderer {
        private long time = System.currentTimeMillis();
        public CelestialSphereRenderer() {

        }
        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {

        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int width, int height) {
            if(mFrameBuffer != null) {
                mFrameBuffer.dispose();
            }
            if(g12) {
                mFrameBuffer = new FrameBuffer(width, height);
            } else {
                mFrameBuffer = new FrameBuffer(gl10, width, height);
            }
            if(master == null) {
                mWorld = new World();
                mWorld.setAmbientLight(20, 20, 20);
                mLight = new Light(mWorld);
                mLight.setIntensity(250, 250, 250);
                Texture texture = new Texture(
                        BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.earth)),
                                256, 256)
                );
                TextureManager.getInstance().addTexture("texture", texture);
                mSphere = Primitives.getCube(10);
                mSphere.calcTextureWrapSpherical();
                mSphere.setTexture("texture");
                mSphere.strip();
                mSphere.build();
                mWorld.addObject(mSphere);

                Camera camera = mWorld.getCamera();
                camera.moveCamera(Camera.CAMERA_MOVEOUT, 50);
                camera.lookAt(mSphere.getTransformedCenter());
                SimpleVector simpleVector = new SimpleVector();
                simpleVector.set(mSphere.getTransformedCenter());
                simpleVector.x -= 100;
                simpleVector.y -= 100;
                mLight.setPosition(simpleVector);
                MemoryHelper.compact();

                if(master == null) {
                    Logger.log("saving master activity");
                    master = SphereActivity.this;
                }
            }
        }

        @Override
        public void onDrawFrame(GL10 gl10) {
              if(mYRotation != 0) {
                  mSphere.rotateY(mYRotation);
                  mYRotation = 0;
              }
              if(mXRotation != 0) {
                  mSphere.rotateX(mXRotation);
                  mXRotation = 0;
              }
              mFrameBuffer.clear(mPainting);
              mWorld.renderScene(mFrameBuffer);
              mWorld.draw(mFrameBuffer);
              mFrameBuffer.display();

              if(System.currentTimeMillis() - time >= 1000) {
                  Logger.log(count + "counting");
                  count = 0;
                  time = System.currentTimeMillis();
              }
              count++;
        }
    }
}