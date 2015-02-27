package de.csw.expertfinder.confluence.api;

import de.csw.expertfinder.mediawiki.api.MediaWikiArticleVersion;

/**
 * contains all information we get from confluence about a given page version
 */
public class ConfluencePageVersionInfo extends MediaWikiArticleVersion {

    public String toString() {
        return this.getClass().getName() + "[id=" + getRevisionId() + '@' + getArticleId() + "]";
        // return ToStringBuilder.reflectionToString(this , ToStringStyle.SIMPLE_STYLE);
    }

}
