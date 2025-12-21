package com.finpro7.oop;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class Main extends Game {

    // skin static buat bisa diakses langsung dari class lain (MenuScreen/GameScreen)
    public static Skin skin;
    public AssetManager assets; // load asset manager di awal

    // palet warna tema game
    private final Color COLOR_GOLD = new Color(1f, 0.84f, 0.0f, 1f);
    private final Color COLOR_BLOOD = new Color(0.9f, 0.1f, 0.1f, 1f);

    @Override
    public void create() {
        // generate style UI secara prosedural sebelum masuk ke game
        createStyle();
        loadAssets();
        // langsung transisi ke MenuScreen buat tampilan awal
        this.setScreen(new MenuScreen(this));
    }

    @Override
    public void render() {
        // super.render() buat manggil method render() milik screen yg lagi aktif
        super.render();
    }

    @Override
    public void dispose() {
        // bersihin resource skin global pas game ditutup
        if (skin != null) skin.dispose();
    }

    // buat Skin UI yg programatis gk pake file .json eksternal
    private void createStyle() {
        skin = new Skin();
        // setup BitmapFont
        // scale up font default biar tajam dan kabaca jelas
        BitmapFont fontTitle = new BitmapFont();
        fontTitle.getData().setScale(5.0f);
        skin.add("font-title", fontTitle);
        skin.add("default-font", fontTitle); // mapping default di beberapa widget dialog
        BitmapFont fontButton = new BitmapFont();
        fontButton.getData().setScale(1.5f);
        skin.add("font-button", fontButton);
        BitmapFont fontSmall = new BitmapFont();
        fontSmall.getData().setScale(1.0f);
        skin.add("font-small", fontSmall);
        // buat tekstur putih 1x1 pixel secara runtime buat bikin warna solid tanpa perlu muat file gambar
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        Texture whiteTex = new Texture(p);
        skin.add("white", whiteTex);
        p.dispose(); // pixmap udh gk dibutuhin pas udh jadi Texture
        // buat Drawable dasar sama variasi overlay gelap
        TextureRegionDrawable whiteDrawable = new TextureRegionDrawable(new TextureRegion(whiteTex));
        Drawable dimDrawable = whiteDrawable.tint(new Color(0f, 0f, 0f, 0.85f));
        skin.add("dim-overlay", dimDrawable);
        // definisiin style Label
        Label.LabelStyle titleStyle = new Label.LabelStyle(fontTitle, COLOR_GOLD);
        skin.add("title", titleStyle);
        Label.LabelStyle shadowStyle = new Label.LabelStyle(fontTitle, Color.BLACK);
        skin.add("shadow", shadowStyle);
        Label.LabelStyle subStyle = new Label.LabelStyle(fontButton, COLOR_BLOOD);
        skin.add("subtitle", subStyle);
        Label.LabelStyle textStyle = new Label.LabelStyle(fontSmall, Color.GRAY);
        skin.add("text", textStyle);
        // style default fallback
        skin.add("default", new Label.LabelStyle(fontButton, Color.WHITE));
        // definisi style TextButton
        // Kita gunakan tinting pada drawable putih untuk efek hover dan click
        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.font = fontButton;
        btnStyle.up = whiteDrawable.tint(new Color(0.1f, 0.1f, 0.1f, 0.6f));   // State normal
        btnStyle.over = whiteDrawable.tint(new Color(0.5f, 0f, 0f, 0.8f));      // State hover (mouse di atas)
        btnStyle.down = whiteDrawable.tint(new Color(0.8f, 0.7f, 0f, 1f));      // State klik
        btnStyle.fontColor = Color.WHITE;
        btnStyle.overFontColor = COLOR_GOLD;
        skin.add("btn-main", btnStyle);
        skin.add("default", btnStyle);
        // definisiin style window buat dialog pas pause
        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.background = dimDrawable;
        windowStyle.titleFont = fontTitle;
        windowStyle.titleFontColor = COLOR_GOLD;
        skin.add("default", windowStyle);
    }

    private void loadAssets(){
        assets = new AssetManager(); // setup asset manager, buat model model 3d
        // load file model dari folder assets/models/
        assets.load("models/pohon.g3dj", Model.class);
        assets.load("textures/batang_pohon.png", Texture.class);
        assets.load("textures/daun_pohon.png", Texture.class);
        assets.load("models/dajjal.g3db", Model.class);
        assets.load("models/majuj/majuj.g3db", Model.class);
        assets.load("models/yajuj/yajuj.g3db", Model.class);
        // load texture model
        assets.load("models/dajjal_diffuse.png", Texture.class);
        assets.load("models/dajjal_glow.png", Texture.class);
        assets.load("models/majuj/majuj1.png", Texture.class);
        assets.load("models/majuj/majuj2.png", Texture.class);
        assets.load("models/yajuj/yajuj1.png", Texture.class);
        assets.load("models/yajuj/yajuj2.png", Texture.class);
        assets.load("models/yajuj/yajuj3.png", Texture.class);
        assets.load("models/yajuj/yajuj4.png", Texture.class);
        assets.finishLoading(); // ngeloading dulu biar simpel codingannyaa
    }
}
