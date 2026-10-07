import { z } from 'zod';
import { type GameData, stackSchema } from './gamedata';
import type { Flags } from './ratios';

/**
 * A draft: number edits on the game data plus drafted items, recipes and concept machines. Applying a draft to
 * the committed game data gives the effective data the lab computes with.
 */

const id = z.string().min(1);
const path = z.array(z.union([z.string(), z.number().int().nonnegative()])).min(2);

export const editSchema = z.object({ path, value: z.number().finite() });

export const draftItemSchema = z.object({ id, name: z.string().min(1), note: z.string().default('') });

/** A drafted recipe. Using the id of an existing recipe replaces that recipe. */
export const draftRecipeSchema = z.object({
	id,
	type: id,
	inputs: z.array(stackSchema),
	outputs: z.array(stackSchema).min(1),
	time: z.number().int().positive(),
	note: z.string().default(''),
});

export const draftMachineSchema = z.object({
	id,
	name: z.string().min(1),
	kind: z.enum(['burner', 'powered', 'crafter']),
	recipe_types: z.array(id).min(1),
	work_ticks: z.number().int().positive(),
	energy_per_tick: z.number().int().nonnegative(),
	fuel_divisor: z.number().int().positive().default(1),
	note: z.string().default(''),
});

export const draftSchema = z.object({
	title: z.string().default(''),
	notes: z.string().default(''),
	edits: z.array(editSchema).default([]),
	items: z.array(draftItemSchema).default([]),
	recipes: z.array(draftRecipeSchema).default([]),
	machines: z.array(draftMachineSchema).default([]),
	/** Chosen route key per item. */
	routes: z.record(id, z.string()).default({}),
});

export type Edit = z.infer<typeof editSchema>;
export type DraftItem = z.infer<typeof draftItemSchema>;
export type DraftRecipe = z.infer<typeof draftRecipeSchema>;
export type DraftMachine = z.infer<typeof draftMachineSchema>;
export type Draft = z.infer<typeof draftSchema>;
export type Path = Edit['path'];

export function emptyDraft(): Draft {
	return draftSchema.parse({});
}

export function isEmptyDraft(draft: Draft): boolean {
	return (
		draft.edits.length === 0 &&
		draft.items.length === 0 &&
		draft.recipes.length === 0 &&
		draft.machines.length === 0 &&
		Object.keys(draft.routes).length === 0 &&
		draft.title === '' &&
		draft.notes === ''
	);
}

export function pathKey(p: Path): string {
	return JSON.stringify(p);
}

/** The number at a path, or undefined when the path does not exist or is not a number. */
export function getNumber(data: unknown, p: Path): number | undefined {
	let node: unknown = data;
	for (const part of p) {
		if (node === null || typeof node !== 'object') {
			return undefined;
		}
		node = (node as Record<string | number, unknown>)[part];
	}
	return typeof node === 'number' ? node : undefined;
}

function setNumber(data: unknown, p: Path, value: number): boolean {
	let node: unknown = data;
	for (const part of p.slice(0, -1)) {
		if (node === null || typeof node !== 'object') {
			return false;
		}
		node = (node as Record<string | number, unknown>)[part];
	}
	const last = p[p.length - 1];
	if (node === null || typeof node !== 'object' || typeof (node as Record<string | number, unknown>)[last] !== 'number') {
		return false;
	}
	(node as Record<string | number, unknown>)[last] = value;
	return true;
}

export interface Applied {
	data: GameData;
	flags: Flags;
	/** Edits whose path does not exist in the data. */
	missing: Edit[];
}

/** Applies drafted content first, then the number edits, so edits may target drafted content too. */
export function applyDraft(base: GameData, draft: Draft): Applied {
	const data = structuredClone(base);
	const items = new Set<string>();
	const recipes = new Set<string>();
	const machines = new Set<string>();

	for (const item of draft.items) {
		data.items[item.id] = { name: item.name };
		items.add(item.id);
	}
	for (const machine of draft.machines) {
		data.machines[machine.id] = {
			kind: machine.kind,
			recipe_types: machine.recipe_types,
			work_ticks: machine.work_ticks,
			energy_per_tick: machine.kind === 'burner' ? 0 : machine.energy_per_tick,
			energy_capacity: 0,
			fuel_divisor: machine.fuel_divisor,
			items_per_tick: 0,
			recipe_slots: 1,
		};
		machines.add(machine.id);
		if (!data.items[machine.id]) {
			data.items[machine.id] = { name: machine.name };
		}
	}
	for (const recipe of draft.recipes) {
		data.recipes[recipe.id] = { type: recipe.type, inputs: recipe.inputs, outputs: recipe.outputs, time: recipe.time };
		recipes.add(recipe.id);
	}

	const missing: Edit[] = [];
	for (const edit of draft.edits) {
		if (!setNumber(data, edit.path, edit.value)) {
			missing.push(edit);
		}
	}
	return { data, flags: { items, recipes, machines }, missing };
}

/** Sets or clears one number edit. Setting the baseline value removes the edit. */
export function withEdit(draft: Draft, base: GameData, p: Path, value: number): Draft {
	const key = pathKey(p);
	const edits = draft.edits.filter((edit) => pathKey(edit.path) !== key);
	if (getNumber(base, p) !== value) {
		edits.push({ path: p, value });
	}
	return { ...draft, edits };
}

export function editFor(draft: Draft, p: Path): Edit | undefined {
	const key = pathKey(p);
	return draft.edits.find((edit) => pathKey(edit.path) === key);
}
