package com.finpro7.oop.managers;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.utils.Disposable;

public class ResourceManager implements Disposable {

    // ini singletonnya, variabel statis buat nyimpen diri sendiri
    private static ResourceManager instance;

    // ini objek assetmanager asli punya libgdx yang kita amankan
    public AssetManager assets;

    // konstruktornya harus private biar gak ada yang bisa bikin baru sembarangan pake 'new'
    private ResourceManager() {
        assets = new AssetManager();
    }

    // ini pintu akses satu satunya (global access point)
    public static ResourceManager getInstance() {
        // kalo belum ada, kita bikin baru
        if (instance == null) {
            instance = new ResourceManager();
        }
        // kalo udah ada, balikin yang lama
        return instance;
    }

    // method bantuan buat loading biar kodingan di main lebih bersih
    public void loadAllAssets() {
        // load models
        assets.load("models/pohon.g3dj", Model.class);
        assets.load("models/dajjal.g3db", Model.class);
        assets.load("models/majuj/majuj.g3db", Model.class);
        assets.load("models/yajuj/yajuj.g3db", Model.class);
        assets.load("models/medkit.g3db", Model.class);
        assets.load("models/ammo.g3db", Model.class);
        assets.load("models/weapons.g3db", Model.class);

        // load textures
        assets.load("textures/batang_pohon.png", Texture.class);
        assets.load("textures/daun_pohon.png", Texture.class);
        assets.load("models/dajjal_diffuse.png", Texture.class);
        assets.load("models/dajjal_glow.png", Texture.class);
        assets.load("models/majuj/majuj1.png", Texture.class);
        assets.load("models/majuj/majuj2.png", Texture.class);
        assets.load("models/yajuj/yajuj1.png", Texture.class);
        assets.load("models/yajuj/yajuj2.png", Texture.class);
        assets.load("models/yajuj/yajuj3.png", Texture.class);
        assets.load("models/yajuj/yajuj4.png", Texture.class);
        assets.load("models/ammo.png", Texture.class);
        assets.load("models/bullet1.png", Texture.class);
        assets.load("models/bullet2.png", Texture.class);
        assets.load("textures/crosshair.png", Texture.class);
    }

    // bersih bersih memori pas game ditutup
    @Override
    public void dispose() {
        assets.dispose();
        instance = null;
    }
}
