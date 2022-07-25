package ph.bohol.util.stemmer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.LinkedList;

import org.junit.Test;

public class StemmerTest {
    @Test
    public final void testLoad() {
        InputStream stream = StemmerTest.class.getResourceAsStream("stemmerTest.xml");

        StemmerParser parser = new StemmerParser();
        Stemmer stemmer = parser.parse(stream);

        System.out.print(stemmer.toString());

        LinkedList<Derivation> results = testDerivations(stemmer, "makasabut");
        assertEquals(3, results.size());

        results = testDerivations(stemmer, "balaya");
        assertEquals(2, results.size());

        stemmer.setRootProvider(new TestRootWordProvider());

        results = testDerivations(stemmer, "makasabut");
        assertEquals(1, results.size());

        results = testDerivations(stemmer, "balaya");
        assertEquals(1, results.size());
    }

    @Test
    public final void testLargeLoad() {
        InputStream stream = StemmerParserTest.class.getResourceAsStream("stemmerLargeTest.xml");

        StemmerParser parser = new StemmerParser();
        Stemmer stemmer = parser.parse(stream);

        System.out.print(stemmer.toString());

        TestRootWordProvider provider = new TestRootWordProvider();
        stemmer.setRootProvider(provider);

        //LinkedList<Derivation> results;
        /*results = */testDerivations(stemmer, "makasabut");
        /*results = */testDerivations(stemmer, "mangaun");
        /*results = */testDerivations(stemmer, "balaya");
        /*results = */testDerivations(stemmer, "pag-abut");
        /*results = */testDerivations(stemmer, "binisaya");
        System.out.print("Calls to root-word provider: " + provider.getCalls());
    }

    private LinkedList<Derivation> testDerivations(final Stemmer stemmer, final String word) {
        LinkedList<Derivation> derivations = stemmer.findDerivations(word);

        for (Derivation derivation : derivations) {
            System.out.println("Potential derivation: " + derivation.toString());
        }
        return derivations;
    }
}
