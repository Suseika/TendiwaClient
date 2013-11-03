package org.tendiwa.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import tendiwa.core.TendiwaClient;

public class TendiwaGame extends Game implements TendiwaClient {

public static int WIDTH = 800;
public static int HEIGHT = 600;
private TendiwaClientLibgdxEventManager eventManager;
LwjglApplicationConfiguration cfg;
private GameScreen gameScreen;

public TendiwaGame() {
}

@Override
public void create() {
	gameScreen = new GameScreen(this);
	eventManager = new TendiwaClientLibgdxEventManager(gameScreen);
	setScreen(gameScreen);
}

@Override
public void render() {
	super.render();
}

@Override
public void startup() {
	this.cfg = new LwjglApplicationConfiguration();
	cfg.title = "Title";
	cfg.useGL20 = true;
	cfg.width = 1024;
	cfg.height = 768;
	cfg.resizable = false;
//	cfg.vSyncEnabled = false;
//  new LwjglApplication(new BookFun(), cfg);
	new LwjglApplication(this, cfg);
}

@Override
public TendiwaClientLibgdxEventManager getEventManager() {
	return eventManager;
}

}
