package de.csw.expertfinder.confluence.uima;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.csw.expertfinder.confluence.api.ConfluenceRestClient;

public class WikiDocumentIterator {
    
    private static final Log LOG = LogFactory.getLog(WikiDocumentIterator.class);

    private ConfluenceRestClient connector;

    private List<String> pageIds;

    public WikiDocumentIterator() {
    }
    

    public ConfluenceRestClient getConnector() {
        return connector;
    }

    public void setConnector(ConfluenceRestClient connector) {
        this.connector = connector;
    }

    public void initialize() {
        
        pageIds = new ArrayList<String>();
        
        for (String spaceKey: connector.getAllSpaceKeys()) {
            pageIds.addAll( connector.getAllPageIdsForSpace(spaceKey));
        }
        
        LOG.info("found pages " + pageIds.size());
    }

    public boolean hasNextPage() {
        return ! pageIds.isEmpty();
    }
    
    public String nextPageId() {
        return pageIds.remove(0);
    }
}
