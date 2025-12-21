package com.finpro7.oop.entities;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class Coin {
    public ModelInstance instance;
    public Vector3 position = new Vector3();

    private float originalY; // Nyimpen tinggi aslinya biar gak terbang kejauhan
    private float angle = 0f;
    private float time = 0f; // Penanda waktu buat animasi naik turun

    public Coin(Model model, float x, float y, float z) {
        this.instance = new ModelInstance(model);
        this.position.set(x, y, z);
        this.originalY = y;
    }

    public void update(float delta) {
        time += delta;

        // 1. Puteran (makin gede angkanya makin ngebut)
        angle += 150f * delta;

        // 2. Efek Naik Turun (Floating/Bobbing)
        // Pake Sinus biar gerakannya halus naik turun kyak napas
        // 3f = kecepatan naik turun, 0.25f = jarak naik turunnya
        float bobOffset = MathUtils.sin(time * 3f) * 0.25f;

        // 3. LOGIKA MATRIKS: RESET -> PINDAH+BOB -> PUTAR -> SCALE
        this.instance.transform.idt();

        // Pindah posisi + efek naik turun (pake originalY biar patokannya tetep)
        this.instance.transform.translate(position.x, originalY + bobOffset, position.z);

        // Putar sumbu Y (Spinning)
        this.instance.transform.rotate(Vector3.Y, angle);

        // Putar sumbu Z 90 derajat (Biar berdiri)
        this.instance.transform.rotate(Vector3.Z, 90f);

        // KECILIN DI SINI
        // 0.7f artinya ukurannya 70% dari asli. Ubah aja kalo masih kegedean.
        this.instance.transform.scale(0.7f, 0.7f, 0.7f);
    }
}
