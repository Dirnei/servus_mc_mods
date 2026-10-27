package io.github.dirnei.mc.servus.alchemy;

/** English text made from ids. Pure Java, no Minecraft types. */
public final class IdText {

    private IdText() {
    }

    /** An id path as English words: {@code caput_mortuum} gives Caput Mortuum. */
    public static String titleCase(String path) {
        StringBuilder name = new StringBuilder();
        for (String word : path.split("_")) {
            if (!word.isEmpty()) {
                if (!name.isEmpty()) {
                    name.append(' ');
                }
                name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return name.toString();
    }
}
