package io.github.dirnei.mc.servus.foundation.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * A search text read with JEI's syntax: {@code @} mod, {@code $} tag,
 * {@code #} tooltip, {@code %} creative tab, {@code &} id, a plain word the
 * name. Spaces separate terms that must all match, {@code |} separates
 * alternatives of which one must match, a leading {@code -} negates a term and
 * double quotes keep spaces in one term. Matching is a case insensitive
 * "contains". Pure logic.
 */
public final class SearchQuery {

    /** What a term looks at. */
    public enum Kind {
        NAME(SearchFacts::names),
        MOD(SearchFacts::mods),
        TAG(SearchFacts::tags),
        TOOLTIP(SearchFacts::tooltip),
        TAB(SearchFacts::tabs),
        ID(SearchFacts::ids);

        private final Function<SearchFacts, List<String>> facts;

        Kind(Function<SearchFacts, List<String>> facts) {
            this.facts = facts;
        }

        /** The kind a prefix character picks, or null when it is no prefix. */
        static Kind ofPrefix(char c) {
            return switch (c) {
                case '@' -> MOD;
                case '$' -> TAG;
                case '#' -> TOOLTIP;
                case '%' -> TAB;
                case '&' -> ID;
                default -> null;
            };
        }
    }

    /** One term: what it looks at, its lower case text, and whether it is negated. */
    public record Term(Kind kind, String text, boolean negated) {

        boolean matches(SearchFacts facts) {
            boolean found = false;
            for (String fact : kind.facts.apply(facts)) {
                if (fact.toLowerCase(Locale.ROOT).contains(text)) {
                    found = true;
                    break;
                }
            }
            return found != negated;
        }
    }

    private static final SearchQuery EMPTY = new SearchQuery(List.of());

    private final List<List<Term>> alternatives;

    private SearchQuery(List<List<Term>> alternatives) {
        this.alternatives = alternatives;
    }

    /** Reads a search text; null or blank gives a query that matches everything. */
    public static SearchQuery parse(String text) {
        if (text == null || text.isBlank()) {
            return EMPTY;
        }
        List<List<Term>> alternatives = new ArrayList<>();
        List<Term> terms = new ArrayList<>();
        TermBuilder term = new TermBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                quoted = !quoted;
                term.startText();
            } else if (quoted) {
                term.append(c);
            } else if (Character.isWhitespace(c)) {
                term.finishInto(terms);
                term = new TermBuilder();
            } else if (c == '|') {
                term.finishInto(terms);
                term = new TermBuilder();
                addIfAny(alternatives, terms);
                terms = new ArrayList<>();
            } else {
                term.head(c);
            }
        }
        term.finishInto(terms);
        addIfAny(alternatives, terms);
        return alternatives.isEmpty() ? EMPTY : new SearchQuery(List.copyOf(alternatives));
    }

    /** Whether the query has no terms and so matches everything. */
    public boolean isEmpty() {
        return alternatives.isEmpty();
    }

    /** The alternatives, each a list of terms that must all match. */
    public List<List<Term>> alternatives() {
        return alternatives;
    }

    /** Whether any alternative has all its terms matching; asks {@code facts} only for what those terms need. */
    public boolean matches(SearchFacts facts) {
        if (alternatives.isEmpty()) {
            return true;
        }
        for (List<Term> terms : alternatives) {
            if (allMatch(terms, facts)) {
                return true;
            }
        }
        return false;
    }

    private static boolean allMatch(List<Term> terms, SearchFacts facts) {
        for (Term term : terms) {
            if (!term.matches(facts)) {
                return false;
            }
        }
        return true;
    }

    private static void addIfAny(List<List<Term>> alternatives, List<Term> terms) {
        if (!terms.isEmpty()) {
            alternatives.add(List.copyOf(terms));
        }
    }

    /** Collects one term: an optional {@code -}, an optional prefix, then its text. */
    private static final class TermBuilder {

        private final StringBuilder text = new StringBuilder();
        private Kind kind = Kind.NAME;
        private boolean negated;
        private boolean inText;

        /** An unquoted character: part of the head while no text has started, text after. */
        void head(char c) {
            if (!inText) {
                if (c == '-' && !negated && kind == Kind.NAME) {
                    negated = true;
                    return;
                }
                Kind prefix = Kind.ofPrefix(c);
                if (prefix != null && kind == Kind.NAME) {
                    kind = prefix;
                    return;
                }
            }
            append(c);
        }

        void startText() {
            inText = true;
        }

        void append(char c) {
            inText = true;
            text.append(c);
        }

        void finishInto(List<Term> terms) {
            if (!text.isEmpty()) {
                terms.add(new Term(kind, text.toString().toLowerCase(Locale.ROOT), negated));
            }
        }
    }
}
