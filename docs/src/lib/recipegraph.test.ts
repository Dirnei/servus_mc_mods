import { describe, expect, test } from 'vitest';
// Tier 1 data from before the alchemy rethink, until the lab is rebuilt on alchemy data.
import raw from './fixtures/tier1-gamedata.json';
import { type GameData, parseGameData } from './gamedata';
import { ancestors, buildRecipeGraph, edgeId, itemNode, modOf, rawCost, recipeKind, recipeNode } from './recipegraph';

const data = parseGameData(raw);

/** Game data with only the given crafting entries, one input and one output each. */
function fixture(entries: Record<string, [string, number, string, number][]>): GameData {
	const crafting: GameData['crafting'] = {};
	for (const [id, uses] of Object.entries(entries)) {
		const [, , output, made] = uses[0];
		crafting[id] = {
			type: 'minecraft:crafting_shapeless',
			inputs: uses.map(([item, count]) => ({ item, count })),
			outputs: [{ item: output, count: made }],
		};
	}
	return { ...data, recipes: {}, crafting };
}

describe('recipe graph', () => {
	test('mods come from the namespace', () => {
		expect(modOf('servus_storage:shuttle')).toBe('storage');
		expect(modOf('servus_foundation:data_cable')).toBe('foundation');
		expect(modOf('minecraft:redstone')).toBe('vanilla');
		expect(modOf('#minecraft:planks')).toBe('vanilla');
	});

	test('recipe kinds', () => {
		expect(recipeKind('servus_manufacturing:assembling', false)).toBe('Assembler');
		expect(recipeKind('minecraft:smelting', false)).toBe('Smelting');
		expect(recipeKind('servus_storage:heavy_rack', true)).toBe('Crafting');
		expect(recipeKind('servus_foo:bar', false)).toBe('servus_foo:bar');
	});

	test('only Servus recipes are drawn, whatever they make', () => {
		const graph = buildRecipeGraph(data);
		expect(graph.recipes.has('minecraft:iron_ingot_from_smelting_raw_iron')).toBe(false);
		expect(graph.recipes.has('servus_manufacturing:copper_ingot_from_grit_smelting')).toBe(true);
		expect(graph.producers.get('minecraft:copper_ingot')).toContain('servus_manufacturing:copper_ingot_from_grit_smelting');
	});

	test('redstone is a leaf', () => {
		const graph = buildRecipeGraph(data);
		expect(graph.items).toContain('minecraft:redstone');
		expect(graph.producers.has('minecraft:redstone')).toBe(false);
	});

	test('power cable edges carry their counts', () => {
		const graph = buildRecipeGraph(data);
		const cable = recipeNode('servus_foundation:power_cable');
		expect(graph.edges).toContainEqual({ from: itemNode('minecraft:copper_ingot'), to: cable, count: 6 });
		expect(graph.edges).toContainEqual({ from: cable, to: itemNode('servus_foundation:power_cable'), count: 8 });
	});

	test('hiding mods keeps only the visible recipes and their items', () => {
		const graph = buildRecipeGraph(data, ['storage']);
		expect([...graph.recipes.values()].every((recipe) => recipe.mod === 'storage')).toBe(true);
		expect(graph.recipes.has('servus_storage:shuttle')).toBe(true);
		expect(graph.items).not.toContain('servus_manufacturing:copper_wire');
	});

	test('ancestors of the shuttle reach the leaves and skip unrelated recipes', () => {
		const graph = buildRecipeGraph(data);
		const { nodes, edges } = ancestors(graph, 'servus_storage:shuttle');
		for (const node of [
			recipeNode('servus_storage:shuttle'),
			recipeNode('servus_storage:rail'),
			itemNode('servus_storage:rail'),
			itemNode('minecraft:iron_ingot'),
			itemNode('minecraft:redstone'),
			itemNode('minecraft:stick'),
		]) {
			expect(nodes).toContain(node);
		}
		expect(nodes).not.toContain(recipeNode('servus_storage:crate'));
		expect(edges).toContain(edgeId({ from: itemNode('minecraft:stick'), to: recipeNode('servus_storage:rail') }));
	});

	test('the facade is a special node without edges', () => {
		const graph = buildRecipeGraph(data);
		const facade = graph.recipes.get('servus_foundation:facade')!;
		expect(facade.special).toBe(true);
		expect(facade.kind).toBe('Special');
		const node = recipeNode('servus_foundation:facade');
		expect(graph.edges.some((edge) => edge.from === node || edge.to === node)).toBe(false);
	});
});

describe('raw cost', () => {
	test('one power cable', () => {
		const cost = rawCost(buildRecipeGraph(data), 'servus_foundation:power_cable');
		expect(cost.amounts.get('minecraft:copper_ingot')).toBeCloseTo(0.75);
		expect(cost.amounts.get('minecraft:redstone')).toBeCloseTo(0.25);
		expect(cost.amounts.get('minecraft:glass')).toBeCloseTo(0.125);
		expect(cost.amounts.size).toBe(3);
	});

	test('vanilla items are not expanded', () => {
		const cost = rawCost(buildRecipeGraph(data), 'servus_manufacturing:copper_wire');
		expect([...cost.amounts]).toEqual([['minecraft:copper_ingot', 0.5]]);
	});

	test('Servus intermediates are expanded', () => {
		const cost = rawCost(buildRecipeGraph(data), 'servus_storage:shuttle');
		// 5 iron in the shuttle plus 6 of the 8 per rail craft: 5 + 6 / 8.
		expect(cost.amounts.get('minecraft:iron_ingot')).toBeCloseTo(5.75);
		expect(cost.amounts.has('servus_storage:rail')).toBe(false);
	});

	test('a Servus item without a recipe is marked', () => {
		const graph = buildRecipeGraph(fixture({ 'servus_foundation:board': [['servus_foundation:chip', 2, 'servus_foundation:board', 1]] }));
		const cost = rawCost(graph, 'servus_foundation:board');
		expect(cost.noRecipe).toContain('servus_foundation:chip');
		expect(cost.amounts.get('servus_foundation:chip')).toBe(2);
	});

	test('a cycle stops without looping', () => {
		const graph = buildRecipeGraph(
			fixture({
				'servus_foundation:a': [['servus_foundation:b', 1, 'servus_foundation:a', 1]],
				'servus_foundation:b': [['servus_foundation:a', 1, 'servus_foundation:b', 1]],
			}),
		);
		const cost = rawCost(graph, 'servus_foundation:a');
		expect(cost.cycles).toContain('servus_foundation:a');
		expect(cost.amounts.size).toBe(0);
	});

	test('two routes give a choice and switching changes the cost', () => {
		const graph = buildRecipeGraph(
			fixture({
				'servus_foundation:circuit_by_hand': [['minecraft:copper_ingot', 4, 'servus_foundation:circuit', 1]],
				'servus_foundation:circuit_in_assembler': [['minecraft:copper_ingot', 2, 'servus_foundation:circuit', 1]],
				'servus_storage:shuttle': [['servus_foundation:circuit', 2, 'servus_storage:shuttle', 1]],
			}),
		);
		const first = rawCost(graph, 'servus_storage:shuttle');
		expect(first.routes.get('servus_foundation:circuit')).toEqual({
			options: ['servus_foundation:circuit_by_hand', 'servus_foundation:circuit_in_assembler'],
			chosen: 'servus_foundation:circuit_by_hand',
		});
		expect(first.amounts.get('minecraft:copper_ingot')).toBe(8);

		const switched = rawCost(graph, 'servus_storage:shuttle', { 'servus_foundation:circuit': 'servus_foundation:circuit_in_assembler' });
		expect(switched.amounts.get('minecraft:copper_ingot')).toBe(4);
	});
});
