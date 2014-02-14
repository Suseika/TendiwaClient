package org.tendiwa.client.ui.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tendiwa.core.EnhancedPoint;

@Singleton
public class CursorPosition {
private int x = 0;
private int y = 0;

@Inject
public CursorPosition() {
}

public void moveCursorBy(int dx, int dy) {
	x += dx;
	y += dy;
}

private void moveCursorTo(int x, int y) {
	this.x = x;
	this.y = y;
}

public int getWorldX() {
	return x;
}

public int getWorldY() {
	return y;
}

public EnhancedPoint getPoint() {
	return new EnhancedPoint(x, y);
}

public void setPoint(EnhancedPoint point) {
	x = point.x;
	y = point.y;
}
}
