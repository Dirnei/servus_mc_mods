import { describe, expect, test } from 'vitest';
// Tier 1 data from before the alchemy rethink, until the lab is rebuilt on alchemy data.
import raw from './fixtures/tier1-gamedata.json';
import { parseGameData } from './gamedata';
import { type Chain, computeChain, routesFor } from './ratios';

const data = parseGameData(raw);
const RED_PACK = 'servus_alchemy:red_pack';
const node = (chain: Chain, item: string) => {
	const found = chain.nodes.find((n) => n.item === item);
	if (!found) throw new Error(`no node ${item}`);
	return found;
};

describe('reference block', () => {
	const chain = computeChain(data, { item: RED_PACK, perSecond: 1 / 5 });

	test('the target leads the node order', () => {
		expect(chain.nodes[0].item).toBe(RED_PACK);
	});

	test('machine counts match PLAN.md', () => {
		expect(node(chain, 'minecraft:raw_iron').count).toBe(2);
		expect(node(chain, 'minecraft:raw_iron').route?.machine).toBe('servus_alchemy:burner_extractor');
		expect(node(chain, 'minecraft:iron_ingot').count).toBe(2);
		expect(node(chain, 'minecraft:iron_ingot').route?.machine).toBe('servus_alchemy:burner_smelter');
		expect(node(chain, 'minecraft:raw_copper').count).toBe(1);
		expect(node(chain, 'minecraft:copper_ingot').count).toBe(1);
		expect(node(chain, 'minecraft:coal').count).toBe(1);
		expect(chain.totals.machines['servus_alchemy:assembler']).toBe(3);
		expect(chain.consumer?.count).toBe(1);
		expect(chain.consumer?.machine).toBe('servus_alchemy:research_lab');
	});

	test('power is weighted by utilization and fits one generator', () => {
		// Gear 20 % and wire 10 % of an assembler at 20 FE/t, pack assembler and lab at full load.
		expect(chain.totals.fePerTick).toBeCloseTo(4 + 2 + 20 + 10, 6);
		expect(chain.totals.generators).toBe(1);
	});

	test('fuel includes the coal extractor burning its own coal', () => {
		const burners = (2 + 2 + 1 + 1) * 0.8 / 80;
		const generator = (36 / 40) / 20;
		const coal = node(chain, 'minecraft:coal');
		expect(coal.rate).toBeCloseTo(chain.totals.fuelPerSecond, 9);
		expect(chain.totals.fuelPerSecond).toBeCloseTo(burners + generator + coal.utilization / 80, 9);
	});
});

describe('ratio rules', () => {
	test('a generator at full load uses one coal per 20 seconds', () => {
		// One assembler makes 4 wire/s at 20 FE/t, so 8 wire/s keeps two assemblers busy: 40 FE/t, one generator.
		const chain = computeChain(data, { item: 'servus_alchemy:copper_wire', perSecond: 8 });
		expect(chain.totals.generators).toBe(1);
		expect(chain.totals.fePerTick).toBeCloseTo(40, 9);
		expect(chain.totals.generatorFuelPerSecond).toBeCloseTo(1 / 20, 9);
	});

	test('an assembler at 20 percent draws 4 FE per tick', () => {
		const chain = computeChain(data, { item: 'servus_alchemy:iron_gear', perSecond: 0.2 });
		const gear = node(chain, 'servus_alchemy:iron_gear');
		expect(gear.utilization).toBeCloseTo(0.2, 9);
		expect(gear.fePerTick).toBeCloseTo(4, 9);
	});

	test('assembling takes the recipe time, smelting the machine work ticks', () => {
		const chain = computeChain(data, { item: RED_PACK, perSecond: 1 });
		expect(node(chain, RED_PACK).route?.cycleTicks).toBe(100);
		expect(node(chain, 'minecraft:iron_ingot').route?.cycleTicks).toBe(80);
	});

	test('the grit route adds a crusher and halves iron extractors per ingot', () => {
		const ingots = { item: 'minecraft:iron_ingot', perSecond: 0.4 };
		const plain = computeChain(data, ingots, { research: false });
		const grit = computeChain(data, ingots, {
			research: false,
			routes: {
				'minecraft:iron_ingot':
					'recipe:servus_alchemy:iron_ingot_from_grit_smelting@servus_alchemy:burner_smelter',
			},
		});
		expect(node(plain, 'minecraft:raw_iron').rate).toBeCloseTo(0.4, 9);
		expect(node(grit, 'minecraft:raw_iron').rate).toBeCloseTo(0.2, 9);
		expect(node(grit, 'servus_alchemy:iron_grit').route?.machine).toBe('servus_alchemy:crusher');
		expect(grit.totals.machines['servus_alchemy:crusher']).toBe(1);
		expect(plain.totals.machines['servus_alchemy:crusher']).toBeUndefined();
	});

	test('routes list deposits first, then recipes with every capable machine', () => {
		const iron = routesFor(data, 'minecraft:raw_iron').map((r) => r.key);
		expect(iron).toEqual([
			'deposit:servus_alchemy:iron@servus_alchemy:burner_extractor',
			'deposit:servus_alchemy:iron@servus_alchemy:electric_extractor',
		]);
		const ingot = routesFor(data, 'minecraft:iron_ingot').map((r) => r.source);
		expect(ingot[0]).toBe('minecraft:iron_ingot_from_smelting_raw_iron');
		expect(ingot).toContain('servus_alchemy:iron_ingot_from_grit_smelting');
		expect(ingot).not.toContain('minecraft:iron_ingot_from_blasting_raw_iron');
	});

	test('an override changes the counts', () => {
		const faster = structuredClone(data);
		faster.recipes['servus_alchemy:assembling/red_pack'].time = 50;
		const chain = computeChain(faster, { item: RED_PACK, perSecond: 0.4 });
		expect(node(chain, RED_PACK).count).toBe(1);
		expect(node(computeChain(data, { item: RED_PACK, perSecond: 0.4 }), RED_PACK).count).toBe(2);
	});
});
