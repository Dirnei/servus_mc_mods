import { useState } from 'preact/hooks';
import { gameData } from '../../lib/committed';
import { emptyDraft, isEmptyDraft } from '../../lib/draft';
import {
	acceptConflict,
	type Conflict,
	encodeShare,
	exportProposal,
	importProposal,
	MAX_SHARE_LENGTH,
	proposalFileName,
} from '../../lib/proposal';
import { shareUrl, type LabState } from './store';

declare global {
	interface Window {
		SERVUS_COMMIT?: string;
	}
}

function describe(value: unknown): string {
	return typeof value === 'number' || value === null ? String(value ?? 'missing') : JSON.stringify(value);
}

async function copy(text: string): Promise<boolean> {
	try {
		await navigator.clipboard.writeText(text);
		return true;
	} catch {
		return false;
	}
}

export function ProposalBar({ lab, target }: { lab: LabState; target: { item: string; perSecond: number } | null }) {
	const { draft, setDraft, setNotice } = lab;
	const [conflicts, setConflicts] = useState<Conflict[]>([]);
	const [shareText, setShareText] = useState<string | null>(null);

	const proposal = () =>
		exportProposal(gameData, draft, { schema: gameData.schema, mod_version: gameData.mod_version, commit: window.SERVUS_COMMIT ?? 'unknown' }, target);

	const download = () => {
		const file = proposal();
		const blob = new Blob([`${JSON.stringify(file, null, 2)}\n`], { type: 'application/json' });
		const url = URL.createObjectURL(blob);
		const link = document.createElement('a');
		link.href = url;
		link.download = proposalFileName(file);
		link.click();
		URL.revokeObjectURL(url);
	};

	const importFile = async (file: File) => {
		try {
			const result = importProposal(gameData, JSON.parse(await file.text()));
			setDraft(result.draft);
			setConflicts(result.conflicts);
			setNotice(
				result.conflicts.length > 0
					? `Imported "${file.name}" with ${result.conflicts.length} conflict(s). They are listed below and not applied.`
					: `Imported "${file.name}".`,
			);
		} catch (error) {
			setNotice(`Could not import "${file.name}": ${(error as Error).message} Your current draft is unchanged.`);
		}
	};

	const share = async () => {
		const encoded = encodeShare(draft);
		if (encoded === null) {
			setShareText(null);
			setNotice(`This draft is too large for a link (over ${MAX_SHARE_LENGTH} characters). Use Export instead.`);
			return;
		}
		const url = shareUrl(encoded);
		setShareText(url);
		setNotice((await copy(url)) ? 'Share link copied.' : 'Copy the share link below.');
	};

	return (
		<section class="lab-proposal" aria-label="Proposal">
			<div class="lab-controls">
				<label class="lab-grow">
					Proposal title{' '}
					<input value={draft.title} placeholder="Faster Red Packs" onInput={(e) => setDraft({ ...draft, title: e.currentTarget.value })} />
				</label>
				<button type="button" onClick={download}>Export</button>
				<button
					type="button"
					onClick={async () => setNotice((await copy(JSON.stringify(proposal(), null, 2))) ? 'Proposal JSON copied.' : 'Copying failed; use Export.')}
				>
					Copy JSON
				</button>
				<label class="lab-file">
					Import
					<input
						type="file"
						accept="application/json,.json"
						onChange={(e) => {
							const file = e.currentTarget.files?.[0];
							if (file) void importFile(file);
							e.currentTarget.value = '';
						}}
					/>
				</label>
				<button type="button" onClick={share}>Share link</button>
				<button
					type="button"
					disabled={isEmptyDraft(draft)}
					onClick={() => {
						if (window.confirm('Clear all edits, drafts and notes?')) {
							setDraft(emptyDraft());
							setConflicts([]);
						}
					}}
				>
					Clear
				</button>
			</div>
			<label class="lab-notes">
				Notes for the proposal
				<textarea rows={2} value={draft.notes} placeholder="Why this change?" onInput={(e) => setDraft({ ...draft, notes: e.currentTarget.value })} />
			</label>
			{shareText && <input class="lab-share" readOnly value={shareText} aria-label="Share link" onFocus={(e) => e.currentTarget.select()} />}
			{conflicts.length > 0 && (
				<div class="lab-conflicts" role="alert">
					<h3>Conflicts</h3>
					<p>The game data changed since this proposal was written. These parts were not applied:</p>
					<ul>
						{conflicts.map((conflict, i) => (
							<li key={i}>
								<code>{conflict.path.join(' / ')}</code>: proposal baseline {describe(conflict.baseline)}, now{' '}
								{describe(conflict.current)}, proposed {describe(conflict.proposed)}{' '}
								<button
									type="button"
									onClick={() => {
										setDraft(acceptConflict(draft, conflict));
										setConflicts(conflicts.filter((_, j) => j !== i));
									}}
								>
									Apply anyway
								</button>{' '}
								<button type="button" onClick={() => setConflicts(conflicts.filter((_, j) => j !== i))}>Drop</button>
							</li>
						))}
					</ul>
				</div>
			)}
		</section>
	);
}
