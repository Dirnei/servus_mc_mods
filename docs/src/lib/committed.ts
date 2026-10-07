import raw from '../data/gamedata.json';
import { parseGameData } from './gamedata';

/** The committed game data. Importing this module fails the build when the file is invalid. */
export const gameData = parseGameData(raw);
