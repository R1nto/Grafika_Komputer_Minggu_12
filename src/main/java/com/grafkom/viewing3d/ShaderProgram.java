package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

/** Membungkus kompilasi, linking, dan pengiriman uniform GLSL. */
public class ShaderProgram {
    private final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();

    public ShaderProgram(String vertexResource, String fragmentResource) {
        int vs = compile(GL_VERTEX_SHADER, readResource(vertexResource));
        int fs = compile(GL_FRAGMENT_SHADER, readResource(fragmentResource));

        programId = glCreateProgram();
        glAttachShader(programId, vs);
        glAttachShader(programId, fs);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Link shader gagal: " + glGetProgramInfoLog(programId, 4096));
        }
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private static String readResource(String path) {
        try (InputStream in = ShaderProgram.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Resource shader tidak ditemukan: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca shader: " + path, e);
        }
    }

    private static int compile(int type, String source) {
        int id = glCreateShader(type);
        glShaderSource(id, source);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Compile shader gagal: " + glGetShaderInfoLog(id, 4096));
        }
        return id;
    }

    public void use() {
        glUseProgram(programId);
    }

    private int loc(String name) {
        return uniformCache.computeIfAbsent(name, n -> glGetUniformLocation(programId, n));
    }

    public void setInt(String name, int value) {
        glUniform1i(loc(name), value);
    }

    public void setBool(String name, boolean value) {
        glUniform1i(loc(name), value ? 1 : 0);
    }

    public void setFloat(String name, float value) {
        glUniform1f(loc(name), value);
    }

    public void setVec3(String name, Vector3f v) {
        glUniform3f(loc(name), v.x, v.y, v.z);
    }

    public void setMat4(String name, Matrix4f m) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            m.get(fb);
            glUniformMatrix4fv(loc(name), false, fb);
        }
    }

    public void cleanup() {
        glDeleteProgram(programId);
    }
}
