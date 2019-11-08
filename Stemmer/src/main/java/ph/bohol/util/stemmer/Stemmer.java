package ph.bohol.util.stemmer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class Stemmer {
    private String language;
    private boolean compiled = false;
    private final Map<String, String> constants = new HashMap<>();
    private final Vector<AffixGroup> groups = new Vector<>();
    private RootWordProvider rootWordProvider = null;

    final void addGroup(final AffixGroup group) {
        groups.addElement(group);
        compiled = false;
    }

    final void compile() {
        for (AffixGroup group : groups) {
            group.compile(constants);
        }
        compiled = true;
    }

    final void addConstant(final String key, final String value) {
        constants.put(key, value);
    }

    public final LinkedList<Derivation> findDerivations(final String word) {
        if (!compiled) {
            compile();
        }
        Set<String> roots = new HashSet<>();
        return innerFindDerivations(word, roots, 0);
    }


    private LinkedList<Derivation> innerFindDerivations(final String word, final Set<String> roots, final int level) {
        LinkedList<Derivation> derivations;
        if (groups.size() <= level) {
            derivations = new LinkedList<>();
            if (!roots.contains(word) && isRootWord(word)) {
                Derivation derivation = new Derivation(word);
                derivations.add(derivation);
                roots.add(word);
            }
            return derivations;
        } else {
            derivations = innerFindDerivations(word, roots, level + 1);
        }

        AffixGroup group = groups.get(level);
        LinkedList<Affix> affixes = group.getAffixes();
        for (Affix affix : affixes) {
            LinkedList<String> rootCandidates = affix.rootCandidates(word);
            for (String root : rootCandidates) {
                if (!roots.contains(root) && isRootWord(root)) {
                    Derivation derivation = new Derivation(root);
                    derivation.addAffix(affix);
                    derivations.add(derivation);
                    roots.add(root);
                }

                LinkedList<Derivation> innerDerivations = innerFindDerivations(root, roots, level + 1);

                // Copy the found derivations to the result list with the current affix as additional affix:
                for (Derivation derivation : innerDerivations) {
                    derivation.addAffix(affix);
                    derivations.add(derivation);
                }
            }
        }
        return derivations;
    }

    private boolean isRootWord(final String word) {
        return (rootWordProvider == null || rootWordProvider.isRootWord(word));
    }

    public final String getLanguage() {
        return language;
    }

    final void setLanguage(final String newLanguage) {
        this.language = newLanguage;
    }

    public final String toString() {
        StringBuilder result = new StringBuilder("<stemmer language='" + language + "'>");

        for (String key : constants.keySet()) {
            result.append("\n<constant name='")
                    .append(key)
                    .append("' value='")
                    .append(constants.get(key))
                    .append("'/>");
        }

        for (AffixGroup group : groups) {
            result.append(group.toString());
        }

        return result.append("</stemmer>\n").toString();
    }

    public final RootWordProvider getRootProvider() {
        return rootWordProvider;
    }

    public final void setRootProvider(final RootWordProvider rootProvider) {
        this.rootWordProvider = rootProvider;
    }
}
