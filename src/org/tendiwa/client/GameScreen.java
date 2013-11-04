package org.tendiwa.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import tendiwa.core.*;
import tendiwa.core.Character;
import tendiwa.core.meta.Chance;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class GameScreen implements Screen {

static final int TILE_SIZE = 32;
final Stage stage;
final int maxStartX;
final int maxStartY;
private final TendiwaGame game;
private final PixmapTextureAtlas pixmapTextureAtlasFloors;
private final SpriteBatch batch;
private final Cell[][] cells;
private final int windowHeight;
private final int windowWidth;
private final Map<CardinalDirection, Map<Integer, Pixmap>> floorTransitions = new HashMap<>();
private final Texture cursor;
private final int windowWidthCells;
private final int windowHeightCells;
private final TextureAtlas atlasFloors;
private final TextureAtlas atlasObjects;
private final int transitionsAtlasSize = 1024;
private final FrameBuffer transitionsFrameBuffer;
private final SpriteBatch defaultBatch;
protected int startCellX;
OrthographicCamera camera;
String vertexShader = "attribute vec4 a_position;    \n" +
	"attribute vec4 a_color;\n" +
	"attribute vec2 a_texCoord0;\n" +
	"uniform mat4 u_worldView;\n" +
	"uniform mat4 u_projTrans;\n" +
	"varying vec4 v_color;" +
	"varying vec2 v_texCoords;" +
	"void main()                  \n" +
	"{                            \n" +
	"   v_color = vec4(1, 1, 1, 1); \n" +
	"   v_texCoords = a_texCoord0; \n" +
	"   gl_Position =  u_projTrans * a_position;  \n" +
	"}                            \n";
String fragmentShader = "#ifdef GL_ES\n" +
	"precision mediump float;\n" +
	"#endif\n" +
	"varying vec4 v_color;\n" +
	"varying vec2 v_texCoords;\n" +
	"uniform sampler2D u_texture;\n" +
	"void main()                                  \n" +
	"{                                            \n" +
	"  gl_FragColor = v_color* texture2D(u_texture, v_texCoords);\n" +
	"}";
World WORLD;
PlayerCharacter PLAYER;
int startCellY;
int centerPixelX;
int centerPixelY;
int cameraMoveStep = 1;
private BitmapFont font = new FreeTypeFontGenerator(Gdx.files.internal("assets/DejaVuSansMono.ttf")).generateFont(20, "qwertyuiop[]asdfghjkl;'zxcvbnm,./1234567890-=!@#$%^&*()_+QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>?\\|", true);
private Texture bufTexture0 = new Texture(new Pixmap(TILE_SIZE, TILE_SIZE, Pixmap.Format.RGBA8888));
private Texture bufTexture1 = new Texture(new Pixmap(TILE_SIZE, TILE_SIZE, Pixmap.Format.RGBA8888));
private Texture bufTexture2 = new Texture(new Pixmap(TILE_SIZE, TILE_SIZE, Pixmap.Format.RGBA8888));
private Texture bufTexture3 = new Texture(new Pixmap(TILE_SIZE, TILE_SIZE, Pixmap.Format.RGBA8888));
private Map<String, TextureRegion> transitionsMap = new HashMap<>();
private FrameBuffer cellNetFramebuffer;
private Map<Character, Actor> characterActors = new HashMap<>();
private boolean eventResultProcessingIsGoing = false;
int startPixelX;
int startPixelY;
private final GameScreenInputProcessor controller;

public GameScreen(final TendiwaGame game) {
	WORLD = Tendiwa.getWorld();
	PLAYER = WORLD.getPlayerCharacter();

	windowWidth = game.cfg.width;
	windowHeight = game.cfg.height;
	windowWidthCells = (int) Math.ceil(((float) windowWidth) / 32);
	windowHeightCells = (int) Math.ceil(((float) windowHeight) / 32);

	camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	camera.setToOrtho(true, windowWidth, windowHeight);
	centerCamera(PLAYER.getX() * TILE_SIZE, PLAYER.getY() * TILE_SIZE);
	camera.update();

	this.game = game;

	atlasFloors = new TextureAtlas(Gdx.files.internal("pack/floors.atlas"), true);
	atlasObjects = new TextureAtlas(Gdx.files.internal("pack/objects.atlas"), true);
	pixmapTextureAtlasFloors = createPixmapTextureAtlas("floors");

	// Sprite batch for drawing to world.
	batch = new SpriteBatch();
	// Utility batch with no transformations.
	defaultBatch = new SpriteBatch();

	stage = new Stage(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true, batch);
	stage.setCamera(camera);

	cells = WORLD.getCellContents();

	cursor = buildCursorTexture();

	maxStartX = TendiwaGame.WIDTH - windowWidthCells - cameraMoveStep;
	maxStartY = TendiwaGame.HEIGHT - windowHeightCells - cameraMoveStep;

	transitionsFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, game.cfg.width, game.cfg.height, false);
	cellNetFramebuffer = new FrameBuffer(Pixmap.Format.RGBA8888, game.cfg.width+TILE_SIZE, game.cfg.height+TILE_SIZE, false);

	buildNet();
	initializeActors();
	controller = new GameScreenInputProcessor(this);
	Gdx.input.setInputProcessor(controller);

//	Gdx.graphics.setContinuousRendering(false);
//	Gdx.graphics.requestRendering();
}

void centerCamera(int x, int y) {
	startCellX = (x - x % TILE_SIZE) / TILE_SIZE - windowWidthCells / 2;
	startCellY = (y - y % TILE_SIZE) / TILE_SIZE - windowHeightCells / 2;
	centerPixelX = x;
	centerPixelY = y;
	startPixelX = centerPixelX - windowWidth / 2;
	startPixelY = centerPixelY - windowHeight / 2;
}

private PixmapTextureAtlas createPixmapTextureAtlas(String name) {
	return new PixmapTextureAtlas(Gdx.files.internal("pack/" + name + ".png"), Gdx.files.internal("pack/" + name + ".atlas"));
}

@Override
public void render(float delta) {


	Actor characterActor = getCharacterActor(Tendiwa.getPlayer());
	stage.act(Gdx.graphics.getDeltaTime());
	controller.executeCurrentTask();
	processEvents();
	centerCamera(
		(int) (characterActor.getX() * TILE_SIZE),
		(int) (characterActor.getY() * TILE_SIZE)
	);

	camera.position.set(centerPixelX, centerPixelY, 0);
	Gdx.gl.glClearColor(0, 0, 0, 1);
	Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
	camera.update();
	batch.setProjectionMatrix(camera.combined);

	// Draw whole floor tiles
	batch.begin();
	int maxX = getMaxRenderCellX();
	int maxY = getMaxRenderCellY();
	for (int x = startCellX; x < maxX; x++) {
		for (int y = startCellY; y < maxY; y++) {
			TextureRegion floor = getFloorTextureByCell(x, y);
			batch.draw(floor, x * TILE_SIZE, y * TILE_SIZE);
		}
	}
//	batch.drawWorld(transitionsFrameBuffer.getColorBufferTexture(), 0, 0);

	batch.end();

	// Draw transitions
	for (int x = 0; x < windowWidth / TILE_SIZE + 1; x++) {
		for (int y = 0; y < windowHeight / TILE_SIZE + 1; y++) {
			getTransitionTextureByCell(startCellX + x, startCellY + y);
		}
	}

	int cursorX = Gdx.input.getX();
	int cursorY = Gdx.input.getY();
	int cursorScreenCoordX = (cursorX - cursorX % TILE_SIZE);
	int cursorScreenCoordY = (cursorY - cursorY % TILE_SIZE);
	int cursorWorldX = startCellX + cursorScreenCoordX / TILE_SIZE;
	int cursorWorldY = startCellY + cursorScreenCoordY / TILE_SIZE;

	// Draw objects and characters
	batch.begin();
	drawNet();
	batch.end();
	stage.draw();
	batch.begin();
	batch.draw(cursor, cursorWorldX * TILE_SIZE, cursorWorldY * TILE_SIZE);
	// But first drawWorld cursor before drawing objects
	for (int x = 0; x < windowWidth / TILE_SIZE + 1; x++) {
		// Objects are drawn for one additional row to see high objects
		for (int y = 0; y < windowHeight / TILE_SIZE + 2; y++) {
			Cell cell = cells[startCellX + x][startCellY + y];
			if (cell.object() != ObjectType.VOID.getId()) {
				TextureAtlas.AtlasRegion objectTexture = getObjectTextureByCell(startCellX + x, startCellY + y);
				int textureX = (startCellX + x) * TILE_SIZE - (objectTexture.getRegionWidth() - TILE_SIZE) / 2;
				int textureY = (startCellY + y) * TILE_SIZE - (objectTexture.getRegionHeight() - TILE_SIZE);
				batch.draw(objectTexture, textureX, textureY);
			}
		}
	}
	// Draw stats
	font.draw(batch, Gdx.graphics.getFramesPerSecond() + "; " + startCellX + ":" + startCellY + ", worldMouse: " + cursorWorldX + ":" + cursorWorldY, startPixelX + 100, startPixelY + 100);
	batch.end();



}

private int getMaxRenderCellX() {
	return startCellX + windowWidthCells + (startPixelX % TILE_SIZE == 0 ? 0 : 1);
}

private int getMaxRenderCellY() {
	return startCellY + windowHeightCells + (startPixelY % TILE_SIZE == 0 ? 0 : 1);
}

private void processEvents() {
	Queue<EventResult> queue = game.getEventManager().getPendingOperations();
	// Loop variable will remain true if it is not set to true inside process.
	while (!eventResultProcessingIsGoing && !queue.isEmpty()) {
		EventResult result = queue.remove();
		eventResultProcessingIsGoing = true;
		result.process();
	}
}

void eventProcessingDone() {
	eventResultProcessingIsGoing = false;
}

boolean isEventProcessingGoing() {
	return eventResultProcessingIsGoing;
}

private Actor createCharacterActor(Character character) {
	return new CharacterActor(character);
}

private void initializeActors() {
	TimeStream timeStream = WORLD.getPlayerCharacter().getTimeStream();
	for (Character character : timeStream.getCharacters()) {
		Actor actor = createCharacterActor(character);
		characterActors.put(character, actor);
		stage.addActor(actor);
	}
}

private void buildNet() {
	cellNetFramebuffer.begin();

	// Imitating the same camera as for batch, but with a greater viewport.
	OrthographicCamera adHocCamera = new OrthographicCamera(windowWidth+TILE_SIZE, windowHeight+TILE_SIZE);
	adHocCamera.setToOrtho(true, windowWidth + TILE_SIZE, windowHeight + TILE_SIZE);

	ShapeRenderer shapeRenderer = new ShapeRenderer();
	shapeRenderer.setProjectionMatrix(adHocCamera.combined);
	shapeRenderer.setColor(0, 0, 0, 0.2f);
	shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
	for (int cellX = 0; cellX < windowWidth / TILE_SIZE + 2; cellX++) {
		shapeRenderer.line(cellX * TILE_SIZE, 0, cellX * TILE_SIZE, windowHeight+TILE_SIZE);
	}
	for (int cellY = 0; cellY < windowWidth / TILE_SIZE + 2; cellY++) {
		shapeRenderer.line(0, cellY * TILE_SIZE, windowWidth+TILE_SIZE, cellY * TILE_SIZE);
	}
	shapeRenderer.end();
	cellNetFramebuffer.end();

}

private void drawNet() {
	batch.draw(cellNetFramebuffer.getColorBufferTexture(), startCellX*TILE_SIZE, startCellY*TILE_SIZE);
}

private TextureAtlas.AtlasRegion getObjectTextureByCell(int x, int y) {
	ObjectType objectType = ObjectType.getById(cells[x][y].object());
	String name = objectType.getName();
	if (objectType.getObjectClass() == ObjectType.ObjectClass.WALL) {
		int index = 0;
		if (cells[x][y - 1].contains(objectType)) {
			index += 1000;
		}
		if (cells[x + 1][y].contains(objectType)) {
			index += 100;
		}
		if (cells[x][y + 1].contains(objectType)) {
			index += 10;
		}
		if (cells[x - 1][y].contains(objectType)) {
			index += 1;
		}
		return atlasObjects.findRegion(name, index);
	} else {
		return atlasObjects.findRegion(
			name
		);
	}
}

private void getTransitionTextureByCell(int x, int y) {
	int self = cells[x][y].floor();
	int north = y + 1 < TendiwaGame.HEIGHT ? cells[x][y + 1].floor() : self;
	int east = x + 1 < TendiwaGame.WIDTH ? cells[x + 1][y].floor() : self;
	int south = y > 0 ? cells[x][y - 1].floor() : self;
	int west = x > 0 ? cells[x - 1][y].floor() : self;

	if (north != self || east != self || south != self || west != self) {
		drawCellWithTransitions(x, y, north, east, south, west);
	}
}

private TextureRegion getFloorTextureByCell(int x, int y) {
	String name = FloorType.getById(cells[x][y].floor()).getName();
	// TODO: Randomized tile indexes
	return atlasFloors.findRegion(
		name
	);
}

private void drawCellWithTransitions(int x, int y, int north, int east, int south, int west) {
	String transitionKey = north + "_" + east + "_" + south + "_" + west;
	// Get individual transition pixmap for each side
	TextureRegion region;
	if (transitionsMap.containsKey(transitionKey)) {
		region = transitionsMap.get(transitionKey);
	} else {
		region = createNewFloorTransitionRegion(north, east, south, west);
	}
	batch.begin();
	batch.draw(region, x * TILE_SIZE, y * TILE_SIZE);
	batch.end();
}

private TextureRegion createNewFloorTransitionRegion(int north, int east, int south, int west) {
	String transitionKey = north + "_" + east + "_" + south + "_" + west;

	Pixmap.Blending previousBlending = Pixmap.getBlending();
	Pixmap.setBlending(Pixmap.Blending.None);
	Pixmap transE = getTransition(Directions.E, east);
	Pixmap transW = getTransition(Directions.W, west);
	Pixmap transN = getTransition(Directions.N, north);
	Pixmap transS = getTransition(Directions.S, south);
	int atlasX = transitionsMap.size() * TILE_SIZE % transitionsAtlasSize;
	int atlasY = transitionsMap.size() * TILE_SIZE / transitionsAtlasSize * TILE_SIZE;

	Pixmap.setBlending(Pixmap.Blending.SourceOver);

	bufTexture0.draw(transN, 0, 0);
	bufTexture1.draw(transE, 0, 0);
	bufTexture2.draw(transS, 0, 0);
	bufTexture3.draw(transW, 0, 0);
	transitionsFrameBuffer.begin();
	defaultBatch.begin();
	defaultBatch.draw(bufTexture0, atlasX, atlasY);
	defaultBatch.draw(bufTexture1, atlasX, atlasY);
	defaultBatch.draw(bufTexture2, atlasX, atlasY);
	defaultBatch.draw(bufTexture3, atlasX, atlasY);
	defaultBatch.end();
	transitionsFrameBuffer.end();

	TextureRegion textureRegion = new TextureRegion(transitionsFrameBuffer.getColorBufferTexture(), atlasX, atlasY, TILE_SIZE, TILE_SIZE);
	transitionsMap.put(transitionKey, textureRegion);

	Pixmap.setBlending(previousBlending);
	return textureRegion;
}

private Pixmap getTransition(CardinalDirection dir, int floorId) {
	if (!floorTransitions.containsKey(dir)) {
		floorTransitions.put(dir, new HashMap<Integer, Pixmap>());
	}
	if (!floorTransitions.get(dir).containsKey(floorId)) {
		floorTransitions.get(dir).put(floorId, createTransition(dir, floorId));
	}
	return floorTransitions.get(dir).get(floorId);
}

private Pixmap createTransition(CardinalDirection dir, int floorId) {
	int diffusionDepth = 13;
	if (dir.isVertical()) {
		dir = dir.opposite();
	}
	Pixmap.setBlending(Pixmap.Blending.None);
	Pixmap pixmap = pixmapTextureAtlasFloors.createPixmap(FloorType.getById(floorId).getName());
	CardinalDirection opposite = dir.opposite();
	EnhancedRectangle transitionRec = DSL.rectangle(TILE_SIZE, TILE_SIZE).getSideAsSidePiece(dir).createRectangle(diffusionDepth);
	EnhancedRectangle clearRec = DSL.rectangle(TILE_SIZE, TILE_SIZE).getSideAsSidePiece(opposite).createRectangle(TILE_SIZE - diffusionDepth);
	pixmap.setColor(0, 0, 0, 0);
	// Fill the most of the pixmap with transparent pixels.
	pixmap.fillRectangle(clearRec.x, clearRec.y, clearRec.width, clearRec.height);
	Segment sideSegment = transitionRec.getSideAsSegment(dir);
	EnhancedPoint point = new EnhancedPoint(sideSegment.x, sideSegment.y);
	pixmap.setColor(0, 0, 0, 0);
	CardinalDirection dynamicGrowingDir = dir.isVertical() ? Directions.E : Directions.S;
	int startI = sideSegment.getStaticCoord();
	int oppositeGrowing = opposite.getGrowing();
	int iterationsI = 0;
	for (
		int i = startI;
		i != startI + (diffusionDepth + 1) * opposite.getGrowing();
		i += oppositeGrowing
		) {
		for (
			int j = sideSegment.getStartCoord();
			j <= sideSegment.getEndCoord();
			j += 1
			) {
			if (Chance.roll((i - startI) / oppositeGrowing * 100 / diffusionDepth + 50)) {
				// Set transparent pixels to leave only some non-transparent ones.
				pixmap.drawPixel(point.x, point.y);
			}
			point.moveToSide(dynamicGrowingDir);
		}
		point.setLocation(sideSegment.x, sideSegment.y);
		point.moveToSide(opposite, iterationsI++);
	}
	return pixmap;
}

@Override
public void resize(int width, int height) {

}

@Override
public void show() {

}

@Override
public void hide() {

}

@Override
public void pause() {

}

@Override
public void resume() {

}

@Override
public void dispose() {

}

Texture buildCursorTexture() {
	Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
	pixmap.setColor(1, 1, 0, 0.3f);
	pixmap.fillRectangle(0, 0, 31, 31);
	return new Texture(pixmap);
}

public Actor getCharacterActor(Character character) {
	return characterActors.get(character);
}

}
