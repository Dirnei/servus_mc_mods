import { describe, expect, test } from 'vitest';
// Tier 1 data from before the alchemy rethink, until the lab is rebuilt on alchemy data.
import raw from './fixtures/tier1-gamedata.json';
import { type GameData, parseGameData } from './gamedata';
import { computeChain } from './ratios';
import { researchTime, techOrder } from './research';

const data = parseGameData(raw);
const RED_PACK = 'servus_manufacturing:red_pack';

describe('research time', () => {
	test('10 Red Packs at 100 ticks with 1 lab and one pack per 5 s take 50 s', () => {
		const tech: GameData['techs'][string] = {
			parents: [],
			cost: [{ item: RED_PACK, count: 10 }],
			ticks_per_pack: 100,
			unlocks: [],
		};
		const time = researchTime({ ...data, techs: { 'test:tech': tech } }, 'test:tech', 1, { [RED_PACK]: 0.2 });
		expect(time.seconds).toBeCloseTo(50, 9);
		expect(time.labSeconds).toBeCloseTo(50, 9);
		expect(time.supplySeconds).toBeCloseTo(50, 9);
	});

	test('more labs only help until supply limits', () => {
		const tech = Object.keys(data.techs).find((id) => data.techs[id].cost.length === 1)!;
		const time = researchTime(data, tech, 4, { [RED_PACK]: 0.2 });
		expect(time.limitedBy).toBe('supply');
		expect(time.seconds).toBeCloseTo(time.supplySeconds, 9);
	});

	test('no supply never finishes', () => {
		const tech = Object.keys(data.techs)[0];
		expect(researchTime(data, tech, 1, {}).seconds).toBe(Infinity);
	});

	test('parents come before children', () => {
		const order = techOrder(data);
		for (const id of order) {
			for (const parent of data.techs[id].parents) {
				expect(order.indexOf(parent)).toBeLessThan(order.indexOf(id));
			}
		}
		expect(order).toHaveLength(Object.keys(data.techs).length);
	});
});

describe('graph edge cases', () => {
	const recipe = (input: string, output: string) => ({
		type: 'servus_manufacturing:assembling',
		inputs: [{ item: input, count: 1 }],
		outputs: [{ item: output, count: 1 }],
		time: 20,
	});

	test('a two recipe cycle terminates with a loop node', () => {
		const looped: GameData = {
			...data,
			recipes: { 'test:a_from_b': recipe('test:b', 'test:a'), 'test:b_from_a': recipe('test:a', 'test:b') },
		};
		const chain = computeChain(looped, { item: 'test:a', perSecond: 1 }, { research: false });
		// Coal follows for the generators powering the assemblers.
		expect(chain.nodes.map((n) => n.item)).toEqual(['test:a', 'test:b', 'minecraft:coal']);
		expect(chain.nodes.find((n) => n.item === 'test:b')?.loop).toBe(true);
		expect(chain.nodes.find((n) => n.item === 'test:a')?.loop).toBe(false);
	});

	test('a missing input is a no source leaf and totals still render', () => {
		const missing: GameData = { ...data, recipes: { ...data.recipes, 'test:coil': recipe('test:nothing', 'test:coil') } };
		const chain = computeChain(missing, { item: 'test:coil', perSecond: 1 }, { research: false });
		const leaf = chain.nodes.find((n) => n.item === 'test:nothing');
		expect(leaf?.noSource).toBe(true);
		expect(leaf?.count).toBe(0);
		expect(chain.totals.machines['servus_manufacturing:assembler']).toBe(1);
		expect(chain.totals.fePerTick).toBeGreaterThan(0);
	});
});
