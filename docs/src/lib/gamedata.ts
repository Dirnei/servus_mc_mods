import { z } from 'zod';

/** The game data schema this site understands. */
export const GAMEDATA_SCHEMA = 1;

const id = z.string().min(1);

export const stackSchema = z.union([
	z.object({ item: id, count: z.number().int().positive() }).strict(),
	z.object({ tag: id, count: z.number().int().positive() }).strict(),
]);

export const machineSchema = z.object({
	kind: z.enum(['burner', 'powered', 'crafter', 'generator']),
	recipe_types: z.array(id),
	work_ticks: z.number().int(),
	energy_per_tick: z.number().int(),
	energy_capacity: z.number().int(),
	fuel_divisor: z.number().int().positive(),
	items_per_tick: z.number().int(),
	recipe_slots: z.number().int(),
	fuel_reserve: z.number().int().nonnegative().default(0),
});

export const recipeSchema = z.object({
	type: id,
	inputs: z.array(stackSchema),
	outputs: z.array(stackSchema),
	time: z.number().int().positive(),
});

export const depositSchema = z.object({ block: id, yield: stackSchema });

export const techSchema = z.object({
	parents: z.array(id),
	cost: z.array(stackSchema),
	ticks_per_pack: z.number().int().positive(),
	unlocks: z.array(id),
});

export const gameDataSchema = z.object({
	schema: z.literal(GAMEDATA_SCHEMA),
	mod_version: z.string(),
	machines: z.record(id, machineSchema),
	recipes: z.record(id, recipeSchema),
	deposits: z.record(id, depositSchema),
	techs: z.record(id, techSchema),
	fuels: z.record(id, z.number().int().positive()),
	items: z.record(id, z.object({ name: z.string() })),
});

export type Stack = z.infer<typeof stackSchema>;
export type MachineData = z.infer<typeof machineSchema>;
export type RecipeData = z.infer<typeof recipeSchema>;
export type DepositData = z.infer<typeof depositSchema>;
export type TechData = z.infer<typeof techSchema>;
export type GameData = z.infer<typeof gameDataSchema>;

export class GameDataError extends Error {}

/** Parses game data, with a readable error for a wrong schema version or invalid content. */
export function parseGameData(raw: unknown): GameData {
	const schema = (raw as { schema?: unknown } | null)?.schema;
	if (schema !== GAMEDATA_SCHEMA) {
		throw new GameDataError(
			`Game data has schema ${String(schema)}, this site understands schema ${GAMEDATA_SCHEMA}.`,
		);
	}
	const result = gameDataSchema.safeParse(raw);
	if (!result.success) {
		const issue = result.error.issues[0];
		throw new GameDataError(`Invalid game data at ${issue.path.join('.')}: ${issue.message}`);
	}
	return result.data;
}

/** The item id of a stack, or the tag id prefixed with '#'. */
export function stackKey(stack: Stack): string {
	return 'item' in stack ? stack.item : `#${stack.tag}`;
}

export function itemName(data: GameData, key: string): string {
	return data.items[key]?.name ?? key;
}
