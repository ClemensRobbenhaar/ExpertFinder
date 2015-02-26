package de.csw.expertfinder.confluence.uima;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jws.soap.InitParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import de.csw.expertfinder.confluence.api.ConfluencePageVersionInfo;
import de.csw.expertfinder.confluence.api.ConfluenceRestClient;
import de.csw.expertfinder.document.Document;
import de.csw.expertfinder.document.Revision;
import de.csw.expertfinder.mediawiki.uima.types.ArticleRevisionInfo;
import de.csw.expertfinder.persistence.PersistenceStoreFacade;

/**
 * ToDo: both a collector of all wiki pages, *and* an "article producer" for UIMA.
 * However the collections of articles are actually the versions for a single document.
 * These two roles should be separated ...
 */
public class WikiArticleCollectionReader extends CollectionReader_ImplBase {

    private static final Progress[] __dummy = new Progress[0];
    private static final Log LOG = LogFactory.getLog(WikiArticleCollectionReader.class);
    
    private List<Progress> allProgress = new ArrayList<Progress>();
    
    private ProgressImpl versionProgress;

    private String currentPageId;
    List<ConfluencePageVersionInfo> currentPageVersions;
    
    private ConfluenceRestClient connector;

    public void setCurrentPageId(String currentPageId) {
        this.currentPageId = currentPageId;
        initPage();
    }


    public ConfluenceRestClient getConnector() {
        return connector;
    }


    public void setConnector(ConfluenceRestClient connector) {
        this.connector = connector;
    }

    
    /** this just initializes for every version extraction ... */
    public void initialize() throws ResourceInitializationException {
        super.initialize();
        
        connector = new ConfluenceRestClient((String)getUimaContext().getConfigParameterValue("baseUrl"));
        connector.setUsername((String)getUimaContext().getConfigParameterValue("username"));
        connector.setPassword((String)getUimaContext().getConfigParameterValue("password"));
    }
    
    /// woha, uh, oh: callback *after* being initialized ... as we need the config first get the page ids ...
    /// the alternative is to mock up the configuration, like it is done in MediaWikiCPERunner
    private void initPage() {
        
        // here we could store the verisons read so far.
        // storePage();
        
        currentPageVersions = connector.getVersionsInfoForPageId(currentPageId);
        
        // we get the version in the wrong order; we want oldest first
        Collections.reverse(currentPageVersions);
        
        // ToDo: filter versions for "automatic conversions" and "revert to previous"
        
        // we treat one page as progress for one step ...
        versionProgress = new ProgressImpl(0, currentPageVersions.size(), "docs", true);
        allProgress.clear();
        allProgress.add(versionProgress);
    }

    @SuppressWarnings("unused")
    private void storePage() {
        
        final long pageId = Long.parseLong(currentPageId);
        
        PersistenceStoreFacade persistentStore = PersistenceStoreFacade.get();
        
        LOG.info("starting to use db");
        persistentStore.beginTransaction();
        // Check if we have seen this article before and this is just an update.
        Document document = persistentStore.getDocument(pageId);
        
        long lastPersistedRevisionId;
        
        if (document == null) {
            // this is the first time we see this article. Create a new Document in the persistent store.
            lastPersistedRevisionId = 0;
            document = new Document(pageId, ""); // FIXME: here we would need an (unique!) document title
            persistentStore.save(document);
        } else {
            // we have seen this article before. We only need to start with the last revision.
            Revision lastPersistedRevision = PersistenceStoreFacade.get().getLatestPersistedRevision(pageId);
            lastPersistedRevisionId = lastPersistedRevision == null ? 0 : lastPersistedRevision.getId();
        }
        
        persistentStore.commitChanges();

    }


    @Override
    public void close() throws IOException {
        // nothing to do here (yet)
        
    }
    
    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return !currentPageVersions.isEmpty();
    }

    public void getNext(CAS baseCas) throws IOException, CollectionException {
        ConfluencePageVersionInfo currentVersion = currentPageVersions.remove(0);
        
        baseCas.setDocumentText(currentVersion.getText());
        
        //
        // the following is Copy & Paste from MedaiWikiArticleCollectionReader#getNext
        //
        
        JCas jCas;
        try {
            jCas = baseCas.getJCas();
        } catch (CASException e) {
            LOG.error("Error creating JCas from CAS", e);
            throw new CollectionException("Error creating JCas from CAS", null, e);
        }
        
        jCas.setDocumentText(currentVersion.getText());

        // ToDo: maybe we should have used ArticleRevisionInfo directly ?

        ArticleRevisionInfo articleRevisionInfo = new ArticleRevisionInfo(jCas);
        articleRevisionInfo.setArticleId(currentVersion.getArticleId());
        articleRevisionInfo.setTitle(currentVersion.getTitle());
        articleRevisionInfo.setRevisionId(currentVersion.getRevisionId());
        articleRevisionInfo.setAuthorName(currentVersion.getUser());
        articleRevisionInfo.setTimestamp(currentVersion.getTimestamp().getTime());
        articleRevisionInfo.addToIndexes(jCas);
        
        versionProgress.increment(1);
    }

    @Override
    public Progress[] getProgress() {
        return allProgress.toArray(__dummy);
    }

}
