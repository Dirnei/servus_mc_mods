import LZString from 'lz-string';
import { z } from 'zod';
import {
	applyDraft,
	type Draft,
	draftItemSchema,
	draftMachineSchema,
	draftRecipeSchema,
	draftSchema,
	type Edit,
	getNumber,
	type Path,
} from './draft';
import { type GameData, recipeSchema } from './gamedata';
import { type Chain, computeChain } from './ratios';

/** The proposal file format this site writes and reads. */
export const PROPOSAL_SCHEMA = 1;
/** Longest share link fragment; bigger drafts go through export. */
export const MAX_SHARE_LENGTH = 8000;

export interface BuildInfo {
	schema: number;
	mod_version: string;
	commit: string;
}

const summarySchema = z.object({
	machines: z.record(z.string(), z.number()),
	fe_per_tick: z.number(),
	generators: z.number(),
	fuel_per_second: z.number(),
});

const proposalSchema = z.object({
	servus_proposal: z.literal(PROPOSAL_SCHEMA),
	title: z.string(),
	data: z.object({ schema: z.number(), mod_version: z.string(), commit: z.string() }),
	edits: z.array(
		z.object({
			path: z.array(z.union([z.string(), z.number()])),
			baseline: z.number().nullable(),
			value: z.number(),
		}),
	),
	new: z.object({
		items: z.array(draftItemSchema),
		recipes: z.array(draftRecipeSchema),
		machines: z.array(draftMachineSchema.extend({ needs_code: z.literal(true) })),
	}),
	changed_recipes: z.array(z.object({ baseline: recipeSchema, recipe: draftRecipeSchema })),
	routes: z.record(z.string(), z.string()),
	impact: z
		.object({
			target: z.object({ item: z.string(), per_second: z.number() }),
			before: summarySchema,
			after: summarySchema,
		})
		.nullable(),
	notes: z.string(),
});

export type Proposal = z.infer<typeof proposalSchema>;

export interface Conflict {
	kind: 'edit' | 'recipe';
	path: Path;
	/** The proposal's baseline: a number for edits, a recipe for changed recipes. */
	baseline: unknown;
	current: unknown;
	proposed: unknown;
}

export class ProposalError extends Error {}

function summary(chain: Chain): z.infer<typeof summarySchema> {
	return {
		machines: chain.totals.machines,
		fe_per_tick: round(chain.totals.fePerTick),
		generators: chain.totals.generators,
		fuel_per_second: round(chain.totals.fuelPerSecond),
	};
}

function round(value: number): number {
	return Math.round(value * 1e6) / 1e6;
}

/** Builds the proposal file for a draft against the committed data, with the impact on one chain. */
export function exportProposal(
	base: GameData,
	draft: Draft,
	build: BuildInfo,
	target: { item: string; perSecond: number } | null,
): Proposal {
	const applied = applyDraft(base, draft);
	const impact =
		target === null
			? null
			: {
					target: { item: target.item, per_second: target.perSecond },
					before: summary(computeChain(base, target)),
					after: summary(computeChain(applied.data, target, { routes: draft.routes, flags: applied.flags })),
				};
	return {
		servus_proposal: PROPOSAL_SCHEMA,
		title: draft.title,
		data: { schema: build.schema, mod_version: build.mod_version, commit: build.commit },
		edits: draft.edits.map((edit) => ({
			path: edit.path,
			baseline: getNumber(applyDraft(base, { ...draft, edits: [] }).data, edit.path) ?? null,
			value: edit.value,
		})),
		new: {
			items: draft.items,
			recipes: draft.recipes.filter((recipe) => !base.recipes[recipe.id]),
			machines: draft.machines.map((machine) => ({ ...machine, needs_code: true as const })),
		},
		changed_recipes: draft.recipes
			.filter((recipe) => base.recipes[recipe.id])
			.map((recipe) => ({ baseline: base.recipes[recipe.id], recipe })),
		routes: draft.routes,
		impact,
		notes: draft.notes,
	};
}

export function proposalFileName(proposal: Proposal): string {
	const slug = proposal.title
		.toLowerCase()
		.replace(/[^a-z0-9]+/g, '-')
		.replace(/^-|-$/g, '');
	return `servus-proposal-${slug || 'draft'}.json`;
}

/**
 * Reads a proposal file back into a draft. Edits and changed recipes whose baseline no longer matches the current
 * data are left out of the draft and returned as conflicts.
 */
export function importProposal(base: GameData, input: unknown): { draft: Draft; conflicts: Conflict[] } {
	const version = (input as { servus_proposal?: unknown } | null)?.servus_proposal;
	if (version !== PROPOSAL_SCHEMA) {
		throw new ProposalError(
			`This proposal has schema ${String(version)}, this site understands schema ${PROPOSAL_SCHEMA}.`,
		);
	}
	const parsed = proposalSchema.safeParse(input);
	if (!parsed.success) {
		const issue = parsed.error.issues[0];
		throw new ProposalError(`Invalid proposal at ${issue.path.join('.')}: ${issue.message}`);
	}
	const proposal = parsed.data;
	const conflicts: Conflict[] = [];

	const recipes = [...proposal.new.recipes];
	for (const change of proposal.changed_recipes) {
		const current = base.recipes[change.recipe.id];
		if (current && JSON.stringify(current) === JSON.stringify(change.baseline)) {
			recipes.push(change.recipe);
		} else {
			conflicts.push({
				kind: 'recipe',
				path: ['recipes', change.recipe.id],
				baseline: change.baseline,
				current: current ?? null,
				proposed: change.recipe,
			});
		}
	}

	const draftWithoutEdits = draftSchema.parse({
		title: proposal.title,
		notes: proposal.notes,
		items: proposal.new.items,
		recipes,
		machines: proposal.new.machines.map(({ needs_code: _, ...machine }) => machine),
		routes: proposal.routes,
	});
	const drafted = applyDraft(base, draftWithoutEdits).data;
	const edits: Edit[] = [];
	for (const edit of proposal.edits) {
		const current = getNumber(drafted, edit.path) ?? null;
		if (current === edit.baseline) {
			edits.push({ path: edit.path, value: edit.value });
		} else {
			conflicts.push({ kind: 'edit', path: edit.path, baseline: edit.baseline, current, proposed: edit.value });
		}
	}
	return { draft: { ...draftWithoutEdits, edits }, conflicts };
}

/** Applies a conflicting edit or recipe anyway, on the user's request. */
export function acceptConflict(draft: Draft, conflict: Conflict): Draft {
	if (conflict.kind === 'edit') {
		return { ...draft, edits: [...draft.edits, { path: conflict.path, value: conflict.proposed as number }] };
	}
	return { ...draft, recipes: [...draft.recipes, draftRecipeSchema.parse(conflict.proposed)] };
}

/** The URL fragment value for a share link, or null when the draft is too large for a link. */
export function encodeShare(draft: Draft): string | null {
	const encoded = LZString.compressToEncodedURIComponent(JSON.stringify(draft));
	return encoded.length > MAX_SHARE_LENGTH ? null : encoded;
}

export function decodeShare(encoded: string): Draft {
	const json = LZString.decompressFromEncodedURIComponent(encoded);
	if (!json) {
		throw new ProposalError('This share link is damaged or incomplete.');
	}
	let value: unknown;
	try {
		value = JSON.parse(json);
	} catch {
		throw new ProposalError('This share link is damaged or incomplete.');
	}
	const parsed = draftSchema.safeParse(value);
	if (!parsed.success) {
		throw new ProposalError(`This share link holds an invalid draft: ${parsed.error.issues[0].message}`);
	}
	return parsed.data;
}
