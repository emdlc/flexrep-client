package com.marklogic.flexrep.client;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.InputSource;

import com.marklogic.xcc.Content;
import com.marklogic.xcc.ContentCreateOptions;
import com.marklogic.xcc.ContentFactory;

public class FileSystemApplyServletTest {

	private static final Logger logger = Logger
			.getLogger(FileSystemApplyServletTest.class.getName());

	private static final String FLEX_REP_ROOT = "/tmp";
	
	private static final String NON_CHUNKING_BINARY_SMALL = "/canyon-single-small.jpg";
	private static final String NON_CHUNKING_BINARY_0_97MB = "/canyon-single-0.97MB.jpg";
	private static final String NON_CHUNKING_BINARY_1_2MB = "/canyon-single-1.2MB.jpg";
	
	//private static final String NON_CHUNKING_FILENAME_URI = FLEX_REP_ROOT + NON_CHUNKING_FILENAME;
	
	private static final String XML_FILENAME1 = "/books.xml";
	private static final String XML_FILENAME2 = "/books2.xml";
	private static final String XML_FILENAME3 = "/books3.xml";
	private static final String XML_FILENAME4 = "/books4.xml";

	//private static final String XML_FILENAME_URI = FLEX_REP_ROOT + XML_FILENAME;
	
	private static final String multi_XML_URIs[] = {XML_FILENAME2, XML_FILENAME3, XML_FILENAME4};

	private static Properties testingProperties = null;

	private static XccHelper xccHelper = null;
	
	private static NamespaceContext CONTEXT = new NamespaceContextMap("doc", "xdmp:document-load", 
			"flexrep", "http://marklogic.com/xdmp/flexible-replication", 
			"prop", "http://marklogic.com/xdmp/property");
	
    /** The number of fields that must be found. */
    public static final int NUM_FIELDS = 9;

	public FileSystemApplyServletTest() {
		try {
			if (testingProperties == null)
				testingProperties = Utils
						.getPropertiesFromClasspath("/flexrep-client.properties");
			logger.info("url="
					+ testingProperties.getProperty("ml.flexrep.xcc.url"));
			if (xccHelper == null)
				xccHelper = new XccHelper(
						testingProperties.getProperty("ml.flexrep.xcc.url"));
		} catch (IOException | XccHelperException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testNonChunkingBinaryReplication() {

		try {
			assertTomcatFlexRepReplicaIsAlive();
			assertMarkLogicIsAlive();

			String filename = NON_CHUNKING_BINARY_SMALL;
			insertNonChunkingBinary(filename);

			Thread.sleep(2000);
			assertFileWrittenAndReplicatedPerMaster(filename);
			// TODO: Check the destination tomcat to see if it fired (TBC on
			// tomcat)  // check access log for a 200 response with a timestamp of last 20 secs

			String ML_URI = FLEX_REP_ROOT + filename;
			assertFileWrittenToFileSystem(ML_URI);

			assertFileHasValidChecksum(filename);
			
		} catch (FileSystemApplyInsertException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testXMLReplication() {
		try {

			assertTomcatFlexRepReplicaIsAlive();
			assertMarkLogicIsAlive();

			String filename = XML_FILENAME1;
			insertXmlFile(filename);

			Thread.sleep(2000);
			assertFileWrittenAndReplicatedPerMaster(filename);

			String ML_URI = FLEX_REP_ROOT + filename;
			assertFileWrittenToFileSystem(ML_URI);
			
			assertFileHasValidChecksum(filename);
			
			//TODO: not yet implemented
			//assertTargetTomcatLoggedPost();
			
		} catch (FileSystemApplyInsertException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	/**
	 * Verify the Servlet handles multiple request.
	 */
	public void testXMLReplicationMultipleFiles() {
		try {

			assertTomcatFlexRepReplicaIsAlive();
			assertMarkLogicIsAlive();

			//insertXmlFiles();
			for(int i=0; i<multi_XML_URIs.length; i++) {
				String filename =  multi_XML_URIs[i];
				insertXmlFile(filename);
				Thread.sleep(2000);  // give the server time to process request
				assertFileWrittenAndReplicatedPerMaster(filename);

				String ML_URI = FLEX_REP_ROOT + filename;
				assertFileWrittenToFileSystem(ML_URI);
				
				assertFileHasValidChecksum(filename);
			}			
		} catch (FileSystemApplyInsertException | InterruptedException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testMaximumFileSize() {
		assertTomcatFlexRepReplicaIsAlive();
		assertMarkLogicIsAlive();
		
		try {

			insertNonChunkingBinary(NON_CHUNKING_BINARY_0_97MB);
			insertNonChunkingBinary(NON_CHUNKING_BINARY_1_2MB);
			
			Thread.sleep(2000);  // give the server time to process request
			
			// This file should pass both tests
			assertFileWrittenToFileSystem(FLEX_REP_ROOT+NON_CHUNKING_BINARY_0_97MB);
			assertFileHasValidChecksum(NON_CHUNKING_BINARY_0_97MB);

			// this should pass
			assertFileWrittenToFileSystem(FLEX_REP_ROOT+NON_CHUNKING_BINARY_1_2MB);
			// TODO: this test fails since ApplyServlet can only handle 1MB files
			assertFileHasValidChecksum(NON_CHUNKING_BINARY_1_2MB);
			
		} catch (FileSystemApplyInsertException | InterruptedException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testUpdateProperties()
			throws FileSystemApplyInsertException {
		try {			
			assertTomcatFlexRepReplicaIsAlive();
			assertMarkLogicIsAlive();

			String filename = XML_FILENAME2;
			String ML_URI = FLEX_REP_ROOT + filename;
			// update properties
			String status = "UPDATED PROPS ONLY";
			String xquery = "xdmp:document-set-properties('"+ML_URI+"', <status>"+status+"</status>)";
			String response = xccHelper.executeXquery(xquery);
			
			//logger.info(response);
			// should be no response for this
			Assert.assertEquals("MarkLogic xquery error", "",
					response);
			
			Thread.sleep(2000);
			
			// need to perform same assertions as for document insert
			assertFileWrittenAndReplicatedPerMaster(filename);
			assertFileWrittenToFileSystem(ML_URI);
			assertFileHasValidChecksum(filename);
			
			String propFileName = ML_URI+".metadata";
			assertLocalPropertiesFileUpdated(propFileName, status);

		} catch (XccHelperException | InterruptedException e) {
			Assert.fail("MarkLogic server is unreachable");
		}
	}

	//@Ignore
	@Test
	public void testChunkingReplication() {
		fail("Chunking Replication NOT yet implemented");
	}
	
	@Test
	public void testLargeBinaryReplication() {

		try {
			assertTomcatFlexRepReplicaIsAlive();
			assertMarkLogicIsAlive();

			String filename = NON_CHUNKING_BINARY_1_2MB;
			insertNonChunkingBinary(filename);

			Thread.sleep(2000);
			assertFileWrittenAndReplicatedPerMaster(filename);
			// TODO: Check the destination tomcat to see if it fired (TBC on
			// tomcat)  // check access log for a 200 response with a timestamp of last 20 secs

			String ML_URI = FLEX_REP_ROOT + filename;
			assertFileWrittenToFileSystem(ML_URI);

			assertFileHasValidChecksum(filename);
			
		} catch (FileSystemApplyInsertException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void assertTargetTomcatLoggedPost() {
		// TODO: Check the destination tomcat to see if it fired (TBC on tomcat)  
		// TODO: check access log for a 200 response with a timestamp of last 20 secs
		
		String TOMCAT_LOG_PATH = testingProperties.getProperty("tomcat.location.logs");
		String TOMCAT_LOG_PREFIX = testingProperties.getProperty("tomcat.location.logs.prefix");

		Date now = new Date();
		SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
		String formattedDate = format1.format(now);
		String EXT = ".txt";

		String accessLogFilePath = TOMCAT_LOG_PATH + File.separator + TOMCAT_LOG_PREFIX + formattedDate + EXT;
		logger.info("accessLogFilePath = " + accessLogFilePath);
		
		// TODO: assert that log file at 'accessLogFilePath' contains a line similar to the example below:
		//String logEntryLine = "127.0.0.1 - - [30/Apr/2015:22:03:27 -0400] \"POST /flexrep-client/apply.xqy HTTP/1.1\" 200 -";
		//boolean logContainsValidTimestamp = false;
		//Assert.assertTrue(logContainsValidTimestamp);
		
		fail("Not yet implemented");
	}

	private void assertFileHasValidChecksum(String filename) {
		InputStream is1;
		InputStream is2;
		
		try {
			is1 = Utils.getClasspathContentAsStream(filename);
			byte[] bytes1;
			bytes1 = IOUtils.toByteArray(is1);
			
			Checksum checksum = new CRC32();
			checksum.update(bytes1, 0, bytes1.length);
			long resourceChecksumValue = checksum.getValue();
			logger.info("Checksum1 = " + resourceChecksumValue);
			
			String fileSystemRoot = testingProperties.getProperty("tomcat.replication.path");
			String filepath = fileSystemRoot + FLEX_REP_ROOT + filename;
			File file = new File(filepath);
			is2 = new FileInputStream(file);
			byte[] bytes2 = IOUtils.toByteArray(is2);
			
			Checksum checksum2 = new CRC32();
			checksum2.update(bytes2, 0, bytes2.length);
			long fileSystemChecksumValue = checksum2.getValue();
			logger.info("Checksum2 = " + fileSystemChecksumValue);
			
			Assert.assertEquals("Checksums are not equal", resourceChecksumValue, fileSystemChecksumValue);
			
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
	}

	private void assertFileWrittenToFileSystem(String filename) {
		String fileSystemRoot = testingProperties.getProperty("tomcat.replication.path");
		String filepath = fileSystemRoot + filename;
		logger.info("check to see if file written to path: " + filepath);
		File xmlFile = new File(filepath);
		Assert.assertTrue("XML file not found on file system", xmlFile.exists());
	}

	private void assertFileWrittenAndReplicatedPerMaster(String filename) {
		try {
			String ML_URI = FLEX_REP_ROOT + filename;
			Assert.assertEquals("true", xccHelper.executeXquery(String.format(
					"xdmp:exists(fn:doc(\"%s\"))", ML_URI)));
			// TODO: getting last-success needs better fix 
			// Changed to only get first "last-success" element
			// parseDateTime method below breaks if more than 1 "last-success"
			// (which can happen if multiple flexrep targets
			String iso8601DateString = xccHelper.executeXquery(String.format(
					"(xdmp:document-properties(\"%s\")//*:last-success)[1]/text()",
					ML_URI));

			//System.out.println("iso8601DateString+"+iso8601DateString);
			System.out.println(iso8601DateString);
			Date successDate = javax.xml.bind.DatatypeConverter.parseDateTime(
					iso8601DateString).getTime();
			double deltaInSeconds = ((new Date()).getTime() - successDate
					.getTime()) / 1000d;
			logger.info("last success date=" + iso8601DateString);
			logger.info("delta in seconds=" + deltaInSeconds);
			Assert.assertTrue("FlexRep Last Success is NOT recent",
					deltaInSeconds < 10d);

		} catch (XccHelperException e) {
			Assert.fail(e.getMessage());
		}
	}

	private void assertLocalPropertiesFileUpdated(String filePath, String contents) {
		String fileSystemRoot = testingProperties.getProperty("tomcat.replication.path");
		String localFilepath = fileSystemRoot + filePath;
		logger.info("check to see if property file written to path: " + localFilepath);
		File propFile = new File(localFilepath);
		Assert.assertTrue("Property file not found on file system", propFile.exists());
		
		
		logger.info("check to make sure property file <status> contains: " + contents);
		
		String fileContents = null;
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(localFilepath);
			fileContents = IOUtils.toString(inputStream);
		
			XPath xpath = XPathFactory.newInstance().newXPath();
			xpath.setNamespaceContext(CONTEXT);
			String statusInFile = null;
			statusInFile = xpath.compile("/flexrep:update/prop:properties/status/text()")
					.evaluate(new InputSource(new StringReader(fileContents)));
			logger.info(statusInFile);
			
			Assert.assertEquals("Properties file has wrong contents", contents, statusInFile);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
		//Assert.fail("NOT YET IMPLEMENTED");
	}

	private void assertTomcatFlexRepReplicaIsAlive() {
		HttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet(String.format("http://%s:%s/flexrep-client/apply.xqy",
				testingProperties.getProperty("tomcat.http.host"),
				testingProperties.getProperty("tomcat.http.port")));
		try {
			HttpResponse response = client.execute(get);
			Assert.assertEquals(
					"Tomcat is not alive and responding to /apply.xqy", 200,
					response.getStatusLine().getStatusCode());
		} catch (IOException e) {
			Assert.fail("Tomcat is not alive and responding to /apply.xqy");
		}
	}
	
	private void assertMarkLogicIsAlive() {
		try {
			String response = xccHelper.executeXquery("'I''m alive'");
			Assert.assertEquals("MarkLogic server is unreachable", "I'm alive",
					response);
		} catch (XccHelperException e) {
			Assert.fail("MarkLogic server is unreachable");
		}
	}

	private void insertNonChunkingBinary(String filename)
			throws FileSystemApplyInsertException {
		try {
			ContentCreateOptions options = new ContentCreateOptions();
			options.setFormatBinary();
			Content content = ContentFactory.newContent(
					FLEX_REP_ROOT+filename,
					Utils.getClasspathContentAsStream(filename),
					options);
			xccHelper.insertBinary(content);
		} catch (XccHelperException | IOException e) {
			throw new FileSystemApplyInsertException(e);
		}
	}
	
	
	
	private void insertXmlFile(String filename)
			throws FileSystemApplyInsertException {
		try {
			ContentCreateOptions options = new ContentCreateOptions();
			options.setFormatXml();
			Content content = ContentFactory.newContent(
					FLEX_REP_ROOT + filename,
					Utils.getClasspathContentAsStream(filename),
					options);
			xccHelper.insertXml(content);
		} catch (XccHelperException | IOException e) {
			throw new FileSystemApplyInsertException(e);
		}
	}
	
	@After
	public void clearMarkLogicTmpAndFilesystemDirectory() {

	}


}