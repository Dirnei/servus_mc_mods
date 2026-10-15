import { type GameData, type Stack, stackKey } from './gamedata';

/** The Servus mods in display order. Everything else, tags included, counts as vanilla. */
export const MODS = ['foundation', 'alchemy', 'shuttles'] as const;
export type ServusMod = (typeof MODS)[number];
export type Mod = ServusMod | 'vanilla';

/** A drawn recipe: a production recipe or a crafting entry from the game data. */
export interface GraphRecipe {
	id: string;
	type: string;
	mod: ServusMod;
	kind: string;
	special: boolean;
	pattern?: string[];
	key?: Record<string, { item: string } | { tag: string }>;
	inputs: Stack[];
	outputs: Stack[];
}

export interface GraphEdge {
	from: string;
	to: string;
	count: number;
}

export interface RecipeGraph {
	recipes: Map<string, GraphRecipe>;
	/** Item and tag keys (tags start with '#') touched by a drawn recipe, sorted. */
	items: string[];
	/** Node ids: `recipeNode(id)` and `itemNode(key)`. */
	edges: GraphEdge[];
	/** Recipe ids making an item, sorted by id. */
	producers: Map<string, string[]>;
	/** Recipe ids using an item, sorted by id. */
	consumers: Map<string, string[]>;
}

export const recipeNode = (id: string) => `r:${id}`;
export const itemNode = (key: string) => `i:${key}`;
export const edgeId = (edge: { from: string; to: string }) => `${edge.from}>${edge.to}`;

/** The mod an item, tag or recipe id belongs to. */
export function modOf(key: string): Mod {
	const namespace = key.replace(/^#/, '').split(':')[0];
	const mod = namespace.startsWith('servus_') ? namespace.slice('servus_'.length) : '';
	return (MODS as readonly string[]).includes(mod) ? (mod as ServusMod) : 'vanilla';
}

const KINDS: Record<string, string> = {
	'servus_alchemy:assembling': 'Assembler',
	'servus_alchemy:crushing': 'Crusher',
	'minecraft:smelting': 'Smelting',
	'minecraft:blasting': 'Blasting',
};

/** What makes a recipe, as shown on its node. */
export function recipeKind(type: string, crafting: boolean): string {
	return KINDS[type] ?? (crafting ? 'Crafting' : type);
}

/** Every recipe in a `servus_*` namespace whose mod is visible, with the items it touches. */
export function buildRecipeGraph(data: GameData, visible: Iterable<ServusMod> = MODS): RecipeGraph {
	const shown = new Set(visible);
	const recipes = new Map<string, GraphRecipe>();
	const add = (recipe: GraphRecipe) => {
		if (shown.has(recipe.mod)) {
			recipes.set(recipe.id, recipe);
		}
	};
	for (const [id, recipe] of Object.entries(data.recipes)) {
		const mod = modOf(id);
		if (mod !== 'vanilla') {
			add({ id, type: recipe.type, mod, kind: recipeKind(recipe.type, false), special: false, inputs: recipe.inputs, outputs: recipe.outputs });
		}
	}
	for (const [id, entry] of Object.entries(data.crafting)) {
		const mod = modOf(id);
		if (mod !== 'vanilla') {
			add({
				id,
				type: entry.type,
				mod,
				kind: entry.special ? 'Special' : recipeKind(entry.type, true),
				special: entry.special ?? false,
				pattern: entry.pattern,
				key: entry.key,
				inputs: entry.inputs,
				outputs: entry.outputs,
			});
		}
	}

	const items = new Set<string>();
	const edges: GraphEdge[] = [];
	const producers = new Map<string, string[]>();
	const consumers = new Map<string, string[]>();
	const push = (map: Map<string, string[]>, key: string, id: string) => {
		const list = map.get(key) ?? [];
		if (!list.includes(id)) {
			list.push(id);
		}
		map.set(key, list);
	};
	for (const id of [...recipes.keys()].sort()) {
		const recipe = recipes.get(id)!;
		for (const input of recipe.inputs) {
			const key = stackKey(input);
			items.add(key);
			edges.push({ from: itemNode(key), to: recipeNode(id), count: input.count });
			push(consumers, key, id);
		}
		for (const output of recipe.outputs) {
			const key = stackKey(output);
			items.add(key);
			edges.push({ from: recipeNode(id), to: itemNode(key), count: output.count });
			push(producers, key, id);
		}
	}
	return { recipes, items: [...items].sort(), edges, producers, consumers };
}

/** The nodes and edges an item is made from: its producing recipes, their inputs, and so on down to the leaves. */
export function ancestors(graph: RecipeGraph, key: string): { nodes: Set<string>; edges: Set<string> } {
	const nodes = new Set<string>([itemNode(key)]);
	const edges = new Set<string>();
	const queue = [key];
	while (queue.length > 0) {
		const item = queue.shift()!;
		for (const id of graph.producers.get(item) ?? []) {
			const recipe = graph.recipes.get(id)!;
			edges.add(edgeId({ from: recipeNode(id), to: itemNode(item) }));
			if (nodes.has(recipeNode(id))) {
				continue;
			}
			nodes.add(recipeNode(id));
			for (const input of recipe.inputs) {
				const inputKey = stackKey(input);
				edges.add(edgeId({ from: itemNode(inputKey), to: recipeNode(id) }));
				if (!nodes.has(itemNode(inputKey))) {
					nodes.add(itemNode(inputKey));
					queue.push(inputKey);
				}
			}
		}
	}
	return { nodes, edges };
}

export interface RawCost {
	/** Item or tag key to amount needed for one of the selected item, sorted by key. */
	amounts: Map<string, number>;
	/** Servus items no drawn recipe makes; their amounts are in `amounts` too. */
	noRecipe: Set<string>;
	/** Items where the expansion met itself again and stopped. */
	cycles: Set<string>;
	/** Items in the tree with more than one producing recipe, and the recipe used. */
	routes: Map<string, { options: string[]; chosen: string }>;
}

/**
 * The raw cost of one item: inputs divided by the recipe's output count, expanded through Servus items only and
 * ending at vanilla items and tags. The selected item itself is always expanded. The route per item is
 * `chosen[item]` when it is one of its recipes, otherwise the first recipe by id.
 */
export function rawCost(graph: RecipeGraph, key: string, chosen: Record<string, string> = {}): RawCost {
	const amounts = new Map<string, number>();
	const noRecipe = new Set<string>();
	const cycles = new Set<string>();
	const routes = new Map<string, { options: string[]; chosen: string }>();
	const addAmount = (item: string, amount: number) => amounts.set(item, (amounts.get(item) ?? 0) + amount);

	const expand = (item: string, amount: number, path: string[]) => {
		const root = path.length === 0;
		if (!root && modOf(item) === 'vanilla') {
			addAmount(item, amount);
			return;
		}
		const options = graph.producers.get(item) ?? [];
		if (options.length === 0) {
			if (!root) {
				addAmount(item, amount);
			}
			noRecipe.add(item);
			return;
		}
		if (path.includes(item)) {
			cycles.add(item);
			return;
		}
		const recipeId = options.includes(chosen[item]) ? chosen[item] : options[0];
		if (options.length > 1) {
			routes.set(item, { options, chosen: recipeId });
		}
		const recipe = graph.recipes.get(recipeId)!;
		const made = recipe.outputs.filter((stack) => stackKey(stack) === item).reduce((sum, stack) => sum + stack.count, 0);
		for (const input of recipe.inputs) {
			expand(stackKey(input), (amount * input.count) / made, [...path, item]);
		}
	};
	expand(key, 1, []);
	return { amounts: new Map([...amounts].sort(([a], [b]) => a.localeCompare(b))), noRecipe, cycles, routes };
}
