/*
 * Copyright (c) 2026, Kas3m <https://github.com/Kas3m>
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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.VarClientInt;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

class TeleportWidgetOverlay extends Overlay
{
	private final Client client;
	private final MahoganyHomesPlugin plugin;
	private final MahoganyHomesConfig config;

	@Inject
	TeleportWidgetOverlay(final Client client, final MahoganyHomesPlugin plugin, final MahoganyHomesConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(final Graphics2D graphics)
	{
		final Color color = config.highlightTeleportsColor();
		final TeleportItem teleportItem = plugin.getTeleportItem();
		if (color == null || plugin.isPluginTimedOut() || !config.highlightTeleports() || teleportItem == null
			|| (plugin.getCurrentHome() == null && plugin.getCurrentContractor() == null))
		{
			return null;
		}

		final Graphics2D overlayGraphics = (Graphics2D) graphics.create();
		try
		{
			if (config.highlightTabIcons())
			{
				final TeleportItem.TeleportTab tab = teleportItem.getTab(client);
				if (tab != null && client.getVarcIntValue(VarClientInt.INVENTORY_TAB) != tab.getVarcValue())
				{
					renderHighlight(overlayGraphics, getTabWidget(tab), color);
				}
			}

			if (teleportItem.isSpell())
			{
				renderHighlight(overlayGraphics, client.getWidget(teleportItem.getSpellWidgetId()), color);
			}
		}
		finally
		{
			overlayGraphics.dispose();
		}
		return null;
	}

	private void renderHighlight(final Graphics2D graphics, final Widget widget, final Color color)
	{
		if (widget == null || widget.isHidden())
		{
			return;
		}
		final Rectangle bounds = widget.getBounds();
		if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
		{
			return;
		}
		graphics.setColor(color);
		graphics.fill(bounds);
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue()));
		graphics.draw(bounds);
	}

	Widget getTabWidget(final TeleportItem.TeleportTab tab)
	{
		// The three viewport layouts have distinct components, even with InterfaceID.
		switch (tab)
		{
			case INVENTORY:
				return getFirstVisibleWidget(InterfaceID.Toplevel.STONE3, InterfaceID.ToplevelOsrsStretch.STONE3, InterfaceID.ToplevelPreEoc.STONE3);
			case EQUIPMENT:
				return getFirstVisibleWidget(InterfaceID.Toplevel.STONE4, InterfaceID.ToplevelOsrsStretch.STONE4, InterfaceID.ToplevelPreEoc.STONE4);
			case MAGIC:
				return getFirstVisibleWidget(InterfaceID.Toplevel.STONE6, InterfaceID.ToplevelOsrsStretch.STONE6, InterfaceID.ToplevelPreEoc.STONE6);
			default:
				return null;
		}
	}

	private Widget getFirstVisibleWidget(final int... componentIds)
	{
		for (final int id : componentIds)
		{
			final Widget widget = client.getWidget(id);
			if (widget != null && !widget.isHidden())
			{
				return widget;
			}
		}
		return null;
	}
}
