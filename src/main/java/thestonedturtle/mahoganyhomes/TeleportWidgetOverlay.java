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
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.util.Text;

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
		if (color == null || plugin.isPluginTimedOut())
		{
			return null;
		}

		final TeleportItem teleportItem = plugin.teleportItem;
		if (teleportItem == null || (plugin.getCurrentHome() == null && plugin.getCurrentContractor() == null) || !config.highlightTeleports())
		{
			return null;
		}

		// Highlight side tab if user is on the wrong tab
		if (config.highlightTabIcons())
		{
			renderTabHighlight(graphics, teleportItem, color);
		}

		// Highlight spellbook spell
		if (teleportItem.isSpell())
		{
			renderSpellHighlight(graphics, teleportItem, color);
		}

		return null;
	}

	private void renderTabHighlight(final Graphics2D graphics, final TeleportItem teleportItem, final Color color)
	{
		final TeleportItem.TeleportTab targetTab = plugin.getTargetTeleportTab() != null
			? plugin.getTargetTeleportTab()
			: teleportItem.getTab(client);
		final int currentTabVarc = client.getVarcIntValue(VarClientInt.INVENTORY_TAB);

		if (currentTabVarc == targetTab.getVarcValue())
		{
			return;
		}

		final Widget tabWidget = getTabWidget(targetTab);
		if (tabWidget != null && !tabWidget.isHidden())
		{
			final Rectangle bounds = tabWidget.getBounds();
			if (bounds != null && bounds.width > 0 && bounds.height > 0)
			{
				graphics.setColor(color);
				graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
				graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 255));
				graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
			}
		}
	}

	private void renderSpellHighlight(final Graphics2D graphics, final TeleportItem teleportItem, final Color color)
	{
		final String targetSpell = teleportItem.getSpellName();
		if (targetSpell == null)
		{
			return;
		}

		final Widget spellWidget = findSpellWidget(targetSpell);
		if (spellWidget != null)
		{
			final Rectangle bounds = spellWidget.getBounds();
			if (bounds != null && bounds.width > 0 && bounds.height > 0)
			{
				graphics.setColor(color);
				graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
				graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 255));
				graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
			}
		}
	}

	Widget findSpellWidget(final String targetSpell)
	{
		final Widget spellbook = client.getWidget(ComponentID.SPELLBOOK_PARENT);
		if (spellbook != null && !spellbook.isHidden())
		{
			return scanWidgetTree(spellbook, targetSpell);
		}

		final Widget[] roots = client.getWidgetRoots();
		if (roots != null)
		{
			for (final Widget root : roots)
			{
				if (root != null && !root.isHidden() && (root.getId() >> 16) == (ComponentID.SPELLBOOK_PARENT >> 16))
				{
					final Widget found = scanWidgetTree(root, targetSpell);
					if (found != null)
					{
						return found;
					}
				}
			}
		}

		return null;
	}

	private Widget scanWidgetTree(final Widget widget, final String targetSpell)
	{
		if (widget == null || widget.isHidden())
		{
			return null;
		}

		final String name = widget.getName();
		if (name != null && !name.isEmpty())
		{
			final String cleanName = Text.removeTags(name).trim();
			if (cleanName.equalsIgnoreCase(targetSpell)
				|| cleanName.toLowerCase().contains(targetSpell.toLowerCase()))
			{
				final Rectangle bounds = widget.getBounds();
				if (bounds != null && bounds.width > 0 && bounds.height > 0 && bounds.x >= 0 && bounds.y >= 0)
				{
					return widget;
				}
			}
		}

		final Widget[] nested = widget.getNestedChildren();
		if (nested != null)
		{
			for (final Widget child : nested)
			{
				final Widget found = scanWidgetTree(child, targetSpell);
				if (found != null)
				{
					return found;
				}
			}
		}

		final Widget[] staticChildren = widget.getStaticChildren();
		if (staticChildren != null)
		{
			for (final Widget child : staticChildren)
			{
				final Widget found = scanWidgetTree(child, targetSpell);
				if (found != null)
				{
					return found;
				}
			}
		}

		final Widget[] dynamicChildren = widget.getDynamicChildren();
		if (dynamicChildren != null)
		{
			for (final Widget child : dynamicChildren)
			{
				final Widget found = scanWidgetTree(child, targetSpell);
				if (found != null)
				{
					return found;
				}
			}
		}

		final Widget[] children = widget.getChildren();
		if (children != null)
		{
			for (final Widget child : children)
			{
				final Widget found = scanWidgetTree(child, targetSpell);
				if (found != null)
				{
					return found;
				}
			}
		}

		return null;
	}

	private Widget getTabWidget(final TeleportItem.TeleportTab tab)
	{
		switch (tab)
		{
			case INVENTORY:
				return getFirstVisibleWidget(
					ComponentID.FIXED_VIEWPORT_INVENTORY_TAB,
					ComponentID.RESIZABLE_VIEWPORT_INVENTORY_TAB,
					ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB,
					ComponentID.FIXED_VIEWPORT_INVENTORY_ICON,
					ComponentID.RESIZABLE_VIEWPORT_INVENTORY_ICON,
					ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_ICON
				);
			case EQUIPMENT:
				return getFirstVisibleWidget(
					ComponentID.FIXED_VIEWPORT_EQUIPMENT_TAB,
					ComponentID.RESIZABLE_VIEWPORT_EQUIPMENT_TAB,
					ComponentID.FIXED_VIEWPORT_EQUIPMENT_ICON,
					ComponentID.RESIZABLE_VIEWPORT_EQUIPMENT_ICON,
					ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_EQUIP_ICON
				);
			case MAGIC:
				return getFirstVisibleWidget(
					ComponentID.FIXED_VIEWPORT_MAGIC_TAB,
					ComponentID.RESIZABLE_VIEWPORT_MAGIC_TAB,
					ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MAGIC_TAB,
					ComponentID.FIXED_VIEWPORT_MAGIC_ICON,
					ComponentID.RESIZABLE_VIEWPORT_MAGIC_ICON,
					ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MAGIC_ICON
				);
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

