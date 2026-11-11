package com.grafkom.viewing3d;

public class Main {
    public static void main(String[] args) {
        // Argumen opsional: path gambar tekstur (PNG/JPG). Default: "texture.png" di folder kerja.
        String texturePath = args.length > 0 ? args[0] : "texture.png";
        new VisualRealismApp().run(texturePath);
    }
}
