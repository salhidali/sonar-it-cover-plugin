package fr.icdc.dei.plugins;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Echos an object string to the output screen.
 * @goal echo
 * @requiresProject false
 */

@Mojo( name = "cover")
public class CoverItMojo extends AbstractMojo
{

	@Parameter( property = "itcover.projectKey")
	private String project;

	@Parameter( property = "itcover.metric", defaultValue = "cover" )
	private String metric;
	
	@Parameter( property = "itcover.sonar", defaultValue = "http://localhost:9000" )
	private String sonar;
	
	@Parameter( property = "itcover.username", defaultValue = "admin" )
	private String username;
	
	@Parameter( property = "itcover.password", defaultValue = "admin" )
	private String password;

	@Parameter( property = "itcover.projectDirectory", defaultValue = "admin" )
	private String projectDirectory;
	
	@Parameter( property = "itcover.executionDataFile", defaultValue = "admin" )
	private String executionDataFile;
	
	@Parameter( property = "itcover.classesDirectory", defaultValue = "admin" )
	private String classesDirectory;
	
	@Parameter( property = "itcover.sourceDirectory", defaultValue = "admin" )
	private String sourceDirectory;
	
	@Parameter( property = "itcover.reportDirectory", defaultValue = "admin" )
	private String reportDirectory;

	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void execute() throws MojoExecutionException, MojoFailureException {

		try {
			CloseableHttpClient client = HttpClients.createDefault();

			BasicScheme basicScheme = new BasicScheme();
			UsernamePasswordCredentials creds = new UsernamePasswordCredentials("admin", "admin");
			
			HttpGet httpGet = new HttpGet(sonar+"/api/custom_measures/search?metric="+metric+"&projectKey="+project);
			

			try {
				httpGet.addHeader(basicScheme.authenticate(creds, httpGet, null));
			} catch (AuthenticationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			ResponseHandler responseHandler = (ResponseHandler) new JSONResponseHandler();
			
			JSONObject responseBody = (JSONObject) client.execute(httpGet, responseHandler);
			
			JSONArray customMeasure = (JSONArray) responseBody.get("customMeasures");
			String id = (String) ((JSONObject) customMeasure.get(0)).get("id");

			//HttpPost createPost = new HttpPost(sonar+"/api/custom_measures/create?metricKey="+metric+"&projectKey="+project+"&value="+getCoverage());
			
			HttpPost httpPost = new HttpPost(sonar+"/api/custom_measures/update?id="+id+"&value="+getCoverage());
			try {
				httpPost.addHeader(basicScheme.authenticate(creds, httpGet, null));
			} catch (AuthenticationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			JSONObject responsePostBody = (JSONObject) client.execute(httpPost, responseHandler);
			System.out.println(responsePostBody);

			client.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private double getCoverage() throws IOException {
		final ReportGenerator generator = new ReportGenerator(new File(projectDirectory), executionDataFile, classesDirectory, sourceDirectory,	reportDirectory);
		generator.create();
		return getcoverageFromXml(reportDirectory);
	}
	
	public double getcoverageFromXml(String path) {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		double coverage = 0;
		try {
			final DocumentBuilder builder = factory.newDocumentBuilder();
			
			builder.setEntityResolver(new EntityResolver() {
		        @Override
		        public InputSource resolveEntity(String publicId, String systemId)
		                throws SAXException, IOException {
		            if (systemId.contains("report.dtd")) {
		                return new InputSource(new StringReader(""));
		            } else {
		                return null;
		            }
		        }
		    });
			
			final Document document= builder.parse(new File(path));
			final Element racine = document.getDocumentElement();

			final NodeList racineNoeuds = racine.getChildNodes();
			final int nbRacineNoeuds = racineNoeuds.getLength();

			for (int i = 0; i<nbRacineNoeuds; i++) {
				if(racineNoeuds.item(i).getNodeType() == Node.ELEMENT_NODE) {
					final Element counter = (Element) racineNoeuds.item(i);
					if(counter.getAttribute("type").equals("INSTRUCTION")) {
							System.out.println(counter.getAttribute("missed")+"/"+counter.getAttribute("covered") );
							
							int missed = Integer.parseInt(counter.getAttribute("missed"));
							int covered = Integer.parseInt(counter.getAttribute("covered"));
							
							coverage  = (covered * 100) / (covered + missed);
					}
				}
			}
			
		}catch (final ParserConfigurationException e) {
				e.printStackTrace();
			}
			catch (final SAXException e) {
				e.printStackTrace();
			}
			catch (final IOException e) {
				e.printStackTrace();
			}
		
		return coverage;
	}
 }