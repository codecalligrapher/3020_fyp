package com.helloarbridge4.SizeCheck;

import com.google.ar.core.PointCloud;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.ux.TransformableNode;
import com.helloarbridge4.Object.ObjectCodes;

import java.nio.FloatBuffer;

public class SizeCheckHandler {
    private static final Vector3 REGION_LIMITS = new Vector3(0.8f,0.8f,1.0f);
    private static final float OBJECT_THRESH = 0.5f;
    private final Float REGION_THRESH = 0.3f;
    ObjectCodes objectCode;
    PointCloud pointCloud;

    Quaternion objectRotation;
    Vector3 objectSize;
    TransformableNode objectCentre;

    public void loadPointCloud(PointCloud pointCloud) {
        this.pointCloud = pointCloud;
    }

    public void setObjectCentre(TransformableNode transformableNode) {
        this.objectCentre = transformableNode;
        this.objectRotation = objectCentre.getWorldRotation();
    }

    public void setObjectType(ObjectCodes objectCode) {
        if (objectCode == null) return;
        this.objectCode = objectCode;
        switch(objectCode) {
            case CARRYON:
                    objectSize = ObjectSizes.getCarryOnSize();
                break;
            case DUFFEL:
                    objectSize = ObjectSizes.getDuffelSize();
                break;
            case PERSONAL:
                    objectSize = ObjectSizes.getPersonalItemSize();
                break;
        }
    }
}
