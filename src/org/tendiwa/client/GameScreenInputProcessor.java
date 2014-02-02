package org.tendiwa.client;

import com.badlogic.gdx.Game;
import com.google.inject.Inject;
import org.tendiwa.client.ui.actors.CellSelectionActor;
import org.tendiwa.client.ui.controller.CellSelection;
import org.tendiwa.client.ui.model.CursorPosition;
import org.tendiwa.client.ui.model.MessageLog;
import org.tendiwa.client.ui.uiModes.UiModeManager;
import org.tendiwa.core.*;
import org.tendiwa.core.Character;
import org.tendiwa.core.meta.Condition;
import org.tendiwa.groovy.Registry;

import java.util.LinkedList;

import static com.badlogic.gdx.Input.Keys.*;

final class GameScreenInputProcessor extends TendiwaInputProcessor {

private final Volition volition;
private final Character player;
private final GameScreenViewport viewport;
private final WorldMapScreen worldMapScreen;
private ItemToKeyMapper<Item> mapper = new ItemToKeyMapper<>();
private ItemSelector itemSelector;
private CursorPosition cellSelection;

@Inject
public GameScreenInputProcessor(final Volition volition, final Character player, final GameScreenViewport viewport, final CursorPosition cursorPosition, final UiModeManager uiModeManager, final CellSelectionActor cellSelectionActor, final MessageLog messageLog, final WorldMapScreen worldMapScreen, CursorPosition cellSelection, ItemSelector itemSelector, final GameScreen gameScreen, TaskManager taskManager, EventProcessor eventProcessor, final Game game) {
	super(gameScreen, taskManager, eventProcessor);
	this.volition = volition;
	this.player = player;
	this.viewport = viewport;
	this.worldMapScreen = worldMapScreen;
	this.cellSelection = cellSelection;
	this.itemSelector = itemSelector;
	putAction(LEFT, new UiAction("action.cameraMoveWest") {
		@Override
		public void act() {
			if (viewport.getStartCellX() > viewport.getCameraMoveStep() - 1) {
				viewport.centerCamera(viewport.getCenterPixelX() - GameScreen.TILE_SIZE, viewport.getCenterPixelY());
			}
		}
	});
	putAction(RIGHT, new UiAction("action.cameraMoveEast") {
		@Override
		public void act() {
			if (viewport.getStartCellX() < viewport.getMaxStartX()) {
				viewport.centerCamera(viewport.getCenterPixelX() + GameScreen.TILE_SIZE, viewport.getCenterPixelY());
			}
		}
	});
	putAction(UP, new UiAction("action.cameraMoveNorth") {
		@Override
		public void act() {
			if (viewport.getStartCellY() > viewport.getCameraMoveStep() - 1) {
				viewport.centerCamera(viewport.getCenterPixelX(), viewport.getCenterPixelY() - GameScreen.TILE_SIZE);
			}
		}
	});
	putAction(DOWN, new UiAction("action.cameraMoveSouth") {
		@Override
		public void act() {
			if (viewport.getStartCellY() < viewport.getMaxStartY()) {
				viewport.centerCamera(viewport.getCenterPixelX(), viewport.getCenterPixelY() + GameScreen.TILE_SIZE);
			}
		}
	});
	putAction(H, new UiAction("action.stepWest") {
		@Override
		public void act() {
			moveToOrAttackCharacterInCell(player.getX() - 1, player.getY());
		}
	});
	putAction(L, new UiAction("action.stepEast") {
		@Override
		public void act() {
			moveToOrAttackCharacterInCell(player.getX() + 1, player.getY());
		}
	});
	putAction(J, new UiAction("action.stepSouth") {
		@Override
		public void act() {
			moveToOrAttackCharacterInCell(player.getX(), player.getY() + 1);
		}

	});
	putAction(K, new UiAction("action.stepNorth") {
		@Override
		public void act() {
			moveToOrAttackCharacterInCell(player.getX(), player.getY() - 1);
		}

	});
	putAction(Y, new UiAction("action.stepNorthWest") {
		@Override
		public void act() {
			moveToOrAttackCharacterInCell(player.getX() - 1, player.getY() - 1);
		}
	});
	putAction(U, new UiAction("action.stepNorthEast") {
		@Override
		public void act() {
			moveToOrAttackCharacterInCell(player.getX() + 1, player.getY() - 1);
		}
	});

	putAction(B, new UiAction("action.stepSouthWest") {
		@Override
		public void act() {
			moveToOrAttackCharacterInCell(player.getX() - 1, player.getY() + 1);
		}
	});
	putAction(N, new UiAction("action.stepSouthEast") {
		@Override
		public void act() {
			moveToOrAttackCharacterInCell(player.getX() + 1, player.getY() + 1);
		}
	});
	putAction(F9, new UiAction("action.toggleWorldMapScreen") {
		@Override
		public void act() {
			if (game.getScreen() == gameScreen) {
				game.setScreen(worldMapScreen);
			} else {
				game.setScreen(gameScreen);
			}
		}
	});
	putAction(F10, new UiAction("action.toggleAnimations") {
		@Override
		public void act() {
			gameScreen.getConfig().toggleAnimations();
			messageLog.pushMessage("Animations " + (gameScreen.getConfig().animationsEnabled ? "enabled" : "disabled") + ".");
		}
	});
	putAction(F11, new UiAction("action.toggleStatusBar") {
		@Override
		public void act() {
			gameScreen.toggleStatusbar();
		}
	});
	putAction(G, new UiAction("action.pickUp") {
		@Override
		public void act() {
			if (player.getPlane().hasAnyItems(player.getX(), player.getY())) {
				volition.pickUp();
			}
		}
	});
	putAction(shift + Q, new UiAction("action.quiver") {
		@Override
		public void act() {
			mapper.update(player.getInventory());
			GameScreenInputProcessor.this.itemSelector.startSelection(mapper, new EntityFilter<Item>() {
					@Override
					public boolean check(Item entity) {
						return Items.isShootable(entity.getType());
					}
				}, new EntitySelectionListener<Item>() {
					@Override
					public void execute(Item item) {
						QuiveredItemHolder.setItem(item);
						game.setScreen(gameScreen);
					}
				}
			);
		}
	});
	putAction(T, new UiAction("action.throw") {
		@Override
		public void act() {
			mapper.update(player.getInventory());
			GameScreenInputProcessor.this.itemSelector.startSelection(mapper,
				new EntityFilter<Item>() {
					@Override
					public boolean check(Item entity) {
						return true;
					}
				},
				new EntitySelectionListener<Item>() {
					@Override
					public void execute(final Item item) {
						game.setScreen(gameScreen);
						uiModeManager.pushMode(
							new CellSelection(gameScreen, cursorPosition, cellSelectionActor, new EntitySelectionListener<EnhancedPoint>() {
								@Override
								public void execute(EnhancedPoint point) {
									volition.propel(item.takeSingleItem(), point.x, point.y);
								}
							})
						);
					}
				}
			);
		}
	});
	putAction(F, new UiAction("action.fire") {
		@Override
		public void act() {
			final UniqueItem rangedWeapon = (UniqueItem) player.getEquipment().getWieldedWeaponThatIs(new Condition<Item>() {
				@Override
				public boolean check(Item item) {
					return Items.isRangedWeapon(item.getType());
				}
			});
			final Item quiveredItem = QuiveredItemHolder.getItem();
			if (rangedWeapon != null && quiveredItem != null && Items.isShootable(quiveredItem.getType())) {
				final Shootable shootable = Items.asShootable(quiveredItem.getType());
				if (shootable.getAmmunitionType() == (Items.asRangedWeapon(rangedWeapon.getType())).getAmmunitionType()) {
					uiModeManager.pushMode(
						new CellSelection(gameScreen, cursorPosition, cellSelectionActor, new EntitySelectionListener<EnhancedPoint>() {
							@Override
							public void execute(EnhancedPoint point) {
								volition.shoot(rangedWeapon, quiveredItem.takeSingleItem(), point.x, point.y);
							}
						})
					);
				}
			}
		}
	});
	putAction(W, new UiAction("action.wield") {
		@Override
		public void act() {
			mapper.update(player.getInventory());
			GameScreenInputProcessor.this.itemSelector.startSelection(mapper,
				new EntityFilter<Item>() {
					@Override
					public boolean check(Item entity) {
						return Items.isWieldable(entity.getType());
					}
				},
				new EntitySelectionListener<Item>() {
					@Override
					public void execute(Item item) {
						volition.wield(item);
					}
				}
			);
		}
	});
	putAction(shift + W, new UiAction("action.wear") {
		@Override
		public void act() {
			mapper.update(player.getInventory());
			GameScreenInputProcessor.this.itemSelector.startSelection(mapper, new EntityFilter<Item>() {
					@Override
					public boolean check(Item entity) {
						return Items.isWearable(entity.getType());
					}
				}, new EntitySelectionListener<Item>() {
					@Override
					public void execute(Item item) {
						volition.putOn((UniqueItem) item);
					}
				}
			);
		}
	});
	putAction(shift + S, new UiAction("action.shout") {
		@Override
		public void act() {
			volition.actionWithoutTarget(
				(ActionWithoutTarget) Registry.characterAbilities.get("shout").getAction()
			);
		}
	});
	putAction(S, new UiAction("action.idle") {
		@Override
		public void act() {
			volition.idle();
		}
	});
}

@Override
public boolean touchDown(int screenX, int screenY, int pointer, int button) {
	if (taskManager.hasCurrentTask()) {
		return false;
	}
	final int cellX = (viewport.getStartPixelX() + screenX) / GameScreen.TILE_SIZE;
	final int cellY = (viewport.getStartPixelY() + screenY) / GameScreen.TILE_SIZE;
	if (cellX == gameScreen.player.getX() && cellY == gameScreen.player.getY()) {
		return true;
	}
	LinkedList<EnhancedPoint> path = Paths.getPath(
		player.getX(), player.getY(),
		cellX, cellY,
		player.getPathWalkerOverCharacters(),
		100
	);
	if (path == null || path.size() == 0) {
		return true;
	}
	taskManager.trySettingTask(new Task() {
		public boolean forcedEnd = false;

		@Override
		public boolean ended() {
			return forcedEnd || gameScreen.player.getX() == cellX && gameScreen.player.getY() == cellY;
		}

		@Override
		public void execute() {
			LinkedList<EnhancedPoint> path = Paths.getPath(
				player.getX(), player.getY(),
				cellX, cellY,
				player.getPathWalkerOverCharacters(),
				100
			);
			if (path == null) {
				forcedEnd = true;
				return;
			}
			if (!path.isEmpty()) {
				EnhancedPoint nextStep = path.removeFirst();
				moveToOrAttackCharacterInCell(nextStep.x, nextStep.y);
			}
		}
	});
	return true;
}

/**
 * Is there is an enemy in a cell x:y, then this method will push a request to attack that enemy, otherwise this method
 * will push a request to move to that cell.
 *
 * @param x
 * 	X coordinate in world coordinates of a cell to move-attack to.
 * @param y
 * 	Y coordinate in world coordinates of a cell to move-attack to.
 */
private void moveToOrAttackCharacterInCell(int x, int y) {
	// Only neighbor cells are allowed here
	int dx = player.getX() - x;
	int dy = player.getY() - y;
	assert Math.abs(dx) <= 1 && Math.abs(dy) <= 1;
	if (player.canStepOn(x, y)) {
		volition.move(Directions.shiftToDirection(-dx, -dy));
	} else {
		Character aim = gameScreen.getCurrentBackendPlane().getCharacter(x, y);
		boolean isCharacterPresent = aim != null;
		if (isCharacterPresent) {
			boolean isHeAnEnemy = aim.isEnemy(player);
			if (isHeAnEnemy) {
				volition.attack(aim);
			} else {
				assert false : "This should not happen";
			}
		} else {
			assert false : "This should not happen";
		}
	}
}
}
