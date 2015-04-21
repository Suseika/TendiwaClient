package org.tendiwa.client.extensions.std;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.tendiwa.client.*;
import org.tendiwa.client.ui.actors.CursorActor;
import org.tendiwa.client.ui.cellSelection.CellSelectionActor;
import org.tendiwa.client.ui.factories.CellSelectionFactory;
import org.tendiwa.client.ui.input.*;
import org.tendiwa.client.ui.model.CursorPosition;
import org.tendiwa.client.ui.model.MessageLog;
import org.tendiwa.client.ui.model.QuiveredItemHolder;
import org.tendiwa.core.*;
import org.tendiwa.core.Character;
import org.tendiwa.core.meta.Condition;
import org.tendiwa.core.volition.Volition;
import org.tendiwa.geometry.BasicCell;
import org.tendiwa.groovy.Registry;
import org.tendiwa.pathfinding.dijkstra.Paths;

import java.util.LinkedList;

import static com.badlogic.gdx.Input.Keys.*;

@Singleton
public class StdActions implements ActionsAdder {
private final Character player;
private final GameScreenViewport viewport;
private final MessageLog messageLog;
private final WorldMapScreen worldMapScreen;
private final ItemSelector itemSelector;
private final Screen gameScreen;
private final TaskManager taskManager;
private final Game game;
private final CellSelectionFactory cellSelectionFactory;
private final Volition volition;
private final ItemToKeyMapper<Item> mapper;
private final QuiveredItemHolder quiveredItemHolder;
private final CursorPosition cursorPosition;
private final CursorActor cursorActor;
private final GraphicsConfig graphicsConfig;
private final CellSelectionActor cellSelectionActor;

@Inject
StdActions(
	@Named("player") final Character player,
	final GameScreenViewport viewport,
	final MessageLog messageLog,
	final WorldMapScreen worldMapScreen,
	ItemSelector itemSelector,
	@Named("game") final Screen gameScreen,
	TaskManager taskManager,
	final Game game,
	final CellSelectionFactory cellSelectionFactory,
	Volition volition,
	ItemToKeyMapper<Item> mapper,
	QuiveredItemHolder quiveredItemHolder,
	CursorPosition cursorPosition,
	CursorActor cursorActor,
	GraphicsConfig graphicsConfig,
	CellSelectionActor cellSelectionActor
) {

	this.player = player;
	this.viewport = viewport;
	this.messageLog = messageLog;
	this.worldMapScreen = worldMapScreen;
	this.itemSelector = itemSelector;
	this.gameScreen = gameScreen;
	this.taskManager = taskManager;
	this.game = game;
	this.cellSelectionFactory = cellSelectionFactory;
	this.volition = volition;
	this.mapper = mapper;
	this.quiveredItemHolder = quiveredItemHolder;
	this.cursorPosition = cursorPosition;
	this.cursorActor = cursorActor;
	this.graphicsConfig = graphicsConfig;
	this.cellSelectionActor = cellSelectionActor;
}

@Override
public void addTo(InputToActionMapper actionMapper) {
	actionMapper.putAction(LEFT, new KeyboardAction("action.cameraMoveWest") {
		@Override
		public void act() {
			if (viewport.getStartCellX() > viewport.getCameraMoveStep() - 1) {
				viewport.centerCamera(viewport.getCenterPixelX() - GameScreen.TILE_SIZE, viewport.getCenterPixelY());
			}
		}
	});
	actionMapper.putAction(RIGHT, new KeyboardAction("action.cameraMoveEast") {
		@Override
		public void act() {
			if (viewport.getStartCellX() < viewport.getMaxStartX()) {
				viewport.centerCamera(viewport.getCenterPixelX() + GameScreen.TILE_SIZE, viewport.getCenterPixelY());
			}
		}
	});
	actionMapper.putAction(UP, new KeyboardAction("action.cameraMoveNorth") {
		@Override
		public void act() {
			if (viewport.getStartCellY() > viewport.getCameraMoveStep() - 1) {
				viewport.centerCamera(viewport.getCenterPixelX(), viewport.getCenterPixelY() - GameScreen.TILE_SIZE);
			}
		}
	});
	actionMapper.putAction(DOWN, new KeyboardAction("action.cameraMoveSouth") {
		@Override
		public void act() {
			if (viewport.getStartCellY() < viewport.getMaxStartY()) {
				viewport.centerCamera(viewport.getCenterPixelX(), viewport.getCenterPixelY() + GameScreen.TILE_SIZE);
			}
		}
	});
	actionMapper.putAction(H, new KeyboardAction("action.stepWest") {
		@Override
		public void act() {
			moveToOrAttackCharacterInCell(player.x() - 1, player.y());
		}
	});
	actionMapper.putAction(L, new KeyboardAction("action.stepEast") {
		@Override
		public void act() {
			moveToOrAttackCharacterInCell(player.x() + 1, player.y());
		}
	});
	actionMapper.putAction(J, new KeyboardAction("action.stepSouth") {
		@Override
		public void act() {
			moveToOrAttackCharacterInCell(player.x(), player.y() + 1);
		}

	});
	actionMapper.putAction(K, new KeyboardAction("action.stepNorth") {
		@Override
		public void act() {
			moveToOrAttackCharacterInCell(player.x(), player.y() - 1);
		}

	});
	actionMapper.putAction(Y, new KeyboardAction("action.stepNorthWest") {
		@Override
		public void act() {
			moveToOrAttackCharacterInCell(player.x() - 1, player.y() - 1);
		}
	});
	actionMapper.putAction(U, new KeyboardAction("action.stepNorthEast") {
		@Override
		public void act() {
			moveToOrAttackCharacterInCell(player.x() + 1, player.y() - 1);
		}
	});

	actionMapper.putAction(B, new KeyboardAction("action.stepSouthWest") {
		@Override
		public void act() {
			moveToOrAttackCharacterInCell(player.x() - 1, player.y() + 1);
		}
	});
	actionMapper.putAction(N, new KeyboardAction("action.stepSouthEast") {
		@Override
		public void act() {
			moveToOrAttackCharacterInCell(player.x() + 1, player.y() + 1);
		}
	});
	actionMapper.putAction(F9, new KeyboardAction("action.toggleWorldMapScreen") {
		@Override
		public void act() {
			if (game.getScreen() == gameScreen) {
				game.setScreen(worldMapScreen);
			} else {
				game.setScreen(gameScreen);
			}
		}
	});
	actionMapper.putAction(F10, new KeyboardAction("action.toggleAnimations") {
		@Override
		public void act() {
			graphicsConfig.toggleAnimations();
			messageLog.pushMessage("Animations " + (graphicsConfig.animationsEnabled ? "enabled" : "disabled") + ".");
		}
	});
	actionMapper.putAction(F11, new KeyboardAction("action.toggleStatusBar") {
		@Override
		public void act() {
			graphicsConfig.toggleStatusBar();
		}
	});
	actionMapper.putAction(G, new KeyboardAction("action.pickUp") {
		@Override
		public void act() {
			if (player.getPlane().hasAnyItems(player.x(), player.y())) {
				volition.pickUp();
			}
		}
	});
	actionMapper.putAction(Modifiers.shift + Q, new KeyboardAction("action.quiver") {
		@Override
		public void act() {
			mapper.update(player.getInventory());
			itemSelector.startSelection(mapper, new EntityFilter<Item>() {
					@Override
					public boolean check(Item entity) {
						return Items.isShootable(entity.getType());
					}
				}, new EntitySelectionListener<Item>() {
					@Override
					public void execute(Item item) {
						quiveredItemHolder.setItem(item);
						game.setScreen(gameScreen);
					}
				}
			);
		}
	});
	actionMapper.putAction(T, new KeyboardAction("action.throw") {
		@Override
		public void act() {
			mapper.update(player.getInventory());
			itemSelector.startSelection(mapper,
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
						cursorActor.setVisible(false);
						cellSelectionActor.setVisible(true);
						cellSelectionFactory.create(
							new EntitySelectionListener<BasicCell>() {
								@Override
								public void execute(BasicCell point) {
									volition.propel(item.takeSingleItem(), point.x(), point.y());
								}
							},
							new Runnable() {
								@Override
								public void run() {
									System.out.println("run");
									cursorActor.setVisible(true);
									cellSelectionActor.setVisible(false);
								}
							}
						).start();
					}
				}
			);
		}
	});
	actionMapper.putAction(F, new KeyboardAction("action.fire") {
		@Override
		public void act() {
			final UniqueItem rangedWeapon = (UniqueItem) player.getEquipment().getWieldedWeaponThatIs(new Condition<Item>() {
				@Override
				public boolean check(Item item) {
					return Items.isRangedWeapon(item.getType());
				}
			});
			final Item quiveredItem = quiveredItemHolder.getItem();
			if (rangedWeapon != null && quiveredItem != null && Items.isShootable(quiveredItem.getType())) {
				final Shootable shootable = Items.asShootable(quiveredItem.getType());
				if (shootable.getAmmunitionType() == (Items.asRangedWeapon(rangedWeapon.getType())).getAmmunitionType()) {
					cursorActor.setVisible(false);
					cellSelectionActor.setVisible(true);
					cellSelectionFactory.create(
						new EntitySelectionListener<BasicCell>() {
							@Override
							public void execute(BasicCell point) {
								volition.shoot(rangedWeapon, quiveredItem.takeSingleItem(), point.x(), point.y());
							}
						},
						new Runnable() {
							@Override
							public void run() {
								cursorActor.setVisible(true);
								cellSelectionActor.setVisible(false);
							}
						}
					).start();
				}
			}
		}
	});
	actionMapper.putAction(W, new KeyboardAction("action.wield") {
		@Override
		public void act() {
			mapper.update(player.getInventory());
			itemSelector.startSelection(mapper,
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
	actionMapper.putAction(Modifiers.shift + W, new KeyboardAction("action.wear") {
		@Override
		public void act() {
			mapper.update(player.getInventory());
			itemSelector.startSelection(mapper, new EntityFilter<Item>() {
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
	actionMapper.putAction(Modifiers.shift + S, new KeyboardAction("action.shout") {
		@Override
		public void act() {
			volition.actionWithoutTarget(
				(ActionWithoutTarget) Registry.characterAbilities.get("shout").getAction()
			);
		}
	});
	actionMapper.putAction(S, new KeyboardAction("action.idle") {
		@Override
		public void act() {
			volition.idle();
		}
	});
	actionMapper.putMouseAction(Input.Buttons.LEFT, new MouseAction("actions.mouse.go_or_attack") {
		@Override
		public void act(int screenX, int screenY) {
			final int cellX = (viewport.getStartPixelX() + screenX) / GameScreen.TILE_SIZE;
			final int cellY = (viewport.getStartPixelY() + screenY) / GameScreen.TILE_SIZE;
			if (cellX == player.x() && cellY == player.y()) {
				return;
			}
			LinkedList<BasicCell> path = Paths.getPath(
				player.x(), player.y(),
				cellX, cellY,
				player.getPathWalkerOverCharacters(),
				100
			);
			if (path == null || path.size() == 0) {
				return;
			}
			taskManager.trySettingTask(new Task() {
				public boolean forcedEnd = false;

				@Override
				public boolean ended() {
					return forcedEnd || player.x() == cellX && player.y() == cellY;
				}

				@Override
				public void execute() {
					LinkedList<BasicCell> path = Paths.getPath(
						player.x(), player.y(),
						cellX, cellY,
						player.getPathWalkerOverCharacters(),
						100
					);
					if (path == null) {
						forcedEnd = true;
						return;
					}
					if (!path.isEmpty()) {
						BasicCell nextStep = path.removeFirst();
						moveToOrAttackCharacterInCell(nextStep.x(), nextStep.y());
					}
				}
			});
		}
	});
	actionMapper.putMouseMovedAction(new MouseAction("ui.actions.cell_selection.mouse_moved") {
		@Override
		public void act(int screenX, int screenY) {
			BasicCell point = viewport.screenPixelToWorldCell(screenX, screenY);
			if (!cursorPosition.getPoint().equals(point)) {
				cursorPosition.setPoint(point);
			}
		}
	});
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
	int dx = player.x() - x;
	int dy = player.y() - y;
	assert Math.abs(dx) <= 1 && Math.abs(dy) <= 1;
	if (player.canStepOn(x, y)) {
		volition.move(Directions.shiftToDirection(-dx, -dy));
	} else {
		Character aim = player.getPlane().getCharacter(x, y);
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
