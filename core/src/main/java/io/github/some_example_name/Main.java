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

    private SpriteBatch batch;
    private BitmapFont font;
    private OrthographicCamera camera;
    private GlyphLayout layout;

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

    private Texture blackOverlay;

    private static final float SCREEN_WIDTH = 480;
    private static final float SCREEN_HEIGHT = 800;

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
            this.speed = 100;
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


    private Array<Bullet> bullets;
    private Texture bulletTexture;

    private Texture powerUpTexture;
    private Array<PowerUp> powerUps;
    private float timeSinceLastPowerUp = 0f;
    private float powerUpSpawnCooldown = 10f;

    private float timeSinceLastShot = 0f;
    private float shootCooldown = 0.2f;

    private boolean isTouchingScreen = false;

    private Texture enemy1Texture;
    private Array<Enemy> enemies;
    private float timeSinceLastEnemy = 0f;
    private float enemySpawnCooldown = 0.5f;


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


        pauseIconTexture = new Texture(Gdx.files.internal("PauseButton.png"));
        pauseButton = new Rectangle(SCREEN_WIDTH - 64 - 10, SCREEN_HEIGHT - 64 - 10, 50, 50);
        resumeButton = new Rectangle(140, 400, 200, 60);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 1);
        pixmap.fill();
        blackOverlay = new Texture(pixmap);
        pixmap.dispose();

        bulletTexture = new Texture(Gdx.files.internal("Bullet.png"));
        bullets = new Array<>();

        powerUpTexture = new Texture(Gdx.files.internal("PowerUp.png"));
        powerUps = new Array<>();

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
                }
                return false;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                touchingShip = false;
                isTouchingScreen = false;
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (currentState == GAME && touchingShip) {
                    Vector3 touchPos = new Vector3(screenX, screenY, 0);
                    camera.unproject(touchPos);

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
            enemies.add(new Enemy(x, SCREEN_HEIGHT, enemy1Texture));
        }

// Actualiza enemigos y elimina los que se salen de pantalla
        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            e.update(deltaTime);
            if (e.isOffScreen()) {
                enemies.removeIndex(i);
            }
        }
        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            Rectangle enemyRect = e.getBounds();

            for (int j = bullets.size - 1; j >= 0; j--) {
                Bullet b = bullets.get(j);
                Rectangle bulletRect = new Rectangle(b.x, b.y, b.width, b.height);

                if (enemyRect.overlaps(bulletRect)) {
                    enemies.removeIndex(i);
                    bullets.removeIndex(j);
                    break;
                }
            }
        }

    }

    private void shootBullet() {
        if (timeSinceLastShot < shootCooldown) return;
        timeSinceLastShot = 0f;

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

    }

    private void renderMenu() {
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

    }
}
