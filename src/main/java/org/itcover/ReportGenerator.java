package org.itcover;

/*******************************************************************************
 * Copyright (c) 2009, 2017 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Brock Janiczak - initial API and implementation
 *    
 *******************************************************************************/


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.codehaus.plexus.util.FileUtils;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.maven.FileFilter;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.xml.XMLFormatter;


/**
 * This example creates a HTML report for eclipse like projects based on a
 * single execution data store called jacoco.exec. The report contains no
 * grouping information.
 * 
 * The class files under test must be compiled with debug information, otherwise
 * source highlighting will not work.
 */
public class ReportGenerator {

	private final String title;

	private final File executionDataFile;
	private final File classesDirectory;
	private final File sourceDirectory;
	private final File reportFile;

	private ExecFileLoader execFileLoader;

	/**
	 * Create a new generator based for the given project.
	 * 
	 * @param projectDirectory
	 */
	public ReportGenerator(final File projectDirectory, File executionFile, File classesDirectory, File sourceDirectory, File reportFile) {
		this.title = projectDirectory.getName();
		this.executionDataFile = executionFile;
		this.classesDirectory = classesDirectory;
		this.sourceDirectory = sourceDirectory;
		this.reportFile = reportFile;
//		
//		this.executionDataFile = new File("C:/Users/dali/Desktop/workspace/sonar-custom-plugin/src/main/java/jacoco.exec");
//		this.classesDirectory = new File("C:/Users/dali/Desktop/workspace/usecasetracker-back/target/UseCaseTracker/WEB-INF/classes");
//		this.sourceDirectory = new File("C:/Users/dali/Desktop/workspace/usecasetracker-back/src");
//		this.reportDirectory = new File(projectDirectory, "coveragereport/cover.xml");
	}

	/**
	 * Create the report.
	 * 
	 * @throws IOException
	 */
	public void create() throws IOException {

		// Read the jacoco.exec file. Multiple data files could be merged
		// at this point
		loadExecutionData();

		// Run the structure analyzer on a single class folder to build up
		// the coverage model. The process would be similar if your classes
		// were in a jar file. Typically you would create a bundle for each
		// class folder and each jar you want in your report. If you have
		// more than one bundle you will need to add a grouping node to your
		// report
		final IBundleCoverage bundleCoverage = analyzeStructure();

		createReport(bundleCoverage);

	}

	private void createReport(final IBundleCoverage bundleCoverage)
			throws IOException {

		// Create a concrete report visitor based on some supplied
		// configuration. In this case we use the defaults
		final XMLFormatter xmlFormatter = new XMLFormatter();
		final IReportVisitor visitor = xmlFormatter.createVisitor(new FileOutputStream(reportFile));//htmlFormatter.createVisitor(new FileMultiReportOutput(reportDirectory));
				

		// Initialize the report with all of the execution and session
		// information. At this point the report doesn't know about the
		// structure of the report being created
		visitor.visitInfo(execFileLoader.getSessionInfoStore().getInfos(),
				execFileLoader.getExecutionDataStore().getContents());

		// Populate the report structure with the bundle coverage information.
		// Call visitGroup if you need groups in your report.
		visitor.visitBundle(bundleCoverage, new DirectorySourceFileLocator(
				sourceDirectory, "utf-8", 4));

		// Signal end of structure information to allow report to write all
		// information out
		visitor.visitEnd();

	}

	private void loadExecutionData() throws IOException {
		execFileLoader = new ExecFileLoader();
		execFileLoader.load(executionDataFile);
	}

	@SuppressWarnings("unchecked")
	private IBundleCoverage analyzeStructure() throws IOException {
		final CoverageBuilder coverageBuilder = new CoverageBuilder();
		final Analyzer analyzer = new Analyzer(
				execFileLoader.getExecutionDataStore(), coverageBuilder);

		String[] includes = {"**"};
		String[] excludes = {"{0}"};
		
		FileFilter fileFilter = new FileFilter(Arrays.asList(includes), Arrays.asList(excludes));
		
		try {
			final List<File> filesToAnalyze = FileUtils.getFiles(classesDirectory,fileFilter.getIncludes(), fileFilter.getExcludes());
			for (final File file : filesToAnalyze) {
				analyzer.analyzeAll(file);
			}
		} catch (IOException e) {
			throw new IOException("While reading class directory: " + classesDirectory, e);
		} catch (RuntimeException e) {
			throw new RuntimeException("While reading class directory: " + classesDirectory, e);
		}
		
		//analyzer.analyzeAll(classesDirectory);

		return coverageBuilder.getBundle(title);
	}

}
