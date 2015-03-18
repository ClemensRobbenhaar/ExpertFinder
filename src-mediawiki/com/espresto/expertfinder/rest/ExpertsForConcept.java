package com.espresto.expertfinder.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.espresto.expertfinder.rest.ExpertsReponse.ConceptToExpert;
import com.espresto.expertfinder.rest.ExpertsReponse.ExpertiseScore;

import de.csw.expertfinder.expertise.AuthorCredibility;
import de.csw.expertfinder.expertise.ExpertiseModel;

/**
 * Finds experts (with score) for concepts.
 */
@Path("forConcepts")
public class ExpertsForConcept {

    /**
     * @return matching experts, or maybe an error message
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ExpertsReponse getExpertise(@QueryParam("concept") List<String> conceptIds) {

        ExpertsReponse rsp = new ExpertsReponse();
        rsp.conceptToExperts = new ArrayList<>();

        ExpertiseModel em = ExpertiseModel.get();

        List<String> missingConcepts = new ArrayList<>();
        for (String concept : conceptIds) {
            try {
                Map<String, AuthorCredibility> authors = em.getAuthorCredibilitiesForTopicURI(concept);
                List<ExpertiseScore> exp = new ArrayList<>();
                List<String> rspAuthors = new ArrayList<>(authors.keySet());
                for (String authorName : rspAuthors) {
                    AuthorCredibility cred = authors.get(authorName);
                    if (cred.getExpertise() > 0.0) {
                        ExpertiseScore expertScore = new ExpertiseScore();
                        expertScore.authorName = authorName;
                        expertScore.score = cred.getExpertise();
                        expertScore.reputation = cred.getCredibility();
                        exp.add(expertScore);
                    }
                }
                // TODO: better sort needed / wanted (?)
                Collections.sort(exp, new Comparator<ExpertiseScore>() {

                    @Override
                    public int compare(ExpertiseScore exp1, ExpertiseScore exp2) {
                        int scoreSort = Double.compare(exp1.score, exp2.score);
                        if (scoreSort != 0) {
                            return scoreSort;
                        }
                        return Double.compare(exp1.reputation, exp2.reputation);
                    }
                });
                
                rsp.conceptToExperts.add(new ConceptToExpert(concept, exp));

            } catch (IllegalArgumentException e) {
                missingConcepts.add(concept);
            } catch (Exception e) {
                rsp.error = e.getMessage();
                if (rsp.error == null) {
                    rsp.error = "internal error";
                }
                e.printStackTrace();
            }
        }
        if (!missingConcepts.isEmpty()) {
            rsp.missingConcepts = missingConcepts;
        }

        return rsp;
    }
}
