import { describe, expect, test } from 'vitest';
// Tier 1 data from before the alchemy rethink, until the lab is rebuilt on alchemy data.
import raw from './fixtures/tier1-gamedata.json';
import { applyDraft, type Draft, draftSchema, emptyDraft, getNumber, isEmptyDraft, withEdit } from './draft';
import { parseGameData } from './gamedata';
import { type Chain, computeChain, routesFor } from './ratios';

const data = parseGameData(raw);
const RED_PACK = 'servus_alchemy:red_pack';
const PACK_RECIPE = 'servus_alchemy:assembling/red_pack';
const node = (chain: Chain, item: string) => chain.nodes.find((n) => n.item === item);

describe('drafts', () => {
	test('an edit of the Red Pack time changes the chain and leaves the baseline alone', () => {
		const draft = withEdit(emptyDraft(), data, ['recipes', PACK_RECIPE, 'time'], 50);
		const { data: effective, missing } = applyDraft(data, draft);
		expect(missing).toEqual([]);
		expect(effective.recipes[PACK_RECIPE].time).toBe(50);
		expect(data.recipes[PACK_RECIPE].time).toBe(100);
		expect(node(computeChain(effective, { item: RED_PACK, perSecond: 0.4 }), RED_PACK)?.count).toBe(1);
	});

	test('setting the baseline value again removes the edit', () => {
		const p = ['recipes', PACK_RECIPE, 'time'];
		const draft = withEdit(withEdit(emptyDraft(), data, p, 50), data, p, 100);
		expect(draft.edits).toEqual([]);
		expect(isEmptyDraft(draft)).toBe(true);
	});

	test('an edit on a missing path is reported', () => {
		const draft: Draft = { ...emptyDraft(), edits: [{ path: ['recipes', 'nope:gone', 'time'], value: 5 }] };
		expect(applyDraft(data, draft).missing).toHaveLength(1);
	});

	test('a drafted item and recipe join the chain and are flagged', () => {
		const coil = 'draft:copper_coil';
		const draft = draftSchema.parse({
			items: [{ id: coil, name: 'Copper Coil' }],
			recipes: [
				{
					id: 'draft:assembling/copper_coil',
					type: 'servus_alchemy:assembling',
					inputs: [{ item: 'servus_alchemy:copper_wire', count: 4 }],
					outputs: [{ item: coil, count: 1 }],
					time: 40,
				},
				{
					id: PACK_RECIPE,
					type: 'servus_alchemy:assembling',
					inputs: [
						{ item: 'servus_alchemy:iron_gear', count: 1 },
						{ item: coil, count: 1 },
					],
					outputs: [{ item: RED_PACK, count: 1 }],
					time: 100,
				},
			],
		});
		const { data: effective, flags } = applyDraft(data, draft);
		const chain = computeChain(effective, { item: RED_PACK, perSecond: 0.2 }, { flags });
		const coilNode = node(chain, coil);
		expect(coilNode?.draft).toBe(true);
		expect(coilNode?.rate).toBeCloseTo(0.2, 9);
		expect(node(chain, 'servus_alchemy:copper_wire')?.rate).toBeCloseTo(0.8, 9);
		expect(node(chain, RED_PACK)?.draft).toBe(true);
		expect(effective.items[coil].name).toBe('Copper Coil');
	});

	test('a concept machine with a new recipe type is a selectable route that needs code', () => {
		const drawer = 'draft:wire_drawer';
		const draft = draftSchema.parse({
			machines: [
				{
					id: drawer,
					name: 'Wire Drawer',
					kind: 'powered',
					recipe_types: ['draft:wire_drawing'],
					work_ticks: 10,
					energy_per_tick: 30,
				},
			],
			recipes: [
				{
					id: 'draft:wire_drawing/copper_wire',
					type: 'draft:wire_drawing',
					inputs: [{ item: 'minecraft:copper_ingot', count: 1 }],
					outputs: [{ item: 'servus_alchemy:copper_wire', count: 4 }],
					time: 10,
				},
			],
		});
		const { data: effective, flags } = applyDraft(data, draft);
		const routes = routesFor(effective, 'servus_alchemy:copper_wire', flags);
		const drawerRoute = routes.find((r) => r.machine === drawer);
		expect(drawerRoute?.needsCode).toBe(true);
		expect(routes[0].machine).toBe('servus_alchemy:assembler');

		const chain = computeChain(effective, { item: 'servus_alchemy:copper_wire', perSecond: 8 }, {
			flags,
			routes: { 'servus_alchemy:copper_wire': drawerRoute!.key },
		});
		const wire = node(chain, 'servus_alchemy:copper_wire');
		expect(wire?.route?.needsCode).toBe(true);
		expect(wire?.perMachine).toBeCloseTo(8, 9);
		expect(wire?.fePerTick).toBeCloseTo(30, 9);
		expect(node(chain, 'minecraft:copper_ingot')?.rate).toBeCloseTo(2, 9);
	});

	test('getNumber reads nested numbers only', () => {
		expect(getNumber(data, ['machines', 'servus_alchemy:burner_extractor', 'work_ticks'])).toBe(80);
		expect(getNumber(data, ['machines', 'servus_alchemy:burner_extractor', 'kind'])).toBeUndefined();
		expect(getNumber(data, ['recipes', PACK_RECIPE, 'inputs', 1, 'count'])).toBe(2);
	});
});
