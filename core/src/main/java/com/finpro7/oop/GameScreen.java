package com.finpro7.oop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.finpro7.oop.entities.BaseEnemy;
import com.finpro7.oop.entities.EnemyFactory;
import com.finpro7.oop.logics.WaveManager;
import com.finpro7.oop.world.Terrain;

public class GameScreen implements Screen {

    final Main game;
    private Stage stage;

    // buat sistem 3Dnya
    private PerspectiveCamera cam;
    private Environment env;
    private RenderContext renderContext;
    private ModelBatch modelBatch;

    // buat asset sama world
    private Terrain terrain;
    private PerlinNoise perlin;
    private Model treeModel;
    private Array<ModelInstance> treeInstances = new Array<>();

    // sistem dajjalnya
    private Model dajjalModel; // buat nampung data model 3D nya dari file g3db
    private DajjalEntity dajjal; // entity utamanya yg ngatur logika gerak sama animasinya

    // buat ngontrol pemain
    private float yawDeg, pitchDeg;
    private float mouseSens = 0.14f;
    private float moveSpeed = 10f;
    private float sprintMul = 2.0f;
    private float eyeHeight = 2.0f;
    private float margin = 1.5f;

    // buat konsep fisika, gravitasi, gaya ledak lompat dan cek apa napak tanah
    private float verticalVelocity = 0f;
    private float gravity = 30f;
    private float jumpForce = 15f;
    private boolean isGrounded = false;

    // buat sistem kabutnya
    private Model fogModel;
    private Array<ModelInstance> fogInstances = new Array<>();
    private float fogSpeed = 2.0f;
    private int fogCount = 200;

    // sistem UI & Statenya
    private boolean isPaused = false;
    private Table pauseContainer;
    private Image overlay;

    // variabel buat data UI (Dummy Stats)
    private float missionTimer = 0f;
    private Label coordLabel;
    private Label timeLabel;

    // variabel bantu biar ga new Vector3 terus terusan buat hemat memori
    private final Vector3 tempPos = new Vector3();

    // bagian spawn yajuj majuj
    private Array<BaseEnemy> activeEnemies = new Array<>();
    private EnemyFactory enemyFactory;
    private WaveManager waveManager;

    // komponen buat UI nya pas mulai
    private Label stageLabel; // info stage di pojok
    private Label enemyCountLabel;// info sisa musuh di stage itu
    private Label notificationLabel; // teks gede di tengah
    private Table hudTable; // var table class global
    private float warningCooldown = 0f; // buat cooldown biar notif gak spamming tiap frame

    public GameScreen(final Main game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        setup3DWorld();
        setupPauseInterface();
        // load yajuj majuj
        Model yajujM = game.assets.get("models/yajuj/yajuj.g3db", Model.class);
        Model majujM = game.assets.get("models/majuj/majuj.g3db", Model.class);
        enemyFactory = new EnemyFactory(yajujM, majujM);
        // inisialisasi wave manager yg ngatur stage stagenya
        waveManager = new WaveManager();
        waveManager.initLevelData(terrain);
        setupHUD(); // buat setup HUD teksboard pas mulai
        // biar pas game mulai, cursor langsung masuk ke games
        Gdx.input.setCursorCatched(true);
        Gdx.input.setInputProcessor(null);
    }

    private void setup3DWorld(){
        // setup kamera perspektif biar kyak mata manusia ada jauh dekatnya
        cam = new PerspectiveCamera(67f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 0.1f; // jarak pandang terdekat
        cam.far = 200f; // jarak pandang terjauh, dipendekin jadi 100 buat efek kabut
        // buat setup pencahayaan biar gak gelap gulita
        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f)); // cahaya dasar
        env.add(new DirectionalLight().set(1f, 1f, 1f, -0.6f, -1f, -0.3f)); // matahari
        env.set(new ColorAttribute(ColorAttribute.Fog, 0.08f, 0.1f, 0.14f, 1f)); // tambahin atribut kabut ke env
        renderContext = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.LRU, 1));
        modelBatch = new ModelBatch(); // inisialisasi ModelBatch buat model
        // buat setup generator perlin noisenya biar gunungnya random tiap kali playy
        perlin = new PerlinNoise();
//        perlin.terrainHeight = 6f;
        perlin.amplitude = 80f; // tinggi maksimal gunung
        perlin.frequencyX = 0.08f;
        perlin.frequencyZ = 0.08f;
        perlin.offsetX = MathUtils.random(0f, 999f); // geser seed random
        perlin.offsetZ = MathUtils.random(0f, 999f);
        // ambil model buat pohon, dllnya
        treeModel = game.assets.get("models/pohon.g3dj", Model.class);
        dajjalModel = game.assets.get("models/dajjal.g3db", Model.class);
        terrain = new Terrain(env, perlin, 254, 254, 320f, 320f);
        createFogSystem(terrain);
        // biar daunnya transparan & keliatan dari dua sisi
        for(Material mat : treeModel.materials){
            // cek material daun index 1
            if(treeModel.materials.indexOf(mat, true) == 1){
                mat.set(
                    new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
                    FloatAttribute.createAlphaTest(0.25f),
                    IntAttribute.createCullFace(GL20.GL_NONE)
                );
            }
        }
        terrain.generateTrees(treeModel, treeInstances, 600); // generate pohon pohonnya di terrain sebanyak 600 secara random
        // buat ngatur spawn playernya
        Vector3 startPos = new Vector3();
        terrain.getRoadStartPos(startPos); // minta koordinat start, biar di awal jalan spiral startnya
        cam.position.set(startPos.x + 5.0f, startPos.y + eyeHeight, startPos.z + 5.0f); // buat set posisi kamera
        float spawnX = startPos.x + 15.0f;
        float spawnZ = startPos.z + 15.0f;
        float spawnY = terrain.getHeight(spawnX, spawnZ); // tinggi dari tinggi terrain di titik  x z itu
        // manggil class DajjalEntity
//        dajjal = new DajjalEntity(dajjalModel, spawnX, spawnY, spawnZ);
        Vector3 lookTarget = new Vector3();
        // Panggil methodnya aja, jangan itung manual di sini!
        terrain.getRoadLookAtPos(lookTarget);
        cam.direction.set(lookTarget.sub(cam.position)).nor();
        cam.up.set(Vector3.Y);
        cam.update();
    }

    private void setupPauseInterface(){
        // bikin layar item transparan buat background pas lagi pause biar agak gelap dikit
        overlay = new Image(Main.skin.newDrawable("white", new Color(0.05f, 0.05f, 0.08f, 0.85f)));
        overlay.setFillParent(true);
        overlay.setVisible(false);
        stage.addActor(overlay);
        // wadah utamanya pake Table buat nampung semua elemen menu pausenya
        pauseContainer = new Table();
        pauseContainer.setFillParent(true);
        pauseContainer.setVisible(false);
        // bagian kiri table, bikin table buat nampilin info stats kyak lokasi sama timer di kiri
        Table infoTable = new Table();
        infoTable.setBackground(Main.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.5f)));
        infoTable.pad(30);
        Label sysTitle = new Label("SYSTEM PAUSED", Main.skin, "title");
        sysTitle.setFontScale(0.8f);
        sysTitle.setColor(Color.ORANGE);
        Label missionText = new Label("CURRENT OP: RECON", Main.skin, "text");
        // label yg isinya bakal berubah ubah terus sesuai data ingame
        timeLabel = new Label("T+ 00:00:00", Main.skin, "subtitle");
        coordLabel = new Label("LOC: 000, 000", Main.skin, "text");
        coordLabel.setColor(Color.LIGHT_GRAY);
        infoTable.add(sysTitle).left().padBottom(10).row();
        infoTable.add(missionText).left().padBottom(30).row();
        infoTable.add(timeLabel).left().padBottom(5).row();
        infoTable.add(coordLabel).left().expandY().top();
        // bagian kana table buat nampung tombol tombol navigasinya di kanan
        Table buttonTable = new Table();
        TextButton btnResume = createStyledButton("RESUME MISSION", () -> togglePause());
        TextButton btnRestart = createStyledButton("RESTART SECTOR", () -> game.setScreen(new GameScreen(game)));
        TextButton btnExit = createStyledButton("ABORT OPERATION", () -> {
            game.setScreen(new MenuScreen(game));
            dispose();
        });
        buttonTable.add(btnResume).width(300).height(60).padBottom(15).right().row();
        buttonTable.add(btnRestart).width(300).height(60).padBottom(15).right().row();
        buttonTable.add(btnExit).width(300).height(60).right().row();
        // nyusun layout kiri sama kanan biar rapih posisinya
        pauseContainer.add(infoTable).width(400).expandY().fillY().left();
        pauseContainer.add(buttonTable).expand().bottom().right().pad(50);
        stage.addActor(pauseContainer);
    }

    private TextButton createStyledButton(String text, Runnable action){
        TextButton btn = new TextButton(text, Main.skin, "btn-main");
        btn.setTransform(true);
        btn.setOrigin(Align.right);// titik pusatnya taruh dikanan jadi pas ngescale efeknya dia mundur ke kiri
        btn.addListener(new ClickListener(){

            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor){
                // pas kursor nempel (hover) tombolnya digedein dikit trus geser ke kiri biar kyak kepilih gitu
                if(pointer == -1){
                    btn.addAction(Actions.parallel(
                        Actions.scaleTo(1.05f, 1.05f, 0.1f),
                        Actions.moveBy(-10f, 0f, 0.1f)
                    ));
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor){
                // nah ini pas kursornya pergi, balikin lagi ukuran sama posisi tombolnya ke awal
                if(pointer == -1){
                    btn.addAction(Actions.parallel(
                        Actions.scaleTo(1f, 1f, 0.1f),
                        Actions.moveBy(10f, 0f, 0.1f)
                    ));
                }
            }
        });
        return btn;
    }

    public void togglePause() {
        isPaused = !isPaused; // switch statusnya, on jadi off, off jadi on
        // ngatur visibilitas HUD, kalo lagi pause ya diumpetin biar bersih
        if(hudTable != null) hudTable.setVisible(!isPaused);
        if(isPaused){
            Gdx.input.setCursorCatched(false);
            Gdx.input.setInputProcessor(stage);
            // pas lagi pause lepasin kursor mouse biar bisa ngeklik menu
            overlay.setVisible(true);
            overlay.getColor().a = 0f;
            overlay.addAction(Actions.fadeIn(0.2f));
            // animasi menu utamanya geser dari bawah ke tengah (slide in)
            pauseContainer.setVisible(true);
            pauseContainer.setPosition(0, -50); // start posisi agak di bawah layar
            pauseContainer.getColor().a = 0f;
            // jalanin animasi muncul barengan sama gerak ke atas
            pauseContainer.addAction(Actions.parallel(
                Actions.fadeIn(0.25f, Interpolation.fade),
                Actions.moveTo(0, 0, 0.4f, Interpolation.circleOut)
            ));
        }else{
            // lalo balik main (unpause)
            Gdx.input.setCursorCatched(true); // tangkep lagi kursornya
            Gdx.input.setInputProcessor(null); // balikin kontrol ke game world
            overlay.setVisible(false);
            pauseContainer.setVisible(false); // umpetin semua menu
        }
    }

    private void updateUI(){
        // kalo gak lagi pause ngapain update UI ini, skip aja
        if(!isPaused) return;
        // ngupdate format waktu jadi Menit:Detik biar enak diliat
        int minutes = (int)missionTimer / 60;
        int seconds = (int)missionTimer % 60;
        timeLabel.setText(String.format("T+ %02d:%02d", minutes, seconds));
        // ngupdate info koordinat XYZ player sekarang
        coordLabel.setText(String.format("LOC: %d, %d, %d",
            (int)cam.position.x, (int)cam.position.y, (int)cam.position.z));
    }

    private void setupHUD(){
        // 1. Setup Table buat HUD Pojok Kiri Atas
//        Table hudTable = new Table();
        hudTable = new Table();
        hudTable.top().left(); // tempel di pojok kiri atas
        hudTable.setFillParent(true);
        hudTable.pad(20); // kasig jarak dikit dari pinggir layar biar gk mepet banget
        // label buat info Stage
        stageLabel = new Label("STAGE 1", Main.skin, "title");
        stageLabel.setFontScale(1.4f); // ukurannya pas in aja
        stageLabel.setColor(Color.GOLD); // warna emas
        // label buat info sisa musuh
        enemyCountLabel = new Label("HOSTILES LEFT: 0", Main.skin, "subtitle");
        enemyCountLabel.setColor(Color.WHITE);
        hudTable.add(stageLabel).left().row();
        hudTable.add(enemyCountLabel).left().padTop(5);
        stage.addActor(hudTable);
        // setup label Notifikasi, defaultnya transparan (alpha 0) biar gak ngerusak pemandangan pas awal
        notificationLabel = new Label("", Main.skin, "title");
        notificationLabel.setFontScale(1.5f); // fontnya digedein banget
        notificationLabel.setColor(Color.RED); // merah biar kerasa danger
        notificationLabel.setAlignment(Align.center);
        notificationLabel.setPosition(0, Gdx.graphics.getHeight() / 2f + 50f); // posisi tengah agak ke atas dikit
        notificationLabel.setWidth(Gdx.graphics.getWidth());
        notificationLabel.getColor().a = 0f; // set transparan dulu
        stage.addActor(notificationLabel);
    }

    // method helper buat nampilin notif ganti stage di tengah layar
    private void showStageNotification(int stageNum){
        notificationLabel.setText("ENTERING STAGE " + stageNum);
        notificationLabel.getColor().a = 0f;
        // reset dulu action sebelumnya biar animasinya gak tabrakan/numpuk
        notificationLabel.clearActions();
        // urutan animasinya: muncul pelan -> tahan 2 detik biar kebaca -> ilang pelan pelan
        notificationLabel.addAction(Actions.sequence(
            Actions.fadeIn(0.5f),
            Actions.delay(2.0f),
            Actions.fadeOut(1.0f)
        ));
    }

    @Override
    public void render(float delta) {
        // tombol darurat kalo pencet ESC langsung pause
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) togglePause();
        if(!isPaused){ // kalo game gk pause
            updateMouseLook();
            // kurangi timer cooldown warning notif
            if(warningCooldown > 0) warningCooldown -= delta;
            updateMovement(delta);
            clampAndStickToTerrain(delta); // biar player tetep nempel tanah
            updateFog(delta);
            missionTimer += delta;
            // kalo boss dajjalnya udah ada, suruh dia mikir (update logic) sama ngejar kita
            if(dajjal != null) dajjal.update(delta, cam.position, terrain, treeInstances);
            // urusan spawn musuh kita oper terrain, factory, sama list activeEnemies biar dia bisa nambahin musuh ke situ
            waveManager.update(delta, cam.position, terrain, enemyFactory, activeEnemies);
            // cek ganti stage apa engga
            if(waveManager.justChangedStage){
                showStageNotification(waveManager.getCurrentStageNum()); // tampilin teks gede
                waveManager.justChangedStage = false; // reset flagnya
            }
            // update teks hudnya
            stageLabel.setText("STAGE " + waveManager.getCurrentStageNum());
            int sisa = waveManager.getRemainingEnemies();
            enemyCountLabel.setText("HOSTILES LEFT: " + sisa);
            // kalo musuh tinggal 3 biji atau kurang, warnanya jadi merah biar tegang
            if(sisa <= 3) enemyCountLabel.setColor(Color.RED);
            else enemyCountLabel.setColor(Color.WHITE);
            // pake loop biasa biar aman dari concurrent modification
            for(int i = 0; i < activeEnemies.size; i++){
                BaseEnemy enemy = activeEnemies.get(i);
                // si yajuj majuj bisa liat temen temennya yg lain biar gak tabrakan
                enemy.update(delta, cam.position, terrain, treeInstances, activeEnemies);
                // logika skor kill
                if(enemy.isDead){
                    if(!enemy.countedAsDead){
                        waveManager.reportEnemyDeath(); // lapor ke manager kalo ada yg mati
                        enemy.countedAsDead = true; // tandain biar gak dihitung mati berkali kali
                    }
                    // ngapus dari list kalo animasi mati udah kelar
                    // if (animasiMatiSelesai) activeEnemies.removeIndex(i);
                }
            }
        }else updateUI(); // pas lagi pause, cukup update angka angka di menu aja
        cam.update();
        // bersihin layar, reset canvasnya
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.08f, 0.1f, 0.14f, 1f); // warna background biru gelap malem
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        // mulai gambar terrain dulu
        renderContext.begin();
        terrain.render(cam, renderContext);
        renderContext.end();
        // mulai batch rendering buat model 3D
        modelBatch.begin(cam);
        for(ModelInstance tree : treeInstances) modelBatch.render(tree, env);
        // di sini cuma render yajuj majuj
        for(BaseEnemy enemy : activeEnemies) modelBatch.render(enemy.modelInstance, env);
        // gambar dajjal kalo dia ada
        if(dajjal != null) modelBatch.render(dajjal.badanDajjal, env);
        // suruh GPU selesaikan gambar pohon, musuh, & dajjal dulu
        modelBatch.flush();
//        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        // trik rendering kabut: matiin depth mask bentar biar transparan bener
        Gdx.gl.glDepthMask(false);
        for(ModelInstance fog : fogInstances) modelBatch.render(fog); // baru render kabut
        Gdx.gl.glDepthMask(true); // balikin lagi
//        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        modelBatch.end();
        // terakhir gambar UI/Stage di lapisan paling atas
        stage.act(delta);
        stage.draw();
    }

    private void updateMouseLook(){
        if(isPaused || !Gdx.input.isCursorCatched()) return;
        int dx = Gdx.input.getDeltaX();
        int dy = Gdx.input.getDeltaY();
//        if(skipMouseFrames > 0){ skipMouseFrames--; return; }
        yawDeg -= dx * mouseSens;
        pitchDeg -= dy * mouseSens;
        pitchDeg = MathUtils.clamp(pitchDeg, -89f, 89f);
        float yawRad = yawDeg * MathUtils.degreesToRadians;
        float pitchRad = pitchDeg * MathUtils.degreesToRadians;
        cam.direction.set(MathUtils.sin(yawRad) * MathUtils.cos(pitchRad), MathUtils.sin(pitchRad), MathUtils.cos(yawRad) * MathUtils.cos(pitchRad)).nor();
    }

    private void updateMovement(float delta){
        float speed = moveSpeed;
        if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) speed *= sprintMul;
        Vector3 forward = new Vector3(cam.direction.x, 0f, cam.direction.z).nor();
        Vector3 right = new Vector3(forward).crs(Vector3.Y).nor();
        Vector3 move = new Vector3();
        if(Gdx.input.isKeyPressed(Input.Keys.W)) move.add(forward);
        if(Gdx.input.isKeyPressed(Input.Keys.S)) move.sub(forward);
        if(Gdx.input.isKeyPressed(Input.Keys.D)) move.add(right);
        if(Gdx.input.isKeyPressed(Input.Keys.A)) move.sub(right);
        if(move.len2() > 0){
            move.nor();
            int substeps = 4;
            float subDelta = delta / substeps;
            for(int i = 0; i < substeps; i++){
                float stepX = move.x * speed * subDelta;
                float stepZ = move.z * speed * subDelta;
                float nextX = cam.position.x + stepX;
                float nextZ = cam.position.z + stepZ;
                // logika barier jari jari
                if(!waveManager.isStageCleared()){
                    // ambil sudut frame ini buat basenya
                    float baseAngle = waveManager.getPlayerCurrentAngle();
                    // ngitung delta langkah kecil ini
                    float currentRaw = MathUtils.atan2(cam.position.z, cam.position.x);
                    float nextRaw = MathUtils.atan2(nextZ, nextX);
                    float deltaAngle = nextRaw - currentRaw;
                    if(deltaAngle < -MathUtils.PI) deltaAngle += MathUtils.PI2;
                    else if(deltaAngle > MathUtils.PI) deltaAngle -= MathUtils.PI2;
                    // prediksi sudut total di posisi baru
                    float predictedTotalAngle = baseAngle + deltaAngle;
                    float barrier = waveManager.getAngleBarrier();
                    // tambahin syarat deltaAngle > 0.0001f biar mundur aman
                    if(predictedTotalAngle > barrier && deltaAngle > 0.0001f){
                        showBarrierWarning();
                        break; // stop cuma kalau maksa maju
                    }
                }
                float probeFar = 0.6f;
                float r = (float)Math.sqrt(cam.position.x * cam.position.x + cam.position.z * cam.position.z);
                float slopeLimit = (r < 80f) ? 1.4f : 0.6f;
                float currentY = terrain.getHeight(cam.position.x, cam.position.z);
                boolean safeX = true;
                float dirX = Math.signum(move.x);
                float probeX_Far = cam.position.x + dirX * probeFar;
                if(terrain.getHeight(probeX_Far, cam.position.z) - currentY > slopeLimit) safeX = false;
                // prediksi kalo maju stepX bakal nabrak pohon gakk
                if(cekNabrakPohon(cam.position.x + stepX, cam.position.z)) safeX = false;
                if(safeX) cam.position.x += stepX;
                currentY = terrain.getHeight(cam.position.x, cam.position.z);
                boolean safeZ = true;
                float dirZ = Math.signum(move.z);
                float probeZ_Far = cam.position.z + dirZ * probeFar;
                if(terrain.getHeight(cam.position.x, probeZ_Far) - currentY > slopeLimit) safeZ = false;
                // sekarang cek stepZnya bakal nabrak pohon gakk
                if(cekNabrakPohon(cam.position.x, cam.position.z + stepZ)) safeZ = false;
                if(safeZ) cam.position.z += stepZ;
            }
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && isGrounded){
            verticalVelocity = jumpForce;
            isGrounded = false;
        }
    }

    private void clampAndStickToTerrain(float delta){
        cam.position.x = terrain.clampX(cam.position.x, margin);
        cam.position.z = terrain.clampZ(cam.position.z, margin);
        verticalVelocity -= gravity * delta;
        cam.position.y += verticalVelocity * delta;
        float groundHeight = terrain.getHeight(cam.position.x, cam.position.z);
        float minHeight = groundHeight + eyeHeight;
        if(cam.position.y < minHeight){
            cam.position.y = minHeight;
            verticalVelocity = 0f;
            isGrounded = true;
        }else isGrounded = false;
    }

    private void createFogSystem(Terrain terrain) {
        ModelBuilder modelBuilder = new ModelBuilder();
        // bikin tekstur kabut procedural 128x128 biar lumayan tajem
        Texture kabutTex = createProceduralFogTexture(128);
        Material fogMat = new Material(
            TextureAttribute.createDiffuse(kabutTex),
            // atur blending biar transparan kyak asep beneran
            new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );
        long attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates;
        modelBuilder.begin();
        MeshPartBuilder meshBuilder = modelBuilder.part("fog_cluster", GL20.GL_TRIANGLES, attr, fogMat);
        // loop buat numpuk beberapa plane biar jadi gumpalan yg bervolume
        int planesCount = 1;
        float baseSize = 10f; // ukuran dasar per lembarnya
        for(int i = 0; i < planesCount; i++){
            // cari sumbu putar acak
            Vector3 axis = new Vector3(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f)).nor();
            float angle = MathUtils.random(0f, 360f); // sudut putar random
            // geser dikit posisinya biar ga numpuk pas di tengah
            Vector3 offset = new Vector3(MathUtils.random(-1.5f, 1.5f), MathUtils.random(-1.5f, 1.5f), MathUtils.random(-1.5f, 1.5f));
            // tentuin 4 titik sudut plane secara manual, anggep awalnya madep depan
            Vector3 p1 = new Vector3(-baseSize, -baseSize, 0); // kiri bawah
            Vector3 p2 = new Vector3( baseSize, -baseSize, 0); // kanan bawah
            Vector3 p3 = new Vector3( baseSize,  baseSize, 0); // kanan atas
            Vector3 p4 = new Vector3(-baseSize,  baseSize, 0); // kiri atas
            Vector3 normal = new Vector3(0, 0, 1); // arah normal
            // terapin rotasi sama offset ke titik titik tadi
            p1.rotate(axis, angle).add(offset);
            p2.rotate(axis, angle).add(offset);
            p3.rotate(axis, angle).add(offset);
            p4.rotate(axis, angle).add(offset);
            normal.rotate(axis, angle);
            // masukin data titiknya ke mesh builder
            meshBuilder.rect(
                p1.x, p1.y, p1.z,
                p2.x, p2.y, p2.z,
                p3.x, p3.y, p3.z,
                p4.x, p4.y, p4.z,
                normal.x, normal.y, normal.z
            );
        }
        fogModel = modelBuilder.end();
        // bikin terrainnya grid 254x254, ukuran worldnya 320x320 meter
        terrain = new Terrain(env, perlin, 254, 254, 320f, 320f);
        // sebar objek kabutnya ke seluruh map
        for(int i = 0; i < fogCount; i++){
            ModelInstance fog = new ModelInstance(fogModel);
            // random posisi X sama Z nyaa
            float x = MathUtils.random(-160f, 160f);
            float z = MathUtils.random(-160f, 160f);
            float yT = terrain.getHeight(x, z); // ambil tinggi tanah di koordinat itu biar kabutnya napak
            float y = MathUtils.random(yT, yT + 5f); // posisi y kabut random minimal sesuai tinggi tanah
            fog.transform.setToTranslation(x, y + MathUtils.random(1f, 5f), z); // set posisi awalnya, y ditambah dikit biar ngambang
            fog.transform.rotate(Vector3.Y, MathUtils.random(0f, 360f)); // rotasi acak biar ga keliatan seragam madepnya
            float randomScale = MathUtils.random(1.5f, 5.0f); // random ukurannya biar variatif, ada yg gede ada yg kecil
            fog.transform.scale(randomScale, randomScale * 0.6f, randomScale); // skala Y dipenyetin dikit biar kyak lapisan tipis
            fogInstances.add(fog);
        }
    }

    private void updateFog(float delta){
        for(ModelInstance fog : fogInstances){
            Vector3 pos = fog.transform.getTranslation(new Vector3());
            pos.x += fogSpeed * delta;
            if(pos.x > 160f){
                pos.x = -160f;
                pos.z = MathUtils.random(-160f, 160f);
                pos.y = terrain.getHeight(pos.x, pos.z) + MathUtils.random(1f, 5f);
                fog.transform.idt().setToTranslation(pos).rotate(Vector3.Y, MathUtils.random(0f, 360f));
                float s = MathUtils.random(1.5f, 5.0f);
                fog.transform.scale(s, s * 0.6f, s);
            }else fog.transform.setTranslation(pos);
        }
    }

    private Texture createProceduralFogTexture(int size){
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(size, size, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        PerlinNoise texNoise = new PerlinNoise();
        texNoise.frequencyX = 0.07f; texNoise.frequencyZ = 0.07f;
        texNoise.offsetX = MathUtils.random(0, 1000f); texNoise.offsetZ = MathUtils.random(0, 1000f);
        for(int x = 0; x < size; x++){
            for(int y = 0; y < size; y++){
                if(x == 0 || x == size - 1 || y == 0 || y == size - 1){
                    pixmap.setColor(0f, 0f, 0f, 0f); pixmap.drawPixel(x, y); continue;
                }
                float noiseVal = texNoise.getHeight(x, y);
                float dx = x - size/2f; float dy = y - size/2f;
                float dist = (float)Math.sqrt(dx*dx + dy*dy);
                float sphereMask = Math.max(0, 1.0f - (dist / (size/2f)));
                sphereMask = (float)Math.pow(sphereMask, 3.5f);
                float alpha = Math.min(noiseVal * sphereMask * 1.3f, 1.0f);
                pixmap.setColor(0.92f, 0.96f, 1f, alpha);
                pixmap.drawPixel(x, y);
            }
        }
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    // buat ngecek koordinat x z nabrak pohonapa gak
    private boolean cekNabrakPohon(float x, float z){
        float radiusPlayer = 0.5f; // anggep lebar badan kita segini
        float radiusPohon = 0.8f; // anggep lebar batang pohon segini
        float jarakMinimal = radiusPlayer + radiusPohon;
        float jarakMinimalKuadrat = jarakMinimal * jarakMinimal; // dikuadratin biar ga perlu hitung akar (lebih cepet)
        for (ModelInstance tree : treeInstances){
            tree.transform.getTranslation(tempPos); // ambil posisi pohon
            float dx = x - tempPos.x;
            float dz = z - tempPos.z;
            // itung jarak kuadrat
            float dist2 = dx * dx + dz * dz;
            // cek kalo jaraknya kurang dari batas aman
            if(dist2 < jarakMinimalKuadrat) return true; // nabrak!
        }
        return false; // aman
    }

    private void showBarrierWarning(){
        // cek cooldown cuma munculin pesan tiap 1.5 detik sekali biar enak dilihat
        if(warningCooldown > 0) return;
        warningCooldown = 1.5f;
        int enemiesLeft = waveManager.getRemainingEnemies();
        notificationLabel.setText("ELIMINATE " + enemiesLeft + " REMAINING HOSTILES\nTO PROCEED!");
        notificationLabel.setColor(Color.RED); // merah peringatan
        notificationLabel.setFontScale(1.2f); // agak kecil dikit biar muat 2 baris
        // animasi: muncul -> tahan -> ilang biar aluss
        notificationLabel.clearActions();
        notificationLabel.addAction(Actions.sequence(
            Actions.fadeIn(0.1f),
            Actions.delay(1.0f),
            Actions.fadeOut(0.5f)
        ));
    }

    @Override
    public void resize(int width, int height) {
        cam.viewportWidth = width;
        cam.viewportHeight = height;
        cam.update();
        stage.getViewport().update(width, height, true);
    }

    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void dispose() {
        if(terrain != null) terrain.dispose();
        if(modelBatch != null) modelBatch.dispose();
        if(fogModel != null) fogModel.dispose();
        if(stage != null) stage.dispose();
    }
}
