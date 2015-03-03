package de.csw.expertfinder.test.confluence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.JCasFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.uimafit.util.JCasUtil;

import de.csw.expertfinder.confluence.uima.annotators.ConfluenceSectionDetector;
import de.csw.expertfinder.mediawiki.uima.types.ArticleRevisionInfo;
import de.csw.expertfinder.mediawiki.uima.types.markup.Section;

public class TestConfluenceSectionDetector {

    // 
    public static class ExpectedSection {
        final int level;
        final String title;

        ExpectedSection(int level, String title) {
            this.title = title;
            this.level = level;
        }
    }

    protected JCas jCas;

    private TypeSystemDescription typeSystemDescription;

    public TestConfluenceSectionDetector() {
        // TODO Auto-generated constructor stub
    }

    @BeforeTest
    public void setUp() throws UIMAException {
        // we reuse the media wiki type system
        typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription("desc/MediaWikiArticleDocumentAnnotationTypeSystem",
                "desc/MediaWikiMarkupDetectorTypeSystem");
        jCas = JCasFactory.createJCas(typeSystemDescription);
    }

    @BeforeMethod
    public void resetCas() throws UIMAException {
        jCas.reset();
    }

    @Test
    public void testSimple() throws UIMAException, IOException {

        String text = read("input.html");
        jCas.setDocumentText(text);
        buildVersionInfo("input.version");

        AnalysisEngine sectionDetectedAnnotatorAE = AnalysisEngineFactory.createPrimitive(ConfluenceSectionDetector.class, typeSystemDescription);
        sectionDetectedAnnotatorAE.process(jCas);

        JCas plainText = JCasUtil.getView(jCas, ConfluenceSectionDetector.PLAINTEXT_VIEW, false);
        Assert.assertNotNull("should have a plain text after processing", plainText);

        Assert.assertEquals(read("output.txt"), plainText.getDocumentText());

        Assert.assertEquals("should have no sections in the default, view", 0, JCasUtil.select(jCas, Section.class).size());

        assertEqualsSections(readSections("output.sections"), JCasUtil.select(plainText, Section.class), plainText.getDocumentText());
    }

    private void assertEqualsSections(List<ExpectedSection> expectedSections, Collection<Section> actualSections, String inText) {
        int i = 0;
        for (Section actualSection : actualSections) {
            if (expectedSections.size() <= i) {
                Assert.fail("unexpected extra sections from " + i + " found " + actualSection.getLevel());
            }
            ExpectedSection expectedSection = expectedSections.get(i);
            //  Section actualSection = JCasUtil.selectByIndex(jCas, Section.class, i);
            Assert.assertEquals("for the " + i + ". section", expectedSection.title, actualSection.getTitle());
            Assert.assertEquals("for the " + i + ". section", expectedSection.level, actualSection.getLevel());

            Assert.assertEquals("should have body ", inText.substring(actualSection.getBegin(), actualSection.getEnd()), actualSection.getCoveredText());
            Assert.assertTrue(
                    String.format("should have body starting with [%s] but got [%s]", expectedSection.title,
                            actualSection.getCoveredText().substring(0, expectedSection.title.length())),
                    actualSection.getCoveredText().startsWith(expectedSection.title));
            i++;
        }
        Assert.assertEquals("should have found all expected sections", expectedSections.size(), i);
    }

    private List<ExpectedSection> readSections(String resourceName) throws IOException {
        @SuppressWarnings("unchecked")
        List<String> lines = IOUtils.readLines(getClass().getResourceAsStream(resourceName));
        List<ExpectedSection> sections = new ArrayList<ExpectedSection>();

        // input format is <level>':'<text>
        for (String line : lines) {
            String[] data = line.split(":", 2);
            System.err.println("read line:: " + line);
            ExpectedSection section = new ExpectedSection(Integer.parseInt(data[0]), data[1]);
            sections.add(section);
        }

        return sections;
    }

    private ArticleRevisionInfo buildVersionInfo(String resourceName) throws IOException {
        @SuppressWarnings("unchecked")
        List<String> lines = IOUtils.readLines(getClass().getResourceAsStream(resourceName));

        ArticleRevisionInfo version = new ArticleRevisionInfo(jCas);

        version.setTitle(lines.get(0));
        version.setAuthorName(lines.get(1));
        // there is more, but we do not need this (yet)

        version.addToIndexes();
        return version;
    }

    /*
     * instead use data provider ?  html text, plain text, sectionList ?
       @DataProvider   Marks a method as supplying data for a test method. The annotated method must return an Object[][] where each Object[] can be assigned the parameter list of the test method. The @Test method that wants to receive data from this DataProvider needs to use a dataProvider name equals to the name of this annotation.
       name    The name of this data provider. If it's not supplied, the name of this data provider will automatically be set to the name of the method. 
     */

    protected String read(String resource) throws IOException {
        return IOUtils.toString(TestConfluenceSectionDetector.class.getResourceAsStream(resource), "UTF-8");
    }

}
