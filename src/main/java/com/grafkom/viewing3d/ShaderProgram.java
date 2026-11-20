package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL33.*;

public class ShaderProgram {
    private final int programId;

    public ShaderProgram(String vertResourcePath, String fragResourcePath) {
        int vertId = compileShader(loadResource(vertResourcePath), GL_VERTEX_SHADER);
        int fragId = compileShader(loadResource(fragResourcePath), GL_FRAGMENT_SHADER);

        programId = glCreateProgram();
        glAttachShader(programId, vertId);
        glAttachShader(programId, fragId);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader Link Error: " + glGetProgramInfoLog(programId));
        }
        glDeleteShader(vertId);
        glDeleteShader(fragId);
    }

    private String loadResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("File shader tidak ditemukan: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Gagal membaca file shader: " + path, e);
        }
    }

    private int compileShader(String source, int type) {
        int id = glCreateShader(type);
        glShaderSource(id, source);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader Compile Error: " + glGetShaderInfoLog(id));
        }
        return id;
    }

    public void use() { glUseProgram(programId); }

    public void setInt(String name, int val) {
        glUniform1i(glGetUniformLocation(programId, name), val);
    }

    public void setBool(String name, boolean val) {
        glUniform1i(glGetUniformLocation(programId, name), val ? 1 : 0);
    }

    public void setVec3(String name, Vector3f v) {
        glUniform3f(glGetUniformLocation(programId, name), v.x, v.y, v.z);
    }

    public void setMat4(String name, Matrix4f mat) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(glGetUniformLocation(programId, name), false, mat.get(stack.mallocFloat(16)));
        }
    }
}
