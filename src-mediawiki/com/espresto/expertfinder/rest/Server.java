package com.espresto.expertfinder.rest;

import java.io.IOException;
import java.net.URI;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import de.csw.expertfinder.config.Config;
import de.csw.expertfinder.expertise.ExpertiseModel;

/**
 * Main class.
 * (generated, should use config instead)
 *
 */
public class Server {
    
    // Base URI the Grizzly HTTP server will listen on
    private static String base_URI;
    
    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer() {
        // create a resource config that scans for JAX-RS resources and providers
        // in com.espresto.expertfinder.rest package
        final ResourceConfig rc = new ResourceConfig().packages("com.espresto.expertfinder.rest");
        
        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(base_URI), rc);
    }

    /**
     * Main method.
     * @param args
     * @throws IOException
     */
    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws IOException {
        Config.read(Server.class.getResourceAsStream("/ExpertFinder.properties"));

        ExpertiseModel.get();
        // TODO: we do not really need the ontology index, for now
        //OntologyIndex.get().load(OntologyIndex.class.getResource(Config.getAppProperty(Config.Key.ONTOLOGY_FILE)));

        base_URI = Config.getAppProperty(Config.Key.REST_SERVER_BASE_URL);
        
        final HttpServer server = startServer();
        System.out.println(String.format("Jersey app started with WADL available at "
                + "%sapplication.wadl\nHit enter to stop it...", base_URI));
        System.in.read();
        server.stop();
    }
}

