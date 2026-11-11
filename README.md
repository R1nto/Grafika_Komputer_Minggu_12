# 3D Visual Realism Suite (Grafika Komputer, Minggu 11)

Java 17+, LWJGL 3.3.4, JOML 1.10.8, OpenGL 3.3 core (GLSL 330).

## Struktur project (salin ke IntelliJ, package `com.grafkom.viewing3d`)

```
pom.xml
src/main/java/com/grafkom/viewing3d/
    Main.java  VisualRealismApp.java  ShaderProgram.java  Mesh.java  MeshFactory.java
    Camera.java  HSRManager.java  Texture2D.java  ShadowMapFBO.java
src/main/resources/shaders/
    phong.vert  phong.frag  depth.vert  depth.frag
src/main/resources/textures/texture.png   (opsional, taruh gambar sendiri di sini)
```

## Menjalankan

- IntelliJ: Open `pom.xml` sebagai project, tunggu Maven import, jalankan `Main`.
  Di macOS tambahkan VM option `-XstartOnFirstThread`.
- Terminal (Windows/Linux): `mvn compile exec:java -Dexec.mainClass=com.grafkom.viewing3d.Main`
- Tekstur: urutan pencarian (1) argumen program / `texture.png` di folder kerja, (2) `src/main/resources/textures/texture.png`, (3) tekstur bata prosedural.

## Kontrol

| Tombol | Fungsi | Indikator |
|---|---|---|
| B | Phong / Blinn-Phong | 1 |
| F | Flat / Smooth shading | 2 |
| Z | Z-Buffer on/off | 3 |
| C | Back-Face Culling on/off | 3 |
| T | Tekstur on/off | 4 |
| N | Filter Nearest / Bilinear+Mipmap | 4 |
| S | Shadow mapping on/off | 5 |
| L | Cahaya berputar (lihat bayangan bergerak) | 5 |
| V | V-Sync on/off (matikan untuk ukur FPS) | laporan |
| Spasi / R / Esc | Jeda animasi / reset kamera / keluar | - |
| Mouse kiri drag, scroll | Putar kamera, zoom | - |

Status fitur dan FPS tampil di judul window.

## Tips pengujian (untuk screenshot laporan)

- Z-Buffer OFF: matikan juga Culling (C). Pada objek cembung dengan culling aktif, hasilnya masih tampak benar. Dengan keduanya OFF, sisi belakang torus/bola menimpa sisi depan.
- Flat vs Smooth: bola berfaset jelas pada mode Flat; kubus pada mode Smooth tampak "membulat" karena normal sudut dirata-rata (ini memang perilaku averaging).
- Nearest vs Mipmap: lihat lantai dari sudut rendah, ke arah horizon. Nearest menampilkan aliasing/moire.
- Shadow: tekan L agar cahaya berputar, perhatikan tidak ada acne (garis zebra) maupun peter panning.
