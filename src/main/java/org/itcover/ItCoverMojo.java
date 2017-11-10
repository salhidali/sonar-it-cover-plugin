package org.itcover;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
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
 * @goal itcover
 * @requiresProject false
 */

@Mojo(name="itcover")
public class ItCoverMojo extends AbstractMojo
{

	private static final String API_CUSTOM_MEASURES_CREATE_URI = "/api/custom_measures/create?metricKey=";

	private static final String API_CUSTOM_MEASURES_UPDATE_URI = "/api/custom_measures/update?id=";

	private static final String API_CUSTOM_MEASURES_SEARCH_METRIC_URI = "/api/custom_measures/search?metric=";

	private static final String API_COMPONENTS_SHOW_COMPONENT_URI = "/api/components/show?component=";

	private static final String EMPTY_STRING = "";

	private static final String EMPTY_CONST = "EMPTY";

	/**
	 * @parameter default-value="${project.groupId}:${project.artifactId}" expression="${itcover.project}"
	 */
	//@Parameter( property = "itcover.project", defaultValue = "admin" )
	private String project;

	/**
	 * Sonar custom measure that would be modified
	 * @parameter default-value="it_coverage" expression="${itcover.metricKey}"
	 */
	private String metricKey;
	
	/**
	 * Sonar URL (No default value defined)
	 * @parameter expression="${itcover.sonarUrl}"
	 */
	private String sonarUrl;
	
	/**
	 * Sonar username
	 * @parameter default-value="admin" expression="${itcover.sonarUsername}"
	 */
	private String sonarUsername;
	
	/**
	 * Sonar password
	 * @parameter default-value="admin" expression="${itcover.sonarPassword}"
	 */
	private String sonarPassword;

	/**
	 * The project basedir (Used for report generation)
	 *@parameter default-value="${basedir}"
	 */
	private File basedir;
	
	/**
	 * Execution Data File (generated by jacoco)
	 * @parameter default-value="${project.build.directory}/jacoco-it.exec" expression="${itcover.executionDataFile}"
	 */
	private File executionDataFile;
	
	/**
     * The directory containing generated classes of the project being tested. This will be included after the test
     * classes in the test classpath.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     */
	private File classesDirectory;
	
	/**
     * The source directory containing class sources.
     *
     * @parameter default-value="${project.build.sourceDirectory}"
     */
	private File sourceDirectory;
	
	/**
	 * The generated report file that would be used to compute coverage
	 * @parameter default-value="${project.build.directory}/it-cover.xml" expression="${itcover.reportFile}"
	 */
	private File reportFile;
	
	/**
	 * Indicates wether or not force the build to fail when there is an error during the execution of the plugin
	 * @parameter default-value="false" expression="${itcover.coverPluginFailureIgnore}"
	 */
	private boolean coverPluginFailureIgnore;
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void execute() throws MojoExecutionException, MojoFailureException {

		getLog().info("IT cover plugin : START");
		CloseableHttpClient client = HttpClients.createDefault();
		
		if(EMPTY_CONST.equals(sonarPassword)) {
			sonarPassword = EMPTY_STRING;
		}
		
		try {
			
			//Check wether or not the project exists in sonar before proceeding to the coverage compute and update
			HttpGet projectGet = new HttpGet(sonarUrl+API_COMPONENTS_SHOW_COMPONENT_URI+project);
			
			ResponseHandler responseHandler = (ResponseHandler) new JSONResponseHandler();
			JSONObject responseBody = (JSONObject) client.execute(projectGet, responseHandler);
			
			JSONObject component = (JSONObject) responseBody.get("component");
			
			if(null == component) {
				getLog().error("The requested sonar project does not exist.");
				return;
			} 
			HttpGet httpGet = new HttpGet(sonarUrl+API_CUSTOM_MEASURES_SEARCH_METRIC_URI+metricKey+"&projectKey="+project);
			responseBody = (JSONObject) executeHttpQuery(client, httpGet, responseHandler);
			JSONArray customMeasure = (JSONArray) responseBody.get("customMeasures");
			
			getLog().info(responseBody.toJSONString());
			// Check if the custom measure exists in the sonar projects 
			if(null != customMeasure && !customMeasure.isEmpty()) {// Update the custom measure if it exists
				String id = (String) ((JSONObject) customMeasure.get(0)).get("id");
				HttpPost updatePost = new HttpPost(sonarUrl+API_CUSTOM_MEASURES_UPDATE_URI+id+"&value="+getCoverage());
				responseBody = (JSONObject) executeHttpQuery(client, updatePost, responseHandler);
				getLog().info(responseBody.toJSONString());
				if(StringUtils.isEmpty((String) responseBody.get("id")) || !id.equals((String) responseBody.get("id"))) {
					getLog().error("Updating sonar custom metric failed");
				}
			} else {// Create the custom measure with the coverage value if it does not exist
				HttpPost createPost = new HttpPost(sonarUrl+API_CUSTOM_MEASURES_CREATE_URI+metricKey+"&projectKey="+project+"&value="+getCoverage());
				responseBody = (JSONObject) executeHttpQuery(client, createPost, responseHandler);
				getLog().info(responseBody.toJSONString());
				if(StringUtils.isEmpty((String) responseBody.get("id"))) {
					getLog().error("Creating sonar custom metric failed");
				}
			}
		}
		catch (IOException e) {
			processException(e);
		}
		catch (AuthenticationException e) {
			processException(e);
		}
		catch (ParserConfigurationException e) {
			processException(e);
		}
		catch (SAXException e) {
			processException(e);
		}
		finally {
			try {
				client.close();
			}
			catch (IOException e) {
				// ignore
			}
		}
		getLog().info("IT cover plugin : END");
	}

	/**
	 * This method handles all types of exception by logging the error and depending on the user choice forcing the build failure.
	 * 
	 * @param Exception
	 * @throws MojoFailureException
	 */
	private void processException(Exception e) throws MojoFailureException {
		getLog().error(e);
		if (!coverPluginFailureIgnore) {
			throw new MojoFailureException(e.getMessage());
		}
	}
	
	private <T> T executeHttpQuery(final CloseableHttpClient client, final HttpUriRequest httpRequest, final ResponseHandler<? extends T> responseHandler) throws IOException, AuthenticationException {
		BasicScheme basicScheme = new BasicScheme();
		UsernamePasswordCredentials creds = new UsernamePasswordCredentials(sonarUsername, sonarPassword);
		httpRequest.addHeader(basicScheme.authenticate(creds, httpRequest, null));
		return client.execute(httpRequest, responseHandler);
	}
	
	private double getCoverage() throws ParserConfigurationException, SAXException, IOException {
		final ReportGenerator generator = new ReportGenerator(basedir, executionDataFile, classesDirectory, sourceDirectory,	reportFile);
		generator.create();
		return computeCoverageFromXml(reportFile);
	}
	
	public double computeCoverageFromXml(File reportFile) throws ParserConfigurationException, SAXException, IOException {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		double coverage = 0;
		final DocumentBuilder builder = factory.newDocumentBuilder();
		
		builder.setEntityResolver(new EntityResolver() {
	        @Override
	        public InputSource resolveEntity(String publicId, String systemId)
	                throws SAXException, IOException {
	            if (systemId.contains("report.dtd")) {
	                return new InputSource(new StringReader(EMPTY_STRING));
	            } else {
	                return null;
	            }
	        }
	    });
		
		final Document document= builder.parse(reportFile);
		final Element racine = document.getDocumentElement();

		final NodeList racineNoeuds = racine.getChildNodes();
		final int nbRacineNoeuds = racineNoeuds.getLength();

		for (int i = 0; i<nbRacineNoeuds; i++) {
			if(racineNoeuds.item(i).getNodeType() == Node.ELEMENT_NODE) {
				final Element counter = (Element) racineNoeuds.item(i);
				if(counter.getAttribute("type").equals("INSTRUCTION")) {
						int missed = Integer.parseInt(counter.getAttribute("missed"));
						int covered = Integer.parseInt(counter.getAttribute("covered"));
						
						coverage  = (covered * 100) / (covered + missed);
				}
			}
		}
		
		return coverage;
	}
 }