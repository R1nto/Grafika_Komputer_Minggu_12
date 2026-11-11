package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

/** Pengatur Hidden Surface Removal: Z-Buffer (depth test) dan Back-Face Culling. */
public class HSRManager {
    private static boolean depthTestEnabled = true;
    private static boolean cullingEnabled = true;

    public static void setupDepthTest(boolean enable) {
        if (enable) {
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS); // Pixel lebih dekat lolos uji depth
        } else {
            glDisable(GL_DEPTH_TEST);
        }
    }

    public static void setupBackFaceCulling(boolean enable) {
        if (enable) {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);   // Buang polygon belakang
            glFrontFace(GL_CCW);   // Counter-Clockwise adalah tampak depan
        } else {
            glDisable(GL_CULL_FACE);
        }
    }

    // --- state yang dikendalikan tombol Z dan C ---

    public static void toggleDepthTest() {
        depthTestEnabled = !depthTestEnabled;
    }

    public static void toggleBackFaceCulling() {
        cullingEnabled = !cullingEnabled;
    }

    public static boolean isDepthTestEnabled() {
        return depthTestEnabled;
    }

    public static boolean isCullingEnabled() {
        return cullingEnabled;
    }

    /** Terapkan state HSR saat ini ke OpenGL (dipanggil sebelum render pass utama). */
    public static void applyState() {
        setupDepthTest(depthTestEnabled);
        setupBackFaceCulling(cullingEnabled);
    }
}
