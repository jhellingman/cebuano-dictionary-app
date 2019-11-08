package ph.bohol.util.stemmer;

import java.util.LinkedList;
import java.util.Map;

class AffixGroup {
    private String name;
    private final LinkedList<Affix> affixes;

    AffixGroup() {
        affixes = new LinkedList<>();
    }

    final void addAffix(final Affix pattern) {
        affixes.addLast(pattern);
    }

    final void compile(final Map<String, String> constants) {
        for (Affix affix : affixes) {
            affix.compile(constants);
        }
    }

    public final void setName(final String newName) {
        name = newName;
    }

    public final String toString() {
        StringBuilder result = new StringBuilder("\n<group name='" + name + "'>\n");
        for (Affix affix : affixes) {
            result.append(affix.toString());
        }
        result.append("</group>\n");
        return result.toString();
    }

    final LinkedList<Affix> getAffixes() {
        return affixes;
    }
}
