package org.tendiwa.client;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;

/**
 * This class manages a single framebuffer and maps its TextureRegions to indices from 0 to {@code numberOfIndices-1},
 * thus allowing writing to and reading from texture regions by index. Its main intention is to share a single
 * framebuffer between all the implementations of {@link TransitionPregenerator}.
 */
public class TileTextureRegionProvider {
final SpriteBatch textureDrawingBatch = new SpriteBatch();
private final int regionsPerRow;
private final FrameBuffer fbo;
private final Texture fboTexture;
private final int tileSizeX;
private final int tileSizeY;
private final int maxNumber;
private int lastClaimedRegionNumber = 0;

public TileTextureRegionProvider(int numberOfPlaces, int tileSizeX, int tileSizeY) {
	int fboDimension = MathUtils.nextPowerOfTwo(
		(int) Math.ceil(Math.sqrt(numberOfPlaces * tileSizeX * tileSizeY))
	);
	this.tileSizeX = tileSizeX;
	this.tileSizeY = tileSizeY;
	regionsPerRow = fboDimension / tileSizeX;
	fbo = new FrameBuffer(Pixmap.Format.RGBA8888, fboDimension, fboDimension, false);
	fboTexture = fbo.getColorBufferTexture();
	OrthographicCamera camera = new OrthographicCamera(fboDimension, fboDimension);
	camera.setToOrtho(true, fboDimension, fboDimension);
	textureDrawingBatch.setProjectionMatrix(camera.combined);
	maxNumber = regionsPerRow * regionsPerRow;
}

TextureRegion obtainFboTextureRegion() {
	if (lastClaimedRegionNumber >= maxNumber) {
		throw new RuntimeException("Maximum number of generated textures (" + maxNumber + ") exceeded");
	}
	int startX = (lastClaimedRegionNumber % regionsPerRow) * tileSizeX;
	int startY = (lastClaimedRegionNumber / regionsPerRow) * tileSizeY;
	lastClaimedRegionNumber++;
	return new TextureRegion(
		fboTexture,
		startX,
		startY,
		tileSizeX,
		tileSizeY
	);
}

FrameBuffer getFbo() {
	return fbo;
}

Texture getFboTexture() {
	return fboTexture;
}

SpriteBatch getTextureDrawingBatch() {
	return textureDrawingBatch;
}
}
