package de.csw.expertfinder.confluence.evaluation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.csw.expertfinder.Callback;
import de.csw.expertfinder.ExpertFinder;
import de.csw.expertfinder.ExpertFinder.ApplicationState;
import de.csw.expertfinder.config.Config;
import de.csw.expertfinder.expertise.ExpertiseModel;

public class SimpleExpertEvaluation {

    private SimpleExpertEvaluation() {
        // no instances, please
    }

    public static void main(String[] args) throws Exception {

        // try to outsmart the expert finder asynchronous init, which we do not want here 
        final Object lock_wait = new Object();

        Callback c = new Callback() {
            @Override
            public void notify(ApplicationState state, Throwable... throwables) {
                for (Throwable t : throwables) {
                    System.err.println(t);
                    t.printStackTrace();
                }
                if (state == ApplicationState.INIT_DONE) {
                    synchronized (lock_wait) {
                        lock_wait.notify();
                    }
                }
            }
        };
        ExpertFinder ef = new ExpertFinder();

        ef.init(SimpleExpertEvaluation.class.getResourceAsStream("/ExpertFinder.properties"), c);
        synchronized (lock_wait) {
            lock_wait.wait();
        }

        List<String> authors = ExpertFinder.getKnownAuthors();

        if (authors.isEmpty()) {
            System.err.println("no authors found");
        }

        ExpertiseModel em = ExpertiseModel.get();

        // TODO: first calculate, then return the calculated value in the second run.

        em.calculateExpertiseAndCredibilityForAllAuthors(true);

        for (String author : authors) {
            System.out.println("Autor " + author);

            for (String topic : ExpertFinder.getTopicsForAuthor(author)) {
                double rep = ExpertFinder.getReputation(author, topic);
                double expertise = ExpertFinder.getExpertise(author, topic);

                System.out.println(String.format("%15s : %10g; %10g", topic, expertise, rep));
            }

            System.out.println();
        }

    }

}
