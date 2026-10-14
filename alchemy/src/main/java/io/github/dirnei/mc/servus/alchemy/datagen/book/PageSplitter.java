package io.github.dirnei.mc.servus.alchemy.datagen.book;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a journal text into pages that fit the book at full size. Modonomicon shrinks a page's
 * text until it fits, and a shrunk pixel font draws uneven, thin letters, so every page stays
 * under a character budget. Paragraphs stay whole where they can; a paragraph longer than a page
 * breaks between sentences. The first page carries the title and gets a smaller budget.
 */
public final class PageSplitter {

    /** Characters that fit on a page with a title, counting a paragraph break as one blank line. */
    public static final int TITLED_BUDGET = 200;
    /** Characters that fit on a continuation page without a title. */
    public static final int PLAIN_BUDGET = 240;
    /** What a paragraph break costs: one empty line of the page. */
    static final int BREAK_COST = 20;

    private PageSplitter() {
    }

    public static List<String> split(String text) {
        return split(text, TITLED_BUDGET, PLAIN_BUDGET);
    }

    static List<String> split(String text, int titledBudget, int plainBudget) {
        List<String> pieces = new ArrayList<>();
        for (String paragraph : text.split("\n\n")) {
            // Runs fit the smaller titled budget, so any run can open a page.
            pieces.addAll(sentences(paragraph.strip(), Math.min(titledBudget, plainBudget)));
        }
        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        int used = 0;
        for (String piece : pieces) {
            int budget = pages.isEmpty() ? titledBudget : plainBudget;
            int cost = piece.length() + (page.isEmpty() ? 0 : BREAK_COST);
            if (!page.isEmpty() && used + cost > budget) {
                pages.add(page.toString());
                page.setLength(0);
                used = 0;
                cost = piece.length();
            }
            if (!page.isEmpty()) {
                page.append("\n\n");
            }
            page.append(piece);
            used += cost;
        }
        if (!page.isEmpty()) {
            pages.add(page.toString());
        }
        return pages;
    }

    /** A paragraph as one piece, or as runs of whole sentences when it is longer than a page. */
    private static List<String> sentences(String paragraph, int budget) {
        if (paragraph.length() <= budget) {
            return List.of(paragraph);
        }
        List<String> runs = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        for (String sentence : paragraph.split("(?<=[.!?:])\\s+")) {
            if (!run.isEmpty() && run.length() + 1 + sentence.length() > budget) {
                runs.add(run.toString());
                run.setLength(0);
            }
            if (!run.isEmpty()) {
                run.append(' ');
            }
            run.append(sentence);
        }
        if (!run.isEmpty()) {
            runs.add(run.toString());
        }
        return runs;
    }

    /**
     * Lays out a list as markdown bullet lines over pages: {@code first} items on the first page,
     * which carries a title, and {@code rest} items on every page after it.
     */
    public static List<String> list(List<String> items, int first, int rest) {
        List<String> pages = new ArrayList<>();
        int index = 0;
        while (index < items.size()) {
            int count = pages.isEmpty() ? first : rest;
            List<String> page = items.subList(index, Math.min(items.size(), index + count));
            pages.add(String.join("\n", page.stream().map(item -> "- " + item).toList()));
            index += page.size();
        }
        return pages;
    }
}
