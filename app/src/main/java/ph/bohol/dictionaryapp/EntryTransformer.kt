package ph.bohol.dictionaryapp

import android.content.Context
import android.util.Log
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.io.Writer
import java.util.*
import javax.xml.transform.Source
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

class EntryTransformer private constructor(private val context: Context) {

    private val stylesheets: MutableMap<String, String> = HashMap()
    private val transformers: MutableMap<String, Transformer> = HashMap()

    var fontSize = DEFAULT_FONT_SIZE
    var isExpandAbbreviations = true
    var isUseMetric = false
    var useNightMode = false

    fun transform(entry: String?, presentationStyle: String): String {
        try {
            val writer: Writer = StringWriter()
            getTransformer(presentationStyle).transform(StreamSource(StringReader(entry)), StreamResult(writer))
            return writer.toString()
        } catch (e: IOException) {
            Log.d(TAG, "Failed to transform entry: ${e.message}.")
        } catch (e: TransformerException) {
            Log.d(TAG, "Failed to transform entry: ${e.message}.")
        }
        return ""
    }

    @Throws(IOException::class, TransformerException::class)
    private fun getTransformer(presentationStyle: String): Transformer {
        var transformer = transformers[presentationStyle]
        if (transformer == null) {
            val xsltSourceFileName = getXsltFileName(presentationStyle)
            val xsltSource: Source = StreamSource(context.assets.open(xsltSourceFileName))
            val factory = TransformerFactory.newInstance()
            Log.d(TAG, "Loading XSLT transformer from: $xsltSourceFileName")
            transformer = factory.newTransformer(xsltSource)
            if (transformer == null) {
                throw TransformerException("Loading XSLT transformer from $xsltSourceFileName failed")
            }
            transformers[presentationStyle] = transformer
        }
        transformer.setParameter("useMetric", isUseMetric.toString())
        transformer.setParameter("expandAbbreviations", isExpandAbbreviations.toString())
        transformer.setParameter("fontSize", fontSize.toString())
        transformer.setParameter("useNightMode", useNightMode.toString())
        return transformer
    }

    private fun getXsltFileName(presentationStyle: String): String {
        return stylesheets[presentationStyle] ?: XSLT_TRADITIONAL
    }

    companion object {
        private const val TAG = "EntryTransformer"
        private var instance: EntryTransformer? = null

        private const val XSLT_COMPACT = "xslt/compact.xsl"
        private const val XSLT_STRUCTURAL = "xslt/structural.xsl"
        private const val XSLT_TRADITIONAL = "xslt/typographical.xsl"
        private const val XSLT_DEBUG = "xslt/debug.xsl"

        private const val DEFAULT_FONT_SIZE = 20

        const val STYLE_COMPACT = "compact"
        const val STYLE_STRUCTURAL = "structural"
        const val STYLE_TRADITIONAL = "traditional"
        const val STYLE_DEBUG = "debug"

        @JvmStatic
        fun getInstance(context: Context): EntryTransformer {
            // Use the application context, which will ensure that you do not accidentally leak an Activity's context.
            // See this article for more information: http://bit.ly/6LRzfx
            if (instance == null) {
                Log.d(TAG, "Creating new EntryTransformer object")
                instance = EntryTransformer(context.applicationContext)
            }
            return instance!!
        }
    }

    init {
        stylesheets[STYLE_COMPACT] = XSLT_COMPACT
        stylesheets[STYLE_STRUCTURAL] = XSLT_STRUCTURAL
        stylesheets[STYLE_TRADITIONAL] = XSLT_TRADITIONAL
        stylesheets[STYLE_DEBUG] = XSLT_DEBUG
    }
}