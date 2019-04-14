package org.openmf.mifos.dataimport.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.openmf.mifos.dataimport.dto.AuthToken;
import org.openmf.mifos.dataimport.http.SimpleHttpRequest.Method;

import com.google.gson.Gson;


public class MifosRestClient implements RestClient {
	
	 
    
    private final String baseURL;

    private final String userName;

    private final String password;

    private final String tenantId;

    private String authToken;
    
    static {
    	if(System.getProperty("mifos.endpoint").contains("localhost")) {
	    //for localhost testing only
	       HttpsURLConnection.setDefaultHostnameVerifier(
	       new HostnameVerifier(){

	    	  @Override
	          public boolean verify(String hostname, @SuppressWarnings("unused") SSLSession sslSession) {
	              if (hostname.equals("localhost")) {
	                  return true;
	              }
	              return false;
	          }
	       });
    	}
	}
    
    public MifosRestClient() {
    	
        baseURL  = System.getProperty("mifos.endpoint");
        userName = System.getProperty("mifos.user.id");
        password = System.getProperty("mifos.password");
        tenantId = System.getProperty("mifos.tenant.id");
    };

    public static final class Header {
        public static final String AUTHORIZATION = "Authorization";
        public static final String CONTENT_TYPE = "Content-Type";
        public static final String MIFOS_TENANT_ID = "Fineract-Platform-TenantId";
        public static final String ACCEPT_ENCODING = "Accept-Encoding";
        public static final String ACCEPT = "Accept";
    }
    

    @Override
    public String post(String path, String payload) {
        String url = baseURL + path;
        try {

                SimpleHttpResponse response = new HttpRequestBuilder().withURL(url).withMethod(Method.POST)
                                .addHeader(Header.AUTHORIZATION, "Basic " + authToken)
                                .addHeader(Header.CONTENT_TYPE, "application/json; charset=utf-8")
                                .addHeader(Header.MIFOS_TENANT_ID, tenantId)
                                .addHeader(Header.ACCEPT_ENCODING, "gzip,deflate")
                                .addHeader(Header.ACCEPT, "*/*")
                                .withContent(payload).execute();
                String content = readContentAndClose(response.getContent());
            if (response.getStatus() != HttpURLConnection.HTTP_OK) 
              { 
            	throw new IllegalStateException(content);
              }
            return content;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    public String get(String path) {
    	String url = baseURL + path;
    	try {
    		      SimpleHttpResponse response = new HttpRequestBuilder().withURL(url).withMethod(Method.GET)
    		    		          .addHeader(Header.AUTHORIZATION, "Basic " + authToken)
    		    		          .addHeader(Header.MIFOS_TENANT_ID,tenantId)
    		    		          .addHeader(Header.ACCEPT_ENCODING, "gzip,deflate")
                                          .addHeader(Header.ACCEPT, "*/*")
    		    		          .execute();
    		      String content = readContentAndClose(response.getContent());
    		      if(response.getStatus() != HttpURLConnection.HTTP_OK)
    		      {
    		    	  throw new IllegalStateException(content);
    		      }
    		      return content;
    	} catch (IOException e) {
    		  throw new IllegalStateException(e);
    	}
    }

    @Override
    public void createAuthToken() {
        String url = baseURL + "authentication?username=" + userName + "&password=" + password;
        try {
            SimpleHttpResponse response = new HttpRequestBuilder().withURL(url).withMethod(Method.POST)
                        .addHeader(Header.MIFOS_TENANT_ID, tenantId)
                        .addHeader(Header.CONTENT_TYPE, "application/json")
                        .addHeader(Header.ACCEPT_ENCODING, "gzip,deflate")
                        .addHeader(Header.ACCEPT, "*/*")
                       .execute();
                        
            String content = readContentAndClose(response.getContent());
            AuthToken auth = new Gson().fromJson(content, AuthToken.class);
            authToken = auth.getBase64EncodedAuthenticationKey();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String readContentAndClose(InputStream content) throws IOException {
        GZIPInputStream convert = new GZIPInputStream(content);
        InputStreamReader stream = new InputStreamReader(convert,"UTF-8");
        BufferedReader reader = new BufferedReader(stream);
        String data = reader.readLine();
        stream.close();
        reader.close();
        return data;
    }

}
