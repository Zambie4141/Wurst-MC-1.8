/*
 * Copyright � 2014 - 2016 | Wurst-Imperium | All rights reserved.
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package tk.wurst_client.mods;

import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C02PacketUseEntity;
import tk.wurst_client.events.listeners.UpdateListener;
import tk.wurst_client.navigator.NavigatorItem;
import tk.wurst_client.navigator.settings.CheckboxSetting;
import tk.wurst_client.navigator.settings.SliderSetting;
import tk.wurst_client.navigator.settings.SliderSetting.ValueDisplay;
import tk.wurst_client.special.YesCheatSpf.BypassLevel;
import tk.wurst_client.utils.EntityUtils;
import tk.wurst_client.utils.EntityUtils.TargetSettings;

@Mod.Info(
	description = "Automatically attacks the closest valid entity whenever you click.\n"
		+ "�lWarning:�r ClickAuras generally look more suspicious than Killauras\n"
		+ "and are easier to detect. It is recommended to use Killaura or\n"
		+ "TriggerBot instead.",
	name = "ClickAura",
	tags = "Click Aura,ClickAimbot,Click Aimbot",
	help = "Mods/ClickAura")
@Mod.Bypasses(ghostMode = false)
public class ClickAuraMod extends Mod implements UpdateListener
{
	public CheckboxSetting useKillaura =
		new CheckboxSetting("Use Killaura settings", true)
		{
			@Override
			public void update()
			{
				if(isChecked())
				{
					KillauraMod killaura = wurst.mods.killauraMod;
					speed.lockToValue(killaura.speed.getValue());
					range.lockToValue(killaura.range.getValue());
					fov.lockToValue(killaura.fov.getValue());
					hitThroughWalls.lock(killaura.hitThroughWalls.isChecked());
				}else
				{
					speed.unlock();
					range.unlock();
					fov.unlock();
					hitThroughWalls.unlock();
				}
			};
		};
	public SliderSetting speed =
		new SliderSetting("Speed", 20, 0.1, 20, 0.1, ValueDisplay.DECIMAL);
	public SliderSetting range =
		new SliderSetting("Range", 6, 1, 6, 0.05, ValueDisplay.DECIMAL);
	public SliderSetting fov =
		new SliderSetting("FOV", 360, 30, 360, 10, ValueDisplay.DEGREES);
	public CheckboxSetting hitThroughWalls =
		new CheckboxSetting("Hit through walls", false);
	
	private TargetSettings targetSettings = new TargetSettings()
	{
		@Override
		public boolean targetBehindWalls()
		{
			return hitThroughWalls.isChecked();
		}
		
		@Override
		public float getRange()
		{
			return range.getValueF();
		}
		
		@Override
		public float getFOV()
		{
			return fov.getValueF();
		}
	};
	
	@Override
	public void initSettings()
	{
		settings.add(useKillaura);
		settings.add(speed);
		settings.add(range);
		settings.add(fov);
		settings.add(hitThroughWalls);
	}
	
	@Override
	public NavigatorItem[] getSeeAlso()
	{
		return new NavigatorItem[]{wurst.special.targetSpf,
			wurst.mods.killauraMod, wurst.mods.killauraLegitMod,
			wurst.mods.multiAuraMod, wurst.mods.triggerBotMod};
	}
	
	@Override
	public void onEnable()
	{
		// TODO: Clean up this mess!
		if(wurst.mods.killauraMod.isEnabled())
			wurst.mods.killauraMod.setEnabled(false);
		if(wurst.mods.killauraLegitMod.isEnabled())
			wurst.mods.killauraLegitMod.setEnabled(false);
		if(wurst.mods.multiAuraMod.isEnabled())
			wurst.mods.multiAuraMod.setEnabled(false);
		if(wurst.mods.triggerBotMod.isEnabled())
			wurst.mods.triggerBotMod.setEnabled(false);
		wurst.events.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		wurst.events.remove(UpdateListener.class, this);
		EntityUtils.lookChanged = false;
	}
	
	@Override
	public void onUpdate()
	{
		// update timer
		updateMS();
		
		// check if clicking
		if(!mc.gameSettings.keyBindAttack.pressed)
			return;
		
		// check timer
		if(!hasTimePassedS(speed.getValueF()))
			return;
		
		// set entity
		Entity entity = EntityUtils.getClosestEntity(targetSettings);
		
		// head rotation
		EntityUtils.lookChanged = entity != null;
		if(!EntityUtils.lookChanged)
			return;
		
		// AutoSword
		if(wurst.mods.autoSwordMod.isActive())
			AutoSwordMod.setSlot();
		
		// Criticals
		wurst.mods.criticalsMod.doCritical();
		
		// BlockHit
		wurst.mods.blockHitMod.doBlock();
		
		// face entity
		if(!EntityUtils.faceEntityPacket(entity))
			return;
		
		// attack entity
		mc.player.swingItem();
		mc.player.connection.sendPacket(
			new C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK));
		
		// reset timer
		updateLastMS();
	}
	
	@Override
	public void onYesCheatUpdate(BypassLevel bypassLevel)
	{
		switch(bypassLevel)
		{
			default:
			case OFF:
			case MINEPLEX_ANTICHEAT:
				speed.unlock();
				range.unlock();
				hitThroughWalls.unlock();
				break;
			case ANTICHEAT:
			case OLDER_NCP:
			case LATEST_NCP:
				speed.lockToMax(12);
				range.lockToMax(4.25);
				hitThroughWalls.unlock();
				break;
			case GHOST_MODE:
				speed.lockToMax(12);
				range.lockToMax(4.25);
				hitThroughWalls.lock(false);
				break;
		}
	}
}
