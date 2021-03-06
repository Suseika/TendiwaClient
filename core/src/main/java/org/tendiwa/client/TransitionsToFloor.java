package org.tendiwa.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import org.tendiwa.core.*;
import org.tendiwa.core.meta.Chance;
import org.tendiwa.geometry.Cell;
import org.tendiwa.geometry.DSL;
import org.tendiwa.geometry.Rectangle;
import org.tendiwa.geometry.Segment;

/**
 * This class generates textures of transitions to a certain type of floor and provides access to them.
 */
public class TransitionsToFloor extends TransitionPregenerator {
private static final PixmapTextureAtlas pixmapTextureAtlasFloors;

static {
	pixmapTextureAtlasFloors = createPixmapTextureAtlas("floors");
}

private final FloorType floorType;

/**
 * @param floorType
 * 	Type of a floor whose pixels will be in transitions obtained from this object.
 */
@Inject
TransitionsToFloor(
	@Named("transitions") TileTextureRegionProvider tileTextureRegionProvider,
	@Assisted FloorType floorType
) {
	super(tileTextureRegionProvider, 4);
	this.floorType = floorType;
	createTransitions();
}

private static PixmapTextureAtlas createPixmapTextureAtlas(String name) {
	return new PixmapTextureAtlas(Gdx.files.internal("pack/" + name + ".png"), Gdx.files.internal("pack/" + name + ".atlas"));
}

@Override
public Pixmap createTransition(CardinalDirection dir) {
	int diffusionDepth = 13;
	Pixmap.setBlending(Pixmap.Blending.None);
	Pixmap pixmap = pixmapTextureAtlasFloors.createPixmap(floorType.getResourceName());
	CardinalDirection opposite = dir.opposite();
	Rectangle transitionRec = DSL.rectangle(TILE_SIZE, TILE_SIZE).getSideAsSidePiece(dir).createRectangle(diffusionDepth);
	Rectangle clearRec = DSL.rectangle(TILE_SIZE, TILE_SIZE).getSideAsSidePiece(opposite).createRectangle(TILE_SIZE - diffusionDepth);
	pixmap.setColor(0, 0, 0, 0);
	// Fill the most of the pixmap with transparent pixels.
	pixmap.fillRectangle(clearRec.getX(), clearRec.getY(), clearRec.getWidth(), clearRec.getHeight());
	Segment sideSegment = transitionRec.getSideAsSegment(dir);
	Cell point = new Cell(sideSegment.getX(), sideSegment.getY());
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
				pixmap.drawPixel(point.getX(), point.getY());
			}
			point = point.moveToSide(dynamicGrowingDir);
		}
		point = new Cell(sideSegment.getX(), sideSegment.getY()).moveToSide(opposite, iterationsI++);
	}
	return pixmap;
}
}
