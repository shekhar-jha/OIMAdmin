Version 0.10
-----------------

1. Support for Java 1.7 - The product can now be run using Java 1.7
2. Support for multiple OIM Client libraries - The system can be configured to use specific version of client libraries.
3. Libraries Updates - OIM 11g R2 PS2 & PS3 libraries are now shipped with tool.
4. Better error handling while opening connection trees.
5. Major refactoring to reduce number of connections being created and separate OIM functionality across multiple classes

Version 0.9
-----------------

1. Support for PS3 - The product now supports connecting to both OIM 11g R2 PS2 & PS3 environment.

Version 0.8
-----------------

1. Tracking Request and Orchestration - Ability to track request and orchestration. This release has limited functionality for tracking.

Version 0.7
-----------------

1. Performance Metrics - Ability to capture performance details for specific use-cases like Create user and LDAP accounts. Check out the configuration file for how to setup other use-cases.
2. Cache configuration - Ability to configure and manage OIM caches.

Notes
--------
1. With this release a new format for configuration is being released. The simplest way to migrate is to delete the ~/.oimadm file and restart the application. It will automatically recreate the file.

Version 0.6.2
-----------------

1. Window specific popup menu bug
2. Minor issue with JRE 


Version 0.6.1
-----------------

1. Bug fixes associated with connection management and minor enhancements
2. Connection Delete functionality

Version 0.6
---------------
1. UI classes refactored to be more manageable
2. Added ability to reconnect Connections and reload Event Handlers to avoid application restart
3. Added UI to run OIM Client code

*Known Issue*

1. Connection detail update is not reflected during reconnect.

Version 0.5
---------------
This release provides basic capability to 
1. Manage Connections (Connection deletion is not supported through UI)
2. Explore MDS Repository, change specific item and save it from UI
3. See all the details about existing event handlers configured in system
4. Create a new Event Handler through UI using simple code template.
