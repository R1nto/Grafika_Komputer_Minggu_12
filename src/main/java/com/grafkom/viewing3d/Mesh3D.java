package com.grafkom.viewing3d;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL33.*;

public class Mesh3D {
    private final int vaoId;
    private final int vboId;
    private final int vertexCount;

    public Mesh3D(float[] vertexData) {
        this.vertexCount = vertexData.length / 11;
        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertexData.length);
        buffer.put(vertexData).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        int stride = 11 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);                    // aPos
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);     // aSmoothNormal
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);     // aTexCoords
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8L * Float.BYTES);     // aFlatNormal
        glEnableVertexAttribArray(3);

        glBindVertexArray(0);
    }

    public void draw() {
        glBindVertexArray(vaoId);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    /**
     * Bola dengan segitiga berorientasi CCW dilihat dari luar (front face = CCW),
     * sehingga Back-Face Culling membuang sisi dalam dan flat normal mengarah keluar.
     * Posisi vertex bola berpusat di titik asal, jadi smooth normal = posisi ternormalisasi.
     */
    public static Mesh3D createSphere(float radius, int sectors, int stacks) {
        List<Float> data = new ArrayList<>();
        for (int i = 0; i < stacks; ++i) {
            float phi1 = (float) (Math.PI * i / stacks);
            float phi2 = (float) (Math.PI * (i + 1) / stacks);
            for (int j = 0; j < sectors; ++j) {
                float theta1 = (float) (2.0 * Math.PI * j / sectors);
                float theta2 = (float) (2.0 * Math.PI * (j + 1) / sectors);

                Vector3f p1 = spherePoint(radius, phi1, theta1);
                Vector3f p2 = spherePoint(radius, phi2, theta1);
                Vector3f p3 = spherePoint(radius, phi2, theta2);
                Vector3f p4 = spherePoint(radius, phi1, theta2);

                // v = 1 di kutub atas agar gambar tidak terbalik (stb flip vertical aktif)
                Vector2f uv1 = new Vector2f((float) j / sectors, 1f - (float) i / stacks);
                Vector2f uv2 = new Vector2f((float) j / sectors, 1f - (float) (i + 1) / stacks);
                Vector2f uv3 = new Vector2f((float) (j + 1) / sectors, 1f - (float) (i + 1) / stacks);
                Vector2f uv4 = new Vector2f((float) (j + 1) / sectors, 1f - (float) i / stacks);

                Vector3f n1 = new Vector3f(p1).normalize();
                Vector3f n2 = new Vector3f(p2).normalize();
                Vector3f n3 = new Vector3f(p3).normalize();
                Vector3f n4 = new Vector3f(p4).normalize();

                addTriangle(data, p1, p3, p2, uv1, uv3, uv2, n1, n3, n2);
                addTriangle(data, p1, p4, p3, uv1, uv4, uv3, n1, n4, n3);
            }
        }
        return new Mesh3D(toFloatArray(data));
    }

    /** Lantai di ketinggian yLevel. Normal selalu +Y (bukan posisi ternormalisasi seperti bola). */
    public static Mesh3D createGroundPlane(float size, float yLevel) {
        List<Float> data = new ArrayList<>();
        Vector3f p1 = new Vector3f(-size, yLevel, -size);
        Vector3f p2 = new Vector3f(-size, yLevel, size);
        Vector3f p3 = new Vector3f(size, yLevel, size);
        Vector3f p4 = new Vector3f(size, yLevel, -size);
        Vector3f up = new Vector3f(0f, 1f, 0f);

        addTriangle(data, p1, p2, p3, new Vector2f(0, 4), new Vector2f(0, 0), new Vector2f(4, 0), up, up, up);
        addTriangle(data, p1, p3, p4, new Vector2f(0, 4), new Vector2f(4, 0), new Vector2f(4, 4), up, up, up);
        return new Mesh3D(toFloatArray(data));
    }

    private static Vector3f spherePoint(float r, float phi, float theta) {
        float x = (float) (r * Math.sin(phi) * Math.cos(theta));
        float y = (float) (r * Math.cos(phi));
        float z = (float) (r * Math.sin(phi) * Math.sin(theta));
        return new Vector3f(x, y, z);
    }

    private static void addTriangle(List<Float> data, Vector3f v0, Vector3f v1, Vector3f v2,
                                    Vector2f uv0, Vector2f uv1, Vector2f uv2,
                                    Vector3f n0, Vector3f n1, Vector3f n2) {
        Vector3f edge1 = new Vector3f(v1).sub(v0);
        Vector3f edge2 = new Vector3f(v2).sub(v0);
        Vector3f flatNormal = edge1.cross(edge2);
        if (flatNormal.lengthSquared() < 1e-12f) {
            return; // segitiga degenerate di kutub bola: tidak memiliki luas, dilewati
        }
        flatNormal.normalize();

        appendVertex(data, v0, n0, uv0, flatNormal);
        appendVertex(data, v1, n1, uv1, flatNormal);
        appendVertex(data, v2, n2, uv2, flatNormal);
    }

    private static void appendVertex(List<Float> data, Vector3f pos, Vector3f smoothN,
                                     Vector2f uv, Vector3f flatN) {
        data.add(pos.x); data.add(pos.y); data.add(pos.z);
        data.add(smoothN.x); data.add(smoothN.y); data.add(smoothN.z);
        data.add(uv.x); data.add(uv.y);
        data.add(flatN.x); data.add(flatN.y); data.add(flatN.z);
    }

    private static float[] toFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }
}
