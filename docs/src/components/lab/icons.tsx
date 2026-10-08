/**
 * Item icons rendered by the `iconExport` run. Servus icons are committed; vanilla icons exist only where someone
 * rendered them locally, so a missing file just means no icon.
 */
const files = import.meta.glob('../../icons/**/*.png', { eager: true, query: '?url', import: 'default' }) as Record<string, string>;

const ICONS = new Map<string, string>(
	Object.entries(files).flatMap(([path, url]) => {
		const match = path.match(/icons\/([^/]+)\/(.+)\.png$/);
		return match ? [[`${match[1]}:${match[2]}`, url] as [string, string]] : [];
	}),
);

/** The icon URL of an item id, none for tags or items without an exported icon. */
export function iconUrl(key: string): string | undefined {
	return key.startsWith('#') ? undefined : ICONS.get(key);
}

/** An item's icon, or nothing. */
export function ItemIcon({ item, size = 24 }: { item: string; size?: number }) {
	const url = iconUrl(item);
	return url ? <img class="lab-icon" src={url} alt="" width={size} height={size} loading="lazy" /> : null;
}
