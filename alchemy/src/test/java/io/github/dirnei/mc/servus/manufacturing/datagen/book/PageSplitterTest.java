package io.github.dirnei.mc.servus.manufacturing.datagen.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageSplitterTest {

    @Test
    void shortTextStaysOnePage() {
        assertEquals(List.of("One.\n\nTwo."), PageSplitter.split("One.\n\nTwo."));
    }

    @Test
    void paragraphsMoveToTheNextPageWhole() {
        String a = "a".repeat(60);
        String b = "b".repeat(60);
        List<String> pages = PageSplitter.split(a + "\n\n" + b, 100, 100);
        assertEquals(List.of(a, b), pages);
    }

    @Test
    void breakCostCountsAgainstTheBudget() {
        String a = "a".repeat(40);
        String b = "b".repeat(40);
        assertEquals(1, PageSplitter.split(a + "\n\n" + b, 100, 100).size());
        assertEquals(2, PageSplitter.split(a + "\n\n" + b, 99, 99).size());
    }

    @Test
    void longParagraphBreaksBetweenSentences() {
        String text = "First sentence here. Second sentence here. Third sentence here.";
        List<String> pages = PageSplitter.split(text, 45, 45);
        assertEquals(List.of("First sentence here. Second sentence here.", "Third sentence here."), pages);
    }

    @Test
    void titledPageGetsTheSmallerBudget() {
        String a = "a".repeat(50);
        String b = "b".repeat(50);
        String c = "c".repeat(50);
        List<String> pages = PageSplitter.split(a + "\n\n" + b + "\n\n" + c, 60, 200);
        assertEquals(List.of(a, b + "\n\n" + c), pages);
    }

    @Test
    void everyPageFitsItsBudget() {
        String text = ("Salt is the body of all things. ".repeat(20) + "\n\n").repeat(3).strip();
        List<String> pages = PageSplitter.split(text);
        for (int i = 0; i < pages.size(); i++) {
            int budget = i == 0 ? PageSplitter.TITLED_BUDGET : PageSplitter.PLAIN_BUDGET;
            assertTrue(pages.get(i).replace("\n\n", " ".repeat(PageSplitter.BREAK_COST)).length() <= budget,
                    "page " + i + " too long");
        }
    }

    @Test
    void listFillsTheTitledPageThenTheRest() {
        List<String> pages = PageSplitter.list(List.of("a", "b", "c", "d", "e"), 2, 2);
        assertEquals(List.of("- a\n- b", "- c\n- d", "- e"), pages);
    }

    @Test
    void emptyListHasNoPages() {
        assertEquals(List.of(), PageSplitter.list(List.of(), 3, 4));
    }
}
