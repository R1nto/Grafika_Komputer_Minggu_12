# ThreeDViewing (Grafika Komputer, Minggu 11 & 12)

Struktur dan nama file sesuai modul praktikum. Java 17+, LWJGL 3.3.3, JOML 1.10.5, GLSL 330.

## Menjalankan
- Buka folder `ThreeDViewing` (yang berisi `pom.xml`) sebagai project Maven, tunggu import selesai.
- Minggu 11: jalankan `Week11ShadingApp`. Minggu 12: jalankan `VisualRealismApp`.
- Working directory harus folder project agar `textures/brick.png` terbaca.
  Ganti `textures/brick.png` dengan tekstur kustom Anda (tugas Minggu 12), nama file tetap `brick.png`
  atau ubah path di `VisualRealismApp.init()`.

## Kontrol
Panah = putar kamera | B = Phong/Blinn-Phong | F = Flat/Smooth | Z = Z-Buffer | C = Culling
T = Nearest/Mipmap | M = tekstur on/off | L = ganti posisi cahaya (VisualRealismApp)

## Perbedaan dari kode di modul (semua di Mesh3D dan VisualRealismApp)
1. Bola: urutan vertex segitiga dibalik menjadi CCW dari luar. Di modul urutannya CW, sehingga
   Back-Face Culling membuang sisi luar bola dan flat normal mengarah ke dalam.
2. Lantai: smooth normal dibuat (0,1,0). Di modul dipakai posisi ternormalisasi, sehingga lantai gelap
   dan bayangan hampir tidak terlihat.
3. Pass 1 shadow: depth test selalu ON dan culling OFF, tidak ikut tombol Z/C.
4. UV bola: v dibalik agar gambar tidak terbalik. Segitiga degenerate di kutub dilewati (hindari NaN).
5. Tambahan: tombol L (preset posisi cahaya), pemeriksaan kelengkapan FBO, dan texture/brick.png bawaan.
