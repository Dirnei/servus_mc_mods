import { execSync } from 'node:child_process';

/** Short commit the site is built from: GITHUB_SHA in CI, git locally, "unknown" otherwise. Build time only. */
export function buildCommit(): string {
	const sha = process.env.GITHUB_SHA;
	if (sha) {
		return sha.slice(0, 7);
	}
	try {
		return execSync('git rev-parse --short HEAD', { encoding: 'utf8' }).trim();
	} catch {
		return 'unknown';
	}
}
