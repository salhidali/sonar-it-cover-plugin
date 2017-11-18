# sonar-it-cover-plugin

Since Sonar 6.2, IT coverage metric have been discarded in project dashboard. Only Overall coverage is shown which is the result of merging both both of Jacoco results (jacoco.exec and jacoco-it.exec).

The purpose of this plugin is to:
- Compute the coverage value based on the jacoco-it.exec file.
- update a custom metric value related to the coverage (this custom metric should be created in Sonar). If this metric does not exist in the Sonar project, it will be created with the value computed.


