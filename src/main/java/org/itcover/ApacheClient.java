package org.itcover;

import java.io.IOException;

import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ApacheClient {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] args) throws AuthenticationException, ClientProtocolException, IOException {
		CloseableHttpClient client = HttpClients.createDefault();
	    HttpGet httpGet = new HttpGet("http://localhost:9000/api/custom_measures/search?metric=cover&projectKey=test");
	 
	    UsernamePasswordCredentials creds = new UsernamePasswordCredentials("admin", "admin");
	    httpGet.addHeader(new BasicScheme().authenticate(creds, httpGet, null));
	 
	    ResponseHandler responseHandler = (ResponseHandler) new JSONResponseHandler();
	    
	    JSONObject responseBody = (JSONObject) client.execute(httpGet, responseHandler);
	    

	    JSONArray customMeasure = (JSONArray) responseBody.get("customMeasures");

	    String id = (String) ((JSONObject) customMeasure.get(0)).get("id");

	    
	    String value = "98";

	    HttpPost httpPost = new HttpPost("http://localhost:9000/api/custom_measures/update?id="+id+"&value="+value);
	    httpPost.addHeader(new BasicScheme().authenticate(creds, httpGet, null));
	    
	    JSONObject responsePostBody = (JSONObject) client.execute(httpPost, responseHandler);
	    System.out.println(responsePostBody);

	    client.close();
	}
}
