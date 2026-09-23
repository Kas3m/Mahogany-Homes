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
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

class DialogueHighlighter
{
	private final Client client;
	private final Map<Widget, HighlightedText> highlighted = new IdentityHashMap<>();

	@Inject
	DialogueHighlighter(final Client client)
	{
		this.client = client;
	}

	void refresh(final String tier, final String destination, final Color color)
	{
		clear();
		if (color == null || (tier == null && destination == null))
		{
			return;
		}

		final Widget options = client.getWidget(InterfaceID.Chatmenu.OPTIONS);
		if (options != null && !options.isHidden())
		{
			final Widget[] children = options.getChildren();
			if (children != null)
			{
				// A tier word in an unrelated conversation is not a contractor menu.
				int tiers = 0;
				for (final Widget child : children)
				{
					if (isTierOption(child, "Beginner") || isTierOption(child, "Novice")
						|| isTierOption(child, "Adept") || isTierOption(child, "Expert"))
					{
						tiers++;
					}
				}
				for (final Widget child : children)
				{
					if ((tier != null && tiers > 1 && isTierOption(child, tier)) || containsHint(child, destination))
					{
						highlight(child, color);
					}
				}
			}
		}

		// Xeric's scroll uses group MENU, not the chatbox dialogue group.
		if (destination != null)
		{
			highlightChildren(client.getWidget(InterfaceID.Menu.LJ_LAYER1), destination, color);
		}
	}

	private boolean isTierOption(final Widget widget, final String tier)
	{
		if (widget == null || widget.isHidden() || widget.getText() == null)
		{
			return false;
		}
		final String text = Text.removeTags(widget.getText()).trim().toLowerCase(Locale.ROOT);
		final String name = tier.toLowerCase(Locale.ROOT);
		return text.equals(name) || text.startsWith(name + " ");
	}

	private boolean containsHint(final Widget widget, final String hint)
	{
		return hint != null && !hint.isEmpty() && widget != null && !widget.isHidden() && widget.getText() != null
			&& Text.removeTags(widget.getText()).toLowerCase(Locale.ROOT).contains(hint.toLowerCase(Locale.ROOT));
	}

	private void highlightChildren(final Widget parent, final String hint, final Color color)
	{
		if (parent == null || parent.isHidden() || parent.getChildren() == null)
		{
			return;
		}
		for (final Widget child : parent.getChildren())
		{
			if (containsHint(child, hint))
			{
				highlight(child, color);
			}
		}
	}

	private void highlight(final Widget widget, final Color color)
	{
		final String original = widget.getText();
		// Keep game-provided disabled colors and other plugins' text formatting.
		if (original.contains("<col="))
		{
			return;
		}
		final String replacement = ColorUtil.prependColorTag(original, color) + "</col>";
		highlighted.put(widget, new HighlightedText(original, replacement));
		widget.setText(replacement);
	}

	void clear()
	{
		for (final Map.Entry<Widget, HighlightedText> entry : highlighted.entrySet())
		{
			if (entry.getValue().replacement.equals(entry.getKey().getText()))
			{
				entry.getKey().setText(entry.getValue().original);
			}
		}
		highlighted.clear();
	}

	@RequiredArgsConstructor
	private static class HighlightedText
	{
		private final String original;
		private final String replacement;
	}
}
