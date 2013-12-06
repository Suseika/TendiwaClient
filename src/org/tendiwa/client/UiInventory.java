package org.tendiwa.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import org.tendiwa.events.*;
import tendiwa.core.Equipment;
import tendiwa.core.Item;
import tendiwa.core.Tendiwa;
import tendiwa.core.UniqueItem;

public class UiInventory extends Table {
VerticalFlowGroup flowGroup = new VerticalFlowGroup();

public UiInventory() {
	setBackground(TendiwaUiStage.createImage(new Color(0.2f, 0.2f, 0.2f, 1.0f)).getDrawable());
	setSize(400, 300);
	add(flowGroup).expand().fill();
	update();
}

public void update() {
	flowGroup.clearChildren();
	for (final Item item : Tendiwa.getPlayerCharacter().getEquipment()) {
		Image itemIcon = createItemIcon(item);
		itemIcon.setColor(Color.RED);
		itemIcon.addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (item.getType().isWearable()) {
					Tendiwa.getServer().pushRequest(new RequestTakeOff((UniqueItem) item));
				} else {
					Tendiwa.getServer().pushRequest(new RequestUnwield(item));
				}
				return true;
			}
		});
		flowGroup.addActor(itemIcon);
	}
	for (final Item item : Tendiwa.getPlayerCharacter().getInventory()) {
		Image itemIcon = createItemIcon(item);
		itemIcon.addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
					Tendiwa.getServer().pushRequest(new RequestDrop(item));
				} else {
					if (item.getType().isWearable()) {
						if (Tendiwa.getPlayerCharacter().getEquipment().canPutOn((UniqueItem) item)) {
							Tendiwa.getServer().pushRequest(new RequestPutOn((UniqueItem) item));
						}
					} else {
						if (Tendiwa.getPlayerCharacter().getEquipment().canWield(item)) {
							Tendiwa.getServer().pushRequest(new RequestWield(item));
						}
					}
				}
				return true;
			}
		});
		flowGroup.addActor(itemIcon);
	}
}

private Image createItemIcon(final Item item) {
	assert item != Equipment.nullItem;
	TextureAtlas.AtlasRegion region = AtlasItems.getInstance().findRegion(item.getType().getResourceName());
	TextureRegion newRegion = new TextureAtlas.AtlasRegion(region);
	newRegion.flip(false, true);
	return new Image(newRegion);
}

}
