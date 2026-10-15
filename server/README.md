# Play test server

A NeoForge 26.1.2 server in Docker with the Servus mods and the same helper mods as the dev runs
(Modonomicon, JEI, Jade, FTB Ultimine and FTB Chunks with FTB Library and FTB Teams).

## Host

```powershell
.\server\pack.ps1                      # build, fill server\mods, write the packs to server\dist
copy server\.env.example server\.env   # once: set OPS and WHITELIST
cd server; docker compose up -d
docker compose logs -f                 # wait for "Done"
```

After a code change: run `pack.ps1` again, then `docker compose restart` so the server loads the
same jars as the new pack. Players need the new pack too, or they cannot join. The world lives in
`server\data` and survives restarts. Console: `docker attach servus-playtest`, leave with Ctrl+P Ctrl+Q.

## Players

Send players to the download page at `http://<host>:8095` (the `web` service, port set by
`WEB_PORT` in `.env`). It has the downloads, the FTB App steps and the server address. The page
is rendered from `site\index.html` by `pack.ps1`, so it always links the latest pack.

`pack.ps1` writes three files to `server\dist`, all with the same jars:

| File | For |
|---|---|
| `servus-playtest-<version>.mrpack` | Prism Launcher, Modrinth App |
| `servus-playtest-<version>-curseforge.zip` | FTB App, CurseForge app |
| `servus-playtest-<version>-mods.zip` | any launcher, by hand |

**Prism Launcher:** Add Instance, Import, pick the `.mrpack`.

**FTB App:** Create Instance, Curse Import, pick the `-curseforge.zip`. This import has open bug
reports, some of them for NeoForge packs ([#1170](https://github.com/FTBTeam/FTB-App/issues/1170),
[#1219](https://github.com/FTBTeam/FTB-App/issues/1219),
[#1285](https://github.com/FTBTeam/FTB-App/issues/1285)). If it fails, make the instance by hand:

1. Create Instance, Vanilla, Minecraft 26.1.2 with NeoForge 26.1.2.
2. Open the instance folder and unpack the jars from `-mods.zip` into its `mods` folder.

The hand made instance always works.

Give the instance 4 GB of memory, start it, Multiplayer, add the server.

For a new pack version, the quickest update is to empty the instance's `mods` folder and unpack
the new `-mods.zip`. Deleting the instance and importing the new pack works as well. Worlds are
only on the server, so nothing is lost.

## Address

- Same network: the host's LAN IP, for example `192.168.1.20`.
- Elsewhere: forward TCP 25565 (and the download page port) on the router, or put both machines on Tailscale and use the
  host's Tailscale IP.
