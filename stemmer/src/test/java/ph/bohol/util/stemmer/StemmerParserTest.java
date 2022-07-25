package ph.bohol.util.stemmer;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

public class StemmerParserTest {
    @Test
    public final void testParse() throws IOException {
        InputStream stream = StemmerParserTest.class.getResourceAsStream("stemmerTest.xml");

        StemmerParser parser = new StemmerParser();
        Stemmer stemmer = parser.parse(stream);
        stream.close();
        System.out.print(stemmer.toString());
    }
}
