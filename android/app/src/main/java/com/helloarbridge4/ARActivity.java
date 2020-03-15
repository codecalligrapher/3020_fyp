package com.helloarbridge4;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseTransformableNode;
import com.google.ar.sceneform.ux.SelectionVisualizer;
import com.google.ar.sceneform.ux.TransformableNode;

import com.helloarbridge4.Object.ObjectCodes;
import com.helloarbridge4.Render.PointCloudRenderer;
import com.helloarbridge4.ColourChange.ColourChangeHandler;
import com.helloarbridge4.SizeCheck.SizeCheckHandler;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;


public class ARActivity extends AppCompatActivity {
    private static final String SCN_TAG = "OnSceneUpdate";
    private static final String TAG = "ARActivity";
    private static final double MIN_OPENGL_VERSION = 3.0;
    private static final int FRAME_COUNT_THRESH = 60;
    private static final float LIGHT_THRESH = 0.3f;

    private static final int PERSONAL_ID = R.id.radio_personal;
    private static final int CARRYON_ID = R.id.radio_carryon;
    private static final int DUFFEL_ID = R.id.radio_duffel;

    private ArFragment arFragment;
    private Timer timer;
    private ImageButton removeObjects;
    private TextView onScreenText;
    private ArSceneView arSceneView;
    private boolean cameraPermissionRequested;
    boolean objectPlaced = false;

    private ColourChangeHandler colourChangeHandler;
    private TransformableNode node;
    private Pose planePose;

    private int frames = 0;
    private int framesStart = 0;

    private PointCloudRenderer pointCloudRenderer;
    private SizeCheckHandler sizeHandler;
    private ObjectCodes currentModel;

    //local coordinates of placed object anchor
    private Vector3 anchorPosition;
    private RadioGroup radioGroup;
    private AnchorNode anchorNode;
    private List<Float[]> positions3D;
    PointCloudVisualiser pcVis;

    static TextView debugText_height, debugText_width, debugText_length;

    private Session session;
    private Config config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //RN Bridge
        Intent intent = getIntent();

        String message = intent.getStringExtra(MainActivity.REQ_MSG);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.ar_layout);
        positions3D = new ArrayList<>();
        sizeHandler = new SizeCheckHandler();

        //connecting views
        radioGroup = findViewById(R.id.change_type);
        onScreenText = findViewById(R.id.onScreenText);
        //removeObjects = findViewById(R.id.removeObjects);

        //TODO remove
        debugText_height = findViewById(R.id.debugText_height);
        debugText_width = findViewById(R.id.debugText_width);
        debugText_length = findViewById(R.id.debugText_length);
        //end remove

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        removeObjects = findViewById(R.id.removeObjects);
        colourChangeHandler = new ColourChangeHandler(this.getApplicationContext());


        arFragment.setOnTapArPlaneListener(
            (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                if(plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING) {
                    return;
                }
                planePose = plane.getCenterPose();

                measure();
                disablePlaneDetection();


                if (!objectPlaced) {
                    initAnchor(hitResult);
                    createNode(arFragment);
                    colourChangeHandler.setAnchorNode(anchorNode);
                    colourChangeHandler.setTransformableNode(node);

                    setModel(radioGroup.getCheckedRadioButtonId());
                    objectPlaced = true;
                }
            });


        TextView t = findViewById(R.id.t);

        //buttons
        radioGroup.setOnCheckedChangeListener(
        (group, checkedId) -> {
            setModel(checkedId);
        }
            );

//
        removeObjects.setOnClickListener(
                w -> {
                    if (objectPlaced) {
                        objectPlaced = false;
                        framesStart = 0;
                        try {
                            removeAnchorNode(anchorNode);
                        } catch (NullPointerException e) {
                            Log.w(TAG, e.getLocalizedMessage());
                        }
                        radioGroup.clearCheck();
                    }
                }
        );
    }

    private boolean scan = false;
    public void measure() {
        if (scan) {
            scan = false;
            return;
        }


        if (arFragment.getArSceneView().getArFrame() == null) {
            return;
        }
        // If ARCore is not tracking yet, then don't process anything.
        if (arFragment.getArSceneView().getSession() == null) {
            return;
        }

        scan = true;

        //TODO remove
        pcVis = new PointCloudVisualiser(getApplicationContext());
        arFragment.getArSceneView().getScene().addChild(pcVis);
        // If there is no frame then don't process anything.

        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onSceneUpdate);

    }




    private void removeAnchorNode(AnchorNode nodeToremove) throws NullPointerException {
        //Remove an anchor node
        if (nodeToremove != null) {
            arFragment.getArSceneView().getScene().removeChild(nodeToremove);
            nodeToremove.getAnchor().detach();
            nodeToremove.setParent(null);
            nodeToremove = null;
        }
    }

    private void createNode(ArFragment arFragment) throws NullPointerException {
        try {
            node = new TransformableNode(arFragment.getTransformationSystem());
        } catch (Exception e){
            Log.d(TAG, "objectHandler: " + e.getMessage());
        }
    }

    private void disablePlaneDetection() {
        try {
            //disable plane detection
            arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
            arFragment.getPlaneDiscoveryController().hide();
            arFragment.getPlaneDiscoveryController().setInstructionView(null);
            arFragment.getArSceneView().getPlaneRenderer().setEnabled(false);
        } catch (NullPointerException e){
            Log.e("getSession",e.getMessage());
        }
    }

    private void initAnchor(HitResult hitResult) {
        // Create the Anchor at hit result
        Anchor anchor = hitResult.createAnchor();
        anchorNode = new AnchorNode(anchor);
        anchorPosition = anchorNode.getWorldPosition();
        //attach arFragment to hitResult via anchorNode
        anchorNode.setParent(arFragment.getArSceneView().getScene());
    }

    private void initFragment() {
        try {
            arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
            //TODO
            //scene = arFragment.getArSceneView().getScene();
            //arFragment.getTransformationSystem().setSelectionVisualizer(new CustomVisualizer());
        } catch (NullPointerException n){
            Log.wtf("arFragment", n.getMessage());
        }
    }

    private void initSession() {
        try {
            session = new Session(this);
            config = new Config(session);
            //config.setLightEstimationMode(Config.LightEstimationMode.AMBIENT_INTENSITY);
            config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL);
        } catch (Exception e) {
            Log.e(TAG, "session: " + e.getMessage());
        }
    }

    private void onSceneUpdate(FrameTime frameTime) {
        Log.d(TAG,"onSceneUpdate");
        if (!scan) return;

        //disablePlaneDetection();
        setOnScreenText(arFragment);
        Frame frame = arFragment.getArSceneView().getArFrame();
        PointCloud pointCloud = frame.acquirePointCloud();

        pcVis.update(pointCloud);

        FloatBuffer points = pointCloud.getPoints();

        FitCodes fitCode = sizeHandler.checkIfFits(currentModel,node.getWorldPosition(),points,planePose);
        if (fitCode != null) {
            colourChangeHandler.setObject(fitCode);
            updateDebugText(
                    String.valueOf(sizeHandler.getBoxLength()),
                    String.valueOf(sizeHandler.getBoxWidth()),
                    String.valueOf(sizeHandler.getHighZ()));
        }


        pointCloud.release();





    }
    private boolean nodeNotNull() {
        return (node != null);
    }


    private void setModel(int toggleId) {
        removeAllModels();
        switch (toggleId){
            case PERSONAL_ID:
                currentModel = ObjectCodes.PERSONAL;
                colourChangeHandler.updateObject(currentModel);
                colourChangeHandler.setObject(FitCodes.NONE);
                break;
            case DUFFEL_ID:
                currentModel = ObjectCodes.DUFFEL;
                colourChangeHandler.updateObject(currentModel);
                colourChangeHandler.setObject(FitCodes.NONE);
                break;
            case CARRYON_ID:
                currentModel = ObjectCodes.CARRYON;
                colourChangeHandler.updateObject(currentModel);
                colourChangeHandler.setObject(FitCodes.NONE);
                break;
            default:
                break;
        }
        Log.d("setModel", "exit");
    }

    private void removeAllModels(){
        try{
            node.setRenderable(null);
        } catch (Exception e){
            Log.e("removeAllModels", e.getMessage());
        }
    }

    //API Required Calls
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (arSceneView == null) {
            return;
        }

        if (arSceneView.getSession() == null) {
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                Config.LightEstimationMode lightEstimationMode =
                        Config.LightEstimationMode.ENVIRONMENTAL_HDR;
                Session session = new Session(this);

                Config config = new Config(session);
                config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
                config.setLightEstimationMode(lightEstimationMode);
                session.configure(config);
                if (session == null) {
                    cameraPermissionRequested = true;
                    return;
                } else {
                    arSceneView.setupSession(session);
                }
            } catch (UnavailableException e) {
                Log.e(TAG,e.getMessage());
            }
        }

        try {
            arSceneView.resume();
        } catch (CameraNotAvailableException ex) {
            Log.e(TAG,"No Camera");
            finish();
            return;
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (arSceneView != null) {
            arSceneView.pause();
        }
    }

    public class CustomVisualizer implements SelectionVisualizer {
        @Override
        public void applySelectionVisual(BaseTransformableNode node) {}
        @Override
        public void removeSelectionVisual(BaseTransformableNode node) {}
    }


    //TODO remove
    private void updateDebugText(String textOne, String textTwo, String textThree) {
        String lengthText = "Length: " + textOne;
        String widthText = "Width: " + textTwo;
        String heightText = "HighZ: " + textThree;
        debugText_length.setText(lengthText);
        debugText_width.setText(widthText);
        debugText_height.setText(heightText);

    }

    //TODO remove
    private void setOnScreenText(ArFragment arFragment) throws NullPointerException {
        Frame frame = arFragment.getArSceneView().getArFrame();

        if (frame != null) {
            for (Plane plane : frame.getUpdatedTrackables(Plane.class)) {
                if (plane != null || node.getRenderable() == null) {
                    onScreenText.setText(R.string.planeDetected);
                } if(node.getRenderable() != null) {
                    onScreenText.setText(R.string.objectPlaced);
                }else {
                    onScreenText.setText(R.string.planeNotDetected);
                }
            }
        }

    }



















}
