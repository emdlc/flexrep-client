flexrep-client
==============

Provides a Java web application that mimics a replica FlexRep server in order to process CRUD of documents to a file-system, RDBMS or any other target.

Adheres to the Java Servlet 3 specification.  Running this on Tomcat 8 would be the safest bet since it is what is was written on.

Simply point MarkLogic Flexible Replication to http://<hostname>:<port>/<path>/.  The servlet responds to the apply.xqy POST.


### Setup

The latest version contains a MarkLogic Query Console workspace: [Flexrep-config-workspace.xml](https://github.com/emdlc/flexrep-client/blob/master/flexrep-client/src/xqy/Flexrep-config-workspace.xml)

Import this workspace into your QC and then you'll be able to Create the needed Databases, FlexRep XCC App Server, and related Modules/Triggers DBs for use when testing the Flexrep client Servlet.

### Configuration

There are 2 files that need to be modified from the defaults:  [web.xml](https://github.com/emdlc/flexrep-client/blob/master/flexrep-client/web/WEB-INF/web.xml) and [flexrep-client.properties](https://github.com/emdlc/flexrep-client/blob/master/flexrep-client/src/test/resources/flexrep-client.properties)

web.xml
```
<env-entry>
    <env-entry-name>ml.flexrep.xcc.url</env-entry-name>
    <env-entry-type>java.lang.String</env-entry-type>
    <env-entry-value>xcc://user:password@localhost:9001/Master</env-entry-value>
</env-entry>

<env-entry>
    <env-entry-name>flexrep.root.directory</env-entry-name>
    <env-entry-type>java.lang.String</env-entry-type>
    <env-entry-value>/tmp/flexrep-target</env-entry-value>
    <!--  <env-entry-value>C:\temp\flexrep-target</env-entry-value> -->
</env-entry>
```


flexrep-client.properties
```
ml.flexrep.xcc.url=xcc://user:password@localhost:9001/Master

tomcat.http.host=localhost
tomcat.http.port=8080
tomcat.location.logs=/opt/tomcat-8.0.20/logs
tomcat.location.logs.prefix=localhost_access_log.
tomcat.replication.path=/tmp/flexrep-target
```
