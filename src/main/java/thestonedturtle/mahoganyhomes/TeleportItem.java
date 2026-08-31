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

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.api.gameval.VarbitID;

@Getter
public class TeleportItem
{
	public enum Type
	{
		ITEM,
		SPELL
	}

	public enum TeleportTab
	{
		INVENTORY(3),
		EQUIPMENT(4),
		MAGIC(6);

		@Getter
		private final int varcValue;

		TeleportTab(final int varcValue)
		{
			this.varcValue = varcValue;
		}
	}

	private static final Set<Integer> AIR_SOURCES = ImmutableSet.of(
		ItemID.STAFF_OF_AIR, ItemID.AIR_BATTLESTAFF, ItemID.MYSTIC_AIR_STAFF,
		ItemID.DUST_BATTLESTAFF, ItemID.MYSTIC_DUST_STAFF,
		ItemID.MIST_BATTLESTAFF, ItemID.MYSTIC_MIST_STAFF,
		ItemID.SMOKE_BATTLESTAFF, ItemID.MYSTIC_SMOKE_STAFF
	);

	private static final Set<Integer> WATER_SOURCES = ImmutableSet.of(
		ItemID.STAFF_OF_WATER, ItemID.WATER_BATTLESTAFF, ItemID.MYSTIC_WATER_STAFF,
		ItemID.MUD_BATTLESTAFF, ItemID.MYSTIC_MUD_STAFF,
		ItemID.MIST_BATTLESTAFF, ItemID.MYSTIC_MIST_STAFF,
		ItemID.STEAM_BATTLESTAFF, ItemID.MYSTIC_STEAM_STAFF,
		ItemID.STEAM_BATTLESTAFF_12795, ItemID.MYSTIC_STEAM_STAFF_12796,
		ItemID.TOME_OF_WATER, ItemID.KODAI_WAND
	);

	private static final Set<Integer> EARTH_SOURCES = ImmutableSet.of(
		ItemID.STAFF_OF_EARTH, ItemID.EARTH_BATTLESTAFF, ItemID.MYSTIC_EARTH_STAFF,
		ItemID.DUST_BATTLESTAFF, ItemID.MYSTIC_DUST_STAFF,
		ItemID.MUD_BATTLESTAFF, ItemID.MYSTIC_MUD_STAFF,
		ItemID.LAVA_BATTLESTAFF, ItemID.MYSTIC_LAVA_STAFF,
		ItemID.LAVA_BATTLESTAFF_21198, ItemID.MYSTIC_LAVA_STAFF_21200,
		ItemID.TOME_OF_EARTH
	);

	private static final Set<Integer> FIRE_SOURCES = ImmutableSet.of(
		ItemID.STAFF_OF_FIRE, ItemID.FIRE_BATTLESTAFF, ItemID.MYSTIC_FIRE_STAFF,
		ItemID.LAVA_BATTLESTAFF, ItemID.MYSTIC_LAVA_STAFF,
		ItemID.LAVA_BATTLESTAFF_21198, ItemID.MYSTIC_LAVA_STAFF_21200,
		ItemID.SMOKE_BATTLESTAFF, ItemID.MYSTIC_SMOKE_STAFF,
		ItemID.STEAM_BATTLESTAFF, ItemID.MYSTIC_STEAM_STAFF,
		ItemID.STEAM_BATTLESTAFF_12795, ItemID.MYSTIC_STEAM_STAFF_12796,
		ItemID.TOME_OF_FIRE, ItemID.BRYOPHYTAS_STAFF
	);

	private static final Set<Integer> RUNE_POUCH_IDS = ImmutableSet.of(
		ItemID.RUNE_POUCH, ItemID.RUNE_POUCH_L, ItemID.DIVINE_RUNE_POUCH, ItemID.DIVINE_RUNE_POUCH_L
	);

	private static final int[] RUNEPOUCH_AMOUNT_VARBITS = {
		VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2, VarbitID.RUNE_POUCH_QUANTITY_3,
		VarbitID.RUNE_POUCH_QUANTITY_4, VarbitID.RUNE_POUCH_QUANTITY_5, VarbitID.RUNE_POUCH_QUANTITY_6,
	};

	private static final int[] RUNEPOUCH_RUNE_VARBITS = {
		VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2, VarbitID.RUNE_POUCH_TYPE_3,
		VarbitID.RUNE_POUCH_TYPE_4, VarbitID.RUNE_POUCH_TYPE_5, VarbitID.RUNE_POUCH_TYPE_6,
	};

	// Backward compatibility
	public int ItemId;
	public int Distance;

	private final Type type;
	private final int itemId;
	private final int distance;
	private final String spellName;
	private final int requiredMagicLevel;
	private final String destinationHint;
	private final Map<Integer, Integer> requiredRunes = new HashMap<>();

	public TeleportItem(final int itemId, final int distance)
	{
		this(itemId, distance, null);
	}

	public TeleportItem(final int itemId, final int distance, final String destinationHint)
	{
		this.type = Type.ITEM;
		this.itemId = itemId;
		this.distance = distance;
		this.ItemId = itemId;
		this.Distance = distance;
		this.spellName = null;
		this.requiredMagicLevel = 0;
		this.destinationHint = destinationHint;
	}

	public TeleportItem(final String spellName, final int distance, final int requiredMagicLevel, final Item... runes)
	{
		this.type = Type.SPELL;
		this.itemId = -1;
		this.distance = distance;
		this.ItemId = -1;
		this.Distance = distance;
		this.spellName = spellName;
		this.requiredMagicLevel = requiredMagicLevel;
		this.destinationHint = null;

		for (final Item rune : runes)
		{
			if (rune != null)
			{
				requiredRunes.put(rune.getId(), rune.getQuantity());
			}
		}
	}

	public boolean isSpell()
	{
		return type == Type.SPELL;
	}

	public boolean isAvailableOnPlayer(final Client client)
	{
		if (client == null)
		{
			return false;
		}

		if (type == Type.ITEM)
		{
			final ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
			if (inventory != null && inventory.contains(itemId))
			{
				return true;
			}

			final ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
			return equipment != null && equipment.contains(itemId);
		}

		if (type == Type.SPELL)
		{
			return canCastSpell(client);
		}

		return false;
	}

	public TeleportTab getTab(final Client client)
	{
		if (type == Type.SPELL)
		{
			return TeleportTab.MAGIC;
		}

		final ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory != null && inventory.contains(itemId))
		{
			return TeleportTab.INVENTORY;
		}

		final ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment != null && equipment.contains(itemId))
		{
			return TeleportTab.EQUIPMENT;
		}

		return TeleportTab.INVENTORY;
	}

	public boolean canCastSpell(final Client client)
	{
		// Check standard spellbook (Varbit 4070 == 0)
		if (client.getVarbitValue(Varbits.SPELLBOOK) != 0)
		{
			return false;
		}

		// Check boosted/current magic level
		if (client.getBoostedSkillLevel(Skill.MAGIC) < requiredMagicLevel)
		{
			return false;
		}

		return hasRequiredRunes(client);
	}

	private boolean hasRequiredRunes(final Client client)
	{
		final ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		final ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		final Map<Integer, Integer> runePouchContents = getRunePouchContents(client);

		for (final Map.Entry<Integer, Integer> entry : requiredRunes.entrySet())
		{
			final int runeId = entry.getKey();
			final int requiredAmount = entry.getValue();

			if (getRuneCountWithVariants(runeId, inventory, equipment, runePouchContents) < requiredAmount)
			{
				return false;
			}
		}

		return true;
	}

	private int safeCount(final ItemContainer container, final Map<Integer, Integer> runePouchContents, final int... itemIds)
	{
		if (container == null && (runePouchContents == null || runePouchContents.isEmpty()))
		{
			return 0;
		}

		long total = 0;
		for (final int id : itemIds)
		{
			final int count = container != null ? container.count(id) : 0;
			final int runePouchCount = runePouchContents != null ? runePouchContents.getOrDefault(id, 0) : 0;

			total += (long) count + runePouchCount;

			if (total >= Integer.MAX_VALUE)
			{
				return Integer.MAX_VALUE;
			}
		}

		return (int) total;
	}

	private int getRuneCountWithVariants(final int itemId, final ItemContainer inventory, final ItemContainer equipment, final Map<Integer, Integer> runePouchContents)
	{
		switch (itemId)
		{
			case ItemID.AIR_RUNE:
				if (hasInfiniteRune(inventory, equipment, AIR_SOURCES))
				{
					return Integer.MAX_VALUE;
				}
				return safeCount(inventory, runePouchContents, ItemID.AIR_RUNE, ItemID.DUST_RUNE, ItemID.MIST_RUNE, ItemID.SMOKE_RUNE);
			case ItemID.WATER_RUNE:
				if (hasInfiniteRune(inventory, equipment, WATER_SOURCES))
				{
					return Integer.MAX_VALUE;
				}
				return safeCount(inventory, runePouchContents, ItemID.WATER_RUNE, ItemID.MIST_RUNE, ItemID.MUD_RUNE, ItemID.STEAM_RUNE);
			case ItemID.EARTH_RUNE:
				if (hasInfiniteRune(inventory, equipment, EARTH_SOURCES))
				{
					return Integer.MAX_VALUE;
				}
				return safeCount(inventory, runePouchContents, ItemID.EARTH_RUNE, ItemID.DUST_RUNE, ItemID.MUD_RUNE, ItemID.LAVA_RUNE);
			case ItemID.FIRE_RUNE:
				if (hasInfiniteRune(inventory, equipment, FIRE_SOURCES))
				{
					return Integer.MAX_VALUE;
				}
				return safeCount(inventory, runePouchContents, ItemID.FIRE_RUNE, ItemID.SMOKE_RUNE, ItemID.STEAM_RUNE, ItemID.LAVA_RUNE);
			default:
				return safeCount(inventory, runePouchContents, itemId);
		}
	}

	private boolean hasInfiniteRune(final ItemContainer inventory, final ItemContainer equipment, final Set<Integer> sources)
	{
		if (equipment != null)
		{
			for (final int sourceId : sources)
			{
				if (equipment.contains(sourceId))
				{
					return true;
				}
			}
		}

		if (inventory != null)
		{
			for (final int sourceId : sources)
			{
				if (inventory.contains(sourceId))
				{
					return true;
				}
			}
		}

		return false;
	}

	public static Map<Integer, Integer> getRunePouchContents(final Client client)
	{
		final Map<Integer, Integer> items = new HashMap<>();
		if (client == null)
		{
			return items;
		}

		final ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory == null)
		{
			return items;
		}

		boolean hasPouch = false;
		for (final int pouchId : RUNE_POUCH_IDS)
		{
			if (inventory.contains(pouchId))
			{
				hasPouch = true;
				break;
			}
		}

		if (!hasPouch)
		{
			return items;
		}

		final EnumComposition runepouchEnum = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		if (runepouchEnum == null)
		{
			return items;
		}

		for (int i = 0; i < RUNEPOUCH_AMOUNT_VARBITS.length; i++)
		{
			final int amount = client.getVarbitValue(RUNEPOUCH_AMOUNT_VARBITS[i]);
			if (amount <= 0)
			{
				continue;
			}

			final int runeId = client.getVarbitValue(RUNEPOUCH_RUNE_VARBITS[i]);
			if (runeId == 0)
			{
				continue;
			}

			final int itemId = runepouchEnum.getIntValue(runeId);
			if (itemId > 0)
			{
				items.put(itemId, items.getOrDefault(itemId, 0) + amount);
			}
		}

		return items;
	}
}

