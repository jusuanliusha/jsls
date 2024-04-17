package org.jsls.core;

import java.util.HashSet;
import java.util.Set;

import org.springframework.util.StringUtils;

public class Mark {
    private final Set<String> symbolSet = new HashSet<>();

    private Mark(Set<String> symbolSet) {
        if (symbolSet != null) {
            this.symbolSet.addAll(symbolSet);
        }
    }

    public void addSymbol(String... symbols) {
        if (symbols != null && symbols.length > 0) {
            for (String symbol : symbols) {
                this.symbolSet.add(symbol);
            }
        }
    }

    public void rmSymbol(String... symbols) {
        if (symbols != null && symbols.length > 0) {
            for (String symbol : symbols) {
                this.symbolSet.remove(symbol);
            }
        }
    }

    public boolean anySymbol(String... symbols) {
        if (symbols != null && symbols.length > 0) {
            for (String symbol : symbols) {
                if (hasSymbol(symbol)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasSymbol(String mark) {
        Set<String> ss = useSymbolSet(mark);
        for (String cs : ss) {
            if (!this.symbolSet.contains(cs)) {
                return false;
            }
        }
        return true;
    }

    public boolean hasSymbol(Mark mark) {
        Set<String> ss = mark.symbolSet;
        for (String cs : ss) {
            if (!this.symbolSet.contains(cs)) {
                return false;
            }
        }
        return true;
    }

    public String useText() {
        return StringUtils.collectionToDelimitedString(symbolSet, "|");
    }

    public Mark copy() {
        return new Mark(this.symbolSet);
    }

    public static Mark empty() {
        return new Mark(null);
    }

    public static Mark of(String symbol) {
        return new Mark(useSymbolSet(symbol));

    }

    private static Set<String> useSymbolSet(String symbol) {
        Set<String> ms = new HashSet<>();
        if (StringUtils.hasText(symbol)) {
            String[] ma = symbol.split("|");
            for (String item : ma) {
                if (StringUtils.hasText(item)) {
                    ms.add(item);
                }
            }
        }
        return ms;
    }
}
