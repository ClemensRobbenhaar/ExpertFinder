/*******************************************************************************
 * This file is part of the Corporate Semantic Web Project at Freie Universitaet Berlin.
 * 
 * This work has been partially supported by the ``Corporate Smart Content" project funded by the German Federal
 * Ministry of Education and Research (BMBF) and the BMBF Innovation Initiative for the New German Laender - Entrepreneurial Regions.
 * 
 * http://www.corporate-smart-content.de/
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
package de.csw.expertfinder.confluence.uima.deploy;

import java.io.IOException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.util.XMLInputSource;

import de.csw.expertfinder.config.Config;
import de.csw.expertfinder.confluence.uima.WikiArticleCollectionReader;
import de.csw.expertfinder.confluence.uima.WikiDocumentIterator;
import de.csw.expertfinder.ontology.OntologyIndex;

/**
 * This class provides methods for programmatically running the Confluence Expert
 * Finder UIMA Collection Processing Engine (CPE) defined in a CPE descriptor
 * file.
 * 
 * This version always runs over the complete content of a remote confluence instance.
 * To get at the text of the versions one need to install a custom rest service, as the official confluence API has none.
 *  
 * @author ralph, etc
 */
public class ConfluenceCPERunner {

	private static final Logger log = Logger.getLogger(ConfluenceCPERunner.class);

    private static final URL CPE_DESCRIPTOR_URL = ConfluenceCPERunner.class.getResource("Confluence ExpertFinder CPE Config.xml");

	private URL cpeDescriptorURL;
	private StatusCallbackListener statusCallbackListener;
	
	CollectionProcessingEngine mCPE;

    private WikiDocumentIterator documentList;

	/**
	 * Constructs a {@link ConfluenceCPERunner} instance.
	 * 
	 * @param cpeDescriptorURL
	 *            a URL pointing to the CPE descriptor file defining the
	 *            collection processing engine to run.
	 */
	public ConfluenceCPERunner(URL cpeDescriptorURL) {
		this.cpeDescriptorURL = cpeDescriptorURL;
		// this.documentTitleReader = new BufferedReader(new FileReader(documentTitleFile.getFile()));
	}

	/**
	 * Sets a callback listener for the notifications the UIMA Collection
	 * Processing Management (CPM) emits while it is running.
	 * This is used to start the next page if one run is finished (fishy, but what gives)
	 * 
	 * @param statusCallbackListener
	 */
	public void setStatusCallbackListener(StatusCallbackListener statusCallbackListener) {
		this.statusCallbackListener = statusCallbackListener;
	}

	/**
	 * Processes the versions for the next document from the "ArtivleLister 
	 * 
	 * @throws UIMAException if an exception inside the CPM occurs.
	 * @throws IOException if an error occurs while reading the file containing the document titles.
	 * @throws CpeDescriptorException if an error occurs processing the CPE descriptor.
	 */
	public void process() throws UIMAException, IOException, CpeDescriptorException {

		CpeDescription cpeDesc = UIMAFramework.getXMLParser().parseCpeDescription(new XMLInputSource(cpeDescriptorURL));

		// instantiate CPE
		mCPE = UIMAFramework.produceCollectionProcessingEngine(cpeDesc);

		// Create and register a Status Callback Listener, to start the next run if this one is finished
        mCPE.addStatusCallbackListener(statusCallbackListener);
		
        WikiArticleCollectionReader collectionReader = (WikiArticleCollectionReader)mCPE.getCollectionReader();
        
        // set the current page to analyse: doing it in this awkward way is at most only a little less brainwhacking
        // than mocking up the config, as it is done in MediawikiCPERunner
        
        // on the very first run, fetch all pages (reusing the configured confluence connector)
        // this only works because "getCollectionReader" return an initialized object
        if (documentList == null) {
            log.info("fetch information about all pages");
            documentList = new WikiDocumentIterator();
            documentList.setConnector(collectionReader.getConnector());
            documentList.initialize();
            if (!documentList.hasNextPage()) {
                log.error("no pages found at all!");
                return;
            }
        }

        // get the next page and start over
        String pageId = documentList.nextPageId();
        collectionReader.setCurrentPageId(pageId);
		
        // (Re-)Start Processing
        mCPE.process();		    
		
	}

	/**
	 * This class implements a UIMA CPM status callback listener. It realizes
	 * the batch processing of this runner by subsequently calling the
	 * {@link ConfluenceCPERunner#processNext()} method when one collection
	 * processing has finished successfully.
	 * 
	 * @author ralph
	 * 
	 */
	private static class BatchStatusCallbackListener implements StatusCallbackListener {
		
		private ConfluenceCPERunner caller;

		/**
		 * Constructs a {@link BatchStatusCallbackListener}.
		 * 
		 * @param caller
		 *            the caller on which the
		 *            {@link ConfluenceCPERunner#processNext()} method should be
		 *            called when the processing of a collection has finished
		 *            successfully.
		 */
		public BatchStatusCallbackListener(ConfluenceCPERunner caller) {
			this.caller = caller;
		}

		/**
		 * @see org.apache.uima.collection.StatusCallbackListener#entityProcessComplete(org.apache.uima.cas.CAS,
		 *      org.apache.uima.collection.EntityProcessStatus)
		 */
		public void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
		}
	
		/**
		 * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#aborted()
		 */
		public void aborted() {
			log.error("Aborted");
		}
	
		/**
		 * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#batchProcessComplete()
		 */
		public void batchProcessComplete() {
			log.info("Batch process completed");
		}
	
		/**
		 * Triggers the processing of the next document collection in the batch.
		 * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#collectionProcessComplete()
		 */
		public void collectionProcessComplete() {
			log.info("Collection process completed");
            caller.notifyFinished();
		}
	
		/**
		 * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#initializationComplete()
		 */
		public void initializationComplete() {
			log.info("Initialisation complete");
		}
	
		/**
		 * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#paused()
		 */
		public void paused() {
			log.info("Paused");
		}
	
		/**
		 * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#resumed()
		 */
		public void resumed() {
			log.info("resumed");
		}
	
	}

    public void notifyFinished() {
        // callback after finishing one round
        // try to just restart the thing
        if (documentList.hasNextPage()) {
            log.info("start with next page");
            try {
                process();
            } catch (Exception e) {
                log.error("while restarting with next page", e);
            }
        } else {
            log.info("all pages are finished");
        }
    }

	/**
	 * Runs this {@link ConfluenceCPERunner} with the CPE descriptor specified by
	 * {@link ConfluenceCPERunner#CPE_DESCRIPTOR_URL}.
	 * 
	 * @param args this is what we ignore, for now
	 */
	public static void main(String[] args) {
		try {
			Config.read(ConfluenceCPERunner.class.getResourceAsStream("/ExpertFinder.properties"));
			// we do that later ...
			OntologyIndex.get().load(OntologyIndex.class.getResource(Config.getAppProperty(Config.Key.ONTOLOGY_FILE)));
			final ConfluenceCPERunner runner = new ConfluenceCPERunner(CPE_DESCRIPTOR_URL);
			
			runner.setStatusCallbackListener(new BatchStatusCallbackListener(runner));
			
			try {
				runner.process();
			} catch (UIMAException e) {
				log.fatal("failed due to UIMA exception. Maybe this is a configuration problem?",e);
			} catch (CpeDescriptorException e) {
                log.fatal("failed due to runtime exception. Maybe this is a configuration problem?", e);
			}

		} catch (IOException e) {
            log.fatal("failed due missing/unreadable configuration files.",e);
		}
		
	}

}
