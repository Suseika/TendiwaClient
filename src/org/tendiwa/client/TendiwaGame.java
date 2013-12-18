package org.tendiwa.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.google.common.io.Resources;
import org.tendiwa.lexeme.Language;
import org.tendiwa.lexeme.implementations.Russian;
import tendiwa.core.RequestInitialTerrain;
import tendiwa.core.Tendiwa;
import tendiwa.core.TendiwaClient;
import tendiwa.modules.MainModule;

import java.net.URL;

public class TendiwaGame extends Game implements TendiwaClient {

private static TendiwaGame INSTANCE;
private static ItemSelectionScreen itemSelectionScreen;
public int WIDTH;
public int HEIGHT;
LwjglApplicationConfiguration cfg;
private TendiwaClientLibgdxEventManager eventManager;
private GameScreen gameScreen;
private WorldMapScreen worldMapScreen;
private Language currentLanguage;

public TendiwaGame() {
	if (INSTANCE != null) {
		throw new RuntimeException("Attempting to create multiple TendiwaGame instances");
	}
	INSTANCE = this;
}

public static TendiwaGame getInstance() {
	return INSTANCE;
}

public static void switchToWorldMapScreen() {
	if (INSTANCE.worldMapScreen == null) {
		INSTANCE.worldMapScreen = new WorldMapScreen(INSTANCE);
	}
	INSTANCE.setScreen(INSTANCE.worldMapScreen);
}

public static void switchToGameScreen() {
	INSTANCE.setScreen(INSTANCE.gameScreen);
}

public static boolean isGameScreenActive() {
	return INSTANCE.getScreen() == INSTANCE.gameScreen;
}

public static void switchToItemSelectionScreen() {
	INSTANCE.setScreen(itemSelectionScreen);
}

public static ItemSelectionScreen getItemSelectionScreen() {
	return itemSelectionScreen;
}

public static GameScreen getGameScreen() {
	return INSTANCE.gameScreen;
}

public Language getLanguage() {
	return currentLanguage;
}

@Override
public void create() {
	gameScreen = new GameScreen(this, new ClientConfig());
	itemSelectionScreen = new ItemSelectionScreen();
	eventManager = new TendiwaClientLibgdxEventManager(gameScreen);
	setScreen(gameScreen);
	Tendiwa.getServer().pushRequest(new RequestInitialTerrain());
}

@Override
public void render() {
	super.render();
}

@Override
public void startup() {
	this.cfg = new LwjglApplicationConfiguration();
	cfg.title = "The Tendiwa Erpoge";
	cfg.useGL20 = true;
	cfg.width = 1024;
	cfg.height = 768;
	cfg.resizable = false;
	cfg.vSyncEnabled = false;
	cfg.forceExit = true;
	cfg.foregroundFPS = 10000;
	new LwjglApplication(this, cfg);
	languageSetup();
}

private void languageSetup() {
	currentLanguage = new Russian();
	currentLanguage.loadCorpus(Resources.getResource("language/ru_RU/messages.ru_RU.texts"));
	currentLanguage.loadDictionary(Resources.getResource("language/ru_RU/actions.ru_RU.words"));
	currentLanguage.loadDictionary(Resources.getResource("language/ru_RU/characters.ru_RU.words"));
	System.out.println(currentLanguage.getLoadedWords());
}

@Override
public TendiwaClientLibgdxEventManager getEventManager() {
	return eventManager;
}

@Override
public boolean isAnimationCompleted() {
	return eventManager.getPendingOperations().isEmpty() && !gameScreen.isEventProcessingGoing();
}
}
