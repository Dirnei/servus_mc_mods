// @ts-check
import starlight from '@astrojs/starlight';
import { defineConfig } from 'astro/config';

export default defineConfig({
	site: 'https://dirnei.github.io',
	base: '/servus_mc_mods',
	integrations: [
		starlight({
			title: 'Servus',
			components: { SiteTitle: './src/components/SiteTitle.astro' },
			favicon: '/favicon.png',
			customCss: ['./src/styles/custom.css'],
			head: [
				{ tag: 'link', attrs: { rel: 'preconnect', href: 'https://fonts.googleapis.com' } },
				{ tag: 'link', attrs: { rel: 'preconnect', href: 'https://fonts.gstatic.com', crossorigin: true } },
				{
					tag: 'link',
					attrs: {
						rel: 'stylesheet',
						href: 'https://fonts.googleapis.com/css2?family=Montserrat:ital,wght@0,400;0,500;0,600;0,700;1,400&family=JetBrains+Mono:wght@400;600&display=swap',
					},
				},
			],
			description:
				'Player guide, datapack reference and data line API for the Servus mods for Minecraft 26.1.2 on NeoForge.',
			social: [{ icon: 'github', label: 'GitHub', href: 'https://github.com/Dirnei/servus_mc_mods' }],
			sidebar: [
				{ label: 'Start', items: [{ slug: 'getting-started' }] },
				{
					label: 'Mods',
					items: [{ slug: 'mods/foundation' }, { slug: 'mods/shuttles' }, { slug: 'mods/alchemy' }],
				},
				{
					label: 'Developers',
					items: [
						{ slug: 'developers' },
						{ slug: 'developers/data-line' },
						{ slug: 'developers/join-the-data-line' },
						{ slug: 'developers/deliveries' },
						{ slug: 'developers/machine-adapter' },
					],
				},
				{ label: 'Datapack reference', items: [{ autogenerate: { directory: 'reference/datapack' } }] },
			],
		}),
	],
});
