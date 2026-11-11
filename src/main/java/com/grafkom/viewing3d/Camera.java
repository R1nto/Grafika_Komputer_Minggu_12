package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Kamera orbit: drag mouse kiri untuk memutar, scroll untuk zoom. */
public class Camera {
    private static final float MIN_PITCH = -1.4f;
    private static final float MAX_PITCH = 1.4f;

    private final Vector3f target = new Vector3f(0f, 1f, 0f);
    private float yaw = 0.6f;
    private float pitch = 0.45f;
    private float distance = 14f;

    public void reset() {
        yaw = 0.6f;
        pitch = 0.45f;
        distance = 14f;
    }

    public void rotate(float dYaw, float dPitch) {
        yaw += dYaw;
        pitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, pitch + dPitch));
    }

    public void zoom(float amount) {
        distance = Math.max(3f, Math.min(40f, distance - amount));
    }

    public Vector3f getPosition() {
        float cp = (float) Math.cos(pitch);
        return new Vector3f(
                target.x + distance * cp * (float) Math.sin(yaw),
                target.y + distance * (float) Math.sin(pitch),
                target.z + distance * cp * (float) Math.cos(yaw));
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(getPosition(), target, new Vector3f(0f, 1f, 0f));
    }
}
