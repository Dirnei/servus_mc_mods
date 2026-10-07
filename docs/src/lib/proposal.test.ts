import { describe, expect, test } from 'vitest';
import raw from '../data/gamedata.json';
import { draftSchema, emptyDraft, withEdit } from './draft';
import { parseGameData } from './gamedata';
import {
	acceptConflict,
	decodeShare,
	encodeShare,
	exportProposal,
	importProposal,
	MAX_SHARE_LENGTH,
	ProposalError,
	proposalFileName,
} from './proposal';

const data = parseGameData(raw);
const BUILD = { schema: 1, mod_version: '0.1.0', commit: 'abc1234' };
const RED_PACK = 'servus_manufacturing:red_pack';
const PACK_TIME = ['recipes', 'servus_manufacturing:assembling/red_pack', 'time'];
const TARGET = { item: RED_PACK, perSecond: 0.4 };

const sampleDraft = () =>
	draftSchema.parse({
		title: 'Faster packs',
		notes: 'Red Packs feel slow.',
		edits: [{ path: PACK_TIME, value: 50 }],
		items: [{ id: 'draft:copper_coil', name: 'Copper Coil', note: 'Tier 1 intermediate' }],
		machines: [
			{
				id: 'draft:wire_drawer',
				name: 'Wire Drawer',
				kind: 'powered',
				recipe_types: ['draft:wire_drawing'],
				work_ticks: 10,
				energy_per_tick: 30,
			},
		],
		routes: { 'servus_manufacturing:copper_wire': 'recipe:x@y' },
	});

describe('proposal export', () => {
	test('holds edits with baselines, new content, notes and impact', () => {
		const proposal = exportProposal(data, sampleDraft(), BUILD, TARGET);
		expect(proposal.edits).toEqual([{ path: PACK_TIME, baseline: 100, value: 50 }]);
		expect(proposal.new.items[0].name).toBe('Copper Coil');
		expect(proposal.new.machines[0].needs_code).toBe(true);
		expect(proposal.notes).toBe('Red Packs feel slow.');
		expect(proposal.data.commit).toBe('abc1234');
		expect(proposal.impact?.before.machines['servus_manufacturing:assembler']).toBeGreaterThan(
			proposal.impact!.after.machines['servus_manufacturing:assembler'],
		);
		expect(proposalFileName(proposal)).toBe('servus-proposal-faster-packs.json');
	});

	test('a replaced recipe is exported with its baseline', () => {
		const recipe = {
			id: 'servus_manufacturing:assembling/red_pack',
			type: 'servus_manufacturing:assembling',
			inputs: [{ item: 'servus_manufacturing:iron_gear', count: 2 }],
			outputs: [{ item: RED_PACK, count: 1 }],
			time: 100,
		};
		const proposal = exportProposal(data, draftSchema.parse({ recipes: [recipe] }), BUILD, null);
		expect(proposal.new.recipes).toEqual([]);
		expect(proposal.changed_recipes[0].baseline).toEqual(data.recipes[recipe.id]);
		expect(proposal.impact).toBeNull();
	});
});

describe('proposal import', () => {
	test('export then import restores the draft', () => {
		const draft = sampleDraft();
		const file = JSON.parse(JSON.stringify(exportProposal(data, draft, BUILD, TARGET)));
		const { draft: restored, conflicts } = importProposal(data, file);
		expect(conflicts).toEqual([]);
		expect(restored).toEqual(draft);
	});

	test('a moved baseline is a conflict and is not applied', () => {
		const proposal = exportProposal(data, withEdit(emptyDraft(), data, PACK_TIME, 40), BUILD, null);
		proposal.edits[0].baseline = 80;
		const current = structuredClone(data);
		current.recipes['servus_manufacturing:assembling/red_pack'].time = 60;

		const { draft, conflicts } = importProposal(current, proposal);
		expect(draft.edits).toEqual([]);
		expect(conflicts).toEqual([{ kind: 'edit', path: PACK_TIME, baseline: 80, current: 60, proposed: 40 }]);
		expect(acceptConflict(draft, conflicts[0]).edits).toEqual([{ path: PACK_TIME, value: 40 }]);
	});

	test('a changed recipe whose baseline moved is a conflict', () => {
		const recipe = {
			id: 'servus_manufacturing:assembling/red_pack',
			type: 'servus_manufacturing:assembling',
			inputs: [{ item: 'servus_manufacturing:iron_gear', count: 2 }],
			outputs: [{ item: RED_PACK, count: 1 }],
			time: 100,
		};
		const proposal = exportProposal(data, draftSchema.parse({ recipes: [recipe] }), BUILD, null);
		const current = structuredClone(data);
		current.recipes[recipe.id].time = 60;
		const { draft, conflicts } = importProposal(current, proposal);
		expect(draft.recipes).toEqual([]);
		expect(conflicts[0].kind).toBe('recipe');
	});

	test('schema 2 is rejected naming both versions', () => {
		const proposal = { ...exportProposal(data, sampleDraft(), BUILD, null), servus_proposal: 2 };
		expect(() => importProposal(data, proposal)).toThrowError(ProposalError);
		expect(() => importProposal(data, proposal)).toThrowError(/schema 2.*schema 1/);
	});

	test('invalid content is rejected with its path', () => {
		const proposal = JSON.parse(JSON.stringify(exportProposal(data, sampleDraft(), BUILD, null)));
		proposal.edits[0].value = 'fast';
		expect(() => importProposal(data, proposal)).toThrowError(/edits\.0\.value/);
	});
});

describe('share links', () => {
	test('encode then decode gives the same draft', () => {
		const draft = sampleDraft();
		const encoded = encodeShare(draft);
		expect(encoded).not.toBeNull();
		expect(decodeShare(encoded!)).toEqual(draft);
	});

	test('an oversized draft is refused', () => {
		let seed = 1;
		const random = () => {
			seed = (seed * 16807) % 2147483647;
			return seed.toString(36);
		};
		const items = Array.from({ length: 800 }, (_, i) => ({ id: `draft:item_${i}_${random()}`, name: random() + random() }));
		expect(encodeShare(draftSchema.parse({ items }))).toBeNull();
		expect(MAX_SHARE_LENGTH).toBe(8000);
	});

	test('a damaged link gives a readable error', () => {
		expect(() => decodeShare('not-a-real-link')).toThrowError(ProposalError);
	});
});
