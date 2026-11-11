package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * 3D Visual Realism Suite (Minggu 11).
 *
 * Kontrol:
 *   B = Phong / Blinn-Phong      F = Flat / Smooth shading
 *   Z = Z-Buffer on/off          C = Back-Face Culling on/off
 *   T = tekstur on/off           N = filter Nearest / Bilinear+Mipmap
 *   S = shadow mapping on/off    L = cahaya berputar on/off
 *   SPASI = jeda animasi         V = V-Sync on/off (untuk ukur FPS)
 *   R = reset kamera             ESC = keluar
 *   Mouse kiri (drag) = putar kamera, scroll = zoom
 */
public class VisualRealismApp {

    private static final class SceneObject {
        final Mesh mesh;
        final Vector3f position;
        final Vector3f color;
        final boolean castShadow;
        final boolean textured;
        final Vector3f axis;
        final float spinSpeed; // derajat per detik
        float angle = 0f;

        SceneObject(Mesh mesh, Vector3f position, Vector3f color, boolean castShadow,
                    boolean textured, Vector3f axis, float spinSpeed) {
            this.mesh = mesh;
            this.position = position;
            this.color = color;
            this.castShadow = castShadow;
            this.textured = textured;
            this.axis = axis;
            this.spinSpeed = spinSpeed;
        }

        Matrix4f modelMatrix() {
            return new Matrix4f().translate(position).rotate((float) Math.toRadians(angle), axis);
        }
    }

    private long window;
    private int fbWidth = 1280, fbHeight = 720;

    private ShaderProgram sceneShader;
    private ShaderProgram depthShader;
    private ShadowMapFBO shadowFBO;
    private Texture2D texture;
    private final Camera camera = new Camera();
    private final List<SceneObject> objects = new ArrayList<>();

    // state fitur
    private boolean blinnPhong = true;
    private boolean flatShading = false;
    private boolean useTexture = true;
    private boolean mipmapFilter = true;
    private boolean shadows = true;
    private boolean animate = true;
    private boolean lightOrbit = false;
    private boolean vsync = false;
    private float lightAngle = (float) (Math.PI / 4.0);

    // input mouse
    private boolean dragging = false;
    private double lastX, lastY;

    // FPS
    private int frames = 0;
    private double fpsTimer = 0.0;
    private double fps = 0.0;

    public void run(String texturePath) {
        init(texturePath);
        loop();
        cleanup();
    }

    // ------------------------------------------------------------------ init
    private void init(String texturePath) {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Gagal menginisialisasi GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        window = glfwCreateWindow(1280, 720, "3D Visual Realism Suite", NULL, NULL);
        if (window == NULL) {
            throw new IllegalStateException("Gagal membuat window GLFW (butuh OpenGL 3.3)");
        }
        glfwMakeContextCurrent(window);
        glfwSwapInterval(vsync ? 1 : 0);
        GL.createCapabilities();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            glfwGetFramebufferSize(window, w, h);
            fbWidth = w.get(0);
            fbHeight = h.get(0);
        }

        setupCallbacks();

        glEnable(GL_MULTISAMPLE);
        sceneShader = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");
        depthShader = new ShaderProgram("/shaders/depth.vert", "/shaders/depth.frag");
        shadowFBO = new ShadowMapFBO();
        texture = Texture2D.loadOrProcedural(texturePath);
        buildScene();
    }

    private void buildScene() {
        Mesh floor = MeshFactory.plane(10f, 6f);
        Mesh sphere = MeshFactory.sphere(1.3f, 18, 36);
        Mesh torus = MeshFactory.torus(1.0f, 0.4f, 36, 18, 4f);
        Mesh cube = MeshFactory.cube(0.9f);

        objects.add(new SceneObject(floor, new Vector3f(0, 0, 0), new Vector3f(0.55f, 0.55f, 0.58f),
                false, true, new Vector3f(0, 1, 0), 0f));
        objects.add(new SceneObject(sphere, new Vector3f(0, 1.3f, 0), new Vector3f(0.85f, 0.25f, 0.22f),
                true, true, new Vector3f(0, 1, 0), 20f));
        objects.add(new SceneObject(torus, new Vector3f(3.6f, 1.8f, -1f), new Vector3f(0.95f, 0.75f, 0.2f),
                true, true, new Vector3f(1, 1, 0).normalize(), 45f));
        objects.add(new SceneObject(cube, new Vector3f(-3.4f, 0.9f, 1f), new Vector3f(0.25f, 0.45f, 0.9f),
                true, true, new Vector3f(0, 1, 0), 30f));
    }

    private void setupCallbacks() {
        glfwSetFramebufferSizeCallback(window, (w, width, height) -> {
            fbWidth = Math.max(1, width);
            fbHeight = Math.max(1, height);
        });

        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (action != GLFW_PRESS) return;
            switch (key) {
                case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(window, true);
                case GLFW_KEY_B -> blinnPhong = !blinnPhong;
                case GLFW_KEY_F -> flatShading = !flatShading;
                case GLFW_KEY_Z -> HSRManager.toggleDepthTest();
                case GLFW_KEY_C -> HSRManager.toggleBackFaceCulling();
                case GLFW_KEY_T -> useTexture = !useTexture;
                case GLFW_KEY_N -> {
                    mipmapFilter = !mipmapFilter;
                    texture.setFilterMode(mipmapFilter);
                }
                case GLFW_KEY_S -> shadows = !shadows;
                case GLFW_KEY_L -> lightOrbit = !lightOrbit;
                case GLFW_KEY_SPACE -> animate = !animate;
                case GLFW_KEY_V -> {
                    vsync = !vsync;
                    glfwSwapInterval(vsync ? 1 : 0);
                }
                case GLFW_KEY_R -> camera.reset();
                default -> {
                }
            }
        });

        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                dragging = (action == GLFW_PRESS);
            }
        });

        glfwSetCursorPosCallback(window, (w, x, y) -> {
            if (dragging) {
                camera.rotate((float) (-(x - lastX) * 0.005), (float) ((y - lastY) * 0.005));
            }
            lastX = x;
            lastY = y;
        });

        glfwSetScrollCallback(window, (w, xoff, yoff) -> camera.zoom((float) yoff * 0.8f));
    }

    // ------------------------------------------------------------------ loop
    private void loop() {
        double last = glfwGetTime();
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - last);
            last = now;

            update(dt);

            Vector3f lightPos = currentLightPos();
            Matrix4f lightSpace = computeLightSpaceMatrix(lightPos);

            renderShadowPass(lightSpace);   // Render Pass 1: depth dari sudut pandang cahaya
            renderScenePass(lightPos, lightSpace); // Render Pass 2: render normal dari kamera

            glfwSwapBuffers(window);
            glfwPollEvents();

            updateFps(now);
        }
    }

    private void update(float dt) {
        if (animate) {
            for (SceneObject o : objects) {
                o.angle += o.spinSpeed * dt;
            }
        }
        if (lightOrbit) {
            lightAngle += dt * 0.6f;
        }
    }

    private Vector3f currentLightPos() {
        return new Vector3f((float) Math.cos(lightAngle) * 8f, 10f, (float) Math.sin(lightAngle) * 8f);
    }

    /** Directional light -> proyeksi ortografik dari posisi cahaya menuju titik asal. */
    private Matrix4f computeLightSpaceMatrix(Vector3f lightPos) {
        Matrix4f lightProj = new Matrix4f().ortho(-14f, 14f, -14f, 14f, 1f, 40f);
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, new Vector3f(0f, 0f, 0f), new Vector3f(0f, 1f, 0f));
        return new Matrix4f(lightProj).mul(lightView);
    }

    // ----------------------------------------------------------- render pass 1
    private void renderShadowPass(Matrix4f lightSpace) {
        shadowFBO.bindForWriting();
        HSRManager.setupDepthTest(true);          // depth test wajib aktif untuk membuat shadow map
        HSRManager.setupBackFaceCulling(false);   // render kedua sisi agar tidak ada lubang pada shadow map

        depthShader.use();
        depthShader.setMat4("lightSpaceMatrix", lightSpace);
        for (SceneObject o : objects) {
            if (!o.castShadow) continue;
            depthShader.setMat4("model", o.modelMatrix());
            o.mesh.draw();
        }
        shadowFBO.unbind(fbWidth, fbHeight);
    }

    // ----------------------------------------------------------- render pass 2
    private void renderScenePass(Vector3f lightPos, Matrix4f lightSpace) {
        glClearColor(0.53f, 0.68f, 0.85f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        HSRManager.applyState();                  // Z-Buffer & Culling sesuai tombol Z / C

        // zNear tidak terlalu dekat ke 0 untuk mengurangi Z-fighting
        float aspect = (float) fbWidth / (float) fbHeight;
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(45.0), aspect, 0.1f, 100f);
        Matrix4f view = camera.getViewMatrix();

        sceneShader.use();
        sceneShader.setMat4("projection", projection);
        sceneShader.setMat4("view", view);
        sceneShader.setMat4("lightSpaceMatrix", lightSpace);
        sceneShader.setVec3("lightPos", lightPos);
        sceneShader.setVec3("viewPos", camera.getPosition());
        sceneShader.setVec3("lightColor", new Vector3f(1f, 1f, 1f));
        sceneShader.setBool("useBlinnPhong", blinnPhong);
        sceneShader.setBool("useFlatShading", flatShading);
        sceneShader.setBool("useShadows", shadows);

        texture.bind(0);
        shadowFBO.bindForReading(1);
        sceneShader.setInt("textureSampler", 0);
        sceneShader.setInt("shadowMap", 1);

        for (SceneObject o : objects) {
            sceneShader.setMat4("model", o.modelMatrix());
            sceneShader.setVec3("objectColor", o.color);
            sceneShader.setBool("useTexture", useTexture && o.textured);
            o.mesh.draw();
        }
    }

    // ------------------------------------------------------------------- HUD
    private void updateFps(double now) {
        frames++;
        if (now - fpsTimer >= 0.5) {
            fps = frames / (now - fpsTimer);
            frames = 0;
            fpsTimer = now;
            glfwSetWindowTitle(window, String.format(
                    "3D Visual Realism Suite | FPS: %.0f | %s | Normal: %s | Z-Buffer: %s | Cull: %s | Tekstur: %s | Shadow: %s | VSync: %s",
                    fps,
                    blinnPhong ? "Blinn-Phong" : "Phong",
                    flatShading ? "Flat" : "Smooth",
                    HSRManager.isDepthTestEnabled() ? "ON" : "OFF",
                    HSRManager.isCullingEnabled() ? "ON" : "OFF",
                    !useTexture ? "OFF" : (mipmapFilter ? "Bilinear+Mipmap" : "Nearest"),
                    shadows ? "ON" : "OFF",
                    vsync ? "ON" : "OFF"));
        }
    }

    private void cleanup() {
        for (SceneObject o : objects) o.mesh.cleanup();
        texture.cleanup();
        shadowFBO.cleanup();
        sceneShader.cleanup();
        depthShader.cleanup();
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }
}
