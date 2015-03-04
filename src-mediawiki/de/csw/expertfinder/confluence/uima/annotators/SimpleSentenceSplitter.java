package de.csw.expertfinder.confluence.uima.annotators;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import de.csw.expertfinder.uima.types.Sentence;

// maybe we should use a better splitter, but using newlines is good enough for now ...
public class SimpleSentenceSplitter extends JCasAnnotator_ImplBase {

    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger(SimpleSentenceSplitter.class);
    
    private static final Pattern newLine = Pattern.compile("\\n+");

    public SimpleSentenceSplitter() {
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        
        //AnnotationIndex<Annotation> articleRevisionInfoIndex = jCas.getAnnotationIndex(ArticleRevisionInfo.type);
        //ArticleRevisionInfo articleRevisionInfo = (ArticleRevisionInfo) articleRevisionInfoIndex.iterator().next();

        String documentText = jCas.getDocumentText();

        //AnnotationIndex<Annotation> sectionIndex = jCas.getAnnotationIndex(Section.type);
        
        Matcher matcher = newLine.matcher(documentText); 
        
        int sentenceBegin = 0;
        while (matcher.find()) {
            int sentenceEnd =  matcher.start();
            Sentence sentence = new Sentence(jCas);
            sentence.setBegin(sentenceBegin);
            sentence.setEnd(sentenceEnd);
            sentence.addToIndexes();
            
            sentenceBegin = matcher.end();
        }
        
        // match the rest of the document
        if (sentenceBegin < documentText.length()) {
            Sentence sentence = new Sentence(jCas);
            sentence.setBegin(sentenceBegin);
            sentence.setEnd(documentText.length());
            sentence.addToIndexes();
            log.info( String.format("added last line [%s]", sentence.getCoveredText()));
        }
        
    }

}
