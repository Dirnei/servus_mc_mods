import { describe, expect, test } from 'vitest';
// Tier 1 data from before the alchemy rethink, until the lab is rebuilt on alchemy data.
import raw from './fixtures/tier1-gamedata.json';
import { GameDataError, parseGameData, stackKey } from './gamedata';

describe('game data', () => {
	test('the committed file parses', () => {
		const data = parseGameData(raw);
		expect(data.schema).toBe(2);
		expect(data.machines['servus_manufacturing:burner_extractor'].work_ticks).toBe(80);
		expect(data.recipes['servus_manufacturing:assembling/red_pack'].time).toBe(100);
		expect(data.crafting['servus_foundation:power_cable'].pattern).toEqual(['CCC', 'RGR', 'CCC']);
		expect(data.crafting['servus_foundation:facade'].special).toBe(true);
	});

	test('another schema is rejected naming both versions', () => {
		expect(() => parseGameData({ ...raw, schema: 1 })).toThrowError(GameDataError);
		expect(() => parseGameData({ ...raw, schema: 1 })).toThrowError(/schema 1.*schema 2/);
	});

	test('invalid content names the path', () => {
		const broken = structuredClone(raw) as Record<string, any>;
		broken.recipes['servus_manufacturing:assembling/red_pack'].time = -1;
		expect(() => parseGameData(broken)).toThrowError(/recipes\.servus_manufacturing:assembling\/red_pack\.time/);
	});

	test('stack keys mark tags', () => {
		expect(stackKey({ item: 'minecraft:coal', count: 1 })).toBe('minecraft:coal');
		expect(stackKey({ tag: 'c:ingots/iron', count: 1 })).toBe('#c:ingots/iron');
	});
});
