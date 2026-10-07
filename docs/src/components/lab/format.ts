import type { GameData } from '../../lib/gamedata';

export function name(data: GameData, id: string): string {
	if (id.startsWith('#')) {
		return id;
	}
	return data.items[id]?.name ?? id.slice(id.indexOf(':') + 1);
}

/** A number with up to `digits` decimals and no trailing zeros. */
export function num(value: number, digits = 2): string {
	if (!Number.isFinite(value)) {
		return value > 0 ? 'never' : '-';
	}
	return Number(value.toFixed(digits)).toString();
}

export function percent(value: number): string {
	return `${Math.round(value * 100)}%`;
}

/** Rates read best per minute when they are small. */
export function rate(perSecond: number): string {
	if (perSecond > 0 && perSecond < 1) {
		return `${num(perSecond * 60, 1)}/min`;
	}
	return `${num(perSecond)}/s`;
}

export function seconds(value: number): string {
	if (!Number.isFinite(value)) {
		return 'never';
	}
	if (value >= 120) {
		return `${num(value / 60, 1)} min`;
	}
	return `${num(value, 1)} s`;
}
