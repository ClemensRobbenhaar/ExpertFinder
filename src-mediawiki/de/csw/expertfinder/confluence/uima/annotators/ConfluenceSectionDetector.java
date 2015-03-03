package de.csw.expertfinder.confluence.uima.annotators;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

//import javax.xml.parsers.SAXParser;
//import javax.xml.parsers.SAXParserFactory;
import org.cyberneko.html.parsers.SAXParser;
import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.csw.expertfinder.mediawiki.uima.types.ArticleRevisionInfo;
import de.csw.expertfinder.mediawiki.uima.types.markup.Section;

public class ConfluenceSectionDetector extends JCasAnnotator_ImplBase {

    public static class SectionsExtractor extends DefaultHandler {

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
            System.err.println("asked to resolve " + publicId + " and " + systemId);
            final String fake = "<!ENTITY nbsp \" \">";
            return new InputSource(new java.io.StringReader(fake));
        }

        final static Map<String, Integer> TAG_TO_LEVEL;
        final static Set<String> BLOCK_ELEMENTS;
        static {
            TAG_TO_LEVEL = new HashMap<String, Integer>();
            TAG_TO_LEVEL.put("h1", 1);
            TAG_TO_LEVEL.put("h2", 2);
            TAG_TO_LEVEL.put("h3", 3);
            TAG_TO_LEVEL.put("h4", 4);
            TAG_TO_LEVEL.put("h5", 5);
            TAG_TO_LEVEL.put("h6", 6);

            // todo: more here ...
            BLOCK_ELEMENTS = new HashSet<String>(Arrays.asList("p", "ul", "ol", "li"));
        }

        // final List<Section> sections = new ArrayList<Section>();
        private final Stack<Section> currentSections = new Stack<Section>();

        private final JCas jCas;
        StringBuilder plainText = new StringBuilder();

        private StringBuilder currentHeadline = new StringBuilder();
        private boolean readingHeadline = false;
        private boolean lineBreakAdded = false;

        SectionsExtractor(JCas jCas, String titleText) throws SAXException {
            this.jCas = jCas;
            //if (!PLAINTEXT_VIEW.equals(jCas.getViewName())) {
            //    throw new IllegalArgumentException("view must be plaintext, but is " + jCas.getViewName());
            //}
            startNewSection(0);
            currentHeadline.append(titleText);
            endSectionTitle(0);

            plainText.append(titleText).append("\n\n");
        }

        // we see a new headline and start a new section.
        // if the previous section is of smaller level, it is the parent
        private void startNewSection(int level) throws SAXException {
            if (readingHeadline) {
                throw new SAXException("nested headlines not supported");
            }
            readingHeadline = true;

            final int currentLength = plainText.length();

            Section newSection = new Section(jCas);
            newSection.setLevel(level);
            if (level > 0) {
                newSection.setBegin(currentLength + 1); // we actually start *after* this section
                // if our level is smaller or equal than the one on the stack,
                // then we terminate the previous section and open a
                // new higher level one.
                while (level <= currentSections.peek().getLevel()) {
                    Section terminated = currentSections.pop();
                    terminated.setEnd(currentLength);
                    terminated.addToIndexes();
                }

                // the section with the next larger level than ours is
                // our parent
                newSection.setParent(currentSections.peek());
            } else {
                newSection.setBegin(0);
            }

            currentSections.push(newSection);
        }

        private void endSectionTitle(int level) throws SAXException {
            if (!readingHeadline) {
                throw new SAXException("closing headline which is not open");
            }
            readingHeadline = false;
            if (level != currentSections.peek().getLevel()) {
                throw new SAXException("wriong headline of level " + level);
            }
            currentSections.peek().setTitle(currentHeadline.toString().trim());
            currentHeadline.setLength(0);
        }

        @Override
        public void characters(char[] chars, int start, int length) throws SAXException {
            for (int i = start; i < start + length; i++) {
                char ch = chars[i];
                // line breaks in the html are not line breaks in the plain text
                switch (ch) {
                case '\r':
                case '\n':
                    break;
                default:
                    plainText.append(ch);
                    if (readingHeadline) {
                        currentHeadline.append(ch);
                    }
                    break;
                }
            }
        }

        @Override
        public void startElement(String fullUri, String localName, String qualifiedName, Attributes attributes) throws SAXException {
            final String elementName = qualifiedName.toLowerCase();
            Integer level = TAG_TO_LEVEL.get(elementName);
            if (level != null) {
                startNewSection(level);
                plainText.append("\n");
            }
            lineBreakAdded = false;
        }

        @Override
        public void endElement(String fullUri, String localName, String qualifiedName) throws SAXException {
            final String elementName = qualifiedName.toLowerCase();
            Integer level = TAG_TO_LEVEL.get(elementName);
            if (level != null) {
                endSectionTitle(level);
                plainText.append("\n\n");
            } else if (BLOCK_ELEMENTS.contains(elementName)) {
                if (!lineBreakAdded) {
                    plainText.append("\n");
                    lineBreakAdded = true;
                }
            } else {
                lineBreakAdded = false;
            }

        }

        @Override
        public void startDocument() throws SAXException {
            // nothing really to do here
        }

        @Override
        public void endDocument() throws SAXException {
            int endOfPage = plainText.length();
            while (!currentSections.isEmpty()) {
                Section section = currentSections.pop();
                section.setEnd(endOfPage);
                section.addToIndexes();
            }
        }

    }

    public static String HTML_VIEW = "html";
    public static String PLAINTEXT_VIEW = "plaintext";

    //private SAXParserFactory parserFactory;

    public ConfluenceSectionDetector() {
    }

    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        //parserFactory = SAXParserFactory.newInstance();
        // parserFactory.setNamespaceAware(false);

        // here set parser properties: ...

        // alternative:
        // XMLReader xr = XMLReaderFactory.createXMLReader();

    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {

        JCas htmlCas;
        try {
            htmlCas = jCas.getView(HTML_VIEW);
        } catch (CASException e) {
            throw new AnalysisEngineProcessException(e);
        }

        String documentText = htmlCas.getDocumentText();

        // System.err.println("jCAS: " + jCas.getViewName());
        //System.err.println("CAS: " + cas.getViewName());

        AnnotationIndex<Annotation> articleRevisionInfoIndex = htmlCas.getAnnotationIndex(ArticleRevisionInfo.type);
        ArticleRevisionInfo articleRevisionInfo = (ArticleRevisionInfo) articleRevisionInfoIndex.iterator().next();

        try {
            // create the plain text view
            CAS plainTextView = htmlCas.getCas().createView(PLAINTEXT_VIEW);

            final SectionsExtractor extractor = new SectionsExtractor(plainTextView.getJCas(), articleRevisionInfo.getTitle());
            // FIXME: we only got a fragment ...  this can be done better !
            // String documentTextNoFrament = "<body>" + documentText + "</body>";
            // final InputStream xmlStream = IOUtils.toInputStream(documentTextNoFrament, "UTF-8");

            // SAXParser parser = parserFactory.newSAXParser();
            SAXParser parser = new SAXParser();
            parser.setContentHandler(extractor);

            // parser.setFeature("http://cyberneko.org/html/features/override-namespaces", false);
            parser.setFeature("http://cyberneko.org/html/features/balance-tags", true);

            InputSource src = new InputSource(new StringReader(documentText));

            parser.parse(src);

            plainTextView.setDocumentText(extractor.plainText.toString());

            plainTextView.setDocumentLanguage(htmlCas.getCas().getDocumentLanguage());

            //XXX: bad bad hack - copy over the articleRevisionInfo, instead of having this attached to the "global" CAS
            // just because the consumer only looks into one cas ...
            JCas plainTextViewJCas = plainTextView.getJCas();
            ArticleRevisionInfo articleRevisionInfoCopy = new ArticleRevisionInfo(plainTextViewJCas);
            articleRevisionInfoCopy.setAuthorName(articleRevisionInfo.getAuthorName());
            articleRevisionInfoCopy.setArticleId(articleRevisionInfo.getArticleId());
            articleRevisionInfoCopy.setRevisionId(articleRevisionInfo.getRevisionId());
            articleRevisionInfoCopy.setTitle(articleRevisionInfo.getTitle());
            articleRevisionInfoCopy.setTimestamp(articleRevisionInfo.getTimestamp());
            articleRevisionInfoCopy.addToIndexes();

        } catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        }

    }

}
