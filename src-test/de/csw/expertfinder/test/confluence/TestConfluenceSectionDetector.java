package de.csw.expertfinder.test.confluence;

import java.io.IOException;
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
    
    protected JCas jCas;

    private TypeSystemDescription typeSystemDescription;

    public TestConfluenceSectionDetector() {
        // TODO Auto-generated constructor stub
    }
    
    @BeforeTest
    public void setUp() throws UIMAException {
        // we reuse the media wiki type system
        typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription(
                "desc/MediaWikiArticleDocumentAnnotationTypeSystem", "desc/MediaWikiMarkupDetectorTypeSystem");
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

        JCas plainText = JCasUtil.getView(jCas, "plaintext", false);
        Assert.assertNotNull("should have a plain text after processing", plainText);
        
        Assert.assertEquals( read("output.txt"), plainText.getDocumentText());
        
        Collection<Section> sections = JCasUtil.select(jCas, Section.class);
        Assert.assertEquals( "should have some sections", 7, sections.size());
        
        //Section s = JCasUtil.selectByIndex(jCas, Section.class, 0);
        //RoomNumber roomNumber = JCasUtil.selectByIndex(jCas, RoomNumber.class, 0);
        //assertNotNull(roomNumber);
        //assertEquals("01-144", roomNumber.getCoveredText());
        //assertEquals("Yorktown", roomNumber.getBuilding());
        
        Assert.fail("not implemented");
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
