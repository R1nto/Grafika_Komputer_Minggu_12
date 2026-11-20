package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

public class Week11ShadingApp {
    private static final int WIDTH = 960, HEIGHT = 720;
    private long window;
    private Camera3D camera;
    private PerspectiveProjection projection;
    private ShaderProgram shader;
    private Mesh3D mainSphere, innerSphere;

    private boolean useBlinnPhong = true;   // Key 'B'
    private boolean useFlatShading = false; // Key 'F'
    private boolean enableZBuffer = true;   // Key 'Z'
    private boolean enableCulling = true;   // Key 'C'

    private float azimuth = 30.0f, elevation = 20.0f, angle = 0.0f;

    public void run() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal inisialisasi GLFW");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(WIDTH, HEIGHT, "Tugas Minggu 11 - Shading & HSR", 0, 0);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                if (key == GLFW_KEY_LEFT) azimuth -= 4.0f;
                if (key == GLFW_KEY_RIGHT) azimuth += 4.0f;
                if (key == GLFW_KEY_UP) elevation = Math.min(85.0f, elevation + 4.0f);
                if (key == GLFW_KEY_DOWN) elevation = Math.max(-85.0f, elevation - 4.0f);
            }
            if (action == GLFW_PRESS) {
                if (key == GLFW_KEY_B) useBlinnPhong = !useBlinnPhong;
                if (key == GLFW_KEY_F) useFlatShading = !useFlatShading;
                if (key == GLFW_KEY_Z) enableZBuffer = !enableZBuffer;
                if (key == GLFW_KEY_C) enableCulling = !enableCulling;
                updateWindowTitle();
            }
        });

        camera = new Camera3D(new Vector3f(0, 1, 5), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        projection = new PerspectiveProjection(45.0f, (float) WIDTH / HEIGHT, 0.1f, 50.0f);
        shader = new ShaderProgram("/shaders/week11.vert", "/shaders/week11.frag");

        // Dua objek saling tumpang tindih untuk menguji Z-Buffer Depth Test
        mainSphere = Mesh3D.createSphere(1.1f, 24, 18);
        innerSphere = Mesh3D.createSphere(0.7f, 24, 18);
        updateWindowTitle();

        while (!glfwWindowShouldClose(window)) {
            angle += 0.5f;
            camera.updateOrbitalPosition(5.5f, azimuth, elevation);

            HSRManager.setupDepthTest(enableZBuffer);
            HSRManager.setupBackFaceCulling(enableCulling);

            glClearColor(0.10f, 0.12f, 0.16f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            shader.use();
            shader.setMat4("view", camera.calculateLookAtMatrix());
            shader.setMat4("projection", projection.calculateProjectionMatrix());
            shader.setVec3("lightPos", new Vector3f(3.0f, 4.0f, 3.0f));
            shader.setVec3("viewPos", camera.getEye());
            shader.setVec3("lightColor", new Vector3f(1.0f, 1.0f, 1.0f));
            shader.setBool("useBlinnPhong", useBlinnPhong);
            shader.setBool("useFlatShading", useFlatShading);

            // Objek 1 (Bola Utama)
            shader.setMat4("model", new Matrix4f().rotateY((float) Math.toRadians(angle)));
            shader.setVec3("objectColor", new Vector3f(0.20f, 0.65f, 0.85f));
            mainSphere.draw();

            // Objek 2 (Bola Penguji HSR di depan/samping bola utama)
            shader.setMat4("model", new Matrix4f().translate(0.9f, 0.2f, 0.6f));
            shader.setVec3("objectColor", new Vector3f(0.90f, 0.35f, 0.25f));
            innerSphere.draw();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void updateWindowTitle() {
        glfwSetWindowTitle(window, String.format(
                "Minggu 11 | [B] %s | [F] %s | [Z] Z-Buffer:%s | [C] Culling:%s",
                useBlinnPhong ? "Blinn-Phong" : "Phong",
                useFlatShading ? "Flat" : "Smooth",
                enableZBuffer ? "ON" : "OFF",
                enableCulling ? "ON" : "OFF"));
    }

    public static void main(String[] args) {
        new Week11ShadingApp().run();
    }
}
