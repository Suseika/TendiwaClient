package org.tendiwa.client;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.tendiwa.core.Character;

@Singleton
public class TaskManager {
private final Character player;
Task currentTask;

@Inject
TaskManager(@Named("player") Character player) {
	this.player = player;
}

public boolean trySettingTask(Task task) {
	if (currentTask == null) {
		currentTask = task;
		return true;
	} else {
		return false;
	}
}


public void executeCurrentTask() {
	if (currentTask != null) {
		if (currentTask.ended()) {
			currentTask = null;
		} else {
			currentTask.execute();
			if (player.isUnderAnyThreat()) {
				currentTask = null;
			}
		}
	} else {
		throw new IllegalStateException("Task wasn't set");
	}
}

public boolean hasCurrentTask() {
	return currentTask != null;
}

public void cancelCurrentTask() {
	currentTask = null;
}
}