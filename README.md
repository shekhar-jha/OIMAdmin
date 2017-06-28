OIMAdmin
========

A java swing application to play with Oracle Identity Manager. The code has been tested with OIM11g R2 PS2 and PS3 at this time.

Getting started
---------------

1. Ensure that you are using JRE 1.7 or later (JDK is needed for compilation)
2. Download the latest release from https://github.com/shekhar-jha/OIMAdmin/releases
3. unzip it in any folder e.g. C:\apps
4. Run the application by double clicking the OIMAdmin.jar file or by using following command<br/>
```
$JAVA_HOME\bin\java -jar C:\apps\OIMAdmin\OIMAdmin.jar
```

**Notes:**

1. Please note that application manages temporary files and configuration in {user.home}/.oimadm by default.
This location can be changed by passing java system property "workhome" (-Dworkhome=C:\apps\dist) on the command line.
2. Even though the application is shipped with all required files, if needed, java system property
"XL.HOME" (-DXL.Home) can be used to passed location of design console.

