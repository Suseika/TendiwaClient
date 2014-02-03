package org.tendiwa.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.google.common.collect.ImmutableList;
import org.tendiwa.core.Character;
import org.tendiwa.core.Server;
import org.tendiwa.core.Tendiwa;
import org.tendiwa.core.World;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class TendiwaInputProcessor implements InputProcessor {
public static final int ctrl = 1 << 8;
public static final int alt = 1 << 9;
public static final int shift = 1 << 10;
protected final TaskManager taskManager;
final GameScreen gameScreen;
private Map<KeyCombination, UiAction> combinationToAction = new HashMap<>();
/**
 * Contains same data as {@link TendiwaInputProcessor#combinationToAction}, but in form of list (so it has a defined
 * order and in generally easier to iterate over).
 */
private List<Mapping> mappings = new LinkedList<>();

TendiwaInputProcessor(GameScreen gameScreen, TaskManager taskManager) {
	this.gameScreen = gameScreen;
	this.taskManager = taskManager;
}

public static ImmutableList<Mapping> getCurrentMappings() {
	InputProcessor mainInputProcessor = Gdx.input.getInputProcessor();
	if (mainInputProcessor == null) {
		throw new RuntimeException("No InputProcessor was set yet");
	} else if (mainInputProcessor instanceof TendiwaInputProcessor) {
		return ((TendiwaInputProcessor) mainInputProcessor).getMappings();
	} else if (mainInputProcessor instanceof InputMultiplexer) {
		for (InputProcessor processor : ((InputMultiplexer) mainInputProcessor).getProcessors()) {
			if (processor instanceof TendiwaInputProcessor) {
				return ((TendiwaInputProcessor) processor).getMappings();
			}
		}
		throw new RuntimeException("Can't get current mappings");
	} else {
		throw new RuntimeException("Can't get current mappings");
	}
}

/**
 * Maps a key combination to an action so action will be executed when that key combination is pressed.
 *
 * @param combination
 * 	An integer which is a sum of 4 parameters: <ul><li>Keycode (from {@link Input.Keys})</li><li>{@code isCtrl ? 1 << 8
 * 	: 0}</li><li>{@code isAlt ? 1 << 9 : 0}</li><li>{@code isShift ? 1 << 10 : 0}</li></ul>
 */
public void putAction(int combination, UiAction action) {
	Mapping mapping = new Mapping(KeyCombinationPool.obtainCombination(combination), action);
	assert !combinationToAction.containsKey(mapping.getCombination());
	combinationToAction.put(mapping.getCombination(), mapping.getAction());
	mappings.add(mapping);
}

@Override
public boolean keyDown(int keycode) {
	if (keycode == Input.Keys.ESCAPE && taskManager.hasCurrentTask()) {
		taskManager.cancelCurrentTask();
	}
	if (Server.hasRequestToProcess()) {
		return false;
	}
	switch (keycode) {
		case Input.Keys.SHIFT_LEFT:
		case Input.Keys.SHIFT_RIGHT:
		case Input.Keys.ALT_LEFT:
		case Input.Keys.ALT_RIGHT:
		case Input.Keys.CONTROL_LEFT:
		case Input.Keys.CONTROL_RIGHT:
			return false;
	}
	KeyCombination combination = KeyCombinationPool.obtainCombination(
		keycode,
		Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT),
		Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT),
		Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)
	);
	UiAction action = combinationToAction.get(combination);
	if (action == null) {
		return false;
	} else {
		action.act();
		return true;
	}
}

@Override
public boolean keyUp(int keycode) {
	return false;
}

@Override
public boolean keyTyped(char character) {
	return false;
}

@Override
public boolean touchUp(int screenX, int screenY, int pointer, int button) {
	return false;
}

@Override
public boolean touchDragged(int screenX, int screenY, int pointer) {
	return false;
}

@Override
public boolean mouseMoved(int screenX, int screenY) {
	return false;
}

@Override
public boolean scrolled(int amount) {
	return false;
}

public ImmutableList<Mapping> getMappings() {
	return ImmutableList.copyOf(mappings);
}

private static class KeyCombinationPool {
	static Map<Integer, KeyCombination> combinations = new HashMap<>();

	static KeyCombination obtainCombination(int keycode, boolean ctrl, boolean alt, boolean shift) {
		int compositeKeyCode = computeCompositeKeyCode(keycode, ctrl, alt, shift);
		if (combinations.containsKey(compositeKeyCode)) {
			return combinations.get(compositeKeyCode);
		} else {
			KeyCombination answer = new KeyCombination(keycode, ctrl, alt, shift);
			combinations.put(compositeKeyCode, answer);
			return answer;
		}
	}

	static int computeCompositeKeyCode(int keycode, boolean ctrl, boolean alt, boolean shift) {
		return keycode
			+ (ctrl ? TendiwaInputProcessor.ctrl : 0)
			+ (alt ? TendiwaInputProcessor.alt : 0)
			+ (shift ? TendiwaInputProcessor.shift : 0);
	}

	static KeyCombination obtainCombination(int combination) {
		return obtainCombination(
			combination % (TendiwaInputProcessor.ctrl),
			(combination & TendiwaInputProcessor.ctrl) == TendiwaInputProcessor.ctrl,
			(combination & TendiwaInputProcessor.alt) == TendiwaInputProcessor.alt,
			(combination & TendiwaInputProcessor.shift) == TendiwaInputProcessor.shift
		);
	}
}

}

