package org.tendiwa.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import org.tendiwa.client.ui.fonts.FontRegistry;
import org.tendiwa.client.ui.model.CursorPosition;

import java.util.LinkedList;
import java.util.List;

public class StatusLayer {
private final GameScreen gameScreen;
private final BitmapFont font;
private List<Object> lines = new LinkedList<>();
private int lineHeight = 18;
private int padding = 20;

public StatusLayer(final GameScreen gameScreen, FontRegistry fontRegistry, final CursorPosition cursorPosition) {
	this.font = fontRegistry.obtain(20, true);
	this.gameScreen = gameScreen;
	addLine(new Object() {
		@Override
		public String toString() {
			return Gdx.graphics.getFramesPerSecond() + " FPS";
		}
	});
	addLine(new Object() {
		@Override
		public String toString() {
			return "screen at " + gameScreen.startCellX + ":" + gameScreen.startCellY;
		}
	});
	addLine(new Object() {
		@Override
		public String toString() {
			int worldX = cursorPosition.getWorldX();
			int worldY = cursorPosition.getWorldY();
			return "cursor at " + worldX + ":" + worldY;
		}
	});
//	addLine(new Object() {
//		@Override
//		public String toString() {
//			int worldX = gameScreen.getCursor().getWorldX();
//			int worldY = gameScreen.getCursor().getWorldY();
//			return "visibility previous: " + Tendiwa.getPlayerCharacter().getSeer().getPreviousBorderVisionCache().get(new Border(worldX, worldY, Directions.W));
//		}
//	});
//	addLine(new Object() {
//		@Override
//		public String toString() {
//			int worldX = gameScreen.getCursor().getWorldX();
//			int worldY = gameScreen.getCursor().getWorldY();
//			return "visibility current: " + Tendiwa.getPlayerCharacter().getSeer().getBorderVisionCache().get(new Border(worldX, worldY, Directions.W));
//		}
//	});
}

public void draw() {
	gameScreen.batch.begin();
	int lineNumber = 0;
	for (Object line : lines) {

		font.draw(
			gameScreen.batch,
			line.toString(),
			gameScreen.startPixelX + padding,
			gameScreen.startPixelY + padding + lineHeight * lineNumber
		);
		lineNumber++;
	}
	gameScreen.batch.end();
}

private void addLine(Object line) {
	lines.add(line);
}
}
