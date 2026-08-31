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

import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

@Getter
@AllArgsConstructor
public enum Contractor
{
	AMY("Amy", new int[]{NpcID.AMY, NpcID.AMY_7417}, new WorldPoint(2988, 3365, 0), new WorldArea(2988, 3363, 8, 6, 0), TeleportItems.AMY),
	ANGELO("Angelo", new int[]{NpcID.ANGELO, NpcID.ANGELO_10413}, new WorldPoint(1780, 3626, 0), new WorldArea(1777, 3623, 8, 8, 0), TeleportItems.ANGELO),
	MARLO("Marlo", new int[]{NpcID.MARLO, NpcID.MARLO_10409}, new WorldPoint(3240, 3474, 0), new WorldArea(3238, 3470, 6, 9, 0), TeleportItems.MARLO),
	ELLIE("Ellie", new int[]{NpcID.ELLIE, NpcID.ELLIE_10411}, new WorldPoint(2638, 3293, 0), new WorldArea(2634, 3291, 9, 6, 0), TeleportItems.ELLIE);

	private final String name;
	private final int[] npcIds;
	private final WorldPoint location;
	private final WorldArea area;
	private final TeleportItems teleportItems;

	private static final ImmutableMap<Integer, Contractor> CONTRACTOR_BY_NPC_ID;

	static
	{
		final ImmutableMap.Builder<Integer, Contractor> b = ImmutableMap.builder();
		for (final Contractor c : values())
		{
			for (final int id : c.npcIds)
			{
				b.put(id, c);
			}
		}

		CONTRACTOR_BY_NPC_ID = b.build();
	}

	public static Contractor fromNpc(final NPC npc)
	{
		return npc == null ? null : fromNpcId(npc.getId());
	}

	public static Contractor fromNpcId(final int npcId)
	{
		return CONTRACTOR_BY_NPC_ID.get(npcId);
	}

	public static Contractor fromHome(final Home home)
	{
		if (home == null)
		{
			return null;
		}

		switch (home)
		{
			case LARRY:
			case NORMAN:
			case TAU:
				return AMY;
			case BARBARA:
			case LEELA:
			case MARIAH:
				return ANGELO;
			case BOB:
			case JEFF:
			case SARAH:
				return MARLO;
			case JESS:
			case NOELLA:
			case ROSS:
				return ELLIE;
			default:
				return null;
		}
	}

	public static Contractor getClosestContractor(final WorldPoint playerPos)
	{
		if (playerPos == null)
		{
			return AMY;
		}

		Contractor closest = AMY;
		int minDistance = Integer.MAX_VALUE;

		for (final Contractor c : values())
		{
			final int dist = c.area.distanceTo(playerPos);
			if (dist < minDistance)
			{
				minDistance = dist;
				closest = c;
			}
		}

		return closest;
	}
}
