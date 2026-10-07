import { type GameData, stackKey } from './gamedata';

export interface ResearchTime {
	/** Seconds until the tech completes, the slower of lab time and pack supply. */
	seconds: number;
	labSeconds: number;
	supplySeconds: number;
	/** Packs needed by item. */
	packs: Record<string, number>;
	limitedBy: 'labs' | 'supply';
}

/**
 * Time to research one tech. Labs work in parallel, each pack takes ticks_per_pack in a lab; packs arrive at the
 * given rate per item (items per second). A pack without a supply rate never arrives (Infinity).
 */
export function researchTime(data: GameData, techId: string, labs: number, supply: Record<string, number>): ResearchTime {
	const tech = data.techs[techId];
	if (!tech) {
		throw new Error(`Unknown tech ${techId}`);
	}
	const packs: Record<string, number> = {};
	for (const stack of tech.cost) {
		packs[stackKey(stack)] = (packs[stackKey(stack)] ?? 0) + stack.count;
	}
	const totalPacks = Object.values(packs).reduce((sum, count) => sum + count, 0);
	const labSeconds = labs > 0 ? (totalPacks * tech.ticks_per_pack) / 20 / labs : Infinity;
	let supplySeconds = 0;
	for (const [item, count] of Object.entries(packs)) {
		const rate = supply[item] ?? 0;
		supplySeconds = Math.max(supplySeconds, rate > 0 ? count / rate : Infinity);
	}
	return {
		seconds: Math.max(labSeconds, supplySeconds),
		labSeconds,
		supplySeconds,
		packs,
		limitedBy: labSeconds >= supplySeconds ? 'labs' : 'supply',
	};
}

/** Techs in an order where parents come before children, then by id. */
export function techOrder(data: GameData): string[] {
	const order: string[] = [];
	const seen = new Set<string>();
	const visit = (id: string, path: Set<string>) => {
		if (seen.has(id) || path.has(id) || !data.techs[id]) {
			return;
		}
		path.add(id);
		for (const parent of [...data.techs[id].parents].sort()) {
			visit(parent, path);
		}
		path.delete(id);
		seen.add(id);
		order.push(id);
	};
	for (const id of Object.keys(data.techs).sort()) {
		visit(id, new Set());
	}
	return order;
}
