package de.csw.expertfinder.confluence.api;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.BinaryEncoder;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.fasterxml.jackson.jr.ob.JSON;

import com.espresto.util.URLEncoder;

public class ConfluenceRestClient {

    private static final Log log = LogFactory.getLog(ConfluenceRestClient.class);

    private final String restApiBaseUrl;
    private String username;
    private String password;
    
    private boolean initialized;

    HttpClientConnection connection;
    private HttpClient http = new DefaultHttpClient();

    public ConfluenceRestClient(String baseUrl) {
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        restApiBaseUrl = baseUrl;
    }

    // after properties are set ... (?)
    public void initialize() {
        HttpParams params = this.http.getParams();
        // ToDo: make configureable (one day)
        HttpConnectionParams.setConnectionTimeout(params, 1000);
        HttpConnectionParams.setSoTimeout(params, 1000);
        
        initialized = true;
    }
    
    public List<String> getAllSpaceKeys() {
        // note: we spare ourself the trouble of UrlTemplates
        Map<String, Object> spaceResult = fetchJsonResponse("/rest/api/space", null);
        return extractResults(spaceResult, "key");
    }

    @SuppressWarnings("unchecked")
    public List<String> getAllPageIdsForSpace(String key) {
        Map<String, Object> spaceResult = fetchJsonResponse("/rest/api/space/" + URLEncoder.encode(key)+"/content", null);
        
        // TODO: What other type of pages might we have ?
        Map<String, Object> pages = (Map<String, Object>) spaceResult.get("page");
        List <String> pageIds =  extractResults(pages, "id");
        Map<String, Object> blogs = (Map<String, Object>) spaceResult.get("blogpost");
        pageIds.addAll(extractResults(blogs, "id"));
        
        return pageIds;
    }

    public List<String> getVersionsTextForPageId(String pageId) {
        // ToDo: we have no official way to get at version
        // instead this calls a special version
        Map<String, Object> versionData = fetchJsonResponse("/rest/semanticresource/1.0/versions/" + URLEncoder.encode(pageId), null);
        List <String> versionIds =  extractResults(versionData, "plainContent");
        return versionIds;
    }
    
    @SuppressWarnings("unchecked")
    public List<ConfluencePageVersionInfo> getVersionsInfoForPageId(String pageId, long lastKnownRevision) {
        // FIXME: lastKnownVersion is unused
        // ToDo: we have no official way to get at version ... see above
        Map<String, Object> versionData = fetchJsonResponse("/rest/semanticresource/1.0/versions/" + URLEncoder.encode(pageId), null);
        
        int page_id = ((Number) versionData.get("pageId")).intValue();
        List <ConfluencePageVersionInfo> versions = new LinkedList<ConfluencePageVersionInfo>();//  =  extractResults(versionData, "plainContent");
        for (Map<String,Object> oneVersion : ((List<Map<String,Object>>)versionData.get("results")) ) {
            ConfluencePageVersionInfo info = new ConfluencePageVersionInfo();

            info.setArticleId(page_id);
            // ToDo: for the *last* version this id will change on the next page edit :-/ 
            info.setRevisionId( ((Number)oneVersion.get("pageId")).intValue());
            
            info.setUser( (String)oneVersion.get("userName") );
            info.setTimestamp( new Date( (Long)oneVersion.get("lastModified") ));
            
            info.setComment( (String)oneVersion.get("comment") );
            info.setTitle( (String)oneVersion.get("title") );

            //info.setText( (String)oneVersion.get("plainContent") );
            info.setText( (String)oneVersion.get("content") );
                        
            versions.add(info);
        }
        return versions;
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new RuntimeException("not initialized");
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractResults(Map<String, Object> result, String attrName) {
        return extractResults(result, attrName, true);
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractResults(Map<String, Object> result, String attrName, boolean failIfEmpty) {
        if (failIfEmpty && ((Number) result.get("size")).intValue() == 0) {
            throw new ConfluenceConnectionException("no results found for " + attrName);
        }

        List<String> results = new ArrayList<String>();
        List<Map<String, Object>> spaces = (List<Map<String, Object>>) result.get("results");
        for (Map<String, Object> space : spaces) {
            results.add((String) space.get(attrName));
        }
        return results;
    }

    // ToDo: better JSON bindings ?
    private Map<String, Object> fetchJsonResponse(String restPath, BasicNameValuePair[] params) {
        checkInitialized();
        try {
            StringBuilder url = new StringBuilder(restApiBaseUrl);
            url.append(restPath).append("?os_authType=basic&limit=1000");

            // note: we often have an implicit limit of 25 entries here ... maybe respect that and handle things in chunks ?
            if (params != null)
                for (BasicNameValuePair param : params) {
                    url.append('&').append(URLEncoder.encode(param.getName())).append('=').append(URLEncoder.encode(param.getValue()));
                }

            if (log.isDebugEnabled()) {
                log.debug("url it " + url);
            }
            HttpGet httpGet = new HttpGet(url.toString());
            handleAuth(httpGet);

            // make sure we read the response as UTF-8 (even without hint from the server) 
            HttpResponse response = http.execute(httpGet);
            StringWriter responseWriter = new StringWriter();
            IOUtils.copy(response.getEntity().getContent(), responseWriter, "UTF-8");

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new ConfluenceConnectionException("status is " + response.getStatusLine() + " for " + url);
            }
            // ToDo: shorter:
            // EntityUtils.toString(response.getEntity(), "UTF-8");
            String responseJsonString =  responseWriter.toString();

            Map<String, Object> result = JSON.std.mapFrom(responseJsonString);

            return result;

        } catch (ConfluenceConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfluenceConnectionException(e);
        }
    }

    protected void handleAuth(HttpRequestBase httpRequest) throws EncoderException, UnsupportedEncodingException {
        // quick & dirty instead of UsernamePasswordBasicCredentialsProviderAuthScopeHandlerFactorySetup
        if (StringUtils.isEmpty(username)) { return; }
        String authorisation = username + ":" + password;
        BinaryEncoder base64 = new Base64();
        String encodedAuthorisation = new String(base64.encode(authorisation.getBytes("UTF-8")), "UTF-8");
        httpRequest.setHeader("Authorization", "Basic " + encodedAuthorisation);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static void main(String[] args) {
        ConfluenceRestClient cl = new ConfluenceRestClient("http://localhost:1990/confluence/");

        cl.setUsername("Admin");
        cl.setPassword("admin");

        cl.initialize();
        System.out.println(cl.getAllSpaceKeys());
        System.out.println(cl.getAllPageIdsForSpace("ds").size());
        System.out.println(cl.getVersionsTextForPageId("819617"));
        cl.getVersionsInfoForPageId("819282", 0);
    }
}
