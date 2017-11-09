# sonar-it-cover-plugin

Since Sonar 6.2, IT coverage metric have been discarded in project dashboard. Only Overall coverage is shown which is the result of merging both both of Jacoco results (jacoco.exec and jacoco-it.exec).

The purpose of this plugin is to:
- Compute the coverage value based on the jacoco-it.exec file.
- update a custom metric value related to the coverage (this custom metric should be created in Sonar). If this metric does not exist in the Sonar project, it will be created with the value computed.


mvn fr.icdc.dei.plugins:sonar-it-coverage-plugin:1.0-SNAPSHOT:cover -Ditcover.project="test" -DprojectDirectory="C:/Users/dali/Desktop/workspace/java-jacoco" -Ditcover.executionDataFile="C:/Users/dali/Desktop/workspace/sonar-custom-plugin/src/main/java/jacoco.exec" -Ditcover.classesDirectory="C:/Users/dali/Desktop/workspace/usecasetracker-back/target/UseCaseTracker/WEB-INF/classes" -Ditcover.sourceDirectory="C:/Users/dali/Desktop/workspace/usecasetracker-back/src" -Ditcover.reportDirectory="C:/Users/dali/Desktop/workspace/java-jacoco/coveragereport/cover.xml"
