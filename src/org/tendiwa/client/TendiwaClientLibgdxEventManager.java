package org.tendiwa.client;

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import org.tendiwa.events.*;
import tendiwa.core.*;

import java.util.LinkedList;
import java.util.Queue;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.run;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

/**
 * On each received {@link Event} this class creates a {@link EventResult} pending operation and placed it in a queue.
 * Each time the game is rendered, all EventResults are processed inside {@link GameScreen#render(float)}.
 */
public class TendiwaClientLibgdxEventManager implements TendiwaClientEventManager {
private static boolean animationsEnabled = false;
private GameScreen gameScreen;
private Queue<EventResult> pendingOperations = new LinkedList<>();

TendiwaClientLibgdxEventManager(GameScreen gameScreen) {
	this.gameScreen = gameScreen;
}

public static void toggleAnimations() {
	animationsEnabled = !animationsEnabled;
}

@Override
public void event(final EventMove e) {
	pendingOperations.add(new EventResult() {
		@Override
		public void process() {
			Actor characterActor = gameScreen.getStage().getCharacterActor(e.getCharacter());
			if (animationsEnabled) {
				MoveToAction action = new MoveToAction();
				action.setPosition(Tendiwa.getPlayerCharacter().getX(), Tendiwa.getPlayerCharacter().getY());
				action.setDuration(0.1f);
				Action sequence = sequence(action, run(new Runnable() {
					@Override
					public void run() {
						gameScreen.signalEventProcessingDone();
					}
				}));
				characterActor.addAction(sequence);
			} else {
				characterActor.setX(Tendiwa.getPlayerCharacter().getX());
				characterActor.setY(Tendiwa.getPlayerCharacter().getY());
				gameScreen.signalEventProcessingDone();
			}
		}
	});
}

@Override
public void event(final EventSay e) {
	pendingOperations.add(new EventResult() {
		@Override
		public void process() {

		}
	});
}

@Override
public void event(final EventFovChange eventFovChange) {
	pendingOperations.add(new EventResult() {
		@Override
		public void process() {
			for (Integer coord : eventFovChange.unseen) {
				RenderCell cell = GameScreen.getRenderWorld().getCell(coord);
				cell.setVisible(false);
				if (gameScreen.player.getPlane().hasAnyItems(cell.x, cell.y)) {
					for (Item item : gameScreen.player.getPlane().getItems(cell.x, cell.y)) {
						gameScreen.renderWorld.addUnseenItem(cell.x, cell.y, item);
					}
				}
			}
			for (RenderCell cell : eventFovChange.seen) {
				GameScreen.getRenderWorld().seeCell(cell);
				if (gameScreen.renderWorld.hasAnyUnseenItems(cell.x, cell.y)) {
					gameScreen.renderWorld.removeUnseenItems(cell.x, cell.y);
				}
			}
			gameScreen.signalEventProcessingDone();
		}
	});
}

@Override
public void event(final EventInitialTerrain eventInitialTerrain) {
	pendingOperations.add(new EventResult() {
		@Override
		public void process() {
			for (RenderCell cell : eventInitialTerrain.seen) {
				GameScreen.getRenderWorld().seeCell(cell);
			}
			gameScreen.signalEventProcessingDone();
		}
	});
}

@Override
public void event(final EventItemDisappear eventItemDisappear) {
	pendingOperations.add(new EventResult() {
		@Override
		public void process() {
			ItemActor itemActor = gameScreen.getStage().obtainItemActor(
				eventItemDisappear.x,
				eventItemDisappear.y,
				eventItemDisappear.item
			);
			if (animationsEnabled) {
				AlphaAction alphaAction = new AlphaAction();
				alphaAction.setAlpha(0.0f);
				alphaAction.setDuration(0.1f);
				Action sequence = sequence(alphaAction, run(new Runnable() {
					@Override
					public void run() {
						gameScreen.getStage().removeItemActor(eventItemDisappear.item);
						gameScreen.signalEventProcessingDone();
					}
				}));
				itemActor.addAction(sequence);
			} else {
				gameScreen.getStage().removeItemActor(eventItemDisappear.item);
				gameScreen.signalEventProcessingDone();
			}
		}
	});
}

@Override
public void event(final EventGetItem eventGetItem) {
	pendingOperations.add(new EventResult() {
		@Override
		public void process() {
			TendiwaUiStage.getInventory().update();
			gameScreen.signalEventProcessingDone();
		}
	});
}

@Override
public void event(EventLoseItem eventLoseItem) {
	pendingOperations.add(new EventResult() {
		@Override
		public void process() {
			TendiwaUiStage.getInventory().update();
			gameScreen.signalEventProcessingDone();
		}
	});
}

@Override
public void event(EventItemAppear eventItemAppear) {
}

@Override
public void event(final EventPutOn eventPutOn) {
	pendingOperations.add(new EventResult() {
		@Override
		public void process() {
			if (eventPutOn.getCharacter().isPlayer()) {
				TendiwaUiStage.getInventory().update();
			}
			if (eventPutOn.getCharacter().getType().hasAspect(CharacterAspect.HUMANOID)) {
				gameScreen.getStage().getCharacterActor(eventPutOn.getCharacter()).updateTexture();
			}
			gameScreen.signalEventProcessingDone();
		}
	});
}

public Queue<EventResult> getPendingOperations() {
	return pendingOperations;
}

@Override
public void event(final EventWield eventWield) {
	pendingOperations.add(new EventResult() {
		@Override
		public void process() {
			if (eventWield.getCharacter().isPlayer()) {
				TendiwaUiStage.getInventory().update();
			}
			if (eventWield.getCharacter().getType().hasAspect(CharacterAspect.HUMANOID)) {
				gameScreen.getStage().getCharacterActor(eventWield.getCharacter()).updateTexture();
			}
			gameScreen.signalEventProcessingDone();
		}
	});
}

@Override
public void event(final EventTakeOff eventTakeOff) {
	pendingOperations.add(new EventResult() {
		@Override
		public void process() {
			if (eventTakeOff.getCharacter().isPlayer()) {
				TendiwaUiStage.getInventory().update();
			}
			if (eventTakeOff.getCharacter().getType().hasAspect(CharacterAspect.HUMANOID)) {
				gameScreen.getStage().getCharacterActor(eventTakeOff.getCharacter()).updateTexture();
			}
			gameScreen.signalEventProcessingDone();
		}
	});
}

@Override
public void event(final EventUnwield eventUnwield) {
	pendingOperations.add(new EventResult() {
		@Override
		public void process() {
			if (eventUnwield.getCharacter().isPlayer()) {
				TendiwaUiStage.getInventory().update();
			}
			if (eventUnwield.getCharacter().getType().hasAspect(CharacterAspect.HUMANOID)) {
				gameScreen.getStage().getCharacterActor(eventUnwield.getCharacter()).updateTexture();
			}
			gameScreen.signalEventProcessingDone();
		}
	});
}

@Override
public void event(final EventItemFly eventItemFly) {
	pendingOperations.add(new EventResult() {
		@Override
		public void process() {
			ItemActor itemActor = gameScreen.getStage().obtainFlyingItemActor(
				eventItemFly.item,
				eventItemFly.fromX,
				eventItemFly.fromY,
				eventItemFly.toX,
				eventItemFly.toY
			);
			gameScreen.getStage().addActor(itemActor);

		}
	});
}
}
