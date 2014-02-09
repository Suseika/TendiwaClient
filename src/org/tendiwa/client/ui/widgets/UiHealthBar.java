package org.tendiwa.client.ui.widgets;

import com.badlogic.gdx.graphics.Color;
import com.esotericsoftware.tablelayout.Cell;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.tendiwa.client.TendiwaWidget;
import org.tendiwa.client.ui.factories.ColorFillFactory;
import org.tendiwa.core.events.EventGetDamage;
import org.tendiwa.core.events.EventSelectPlayerCharacter;
import org.tendiwa.core.observation.EventEmitter;
import org.tendiwa.core.observation.Observable;
import org.tendiwa.core.observation.Observer;

public class UiHealthBar extends TendiwaWidget {
private final int width;
private final Cell greenZone;
private final Cell redZone;
private final Cell blackZone;
private final int height;
private int hp;
private int maxHp;

@Inject
private UiHealthBar(
	@Named("tendiwa") Observable model,
	ColorFillFactory colorFillFactory
) {
	super();
	this.width = 200;
	this.height = 16;
	setWidth(width);
	setHeight(height);
	greenZone = add(colorFillFactory.create(Color.GREEN)).width(width / 3).height(height);
	redZone = add(colorFillFactory.create(Color.RED)).width(width / 3).height(height);
	blackZone = add(colorFillFactory.create(Color.BLACK)).width(width / 3).height(height);
	model.subscribe(new Observer<EventGetDamage>() {
		@Override
		public void update(EventGetDamage event, EventEmitter<EventGetDamage> emitter) {
			if (event.character.isPlayer()) {
				changeHp(hp - event.amount);
			}
			emitter.done(this);
		}
	}, EventGetDamage.class);
	model.subscribe(new Observer<EventSelectPlayerCharacter>() {
		@Override
		public void update(EventSelectPlayerCharacter event, EventEmitter<EventSelectPlayerCharacter> emitter) {
			changeHp(event.player.getHp());
			changeMaxHp(event.player.getMaxHP());
		}
	}, EventSelectPlayerCharacter.class);

}

public void changeHp(int hp) {
	greenZone.width(getGreenZoneWidth(hp));
	invalidate();
}

private int getGreenZoneWidth(int hp) {
	return Math.max(width * hp / maxHp, 0);
}

public void changeMaxHp(int maxHp) {
	this.maxHp = maxHp;
	blackZone.width(width - getGreenZoneWidth(this.hp));
	invalidate();
}
}
