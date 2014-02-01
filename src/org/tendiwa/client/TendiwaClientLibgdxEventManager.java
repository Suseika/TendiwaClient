package org.tendiwa.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction;
import com.badlogic.gdx.scenes.scene2d.actions.MoveByAction;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.google.inject.Inject;
import org.tendiwa.client.rendering.effects.Blood;
import org.tendiwa.client.rendering.markers.BorderMarker;
import org.tendiwa.client.ui.model.MessageLog;
import org.tendiwa.client.ui.widgets.UiHealthBar;
import org.tendiwa.core.*;
import org.tendiwa.core.events.*;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.*;

/**
 * On each received {@link org.tendiwa.core.observation.Event} this class creates a {@link EventResult} pending operation and placed
 * it in a queue. Each time the game is rendered, all EventResults are processed inside {@link
 * GameScreen#render(float)}.
 */
public class TendiwaClientLibgdxEventManager implements TendiwaClientEventManager, EventResultProvider {
private final EventProcessor eventProcessor;
private final Game game;
private GameScreen gameScreen;
private EventResult pendingOperation;
private MessageLog messageLog;

@Inject
TendiwaClientLibgdxEventManager(MessageLog messageLog, EventProcessor eventProcessor, GameScreen gameScreen, Game game) {
	this.messageLog = messageLog;
	this.eventProcessor = eventProcessor;
	this.gameScreen = gameScreen;
	this.game = game;
}

public void toggleAnimations() {
	gameScreen.getConfig().toggleAnimations();
}

private void setPendingOperation(EventResult eventResult) {
	assert pendingOperation == null;
	pendingOperation = eventResult;
}

@Override
public void event(final EventMove e) {
	setPendingOperation(new EventResult() {
		@Override
		public String toString() {
			return "result move";
		}

		@Override
		public void process() {
			Actor characterActor = gameScreen.getStage().getCharacterActor(e.character);
			int index = e.character.getY() * Tendiwa.getWorldWidth() + e.character.getX();
			gameScreen.getStage().sortActorsByY();

			if (gameScreen.getConfig().animationsEnabled) {
				Action action;
				if (e.movingStyle == MovingStyle.STEP) {
					action = new MoveToAction();
					((MoveToAction) action).setPosition(e.character.getX(), e.character.getY());
					((MoveToAction) action).setDuration(0.1f);
				} else if (e.movingStyle == MovingStyle.LEAP) {
					MoveByAction moveTo = new MoveByAction();
					moveTo.setAmount(e.character.getX() - e.xPrev, e.character.getY() - e.yPrev);
					float lengthMovingDuration = 0.3f;
					moveTo.setDuration(lengthMovingDuration);
					MoveByAction moveUp = moveBy(0, -1, lengthMovingDuration / 2);
					moveUp.setInterpolation(Interpolation.exp5Out);
					MoveByAction moveDown = moveBy(0, 1, lengthMovingDuration / 2);
					moveDown.setInterpolation(Interpolation.exp5In);
					Action upAndDown = sequence(moveUp, moveDown);
					action = parallel(moveTo, upAndDown);
				} else {
					action = moveTo(e.character.getX(), e.character.getY(), 0.1f);
				}
				Action sequence = sequence(action, run(new Runnable() {
					@Override
					public void run() {
						gameScreen.getStage().updateCharactersVisibility();
						eventProcessor.processOneMoreEventInCurrentFrame();
						eventProcessor.signalEventProcessingDone();
					}
				}));
				characterActor.addAction(sequence);
			} else {
				characterActor.setX(e.character.getX());
				characterActor.setY(e.character.getY());
				gameScreen.getStage().updateCharactersVisibility();
				if (e.character.isPlayer()) {
					// If this is player moving, then the next event will be
					// EventFovChange, and to prevent flickering we make the current event
					// render in the same frame as the previous event.
					eventProcessor.processOneMoreEventInCurrentFrame();
				}
				eventProcessor.signalEventProcessingDone();
			}
		}
	});
}

@Override
public void event(final EventSay e) {
	setPendingOperation(new EventResult() {
		@Override
		public void process() {

		}
	});
}

@Override
public void event(final EventFovChange e) {
	setPendingOperation(new EventResult() {
		@Override
		public String toString() {
			return "result fov change";
		}

		@Override
		public void process() {
			gameScreen.getRenderPlane().updateFieldOfView(e);
			gameScreen.getStage().getMarkersRegistry().clear();
			for (Integer coord : e.unseenCells) {
//				if (gameScreen.getCurrentBackendPlane().hasWall(cell.x, cell.y)) {
//					gameScreen.getStage().removeWallActor(cell.x, cell.y);
//				}
			}
			for (RenderCell cell : e.seenCells) {
				assert gameScreen.getCurrentBackendPlane().containsCell(cell.x, cell.y) : cell;
				if (gameScreen.getCurrentBackendPlane().hasWall(cell.x, cell.y)) {
					if (!gameScreen.getStage().hasWallActor(cell.x, cell.y)) {
						gameScreen.getStage().addWallActor(cell.x, cell.y);
					}
				} else if (gameScreen.getCurrentBackendPlane().hasObject(cell.x, cell.y)) {
					gameScreen.getStage().addObjectActor(cell.x, cell.y);
				}
			}
			for (RenderBorder border : e.seenBorders) {
				gameScreen.getStage().getMarkersRegistry().add(new BorderMarker(border));
				if (!gameScreen.renderPlane.hasUnseenBorderObject(border)) {
					if (border.getObject() != null) {
						gameScreen.getStage().addBorderObjectActor(border);
					}
				}
			}
			for (Border border : e.unseenBorders) {
			}
//			gameScreen.processOneMoreEventInCurrentFrame();
			eventProcessor.signalEventProcessingDone();
		}
	});
}

@Override
public void event(final EventInitialTerrain e) {
	setPendingOperation(new EventResult() {
		@Override
		public String toString() {
			return "result initial terrain";
		}

		@Override
		public void process() {
			System.out.println("process");
			gameScreen.setCurrentPlane(gameScreen.backendWorld.getPlane(e.zLevel));
			gameScreen.getRenderPlane().initFieldOfView(e);
			game.setScreen(gameScreen);
			for (RenderCell cell : e.seenCells) {
				gameScreen.getRenderPlane().seeCell(cell);
				HorizontalPlane plane = gameScreen.getCurrentBackendPlane();
				if (plane.hasWall(cell.x, cell.y)) {
					gameScreen.getStage().addWallActor(cell.x, cell.y);
				} else if (plane.hasObject(cell.x, cell.y)) {
					gameScreen.getStage().addObjectActor(cell.x, cell.y);
				}

			}
			for (RenderBorder border : e.seenBorders) {
				if (border.getObject() != null) {
					gameScreen.getStage().addBorderObjectActor(border);
				}
			}
			eventProcessor.signalEventProcessingDone();
		}
	});
}

@Override
public void event(final EventItemDisappear eventItemDisappear) {
	setPendingOperation(new EventResult() {
		@Override
		public void process() {
			Actor actor = gameScreen.getStage().obtainItemActor(
				eventItemDisappear.x,
				eventItemDisappear.y,
				eventItemDisappear.item
			);
			if (gameScreen.getConfig().animationsEnabled) {
				AlphaAction alphaAction = new AlphaAction();
				alphaAction.setAlpha(0.0f);
				alphaAction.setDuration(0.1f);
				Action sequence = sequence(alphaAction, run(new Runnable() {
					@Override
					public void run() {
						gameScreen.getStage().removeItemActor(eventItemDisappear.item);
						eventProcessor.signalEventProcessingDone();
					}
				}));
				actor.addAction(sequence);
				gameScreen.getStage().addActor(actor);
			} else {
				gameScreen.getStage().removeItemActor(eventItemDisappear.item);
				eventProcessor.signalEventProcessingDone();
			}
		}
	});
}

@Override
public void event(final EventGetItem eventGetItem) {
	setPendingOperation(new EventResult() {
		@Override
		public void process() {
			uiUpdater.update(UiPortion.INVENTORY);
			eventProcessor.signalEventProcessingDone();
		}
	});
}

@Override
public void event(EventLoseItem eventLoseItem) {
	setPendingOperation(new EventResult() {
		@Override
		public void process() {
			uiUpdater.update(UiPortion.INVENTORY);
			eventProcessor.signalEventProcessingDone();
		}
	});
}

@Override
public void event(EventItemAppear eventItemAppear) {
}

@Override
public void event(final EventPutOn eventPutOn) {
	setPendingOperation(new EventResult() {
		@Override
		public void process() {
			if (eventPutOn.getCharacter().isPlayer()) {
				uiUpdater.update(UiPortion.INVENTORY);
			}
			if (eventPutOn.getCharacter().getType().hasAspect(CharacterAspect.HUMANOID)) {
				gameScreen.getStage().getCharacterActor(eventPutOn.getCharacter()).updateTexture();
			}
			eventProcessor.signalEventProcessingDone();
		}
	});
}

@Override
public void event(final EventWield eventWield) {
	setPendingOperation(new EventResult() {
		@Override
		public void process() {
			if (eventWield.getCharacter().isPlayer()) {
				uiUpdater.update(UiPortion.INVENTORY);
			}
			if (eventWield.getCharacter().getType().hasAspect(CharacterAspect.HUMANOID)) {
				gameScreen.getStage().getCharacterActor(eventWield.getCharacter()).updateTexture();
			}
			eventProcessor.signalEventProcessingDone();
		}
	});
}

@Override
public void event(final EventTakeOff eventTakeOff) {
	setPendingOperation(new EventResult() {
		@Override
		public void process() {
			if (eventTakeOff.getCharacter().isPlayer()) {
				uiUpdater.update(UiPortion.INVENTORY);
			}
			if (eventTakeOff.getCharacter().getType().hasAspect(CharacterAspect.HUMANOID)) {
				gameScreen.getStage().getCharacterActor(eventTakeOff.getCharacter()).updateTexture();
			}
			eventProcessor.signalEventProcessingDone();
		}
	});
}

@Override
public void event(final EventUnwield e) {
	setPendingOperation(new EventResult() {
		@Override
		public void process() {
			if (e.getCharacter().isPlayer()) {
				uiUpdater.update(UiPortion.INVENTORY);
			}
			if (e.getCharacter().getType().hasAspect(CharacterAspect.HUMANOID)) {
				gameScreen.getStage().getCharacterActor(e.getCharacter()).updateTexture();
			}
			eventProcessor.signalEventProcessingDone();
		}
	});
}

@Override
public void event(final EventProjectileFly e) {
	setPendingOperation(new EventResult() {
		@Override
		public void process() {
			com.badlogic.gdx.scenes.scene2d.Actor actor = gameScreen.getStage().obtainFlyingProjectileActor(
				e.item,
				e.fromX,
				e.fromY,
				e.toX,
				e.toY,
				e.style
			);
			gameScreen.getStage().addActor(actor);
		}
	});
}

@Override
public void event(final EventSound eventSound) {
	setPendingOperation(new EventResult() {
		@Override
		public void process() {
			com.badlogic.gdx.scenes.scene2d.Actor actor = gameScreen.getStage().obtainSoundActor(
				eventSound.sound,
				eventSound.x,
				eventSound.y
			);
			if (eventSound.source == null) {
				messageLog.pushMessage(
					Languages.getText("events.sound_from_cell", eventSound.sound)
				);
			} else if (eventSound.source == Tendiwa.getPlayerCharacter()) {
				messageLog.pushMessage(
					Languages.getText("events.sound_from_player", eventSound.sound)
				);
			} else {
				messageLog.pushMessage(
					Languages.getText("events.sound_from_source", eventSound.source, eventSound.sound)
				);
			}
			gameScreen.getStage().addActor(actor);
		}
	});
}

@Override
public void event(final EventExplosion e) {
	setPendingOperation(new EventResult() {
		@Override
		public void process() {
//			com.badlogic.gdx.scenes.scene2d.Actor explosionActor = gameScreen.getStage().obtainExplosionActor(
//				e.x,
//				e.y
//			);
//			gameScreen.getStage().addActor(explosionActor);
			eventProcessor.signalEventProcessingDone();
		}
	});
}

@Override
public void event(final EventGetDamage e) {
	setPendingOperation(new EventResult() {
		@Override
		public void process() {
			messageLog.pushMessage(
				Languages.getText("log.get_damage", e.damageSource, e.damageType, e.character)
			);
			final Actor blood = new Blood(e.character.getX(), e.character.getY());
			blood.addAction(sequence(delay(0.3f), run(new Runnable() {
				@Override
				public void run() {
					gameScreen.getStage().getRoot().removeActor(blood);
				}
			})));
			gameScreen.getStage().addActor(blood);
			eventProcessor.signalEventProcessingDone();
		}
	});
}

@Override
public void event(final EventAttack e) {
	setPendingOperation(new EventResult() {
		@Override
		public void process() {
			CharacterActor characterActor = gameScreen.getStage().getCharacterActor(e.attacker);
			float dx = e.aim.getX() - e.attacker.getX();
			float dy = e.aim.getY() - e.attacker.getY();
			characterActor.addAction(sequence(
				moveBy(-dx * 0.2f, -dy * 0.2f, 0.1f),
				moveBy(dx * 0.7f, dy * 0.7f, 0.1f),
				run(new Runnable() {
					@Override
					public void run() {
						eventProcessor.signalEventProcessingDone();
					}
				}),
				moveBy(-dx * 0.5f, -dy * 0.5f, 0.2f)
			));
		}
	});
}

@Override
public void event(final EventDie e) {
	setPendingOperation(new EventResult() {
		@Override
		public void process() {
			CharacterActor characterActor = gameScreen.getStage().getCharacterActor(e.character);
			gameScreen.getStage().getRoot().removeActor(characterActor);
			messageLog.pushMessage(
				Languages.getText("log.death", e.character)
			);
			eventProcessor.signalEventProcessingDone();
		}
	});
}

@Override
public void event(final EventMoveToPlane e) {
	setPendingOperation(new EventResult() {

		@Override
		public void process() {
			gameScreen.getRenderPlane().unseeAllCells();
			gameScreen.getStage().removeActorsOfPlane(gameScreen.getCurrentBackendPlane().getLevel());
			gameScreen.setCurrentPlane(gameScreen.getWorld().getPlane(e.zLevel));
			for (RenderCell cell : e.seenCells) {
				gameScreen.getRenderPlane().seeCell(cell);
				if (gameScreen.getRenderPlane().hasAnyUnseenItems(cell.x, cell.y)) {
					gameScreen.getRenderPlane().removeUnseenItems(cell.x, cell.y);
				}
				if (gameScreen.getCurrentBackendPlane().hasWall(cell.x, cell.y)) {
					if (!gameScreen.getStage().hasWallActor(cell.x, cell.y)) {
						gameScreen.getStage().addWallActor(cell.x, cell.y);
					}
				} else if (gameScreen.getCurrentBackendPlane().hasObject(cell.x, cell.y)) {
					gameScreen.getStage().addObjectActor(cell.x, cell.y);
				}
			}
			eventProcessor.signalEventProcessingDone();
		}
	});
}

@Override
public EventResult provideEventResult() {
	assert pendingOperation != null;
	EventResult answer = pendingOperation;
	pendingOperation = null;
	return answer;
}

@Override
public boolean hasResultPending() {
	return pendingOperation != null;
}
}
