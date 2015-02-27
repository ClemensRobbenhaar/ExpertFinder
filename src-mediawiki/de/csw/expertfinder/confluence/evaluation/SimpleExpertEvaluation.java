package de.csw.expertfinder.confluence.evaluation;

import java.util.List;

import de.csw.expertfinder.ExpertFinder;
import de.csw.expertfinder.config.Config;
import de.csw.expertfinder.ontology.OntologyIndex;

public class SimpleExpertEvaluation {

    private SimpleExpertEvaluation() {
        // no instances, please
    }

    public static void main(String[] args) {
        
        Config.read(SimpleExpertEvaluation.class.getResourceAsStream("/ExpertFinder.properties"));
        OntologyIndex.get().load(OntologyIndex.class.getResource(Config.getAppProperty(Config.Key.ONTOLOGY_FILE)));
        
        List<String> authors = ExpertFinder.getKnownAuthors();
        
        if (authors.isEmpty()) {
            System.err.println("no authors found");
        }
        
        for (String author: authors) {
            System.out.println("Autor " + author);
            for (String topic : ExpertFinder.getTopicsForAuthor(author)) {
                double expertise = ExpertFinder.getExpertise(author, topic);
                double rep = ExpertFinder.getReputation(author, topic);
                
                System.out.println( String.format( "%15s : %10g; %10g", topic, expertise, rep));
            }
            
            System.out.println();
        }

        
    }

}
