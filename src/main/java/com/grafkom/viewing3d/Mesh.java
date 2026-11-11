package com.grafkom.viewing3d;

import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL30.*;

/**
 * Mesh segitiga (tanpa index) yang menyimpan DUA set normal per-vertex:
 *   - flat normal   : normal bidang segitiga (Flat Shading)
 *   - smooth normal : rata-rata normal semua face yang berbagi posisi vertex (Smooth Shading)
 *
 * Layout VBO (11 float per vertex):
 *   [0..2] posisi | [3..5] smooth normal | [6..7] UV | [8..10] flat normal
 */
public class Mesh {
    private static final int FLOATS_PER_VERTEX = 11;

    private final int vao;
    private final int vbo;
    private final int vertexCount;

    /**
     * @param positions xyz tiap vertex, urutan CCW dilihat dari luar, 3 vertex = 1 segitiga
     * @param uvs       uv tiap vertex
     */
    public Mesh(float[] positions, float[] uvs) {
        vertexCount = positions.length / 3;
        if (vertexCount % 3 != 0 || uvs.length != vertexCount * 2) {
            throw new IllegalArgumentException("Data mesh tidak konsisten");
        }

        float[] flat = computeFaceNormals(positions);
        float[] smooth = averageVertexNormals(positions, flat);

        float[] data = new float[vertexCount * FLOATS_PER_VERTEX];
        for (int v = 0; v < vertexCount; v++) {
            int o = v * FLOATS_PER_VERTEX;
            data[o] = positions[v * 3];
            data[o + 1] = positions[v * 3 + 1];
            data[o + 2] = positions[v * 3 + 2];
            data[o + 3] = smooth[v * 3];
            data[o + 4] = smooth[v * 3 + 1];
            data[o + 5] = smooth[v * 3 + 2];
            data[o + 6] = uvs[v * 2];
            data[o + 7] = uvs[v * 2 + 1];
            data[o + 8] = flat[v * 3];
            data[o + 9] = flat[v * 3 + 1];
            data[o + 10] = flat[v * 3 + 2];
        }

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);

        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8L * Float.BYTES);
        glEnableVertexAttribArray(3);
        glBindVertexArray(0);
    }

    /** N_face = normalize((B - A) x (C - A)) untuk setiap segitiga. */
    private static float[] computeFaceNormals(float[] p) {
        int vc = p.length / 3;
        float[] out = new float[p.length];
        for (int t = 0; t < vc; t += 3) {
            Vector3f a = new Vector3f(p[t * 3], p[t * 3 + 1], p[t * 3 + 2]);
            Vector3f b = new Vector3f(p[(t + 1) * 3], p[(t + 1) * 3 + 1], p[(t + 1) * 3 + 2]);
            Vector3f c = new Vector3f(p[(t + 2) * 3], p[(t + 2) * 3 + 1], p[(t + 2) * 3 + 2]);
            Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
            if (n.lengthSquared() > 1e-12f) {
                n.normalize();
            } else {
                n.set(0f, 0f, 0f); // segitiga degenerate
            }
            for (int k = 0; k < 3; k++) {
                out[(t + k) * 3] = n.x;
                out[(t + k) * 3 + 1] = n.y;
                out[(t + k) * 3 + 2] = n.z;
            }
        }
        return out;
    }

    /** N_vertex = (SUM N_face_i) / ||SUM N_face_i|| untuk semua face yang berbagi posisi yang sama. */
    private static float[] averageVertexNormals(float[] p, float[] flat) {
        int vc = p.length / 3;
        Map<String, Vector3f> sums = new HashMap<>();
        for (int v = 0; v < vc; v++) {
            sums.computeIfAbsent(key(p, v), k -> new Vector3f())
                .add(flat[v * 3], flat[v * 3 + 1], flat[v * 3 + 2]);
        }
        float[] out = new float[p.length];
        for (int v = 0; v < vc; v++) {
            Vector3f n = new Vector3f(sums.get(key(p, v)));
            if (n.lengthSquared() > 1e-12f) {
                n.normalize();
            } else {
                n.set(flat[v * 3], flat[v * 3 + 1], flat[v * 3 + 2]);
            }
            out[v * 3] = n.x;
            out[v * 3 + 1] = n.y;
            out[v * 3 + 2] = n.z;
        }
        return out;
    }

    private static String key(float[] p, int v) {
        return Math.round(p[v * 3] * 1000f) + "_" + Math.round(p[v * 3 + 1] * 1000f) + "_" + Math.round(p[v * 3 + 2] * 1000f);
    }

    public void draw() {
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}
