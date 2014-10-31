flexrep-client
==============

Provides a Java web application that mimics a replica FlexRep server in order to process CRUD of documents to a file-system, RDBMS or any other target.

Adheres to the Java Servlet 3 specification.  Running this on Tomcat 8 would be the safest bet since it is what is was written on.

Simply point MarkLogic Flexible Replication to http://<hostname>:<port>/<path>/.  The servlet responds to the apply.xqy POST and in the future will handle applyBinaryChunk.xqy.
