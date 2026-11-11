package com.grafkom.viewing3d;

import java.util.ArrayList;
import java.util.List;

/** Pembuat mesh prosedural. Semua segitiga berorientasi CCW dilihat dari luar (front face = CCW). */
public final class MeshFactory {
    private MeshFactory() {
    }

    /** Titik mesh: {x, y, z, u, v}. */
    private static float[] vtx(float x, float y, float z, float u, float v) {
        return new float[]{x, y, z, u, v};
    }

    private static final class Builder {
        private final List<Float> pos = new ArrayList<>();
        private final List<Float> uv = new ArrayList<>();

        void tri(float[] a, float[] b, float[] c) {
            for (float[] p : new float[][]{a, b, c}) {
                pos.add(p[0]);
                pos.add(p[1]);
                pos.add(p[2]);
                uv.add(p[3]);
                uv.add(p[4]);
            }
        }

        Mesh build() {
            float[] p = new float[pos.size()];
            for (int i = 0; i < p.length; i++) p[i] = pos.get(i);
            float[] t = new float[uv.size()];
            for (int i = 0; i < t.length; i++) t[i] = uv.get(i);
            return new Mesh(p, t);
        }
    }

    // ---------------------------------------------------------------- Sphere
    public static Mesh sphere(float radius, int stacks, int slices) {
        Builder b = new Builder();
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                float[] p00 = spherePoint(radius, stacks, slices, i, j);
                float[] p01 = spherePoint(radius, stacks, slices, i, j + 1);
                float[] p10 = spherePoint(radius, stacks, slices, i + 1, j);
                float[] p11 = spherePoint(radius, stacks, slices, i + 1, j + 1);
                if (i != 0) b.tri(p00, p01, p11);            // di kutub atas segitiga ini degenerate
                if (i != stacks - 1) b.tri(p00, p11, p10);   // di kutub bawah segitiga ini degenerate
            }
        }
        return b.build();
    }

    private static float[] spherePoint(float r, int stacks, int slices, int i, int j) {
        double theta = Math.PI * i / stacks;
        double phi = 2.0 * Math.PI * j / slices;
        return vtx((float) (r * Math.sin(theta) * Math.cos(phi)),
                   (float) (r * Math.cos(theta)),
                   (float) (r * Math.sin(theta) * Math.sin(phi)),
                   (float) j / slices,
                   1f - (float) i / stacks);
    }

    // ----------------------------------------------------------------- Torus
    public static Mesh torus(float majorR, float minorR, int rings, int sides, float uTiles) {
        Builder b = new Builder();
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < sides; j++) {
                float[] q00 = torusPoint(majorR, minorR, rings, sides, uTiles, i, j);
                float[] q10 = torusPoint(majorR, minorR, rings, sides, uTiles, i + 1, j);
                float[] q01 = torusPoint(majorR, minorR, rings, sides, uTiles, i, j + 1);
                float[] q11 = torusPoint(majorR, minorR, rings, sides, uTiles, i + 1, j + 1);
                b.tri(q00, q01, q11);
                b.tri(q00, q11, q10);
            }
        }
        return b.build();
    }

    private static float[] torusPoint(float R, float r, int rings, int sides, float uTiles, int i, int j) {
        double alpha = 2.0 * Math.PI * i / rings;
        double beta = 2.0 * Math.PI * j / sides;
        double ring = R + r * Math.cos(beta);
        return vtx((float) (ring * Math.cos(alpha)),
                   (float) (r * Math.sin(beta)),
                   (float) (ring * Math.sin(alpha)),
                   uTiles * i / rings,
                   (float) j / sides);
    }

    // ------------------------------------------------------------------ Cube
    public static Mesh cube(float h) {
        Builder b = new Builder();
        // tiap sisi: kiri-bawah, kanan-bawah, kanan-atas, kiri-atas (dilihat dari luar)
        face(b, new float[]{h, -h, h}, new float[]{h, -h, -h}, new float[]{h, h, -h}, new float[]{h, h, h});     // +X
        face(b, new float[]{-h, -h, -h}, new float[]{-h, -h, h}, new float[]{-h, h, h}, new float[]{-h, h, -h}); // -X
        face(b, new float[]{-h, h, h}, new float[]{h, h, h}, new float[]{h, h, -h}, new float[]{-h, h, -h});     // +Y
        face(b, new float[]{-h, -h, -h}, new float[]{h, -h, -h}, new float[]{h, -h, h}, new float[]{-h, -h, h}); // -Y
        face(b, new float[]{-h, -h, h}, new float[]{h, -h, h}, new float[]{h, h, h}, new float[]{-h, h, h});     // +Z
        face(b, new float[]{h, -h, -h}, new float[]{-h, -h, -h}, new float[]{-h, h, -h}, new float[]{h, h, -h}); // -Z
        return b.build();
    }

    private static void face(Builder b, float[] bl, float[] br, float[] tr, float[] tl) {
        float[] a = vtx(bl[0], bl[1], bl[2], 0f, 0f);
        float[] bb = vtx(br[0], br[1], br[2], 1f, 0f);
        float[] c = vtx(tr[0], tr[1], tr[2], 1f, 1f);
        float[] d = vtx(tl[0], tl[1], tl[2], 0f, 1f);
        b.tri(a, bb, c);
        b.tri(a, c, d);
    }

    // ----------------------------------------------------------------- Plane
    /** Bidang lantai di y = 0, normal +Y. {@code tiles} = jumlah pengulangan tekstur. */
    public static Mesh plane(float halfSize, float tiles) {
        Builder b = new Builder();
        float s = halfSize;
        float[] a = vtx(-s, 0, -s, 0f, 0f);
        float[] bb = vtx(-s, 0, s, 0f, tiles);
        float[] c = vtx(s, 0, s, tiles, tiles);
        float[] d = vtx(s, 0, -s, tiles, 0f);
        b.tri(a, bb, c);
        b.tri(a, c, d);
        return b.build();
    }
}
