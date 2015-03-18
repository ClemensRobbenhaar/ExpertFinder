/*******************************************************************************
 * This file is part of the Corporate Semantic Web Project at Freie Universitaet Berlin.
 * 
 * This work has been partially supported by the ``InnoProfile-Corporate Semantic Web" project funded by the German Federal
 * Ministry of Education and Research (BMBF) and the BMBF Innovation Initiative for the New German Laender - Entrepreneurial Regions.
 * 
 * http://www.corporate-semantic-web.de/
 * 
 * Freie Universitaet Berlin
 * Copyright (c) 2007-2013
 * 
 * Institut fuer Informatik
 * Working Group Corporate Semantic Web
 * Koenigin-Luise-Strasse 24-26
 * 14195 Berlin
 * 
 * http://www.mi.fu-berlin.de/en/inf/groups/ag-csw/
 * 
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA or see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package de.csw.expertfinder.expertise;

import static de.csw.expertfinder.config.Config.Key.CREDIBILITY_BASE;
import static de.csw.expertfinder.config.Config.Key.EXPERTISE_WEIGHT_SECTION_1_CONTRIBUTION;
import static de.csw.expertfinder.config.Config.Key.EXPERTISE_WEIGHT_SECTION_2_CONTRIBUTION;
import static de.csw.expertfinder.config.Config.Key.EXPERTISE_WEIGHT_SECTION_3_CONTRIBUTION;
import static de.csw.expertfinder.config.Config.Key.EXPERTISE_WEIGHT_SECTION_4_CONTRIBUTION;
import static de.csw.expertfinder.config.Config.Key.EXPERTISE_WEIGHT_SECTION_5_CONTRIBUTION;
import static de.csw.expertfinder.config.Config.Key.EXPERTISE_WEIGHT_SECTION_6_CONTRIBUTION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import de.csw.expertfinder.config.Config;
import de.csw.expertfinder.config.Config.Key;
import de.csw.expertfinder.document.Author;
import de.csw.expertfinder.document.Concept;
import de.csw.expertfinder.document.Document;
import de.csw.expertfinder.ontology.OntologyIndex;
import de.csw.expertfinder.persistence.PersistenceStoreFacade;
import de.csw.expertfinder.util.Pair;

/**
 * @author ralph
 *
 */
public class ExpertiseModel {

    private static final int POS_DOCUMENT_ID = 0;
    private static final int POS_SECTION_ID = 1;
    private static final int POS_SECTION_LEVEL = 2;
    private static final int POS_REVISION_CREATED = 3;
    private static final int POS_REVISION_DELETED = 4;
    private static final int POS_DELETOR = 5;
    private static final int POS_SECTION_SIMILARITY = 6;

    private static final int POS_SINGLE_DOCUMENT_ID = 0;
    private static final int POS_SINGLE_CONTRIB_REVISION_CREATED = 1;
    private static final int POS_SINGLE_CONTRIB_REVISION_DELETED = 2;
    private static final int POS_SINGLE_DELETOR = 3;

    private static ExpertiseModel instance;

    private PersistenceStoreFacade persistenceStore;

    private double credibilityGrowthBase;
    private double minSimilarity;

    private double[] sectionContributionWeights;

    /**
     * Gets the single instance of this class.
     * @return
     */
    public static ExpertiseModel get() {
        if (instance == null) {
            instance = new ExpertiseModel();
        }
        return instance;
    }

    private ExpertiseModel() {
        persistenceStore = PersistenceStoreFacade.get();
        credibilityGrowthBase = Config.getDoubleAppProperty(CREDIBILITY_BASE);
        minSimilarity = Config.getDoubleAppProperty(Key.ONTOLOGY_MIN_TOPIC_SIMILARITY);

        sectionContributionWeights = new double[7];
        sectionContributionWeights[0] = Config.getDoubleAppProperty(EXPERTISE_WEIGHT_SECTION_1_CONTRIBUTION);
        sectionContributionWeights[1] = Config.getDoubleAppProperty(EXPERTISE_WEIGHT_SECTION_2_CONTRIBUTION);
        sectionContributionWeights[2] = Config.getDoubleAppProperty(EXPERTISE_WEIGHT_SECTION_3_CONTRIBUTION);
        sectionContributionWeights[3] = Config.getDoubleAppProperty(EXPERTISE_WEIGHT_SECTION_4_CONTRIBUTION);
        sectionContributionWeights[4] = Config.getDoubleAppProperty(EXPERTISE_WEIGHT_SECTION_5_CONTRIBUTION);
        sectionContributionWeights[5] = Config.getDoubleAppProperty(EXPERTISE_WEIGHT_SECTION_6_CONTRIBUTION);
        // FIXME: 6th same as 5th level - seems we have a one-less error here, or just too many levels
        sectionContributionWeights[6] = Config.getDoubleAppProperty(EXPERTISE_WEIGHT_SECTION_6_CONTRIBUTION);
    }

    /**
     * calculates expertise / credibility for a given author.
     * if these values have already been calculated, they get overwritten
     * @param author the name of the author, which should exists
     */
    public void calculateExpertiseAndCredibilityForAuthor(String author) {
        calculateExpertiseAndCredibilityForAuthor(getAuthorForName(author));
    }

    /**
     * 
     * @param asRegression hack for testing: check that results do not change
     */
    public void calculateExpertiseAndCredibilityForAllAuthors(boolean asRegression) {
        List<AuthorCredibility> credBefore = null;
        persistenceStore.beginTransaction();
        if (asRegression) {
            credBefore = persistenceStore.getAllAuthorCredibility();
        }
        persistenceStore.removeAllAutorCredibility();
        List<Author> authors = persistenceStore.listAllAuthors(0);
        persistenceStore.endTransaction();

        for (Author author : authors) {
            calculateExpertiseAndCredibilityForAuthor(author);
        }

        if (asRegression) {
            persistenceStore.beginTransaction();
            List<AuthorCredibility> credAfter = persistenceStore.getAllAuthorCredibility();
            persistenceStore.endTransaction();
            int minSize = Math.min(credBefore.size(), credAfter.size());
            for (int i = 0; i < minSize; i++) {
                checkEquals(credBefore.get(i), credAfter.get(i));
            }
            for (int i = minSize; i < credBefore.size(); i++) {
                Assert.fail("missing " + credBefore.get(i));
            }
            for (int i = minSize; i < credAfter.size(); i++) {
                Assert.fail("missing " + credAfter.get(i));
            }
        }
    }
    
    private static final double CHECK_ACCURACY_EXPERTISE = 0.002;

    private void checkEquals(AuthorCredibility authorCredibilityBefore, AuthorCredibility authorCredibilityAfter) {
        Assert.assertEquals("raw expertise", authorCredibilityBefore.getExpertise(), authorCredibilityAfter.getExpertise(), CHECK_ACCURACY_EXPERTISE);
        Assert.assertEquals("expertise", authorCredibilityBefore.getExpertiseAll(), authorCredibilityAfter.getExpertiseAll(), CHECK_ACCURACY_EXPERTISE);
        Assert.assertEquals("credibility", authorCredibilityBefore.getCredibility(), authorCredibilityAfter.getCredibility(), CHECK_ACCURACY_EXPERTISE);
        Assert.assertEquals("cred", authorCredibilityBefore.getCredibilityAll(), authorCredibilityAfter.getCredibilityAll(), CHECK_ACCURACY_EXPERTISE);

        Assert.assertEquals("expertise ctr", authorCredibilityBefore.getExpertiseItemCount(), authorCredibilityAfter.getExpertiseItemCount());
        Assert.assertEquals("expertise all ctr", authorCredibilityBefore.getExpertiseItemCountAll(), authorCredibilityAfter.getExpertiseItemCountAll());
        Assert.assertEquals("cred crt", authorCredibilityBefore.getCredibilityItemCount(), authorCredibilityAfter.getCredibilityItemCount());
        Assert.assertEquals("cred all crt", authorCredibilityBefore.getCredibilityItemCountAll(), authorCredibilityAfter.getCredibilityItemCountAll());
    }

    private void calculateExpertiseAndCredibilityForAuthor(Author author) {

        Set<Concept> contributedConcepts = new HashSet<Concept>();
        persistenceStore.beginTransaction();

        List<AuthorContribution> contributions = persistenceStore.getCachedAuthorContributions(author);

        if (!contributions.isEmpty()) {
            for (AuthorContribution authorContribution : contributions) {
                contributedConcepts.add(authorContribution.getConcept());
            }
        } else {
            List<Concept> directContributions = persistenceStore.getContributedTopics(author);
            List<Concept> indirectContributions = persistenceStore.getIndirectContributedTopics(author);
            contributedConcepts.addAll(directContributions);
            contributedConcepts.addAll(indirectContributions);
            // fill cache, as we are at it
            // TODO: do we really want this ?
            for (Concept concept : contributedConcepts) {
                AuthorContribution ac = new AuthorContribution(author, concept);
                persistenceStore.save(ac);
            }
        }

        persistenceStore.endTransaction();

        for (Concept concept : contributedConcepts) {
            // create an AuthorCredibility and
            // set 'credibility' /  'credibilityItemCount'
            getRawCredibility(author, concept);

            // set  'expertise' / 'expertiseItemCount'
            getRawExpertiseScore(author, concept);

            // FIXME: sets the 'credibilityAll' / 'expertiseAll' stuff - but does it do that right ?
            calculateExpertiseScore(author, concept);
        }

        // FIXME: here we loop a second time. this should be unnecessary
        for (Concept concept : contributedConcepts) {
            calculateExpertiseScore(author, concept);
        }
    }

    /**
     * Calculates the given author's credibility or reputation wrt. the topic
     * represented by the given concept, based on the longevity on his/her
     * contributions to that topic, not including similar topics.
     * 
     * @param author
     *            the author
     * @param concept
     *            a concept representing a topic
     * @return the author's credibility wrt. to the given topic.
     */
    public double getRawCredibility(String authorName, String topicName) {
        return getRawCredibility(getAuthorForName(authorName), getTopicForName(topicName));
    }

    /**
     * Calculates the given author's credibility or reputation wrt. the topic
     * represented by the given concept, based on the longevity on his/her
     * contributions to that topic, not including similar topics.
     * 
     * @param author
     *            the author
     * @param concept
     *            a concept representing a topic
     * @return the author's credibility wrt. to the given topic.
     */
    public double getRawCredibility(Author author, Concept concept) {

        // get all contributions of the author to sections associated with the given concept		
        persistenceStore.beginTransaction();

        AuthorCredibility authorCredibility = persistenceStore.getAuthorCredibility(author, concept);

        if (authorCredibility == null) {
            authorCredibility = new AuthorCredibility(author, concept);
        } else {
            Double credibility = authorCredibility.getCredibility();
            if (credibility != null) {
                persistenceStore.endTransaction();
                return credibility;
            }
        }

        List<Object[]> sections = persistenceStore.getContributionsToSectionsWithConceptForAuthor(concept, author);

        double numerator = 0d;
        double denumenator = 0;

        // adding words to a section during one revision
        long sectionId = -1L;
        int sectionLevel = -1;

        long documentId = -1L;

        long latestRevision = -1L;

        for (Object[] contribution : sections) {
            long newDocumentId = (Long) contribution[POS_DOCUMENT_ID];
            if (newDocumentId != documentId) {
                // new document
                documentId = newDocumentId;
                latestRevision = persistenceStore.getLatestPersistedRevision(documentId).getCount();
            }

            long newSectionId = (Long) contribution[POS_SECTION_ID];
            if (newSectionId != sectionId) {
                // new section
                sectionId = newSectionId;
                sectionLevel = (Integer) contribution[POS_SECTION_LEVEL];
            }

            double sectionConceptSimilarity = (Double) contribution[POS_SECTION_SIMILARITY];

            double sectionContributionWeight = sectionContributionWeights[sectionLevel /* - 1 */] * sectionConceptSimilarity;

            long revisionCreated = (Long) contribution[POS_REVISION_CREATED];
            Long revisionDeleted = (Long) contribution[POS_REVISION_DELETED];
            if (revisionDeleted == null) {
                // contribution has not been deleted
                numerator += (1 - Math.pow(credibilityGrowthBase, -(latestRevision + 1 - revisionCreated))) * sectionContributionWeight;
                denumenator += sectionContributionWeight;
            } else {
                // contribution has been deleted
                long deletorId = (Long) contribution[POS_DELETOR];
                if (deletorId != author.getId()) {
                    // Only if the contribution was deleted by a different author, the contribution is counted.
                    // If the deletor is the actual author of the contribution, it is just not taken into account
                    numerator += (1 - Math.pow(credibilityGrowthBase, -(revisionDeleted + 1 - revisionCreated))) * sectionContributionWeight;
                    denumenator += sectionContributionWeight;
                }
            }
        }

        // now the actual contributions (=words matching the concept)

        List<Object[]> words = persistenceStore.getContributionForConcept(concept, author);

        documentId = -1L;
        latestRevision = -1L;

        for (Object[] contribution : words) {
            long newDocumentId = (Long) contribution[POS_SINGLE_DOCUMENT_ID];
            if (newDocumentId != documentId) {
                // new document
                documentId = newDocumentId;
                latestRevision = persistenceStore.getLatestPersistedRevision(documentId).getCount();
            }

            long revisionCreated = (Long) contribution[POS_SINGLE_CONTRIB_REVISION_CREATED];
            Long revisionDeleted = (Long) contribution[POS_SINGLE_CONTRIB_REVISION_DELETED];
            if (revisionDeleted == null) {
                // contribution has not been deleted
                numerator += (1 - Math.pow(credibilityGrowthBase, -(latestRevision + 1 - revisionCreated)));
                denumenator += 1;
            } else {
                // contribution has been deleted
                long deletorId = (Long) contribution[POS_SINGLE_DELETOR];
                if (deletorId != author.getId()) {
                    // Only if the contribution was deleted by a different author, the contribution is counted.
                    // If the deletor is the actual author of the contribution, it is just not taken into account
                    // TODO: this is stupid, because contributions deleted by s.o. else increase the credibility score while
                    // those deleted by the creator himself don't. 
                    // Recap of the facts.
                    // 1. Deletion of content can mean that content is either junk or outdated.
                    // 1a. If content is junk, then it gets deleted soon with high probability (by someone else).
                    //     -> Even if we count junk, then the credibility score for this bit will remain low (because it's deleted very soon).
                    // 1b. If content is outdated, then it's still worth taking it into account for the time it was present.
                    //     => Content deleted by s.o. else should be taken into account.
                    // 2. Deletion of content by the creator can actually mean the same (either junk or outdated).
                    // 2a. If the creator himself decides to withdraw his contribution, then his reason for this is "harder".
                    //     If he thinks he has written junk, then he will delete it very soon.
                    //     If he thinks his contributions are outdated, then it is improbable that he will delete them.
                    // 2b. The only reason why an author may delete his contribution after a long time is that he corrects/replaces content.
                    // => We can never be absolutely sure why someone deletes s.o. else's content or his own. We should always count content
                    // during its lifetime, regardless of whether the deletor is someone else or not.

                    // => we put the following two lines...
                    //					numerator += (1 - Math.pow(credibilityGrowthBase, -(revisionDeleted + 1 - revisionCreated)));
                    //					denumenator += 1;
                }

                // ... down here!
                numerator += (1 - Math.pow(credibilityGrowthBase, -(revisionDeleted + 1 - revisionCreated)));
                denumenator += 1;
            }

        }

        double credibility;

        if (denumenator == 0d) {
            // obviously no contribution to this topic at all
            credibility = 0d;
        } else {
            credibility = numerator / denumenator;
        }

        authorCredibility.setCredibility(credibility);
        authorCredibility.setCredibilityItemCount((long) denumenator);

        persistenceStore.save(authorCredibility);
        persistenceStore.commitChanges();

        return credibility;
    }

    public double getExpertiseScore(String authorName, String topicName) {
        return getExpertiseScore(getAuthorForName(authorName), getTopicForName(topicName));
    }

    public double getExpertiseScore(Author author, Concept topic) {
        persistenceStore.beginTransaction();
        AuthorCredibility authorCredibility = persistenceStore.getAuthorCredibility(author, topic);
        persistenceStore.endTransaction();
        if (authorCredibility == null || authorCredibility.getExpertiseAll() == null) {
            throw new IllegalStateException("no expertise found for " + author.getName() + " and topic " + topic.getUri());
        }
        return authorCredibility.getExpertiseAll();
    }

    /**
     * Returns the given author's consolidated expertise score for the given
     * topic and similar topics, based on the similarity metric and the
     * similarity threshold value specified in the application's configuration
     * file.
     * 
     * @param author
     * @param topic
     * @return
     */
    private void calculateExpertiseScore(String authorName, String topicName) {
        calculateExpertiseScore(getAuthorForName(authorName), getTopicForName(topicName));
    }

    /**
     * Returns the given author's consolidated expertise score for the given
     * topic and similar topics, weighted by the author's credibility wtr. the
     * given topic, based on the similarity metric and the similarity threshold
     * value specified in the application's configuration file.
     * 
     * @param author
     * @param topic
     * @return
     */
    private void calculateExpertiseScore(Author author, Concept topic) {

        persistenceStore.beginTransaction();

        AuthorCredibility authorCredibility = persistenceStore.getAuthorCredibility(author, topic);

        // my expertise for this topic
        Double myExpertise = authorCredibility.getExpertise();
        if (null == myExpertise) {
            throw new NullPointerException("call getRawExpertizeScore first");
        }
        // System.err.println("recalled " + myExpertise);
        if (myExpertise == 0d) {
            // we can stop here, because if my expertise is 0, I won't add anything to related topics either (I would add 0, which has no effect)
            persistenceStore.endTransaction();
            return;
        }

        Long myExpertiseItemCount = authorCredibility.getExpertiseItemCount();

        // my credibility for this topic
        Double myCredibility = authorCredibility.getCredibility();
        Long myCredibilityItemCount = authorCredibility.getCredibilityItemCount();

        // gets similar topics and (including the given one) and their similarity values
        List<Pair<Concept, Double>> similarConcepts = persistenceStore.getMostSimilarConcepts(topic, minSimilarity);
        for (Pair<Concept, Double> similarConceptSimPair : similarConcepts) {
            Concept similarConcept = similarConceptSimPair.getKey();
            if (similarConcept.equals(topic)) {
                // we already have our expertise value. Continue
                continue;
            }

            Double similarity = similarConceptSimPair.getValue();

            AuthorCredibility similarConceptAuthorCredibility = persistenceStore.getAuthorCredibility(author, similarConcept);

            // add this credibility (weighted by similarity) to similar concept credibility (average)
            Double similarConceptCredibility = similarConceptAuthorCredibility.getCredibilityAll();
            Long similarConceptCredibilityItemCount = similarConceptAuthorCredibility.getCredibilityItemCountAll();
            if (similarConceptCredibility == null) {
                similarConceptCredibility = similarConceptAuthorCredibility.getCredibility();
                similarConceptCredibilityItemCount = similarConceptAuthorCredibility.getCredibilityItemCount();
            }

            Long newCredibilityItemCount = similarConceptCredibilityItemCount + myCredibilityItemCount;
            Double denomitator = (double) similarConceptCredibilityItemCount + ((double) myCredibilityItemCount * similarity);
            Double newCredibility = (similarConceptCredibility + (myCredibility * similarity)) / denomitator;

            similarConceptAuthorCredibility.setCredibilityAll(newCredibility);
            similarConceptAuthorCredibility.setCredibilityItemCountAll(newCredibilityItemCount);

            // add this expertise (weighted by similarity) to similar concept expertise (sum)
            Double similarConceptExpertise = similarConceptAuthorCredibility.getExpertiseAll();
            Long similarConceptExpertiseItemCount = similarConceptAuthorCredibility.getExpertiseItemCountAll();
            if (similarConceptExpertise == null) {
                similarConceptExpertise = similarConceptAuthorCredibility.getExpertise();
                similarConceptExpertiseItemCount = similarConceptAuthorCredibility.getExpertiseItemCount();
            }

            Long newExpertiseItemCount = similarConceptExpertiseItemCount + myExpertiseItemCount;
            Double newExpertise = similarConceptExpertise + (myExpertise * similarity);

            similarConceptAuthorCredibility.setExpertiseAll(newExpertise);
            similarConceptAuthorCredibility.setExpertiseItemCountAll(newExpertiseItemCount);

            persistenceStore.save(similarConceptAuthorCredibility);
        }

        // preliminarily at least
        if (authorCredibility.getCredibilityAll() == null) {
            authorCredibility.setCredibilityAll(myCredibility);
            authorCredibility.setCredibilityItemCountAll(myCredibilityItemCount);
            authorCredibility.setExpertiseAll(authorCredibility.getExpertise());
            authorCredibility.setExpertiseItemCountAll(authorCredibility.getExpertiseItemCount());
            persistenceStore.save(authorCredibility);
        }

        persistenceStore.commitChanges();

    }

    /**
     * Adds the given expertise value for the given author and the given topic to the store.
     * If no expertise item exists for the given author-topic combination, a new one is
     * created automatically.
     */
    private AuthorCredibility addExpertiseScore(Author author, Concept topic, double expertiseValue, long itemCount) {
        AuthorCredibility expertise = persistenceStore.getAuthorCredibility(author, topic);
        if (expertise == null) {
            expertise = new AuthorCredibility(author, topic);
        }

        Double oldExpertiseValue = expertise.getExpertise();
        if (oldExpertiseValue == null) {
            oldExpertiseValue = expertise.getCredibility();
            if (oldExpertiseValue == null) {
                oldExpertiseValue = 0d;
            }
        }
        Double newExpertiseValue = oldExpertiseValue + expertiseValue;
        expertise.setExpertise(newExpertiseValue);

        long oldItemCount = expertise.getExpertiseItemCount();
        long newItemCount = oldItemCount + itemCount;
        expertise.setExpertiseItemCount(newItemCount);
        persistenceStore.save(expertise);

        return expertise;
    }

    /**
     * Adds the given credibility value for the given author and the given topic to the store.
     * If no credibility item exists for the given author-topic combination, a new one is
     * created automatically.
     */
    private AuthorCredibility addCredibilityScore(Author author, Concept topic, double credibilityValue, long itemCount) {
        AuthorCredibility credibility = persistenceStore.getAuthorCredibility(author, topic);
        if (credibility == null) {
            credibility = new AuthorCredibility(author, topic);
        }

        Double oldCredibilityValue = credibility.getCredibilityAll();
        if (oldCredibilityValue == null) {
            oldCredibilityValue = credibility.getCredibility();
            if (oldCredibilityValue == null) {
                oldCredibilityValue = 0d;
            }
        }
        Double newCredibilityValue = oldCredibilityValue + credibilityValue;

        credibility.setCredibilityAll(newCredibilityValue);

        long oldItemCount = credibility.getCredibilityItemCount();
        long newItemCount = oldItemCount + itemCount;
        credibility.setCredibilityItemCount(newItemCount);
        persistenceStore.save(credibility);

        return credibility;
    }

    /**
     * Returns the given author's expertise score for the topic specified by the
     * given ontological concept without taking into account similar topics.
     * 
     * @param author
     * @param topic
     * @return
     */
    private double getRawExpertiseScore(Author author, Concept topic) {

        persistenceStore.beginTransaction();
        AuthorCredibility authorCredibility = persistenceStore.getAuthorCredibility(author, topic);

        Double expertise = 0d;

        List<Object[]> sections = persistenceStore.getContributionsToSectionsWithConceptForAuthor(topic, author);

        // adding words to a section during one revision
        long sectionId = -1L;
        int sectionLevel = -1;

        for (Object[] contribution : sections) {
            long newSectionId = (Long) contribution[POS_SECTION_ID];
            if (newSectionId != sectionId) {
                // new section
                sectionId = newSectionId;
                sectionLevel = (Integer) contribution[POS_SECTION_LEVEL];

            }

            double sectionConceptSimilarity = (Double) contribution[POS_SECTION_SIMILARITY];

            double sectionexpt = sectionConceptSimilarity * sectionContributionWeights[sectionLevel /*-1*/];
            if (sectionexpt == 0d) {
                System.err.println("what? zero expertise for " + newSectionId + " as similarity " + sectionConceptSimilarity);
            }
            expertise += sectionexpt;
        }

        // now the actual contributions (=words matching the concept)

        List<Object[]> words = persistenceStore.getContributionForConcept(topic, author);
        for (Object[] contribution : words) {
            expertise += 1d;
        }

        authorCredibility.setExpertise(expertise);
        authorCredibility.setExpertiseItemCount((long) (sections.size() + words.size()));

        persistenceStore.commitChanges();

        return expertise;

    }

    
    /**
     * Returns all names of authors who contributed to the given topic.
     * @param topicURI the name of the topic
     * @return a map containing all names of authors who contributed to the given topic as keys, and their credibility as value
     */
    public Map<String, AuthorCredibility> getAuthorCredibilitiesForTopicURI(String topicURI) {
        Map<String, AuthorCredibility> creds = new HashMap<>();
        Set<Author> authors = new HashSet<>();

        persistenceStore.beginTransaction();

        Concept topic = persistenceStore.getConcept(topicURI);

        if (topic == null) {
            throw new IllegalArgumentException("no topic for " + topicURI);
        }
        
        List<AuthorContribution> contributions = persistenceStore.getCachedAuthorContributions(topic);

        if (!contributions.isEmpty()) {
            for (AuthorContribution authorContribution : contributions) {
                Author author = authorContribution.getAuthor();
                authors.add(author);
            }
        } else {
            authors.addAll(persistenceStore.getAuthorsWhoContributedToTopic(topic));
            authors.addAll(persistenceStore.getAuthorsWhoIndirectlyContributedToTopic(topic));
        }

        for (Author author : authors) {
            AuthorCredibility authorCredibility = persistenceStore.getAuthorCredibility(author, topic);
            if (authorCredibility != null) {
                creds.put(author.getName(), authorCredibility);
            }
        }
        
        persistenceStore.endTransaction();
        
        return creds;
    }

    
    /**
     * Returns all topic names the given author has contributed to.
     * @param authorName the author's name
     * @return a list containing all topic names the given author has contributed to.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getTopicsForAuthor(String authorName) {

        Set<String> result = new TreeSet<String>();

        persistenceStore.beginTransaction();

        Author author = persistenceStore.getAuthor(authorName);
        List<AuthorContribution> contributions = persistenceStore.getCachedAuthorContributions(author);

        persistenceStore.endTransaction();

        if (!contributions.isEmpty()) {
            for (AuthorContribution authorContribution : contributions) {
                addLabels(authorContribution.getConcept(), result);
            }
            return result;
        }

        if (author == null) {
            return Collections.EMPTY_SET;
        }

        persistenceStore.beginTransaction();
        List<Concept> directContributions = persistenceStore.getContributedTopics(author);
        List<Concept> indirectContributions = persistenceStore.getIndirectContributedTopics(author);
        persistenceStore.endTransaction();

        for (Concept topic : directContributions) {
            addLabels(topic, result);
        }

        for (Concept topic : indirectContributions) {
            addLabels(topic, result);
        }

        //System.err.println("got labels "+ result.size());

        return result;
    }

    private static void addLabels(Concept topic, Set<String> result) {
        OntModel model = OntologyIndex.get().getModel();
        OntClass clazz = model.getOntClass(topic.getUri());
        if (clazz == null) {
            throw new NullPointerException("no class for " + topic.getUri());
        }
        // TODO: allow to use labels in all languages - same as OntologyIndex.createIndex
        // ExtendedIterator<RDFNode> iter = clazz.listLabels(Config.getAppProperty(Config.Key.LANGUAGE));
        ExtendedIterator<RDFNode> iter = clazz.listLabels(null);
        while (iter.hasNext()) {
            String label = ((Literal) iter.next()).getString();
            result.add(label);
        }
    }

    /**
     * Returns the names of all known authors.
     * @return a list containing the names of all authors. never null
     */
    public List<String> getKnownAuthors() {
        persistenceStore.beginTransaction();
        List<Author> authors = persistenceStore.listAllAuthors(0);
        persistenceStore.endTransaction();

        List<String> names = new ArrayList<String>(authors.size());
        for (Author author : authors) {
            names.add(author.getName());
        }

        return names;
    }

    /**
     * @param topicName
     * @throws IllegalArgumentException if there is not topic /concept for the given name
     * @throws NullPointerException if topicName is null
     * @return the matching concept, never null
     */
    private Concept getTopicForName(String topicName) {
        OntClass clazz = OntologyIndex.get().getOntClass(topicName);
        if (clazz == null) {
            throw new IllegalArgumentException("no topic for " + topicName);
        }

        final String topicURI = clazz.getURI();

        persistenceStore.beginTransaction();

        Concept topic = persistenceStore.getConcept(topicURI);

        persistenceStore.endTransaction();

        if (topic == null) {
            throw new IllegalArgumentException("no topic for " + topicURI);
        }
        return topic;
    }

    private Author getAuthorForName(String authorName) {
        persistenceStore.beginTransaction();
        Author author = persistenceStore.getAuthor(authorName);
        persistenceStore.endTransaction();
        if (author == null) {
            throw new IllegalArgumentException("no author for " + authorName);
        }
        return author;
    }

    /**
     * Returns the TF/IDF weighting for the given word in the given document.
     * Does no word normalization in terms of lemmatization or stemming! If
     * normalization is needed, it has to be done on the word before calling
     * this method.
     * 
     * @param document
     *            the document
     * @param word
     *            the word (lemma or stem)
     * @return the TF/IDF
     *
    public double getTFIDFWeight(Document document, String word) {
    	return getTFIDFWeight(document.getId(), word);
    } */

    /**
     * Returns the TF/IDF weighting for the given word in the given document.
     * Does no word normalization in terms of lemmatization or stemming! If
     * normalization is needed, it has to be done on the word before calling
     * this method.
     * 
     * @param documentId
     *            the id of the document
     * @param word
     *            the word (lemma or stem)
     * @return the TF/IDF
     *
    public double getTFIDFWeight(Long documentId, String word) {
    	persistenceStore.beginTransaction();
    	
    	SQLQuery q = persistenceStore.createSQLQuery("select count(*) as count from ( " + 
    			"select word from word w,  revision r " + 
    			"where w.id_revision_created = r.id " + 
    			"and w.id_revision_deleted is null " + 
    			"and r.id_document = :documentId " + 
    			"and word = :word) words");
    	
    	q.setLong("documentId", documentId).setString("word", word);
    	q.addScalar("count", Hibernate.INTEGER);
    	
    	int wordDocumentFreq = (Integer)q.uniqueResult();

    	q = persistenceStore.createSQLQuery("select count(*) as count from ( " + 
    			"select word from word w,  revision r " + 
    			"where w.id_revision_created = r.id " + 
    			"and w.id_revision_deleted is null " + 
    			"and r.id_document = :documentId) words");
    	
    	q.setLong("documentId", documentId);
    	q.addScalar("count", Hibernate.INTEGER);
    	
    	int allDocumentFreq = (Integer)q.uniqueResult();
    	
    	q = persistenceStore.createSQLQuery("select count(*) as count from document");
    	q.addScalar("count", Hibernate.INTEGER);
    	
    	int documentCount = (Integer)q.uniqueResult();
    	
    	
    	q = persistenceStore.createSQLQuery("select count(*) as count from ( " + 
    			"	select distinct d.id from document d, revision rc, word w " + 
    			"	where w.word=:word " + 
    			"	and w.id_revision_created = rc.id " + 
    			"	and w.id_revision_deleted is null " + 
    			"	and rc.id_document = d.id) word");
    	
    	q.setString("word", word);
    	q.addScalar("count", Hibernate.INTEGER);
    	
    	int wordCorpusFreq = (Integer)q.uniqueResult();

    	persistenceStore.endTransaction();
    	
    	double tf = (double)wordDocumentFreq / (double)allDocumentFreq;
    	double idf = Math.log((double)documentCount / (double)wordCorpusFreq);
    	
    	return tf * idf;
    } */

}
