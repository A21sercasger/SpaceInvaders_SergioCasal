package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class Main extends ApplicationAdapter {
    private static final int MENU = 0;
    private static final int GAME = 1;
    private static final int PAUSE = 2;
    private int currentState = MENU;
    private static final int GAME_OVER = 3;
    private static final int VICTORY = 4;
    private Texture bossTexture;
    private Boss boss;
    private Array<BossMissile> bossMissiles;

    private SpriteBatch batch;
    private BitmapFont font;
    private OrthographicCamera camera;
    private GlyphLayout layout;

    // Background texture
    private Texture backgroundTexture;
    private Texture whiteTexture;

    private Rectangle playButton;
    private Rectangle exitButton;

    private Texture shipTexture;
    private Rectangle ship;
    private float shipSpeed = 400.0f;
    private boolean touchingShip = false;
    private float touchOffset = 0;

    private Texture pauseIconTexture;
    private Rectangle pauseButton;
    private Rectangle resumeButton;
    private Rectangle playAgainButton;
    private Rectangle backToMenuButton;

    // Sliders para controlar volumen
    private Rectangle musicSlider;
    private Rectangle sfxSlider;
    private Rectangle musicSliderKnob;
    private Rectangle sfxSliderKnob;
    private float musicVolume = 1.0f;
    private float sfxVolume = 1.0f;
    private boolean draggingMusicSlider = false;
    private boolean draggingSfxSlider = false;

    private Texture blackOverlay;

    private com.badlogic.gdx.audio.Music backgroundMusic;

    private static final float SCREEN_WIDTH = 480;
    private static final float SCREEN_HEIGHT = 800;
    private int lives = 5;
    private int score = 0;

    // Bullet stuff
    private class Bullet {
        float x, y, width, height, speed;
        Texture texture;

        public Bullet(float x, float y, Texture texture) {
            this.x = x;
            this.y = y;
            this.texture = texture;
            this.width = 16;
            this.height = 32;
            this.speed = 600;
        }

        public void update(float delta) {
            y += speed * delta;
        }

        public void render(SpriteBatch batch) {
            batch.draw(texture, x, y, width, height);
        }

        public boolean isOffScreen() {
            return y > SCREEN_HEIGHT;
        }
    }

    // PowerUp class - cae desde arriba hacia abajo
    private class PowerUp {
        float x, y, width, height, speed;
        Texture texture;

        public PowerUp(float x, float y, Texture texture) {
            this.x = x;
            this.y = y;
            this.texture = texture;
            this.width = 32;
            this.height = 32;
            this.speed = 150;
        }

        public void update(float delta) {
            y -= speed * delta;
        }

        public void render(SpriteBatch batch) {
            batch.draw(texture, x, y, width, height);
        }

        public boolean isOffScreen() {
            return y + height < 0;
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }

    private class Enemy {
        float x, y, width, height, speed;
        Texture texture;

        public Enemy(float x, float y, Texture texture) {
            this.x = x;
            this.y = y;
            this.texture = texture;
            this.width = 128;
            this.height = 128;
            this.speed = 180;
        }

        public void update(float delta) {
            y -= speed * delta;
        }

        public void render(SpriteBatch batch) {
            batch.draw(texture, x, y, width, height);
        }

        public boolean isOffScreen() {
            return y + height < 0;
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }

    private class Explosion {
        Texture[] frames;
        float x, y;
        float frameTime = 0.1f;
        float stateTime = 0f;
        int currentFrame = 0;
        boolean finished = false;

        public Explosion(Texture[] frames, float x, float y) {
            this.frames = frames;
            this.x = x;
            this.y = y;
        }

        public void update(float delta) {
            stateTime += delta;
            if (stateTime >= frameTime) {
                stateTime = 0f;
                currentFrame++;
                if (currentFrame >= frames.length) {
                    finished = true;
                }
            }
        }

        public void render(SpriteBatch batch) {
            if (!finished) {
                batch.draw(frames[currentFrame], x, y);
            }
        }

        public boolean isFinished() {
            return finished;
        }
    }

    private Texture[] explosionFrames;
    private Array<Explosion> explosions;

    private Array<Bullet> bullets;
    private Texture bulletTexture;
    private com.badlogic.gdx.audio.Sound shootSound;

    private Texture powerUpTexture;
    private Array<PowerUp> powerUps;
    private float timeSinceLastPowerUp = 0f;
    private float powerUpSpawnCooldown = 10f;

    private float timeSinceLastShot = 0f;
    private float shootCooldown = 0.2f;

    private boolean isTouchingScreen = false;

    private Texture enemy1Texture;
    private Array<Enemy> enemies;
    private Texture enemy2Texture;
    private Array<Enemy2> enemies2;
    private float timeSinceLastEnemy = 0f;
    private float enemySpawnCooldown = 0.3f;
    // Enemy2: moves horizontally and vertically
    private class Enemy2 extends Enemy {
        private boolean movingRight = true;

        public Enemy2(float x, float y, Texture texture) {
            super(x, y, texture);
            this.speed = 200;
        }

        @Override
        public void update(float delta) {
            super.update(delta);
            float horizontalSpeed = 100;
            if (movingRight) {
                x += horizontalSpeed * delta;
                if (x + width > SCREEN_WIDTH) {
                    x = SCREEN_WIDTH - width;
                    movingRight = false;
                }
            } else {
                x -= horizontalSpeed * delta;
                if (x < 0) {
                    x = 0;
                    movingRight = true;
                }
            }
        }
    }


    // Doble disparo
    private boolean doubleShotActive = false;
    private float doubleShotTimer = 0f;
    private static final float DOUBLE_SHOT_DURATION = 5f;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(2);
        layout = new GlyphLayout();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, SCREEN_WIDTH, SCREEN_HEIGHT);

        Pixmap whitePixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        whitePixmap.setColor(1, 1, 1, 1); // blanco
        whitePixmap.fill();
        whiteTexture = new Texture(whitePixmap);
        whitePixmap.dispose();

        explosionFrames = new Texture[6];
        for (int i = 0; i < 6; i++) {
            explosionFrames[i] = new Texture(Gdx.files.internal("explosion" + (i + 1) + ".png"));
        }
        explosions = new Array<>();

        // Load background texture
        backgroundTexture = new Texture(Gdx.files.internal("GameBG.png"));

        playButton = new Rectangle(140, 400, 200, 60);
        exitButton = new Rectangle(140, 300, 200, 60);

        shipTexture = new Texture(Gdx.files.internal("Spaceship.png"));
        ship = new Rectangle();
        ship.width = 128;
        ship.height = 128;
        ship.x = SCREEN_WIDTH / 2 - ship.width / 2;
        ship.y = 20;

        enemy1Texture = new Texture(Gdx.files.internal("Enemy1.png"));
        enemies = new Array<>();
        enemy2Texture = new Texture(Gdx.files.internal("Enemy2.png"));
        enemies2 = new Array<>();

        // Inicializar los sliders
        musicSlider = new Rectangle(140, 250, 200, 20);
        sfxSlider = new Rectangle(140, 180, 200, 20);
        musicSliderKnob = new Rectangle(140 + musicVolume * 200 - 10, 250 - 5, 20, 30);
        sfxSliderKnob = new Rectangle(140 + sfxVolume * 200 - 10, 180 - 5, 20, 30);

        pauseIconTexture = new Texture(Gdx.files.internal("PauseButton.png"));
        pauseButton = new Rectangle(SCREEN_WIDTH - 64 - 10, SCREEN_HEIGHT - 64 - 10, 50, 50);
        resumeButton = new Rectangle(140, 400, 200, 60);
        playAgainButton = new Rectangle(140, 400, 200, 60);
        backToMenuButton = new Rectangle(140, 300, 200, 60);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 1);
        pixmap.fill();
        blackOverlay = new Texture(pixmap);
        pixmap.dispose();

        bulletTexture = new Texture(Gdx.files.internal("Bullet.png"));
        bullets = new Array<>();
        shootSound = com.badlogic.gdx.Gdx.audio.newSound(com.badlogic.gdx.Gdx.files.internal("Shoot.mp3"));

        powerUpTexture = new Texture(Gdx.files.internal("PowerUp.png"));
        powerUps = new Array<>();

        bossTexture = new Texture(Gdx.files.internal("Boss.png"));
        bossMissiles = new Array<>();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                Vector3 touchPos = new Vector3(screenX, screenY, 0);
                camera.unproject(touchPos);

                isTouchingScreen = true;

                if (currentState == MENU) {
                    if (playButton.contains(touchPos.x, touchPos.y)) {
                        currentState = GAME;
                        return true;
                    }
                    if (exitButton.contains(touchPos.x, touchPos.y)) {
                        Gdx.app.exit();
                        return true;
                    }
                } else if (currentState == GAME) {
                    if (pauseButton.contains(touchPos.x, touchPos.y)) {
                        currentState = PAUSE;
                        return true;
                    }
                    if (ship.contains(touchPos.x, touchPos.y)) {
                        touchingShip = true;
                        touchOffset = touchPos.x - ship.x - (ship.width / 2);
                        return true;
                    }
                } else if (currentState == PAUSE) {
                    if (resumeButton.contains(touchPos.x, touchPos.y)) {
                        currentState = GAME;
                        return true;
                    }
                    // Detectar si se está tocando algún slider
                    if (musicSliderKnob.contains(touchPos.x, touchPos.y)) {
                        draggingMusicSlider = true;
                        return true;
                    }
                    if (sfxSliderKnob.contains(touchPos.x, touchPos.y)) {
                        draggingSfxSlider = true;
                        return true;
                    }
                    // Detectar si se hace clic directamente en la barra del slider
                    if (musicSlider.contains(touchPos.x, touchPos.y)) {
                        musicVolume = (touchPos.x - musicSlider.x) / musicSlider.width;
                        musicVolume = Math.max(0, Math.min(1, musicVolume));
                        updateMusicSliderPosition();
                        updateMusicVolume();
                        return true;
                    }
                    if (sfxSlider.contains(touchPos.x, touchPos.y)) {
                        sfxVolume = (touchPos.x - sfxSlider.x) / sfxSlider.width;
                        sfxVolume = Math.max(0, Math.min(1, sfxVolume));
                        updateSfxSliderPosition();
                        updateSfxVolume();
                        return true;
                    }
                } else if (currentState == GAME_OVER || currentState == VICTORY) {
                    if (playAgainButton.contains(touchPos.x, touchPos.y)) {
                        resetGame();
                        currentState = GAME;
                        return true;
                    }
                    if (backToMenuButton.contains(touchPos.x, touchPos.y)) {
                        resetGame();
                        currentState = MENU;
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                touchingShip = false;
                isTouchingScreen = false;
                draggingMusicSlider = false;
                draggingSfxSlider = false;
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                Vector3 touchPos = new Vector3(screenX, screenY, 0);
                camera.unproject(touchPos);

                // Manejar el arrastre de los sliders en el menú de pausa
                if (currentState == PAUSE) {
                    if (draggingMusicSlider) {
                        float newX = touchPos.x;
                        musicVolume = (newX - musicSlider.x) / musicSlider.width;
                        musicVolume = Math.max(0, Math.min(1, musicVolume));
                        updateMusicSliderPosition();
                        updateMusicVolume();
                        return true;
                    }
                    if (draggingSfxSlider) {
                        float newX = touchPos.x;
                        sfxVolume = (newX - sfxSlider.x) / sfxSlider.width;
                        sfxVolume = Math.max(0, Math.min(1, sfxVolume));
                        updateSfxSliderPosition();
                        updateSfxVolume();
                        return true;
                    }
                }

                if (currentState == GAME && touchingShip) {
                    float previousX = ship.x;
                    ship.x = touchPos.x - ship.width / 2 - touchOffset;

                    if (ship.x < 0) ship.x = 0;
                    if (ship.x > SCREEN_WIDTH - ship.width) ship.x = SCREEN_WIDTH - ship.width;

                    if (Math.abs(ship.x - previousX) > 1) {
                        shootBullet();
                    }

                    return true;
                }
                return false;
            }
        });

        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("Music.mp3"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(musicVolume);
        backgroundMusic.play();
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.1f, 0.1f, 0.2f, 1);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        if (currentState == MENU) {
            renderMenu();
        } else if (currentState == GAME) {
            updateGame();
            renderGame();
        } else if (currentState == PAUSE) {
            renderGame();
            renderPause();
        } else if (currentState == GAME_OVER) {
            renderGameOver();
        }
        else if (currentState == VICTORY) {
            renderVictory();
        }
        batch.end();
    }

    private void updateGame() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        timeSinceLastShot += deltaTime;
        timeSinceLastPowerUp += deltaTime;

        if (currentState == GAME && isTouchingScreen) {
            shootBullet();
        }

        if (timeSinceLastPowerUp >= powerUpSpawnCooldown) {
            timeSinceLastPowerUp = 0f;
            float x = (float)Math.random() * (SCREEN_WIDTH - 32);
            powerUps.add(new PowerUp(x, SCREEN_HEIGHT, powerUpTexture));
        }

        for (int i = powerUps.size - 1; i >= 0; i--) {
            PowerUp p = powerUps.get(i);
            p.update(deltaTime);

            Rectangle shipRect = new Rectangle(ship.x, ship.y, ship.width, ship.height);
            if (p.getBounds().overlaps(shipRect)) {
                powerUps.removeIndex(i);
                doubleShotActive = true;
                doubleShotTimer = 0f;
            } else if (p.isOffScreen()) {
                powerUps.removeIndex(i);
            }
        }

        if (doubleShotActive) {
            doubleShotTimer += deltaTime;
            if (doubleShotTimer >= DOUBLE_SHOT_DURATION) {
                doubleShotActive = false;
            }
        }

        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.update(deltaTime);
            if (b.isOffScreen()) {
                bullets.removeIndex(i);
            }
        }

        timeSinceLastEnemy += deltaTime;
        if (timeSinceLastEnemy >= enemySpawnCooldown) {
            timeSinceLastEnemy = 0f;
            float x = (float)Math.random() * (SCREEN_WIDTH - 64);
            if (score >= 100) {
                if (Math.random() < 0.7) {
                    enemies2.add(new Enemy2(x, SCREEN_HEIGHT, enemy2Texture));
                } else {
                    enemies.add(new Enemy(x, SCREEN_HEIGHT, enemy1Texture));
                }
            } else {
                enemies.add(new Enemy(x, SCREEN_HEIGHT, enemy1Texture));
            }
        }

        if (score >= 200 && boss == null) {
            boss = new Boss(bossTexture);
            enemies.clear();
            enemies2.clear();
        }

        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            e.update(deltaTime);
            if (e.isOffScreen()) {
                enemies.removeIndex(i);
                lives--;
                if (lives <= 0) {
                    currentState = GAME_OVER;
                    lives = 5;
                    enemies.clear();
                    enemies2.clear();
                    bullets.clear();
                    powerUps.clear();
                }
            }
        }

        for (int i = enemies2.size - 1; i >= 0; i--) {
            Enemy2 e = enemies2.get(i);
            e.update(deltaTime);
            if (e.isOffScreen()) {
                enemies2.removeIndex(i);
                lives--;
                if (lives <= 0) {
                    currentState = GAME_OVER;
                    lives = 5;
                    score = 0;
                    enemies.clear();
                    enemies2.clear();
                    bullets.clear();
                    powerUps.clear();
                }
            }
        }
// Bullet collision with Enemy
        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            Rectangle enemyRect = e.getBounds();

            for (int j = bullets.size - 1; j >= 0; j--) {
                Bullet b = bullets.get(j);
                Rectangle bulletRect = new Rectangle(b.x, b.y, b.width, b.height);

                if (enemyRect.overlaps(bulletRect)) {
                    explosions.add(new Explosion(explosionFrames, e.x, e.y)); // ← Aquí
                    enemies.removeIndex(i);
                    bullets.removeIndex(j);
                    score++;
                    break;
                }
            }
        }

// Bullet collision with Enemy2
        for (int i = enemies2.size - 1; i >= 0; i--) {
            Enemy2 e = enemies2.get(i);
            Rectangle enemyRect = e.getBounds();

            for (int j = bullets.size - 1; j >= 0; j--) {
                Bullet b = bullets.get(j);
                Rectangle bulletRect = new Rectangle(b.x, b.y, b.width, b.height);

                if (enemyRect.overlaps(bulletRect)) {
                    explosions.add(new Explosion(explosionFrames, e.x, e.y)); // ← Aquí
                    enemies2.removeIndex(i);
                    bullets.removeIndex(j);
                    score++;
                    break;
                }
            }
        }

        if (boss != null) {
            boss.update(deltaTime);

            // Update boss missiles
            for (int i = bossMissiles.size - 1; i >= 0; i--) {
                BossMissile m = bossMissiles.get(i);
                m.update(deltaTime);
                if (m.isOffScreen()) {
                    bossMissiles.removeIndex(i);
                } else if (m.getBounds().overlaps(new Rectangle(ship.x, ship.y, ship.width, ship.height))) {
                    bossMissiles.removeIndex(i);
                    lives--;
                    if (lives <= 0) {
                        currentState = GAME_OVER;
                        resetGame();
                    }
                }
            }

            // Bullet hits boss
            for (int i = bullets.size - 1; i >= 0; i--) {
                Bullet b = bullets.get(i);
                if (boss.getBounds().overlaps(new Rectangle(b.x, b.y, b.width, b.height))) {
                    bullets.removeIndex(i);
                    boss.health--;
                    if (boss.health <= 0) {
                        currentState = VICTORY;
                    }
                }
            }
        }
        for (int i = explosions.size - 1; i >= 0; i--) {
            Explosion explosion = explosions.get(i);
            explosion.update(deltaTime);
            if (explosion.isFinished()) {
                explosions.removeIndex(i);
            }
        }
    }

    private void updateMusicSliderPosition() {
        musicSliderKnob.x = musicSlider.x + (musicVolume * musicSlider.width) - (musicSliderKnob.width / 2);
    }

    private void updateSfxSliderPosition() {
        sfxSliderKnob.x = sfxSlider.x + (sfxVolume * sfxSlider.width) - (sfxSliderKnob.width / 2);
    }

    private void updateMusicVolume() {
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(musicVolume);
        }
    }

    private void updateSfxVolume() {
        // El volumen del efecto de sonido se aplicará la próxima vez que se dispare
    }

    private void shootBullet() {
        if (timeSinceLastShot < shootCooldown) return;
        timeSinceLastShot = 0f;

        if (shootSound != null) shootSound.play(0.3f * sfxVolume);

        if (doubleShotActive) {
            float centerX = ship.x + ship.width / 2;
            float bulletY = ship.y + ship.height;
            bullets.add(new Bullet(centerX - 10, bulletY, bulletTexture));
            bullets.add(new Bullet(centerX + 10, bulletY, bulletTexture
            ));
        } else {
            float centerX = ship.x + ship.width / 2;
            float bulletY = ship.y + ship.height;
            bullets.add(new Bullet(centerX, bulletY, bulletTexture));
        }
    }
    private void renderGame() {
        // Draw background - it should be the first thing drawn so it appears behind everything else
        batch.draw(backgroundTexture, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        batch.draw(shipTexture, ship.x, ship.y, ship.width, ship.height);
        batch.draw(pauseIconTexture, pauseButton.x, pauseButton.y, pauseButton.width, pauseButton.height);

        for (Bullet b : bullets) {
            b.render(batch);
        }

        for (PowerUp p : powerUps) {
            p.render(batch);
        }

        for (Enemy e : enemies) {
            e.render(batch);
        }
        for (Enemy2 e : enemies2) {
            e.render(batch);
        }

        if (boss != null) {
            boss.render(batch);
            for (BossMissile m : bossMissiles) {
                m.render(batch);
            }
        }

        for (Explosion ex : explosions) {
            ex.render(batch);
        }

        font.setColor(Color.WHITE);
        layout.setText(font, "Lives: " + lives);
        font.draw(batch, layout, 10, SCREEN_HEIGHT - 10);
        layout.setText(font, "Score: " + score);
        font.draw(batch, layout, 10, SCREEN_HEIGHT - 50);
    }

    private void renderVictory() {
        // Draw background on victory screen too
        batch.draw(backgroundTexture, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        font.setColor(Color.GREEN);
        layout.setText(font, "VICTORY!");
        font.draw(batch, layout, (SCREEN_WIDTH - layout.width) / 2, 700);

        font.setColor(Color.GREEN);
        layout.setText(font, "PLAY AGAIN");
        font.draw(batch, layout, (SCREEN_WIDTH - layout.width) / 2, playAgainButton.y + 40);

        font.setColor(Color.YELLOW);
        layout.setText(font, "MAIN MENU");
        font.draw(batch, layout, (SCREEN_WIDTH - layout.width) / 2, backToMenuButton.y + 40);
    }
    private void resetGame() {
        lives = 5;
        score = 0;
        bullets.clear();
        powerUps.clear();
        enemies.clear();
        enemies2.clear();
        boss = null;
        bossMissiles.clear();
    }

    private class Boss {
        float x, y, width, height;
        Texture texture;
        int health = 30;
        float shootTimer = 0f;
        float shootCooldown = 2f;

        public Boss(Texture texture) {
            this.texture = texture;
            this.width = 300;
            this.height = 150;
            this.x = (SCREEN_WIDTH - width) / 2;
            this.y = SCREEN_HEIGHT - height - 20;
        }

        public void update(float delta) {
            shootTimer += delta;
            if (shootTimer >= shootCooldown) {
                shootTimer = 0f;
                float missileWidth = 16;
                float missileHeight = 32;
                float baseX = x + width / 4 - missileWidth / 2;
                float baseX2 = x + width / 2 - missileWidth / 2;
                float baseX3 = x + 3 * width / 4 - missileWidth / 2;
                float missileY = y;

                bossMissiles.add(new BossMissile(baseX, missileY, missileWidth, missileHeight));
                bossMissiles.add(new BossMissile(baseX2, missileY, missileWidth, missileHeight));
                bossMissiles.add(new BossMissile(baseX3, missileY, missileWidth, missileHeight));
            }
        }

        public void render(SpriteBatch batch) {
            batch.draw(texture, x, y, width, height);
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }

    private class BossMissile {
        float x, y, width, height, speed = 300;

        public BossMissile(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public void update(float delta) {
            y -= speed * delta;
        }

        public void render(SpriteBatch batch) {
            batch.draw(bulletTexture, x, y, width, height);
        }

        public boolean isOffScreen() {
            return y + height < 0;
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }

    private void renderMenu() {
        // Draw background on menu screen too
        batch.draw(backgroundTexture, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        font.setColor(Color.WHITE);
        layout.setText(font, "SPACE INVADERS");
        font.draw(batch, layout, (SCREEN_WIDTH - layout.width) / 2, 700);

        font.setColor(Color.GREEN);
        layout.setText(font, "JUGAR");
        font.draw(batch, layout, (SCREEN_WIDTH - layout.width) / 2, playButton.y + 40);

        font.setColor(Color.RED);
        layout.setText(font, "SALIR");
        font.draw(batch, layout, (SCREEN_WIDTH - layout.width) / 2, exitButton.y + 40);
    }
    private void renderPause() {
        batch.setColor(0, 0, 0, 0.5f);
        batch.draw(blackOverlay, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        batch.setColor(1, 1, 1, 1);

        font.setColor(Color.WHITE);
        layout.setText(font, "PAUSED");
        font.draw(batch, layout, (SCREEN_WIDTH - layout.width) / 2, 700);

        font.setColor(Color.YELLOW);
        layout.setText(font, "RESUME");
        font.draw(batch, layout, (SCREEN_WIDTH - layout.width) / 2, resumeButton.y + 40);

        // Dibujar los sliders y las etiquetas
        font.setColor(Color.WHITE);
        layout.setText(font, "Music");
        font.draw(batch, layout, 140, musicSlider.y + musicSlider.height + 35);

        layout.setText(font, "SFX");
        font.draw(batch, layout, 140, sfxSlider.y + sfxSlider.height + 35);

        // Barra del slider
        batch.setColor(0.3f, 0.3f, 0.3f, 1);
        batch.draw(whiteTexture, musicSlider.x, musicSlider.y, musicSlider.width, musicSlider.height);
        batch.draw(whiteTexture, sfxSlider.x, sfxSlider.y, sfxSlider.width, sfxSlider.height);

        // Knob del slider
        batch.setColor(1, 1, 1, 1);
        batch.draw(blackOverlay, musicSliderKnob.x, musicSliderKnob.y, musicSliderKnob.width, musicSliderKnob.height);
        batch.draw(blackOverlay, sfxSliderKnob.x, sfxSliderKnob.y, sfxSliderKnob.width, sfxSliderKnob.height);

        // Valor numérico del volumen
        font.setColor(Color.WHITE);
        layout.setText(font, String.format("%.0f%%", musicVolume * 100));
        font.draw(batch, layout, musicSlider.x + musicSlider.width + 20, musicSlider.y + 15);

        layout.setText(font, String.format("%.0f%%", sfxVolume * 100));
        font.draw(batch, layout, sfxSlider.x + sfxSlider.width + 20, sfxSlider.y + 15);
    }

    private void renderGameOver() {
        // Draw background on game over screen too
        batch.draw(backgroundTexture, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        font.setColor(Color.RED);
        layout.setText(font, "GAME OVER");
        font.draw(batch, layout, (SCREEN_WIDTH - layout.width) / 2, 700);

        font.setColor(Color.GREEN);
        layout.setText(font, "PLAY AGAIN");
        font.draw(batch, layout, (SCREEN_WIDTH - layout.width) / 2, playAgainButton.y + 40);

        font.setColor(Color.YELLOW);
        layout.setText(font, "MAIN MENU");
        font.draw(batch, layout, (SCREEN_WIDTH - layout.width) / 2, backToMenuButton.y + 40);
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        shipTexture.dispose();
        pauseIconTexture.dispose();
        blackOverlay.dispose();
        bulletTexture.dispose();
        powerUpTexture.dispose();
        enemy1Texture.dispose();
        enemy2Texture.dispose();
        bossTexture.dispose();
        backgroundTexture.dispose(); // Don't forget to dispose the background texture
        if (shootSound != null) {
            shootSound.dispose();
        }
        backgroundMusic.dispose();
        whiteTexture.dispose();

        for (Texture tex : explosionFrames) {
            tex.dispose();
        }
    }
}
