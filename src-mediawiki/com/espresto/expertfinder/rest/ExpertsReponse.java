package com.espresto.expertfinder.rest;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ExpertsReponse {

    @XmlRootElement
    public static class ExpertiseScore {
        
        public ExpertiseScore() {
        }

        public String authorName;
        public double score;
        public double reputation;
    }

    @XmlRootElement
    public static class ConceptToExpert {

        public ConceptToExpert() {
        }

        public ConceptToExpert(String concept, List<ExpertiseScore> rspAuthors) {
            this.concept = concept;
            this.experts = rspAuthors;
        }

        public String concept;
        public List<ExpertiseScore> experts;
    }

    public List<ConceptToExpert> conceptToExperts;
    public List<String> missingConcepts;

    public String error;

    public ExpertsReponse() {
        // jaxb needs this, always
    }

}
