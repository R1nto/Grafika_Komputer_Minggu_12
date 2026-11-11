package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;

/** Pemuat tekstur 2D (STBImage) dengan fallback tekstur bata prosedural. */
public class Texture2D {
    private final int textureID;

    private Texture2D() {
        textureID = glGenTextures();
    }

    /**
     * Urutan pencarian: (1) file di disk, (2) resource classpath "/textures/texture.png",
     * (3) tekstur bata prosedural bila keduanya tidak ada.
     */
    public static Texture2D loadOrProcedural(String filePath) {
        stbi_set_flip_vertically_on_load(true); // v = 0 di bawah gambar, sesuai UV pada MeshFactory

        if (filePath != null && new File(filePath).isFile()) {
            Texture2D t = fromFile(filePath);
            if (t != null) return t;
        }
        Texture2D t = fromClasspath("/textures/texture.png");
        if (t != null) return t;

        System.out.println("Tekstur gambar tidak ditemukan, memakai tekstur bata prosedural.");
        return procedural();
    }

    private static Texture2D fromFile(String filePath) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer c = stack.mallocInt(1);
            ByteBuffer data = stbi_load(filePath, w, h, c, 4);
            if (data == null) {
                System.err.println("Failed to load texture: " + filePath + " (" + stbi_failure_reason() + ")");
                return null;
            }
            Texture2D tex = new Texture2D();
            tex.upload(data, w.get(0), h.get(0));
            stbi_image_free(data);
            return tex;
        }
    }

    private static Texture2D fromClasspath(String resource) {
        try (InputStream in = Texture2D.class.getResourceAsStream(resource)) {
            if (in == null) return null;
            byte[] bytes = in.readAllBytes();
            ByteBuffer raw = MemoryUtil.memAlloc(bytes.length);
            raw.put(bytes).flip();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer c = stack.mallocInt(1);
                ByteBuffer data = stbi_load_from_memory(raw, w, h, c, 4);
                if (data == null) {
                    System.err.println("Failed to decode texture resource: " + stbi_failure_reason());
                    return null;
                }
                Texture2D tex = new Texture2D();
                tex.upload(data, w.get(0), h.get(0));
                stbi_image_free(data);
                return tex;
            } finally {
                MemoryUtil.memFree(raw);
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static Texture2D procedural() {
        final int size = 256, brickW = 64, brickH = 32, mortar = 3;
        ByteBuffer buf = MemoryUtil.memAlloc(size * size * 4);
        for (int y = 0; y < size; y++) {
            int row = y / brickH;
            int offset = (row % 2 == 0) ? 0 : brickW / 2;
            for (int x = 0; x < size; x++) {
                int col = (x + offset) / brickW;
                int inX = (x + offset) % brickW;
                int inY = y % brickH;
                int r, g, b;
                if (inX < mortar || inY < mortar) {
                    r = 200; g = 196; b = 186; // mortar
                } else {
                    int hash = (col * 73856093) ^ (row * 19349663);
                    float shade = 0.80f + 0.30f * (((hash >>> 8) & 0xFF) / 255f);
                    int grain = ((x * 31 + y * 17) % 7) * 2 - 6;
                    r = clamp((int) (170 * shade) + grain);
                    g = clamp((int) (70 * shade) + grain);
                    b = clamp((int) (50 * shade) + grain);
                }
                buf.put((byte) r).put((byte) g).put((byte) b).put((byte) 255);
            }
        }
        buf.flip();
        Texture2D tex = new Texture2D();
        tex.upload(buf, size, size);
        MemoryUtil.memFree(buf);
        return tex;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private void upload(ByteBuffer data, int width, int height) {
        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
        glGenerateMipmap(GL_TEXTURE_2D);
        setFilterMode(true);
    }

    /**
     * true  = Bilinear + Mipmap (GL_LINEAR_MIPMAP_LINEAR / GL_LINEAR)
     * false = Nearest Neighbor (GL_NEAREST)
     */
    public void setFilterMode(boolean bilinearMipmapped) {
        glBindTexture(GL_TEXTURE_2D, textureID);
        if (bilinearMipmapped) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void cleanup() {
        glDeleteTextures(textureID);
    }
}
