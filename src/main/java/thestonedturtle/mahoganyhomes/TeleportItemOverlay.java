/*
 * Copyright (c) 2020, TheStonedTurtle <https://github.com/TheStonedTurtle>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package thestonedturtle.mahoganyhomes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.util.ImageUtil;

class TeleportItemOverlay extends WidgetItemOverlay
{
	private final ItemManager itemManager;
	private final MahoganyHomesPlugin plugin;
	private final MahoganyHomesConfig config;

	@Inject
	private TeleportItemOverlay(final ItemManager itemManager, final MahoganyHomesPlugin plugin, final MahoganyHomesConfig config)
	{
		this.itemManager = itemManager;
		this.plugin = plugin;
		this.config = config;
		showOnInventory();
		showOnEquipment();
	}

	@Override
	public void renderItemOverlay(final Graphics2D graphics, final int itemId, final WidgetItem itemWidget)
	{
		final TeleportItem teleportItem = plugin.teleportItem;

		if (teleportItem == null || teleportItem.isSpell() || itemId != teleportItem.getItemId() || !config.highlightTeleports())
		{
			return;
		}

		final Color color = config.highlightTeleportsColor();

		if (color == null)
		{
			return;
		}

		final Rectangle bounds = itemWidget.getCanvasBounds();

		final BufferedImage outline = itemManager.getItemOutline(teleportItem.getItemId(), itemWidget.getQuantity(), new Color(color.getRGB()));
		graphics.drawImage(outline, (int) bounds.getX(), (int) bounds.getY(), null);

		final Image image = ImageUtil.fillImage(itemManager.getImage(teleportItem.getItemId(), itemWidget.getQuantity(), false), color);
		graphics.drawImage(image, (int) bounds.getX(), (int) bounds.getY(), null);
	}
}
