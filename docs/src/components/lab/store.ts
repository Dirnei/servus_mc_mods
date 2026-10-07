import { useEffect, useMemo, useState } from 'preact/hooks';
import { gameData } from '../../lib/committed';
import { type Applied, applyDraft, type Draft, draftSchema, emptyDraft } from '../../lib/draft';
import { decodeShare } from '../../lib/proposal';

/** Draft state shared by the lab pages: restored from a share link or browser storage, saved on change. */

const STORAGE_KEY = 'servus-lab-draft-v1';
const SHARE_PREFIX = '#d=';

function readStorage(): Draft | null {
	try {
		const raw = window.localStorage.getItem(STORAGE_KEY);
		if (!raw) {
			return null;
		}
		const parsed = draftSchema.safeParse(JSON.parse(raw));
		return parsed.success ? parsed.data : null;
	} catch {
		return null;
	}
}

function writeStorage(draft: Draft) {
	try {
		window.localStorage.setItem(STORAGE_KEY, JSON.stringify(draft));
	} catch {
		// Storage can be unavailable (private mode, blocked site data); the lab still works for this visit.
	}
}

export interface InitialDraft {
	draft: Draft;
	/** A message for the user, for example a damaged share link. */
	notice: string | null;
	fromLink: boolean;
}

function initial(): InitialDraft {
	const hash = window.location.hash;
	if (hash.startsWith(SHARE_PREFIX)) {
		try {
			return { draft: decodeShare(hash.slice(SHARE_PREFIX.length)), notice: null, fromLink: true };
		} catch (error) {
			return { draft: readStorage() ?? emptyDraft(), notice: (error as Error).message, fromLink: false };
		}
	}
	return { draft: readStorage() ?? emptyDraft(), notice: null, fromLink: false };
}

export interface LabState {
	draft: Draft;
	setDraft: (draft: Draft) => void;
	applied: Applied;
	notice: string | null;
	setNotice: (notice: string | null) => void;
}

export function useLab(): LabState {
	const [start] = useState(initial);
	const [draft, setDraftState] = useState<Draft>(start.draft);
	const [notice, setNotice] = useState<string | null>(
		start.notice ?? (start.fromLink ? 'Loaded the draft from the share link. It replaced your saved draft.' : null),
	);

	useEffect(() => {
		writeStorage(draft);
	}, [draft]);

	useEffect(() => {
		if (start.fromLink) {
			// Drop the fragment so a reload keeps later edits instead of reloading the link.
			history.replaceState(null, '', window.location.pathname + window.location.search);
		}
		const onStorage = (event: StorageEvent) => {
			if (event.key === STORAGE_KEY) {
				const restored = readStorage();
				if (restored) {
					setDraftState(restored);
				}
			}
		};
		window.addEventListener('storage', onStorage);
		return () => window.removeEventListener('storage', onStorage);
	}, []);

	const applied = useMemo(() => applyDraft(gameData, draft), [draft]);
	return { draft, setDraft: setDraftState, applied, notice, setNotice };
}

export function shareUrl(encoded: string): string {
	return `${window.location.origin}${window.location.pathname}${SHARE_PREFIX}${encoded}`;
}
