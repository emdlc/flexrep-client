package com.marklogic.flexrep.client;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ch.qos.logback.classic.Level;

import com.marklogic.xcc.Content;
import com.marklogic.xcc.ContentCreateOptions;
import com.marklogic.xcc.ContentFactory;

public class FileSystemApplyServletTest {

	private static final Logger logger = Logger
			.getLogger(FileSystemApplyServletTest.class.getName());

	private static final String NON_CHUNKING_FILENAME = "/canyon-single.jpg";

	private static final String NON_CHUNKING_FILENAME_URI = "/tmp"
			+ NON_CHUNKING_FILENAME;
	
	private static final String XML_FILENAME = "/books.xml";

	private static final String XML_FILENAME_URI = "/tmp"
			+ XML_FILENAME;

	private static Properties testingProperties = null;

	private static XccHelper xccHelper = null;
	
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
	public void testNonChunkingReplication() {

		try {

			assertTomcatFlexRepReplicaIsAlive();
			assertMarkLogicIsAlive();

			insertNonChunkingBinary();

			Thread.sleep(2000);
			assertBinaryWrittenAndReplicatedPerMaster();
			// TODO: Check the destination tomcat to see if it fired (TBC on
			// tomcat)  // check access log for a 200 response with a timestamp of last 20 secs
			// TODO: Verify the file was written to filesystem
			// TODO: Checksum on file to make sure it went over successfully (hash function)
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

			insertXmlFile();

			Thread.sleep(2000);
			assertXmlWrittenAndReplicatedPerMaster();

			assertXmlWrittenToFileSystem();
			
			//TODO: make this test pass
			assertXmlFileHasValidChecksum();
			
			//TODO: not yet implemented
			assertTargetTomcatLoggedPost();
			
		} catch (FileSystemApplyInsertException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void assertTargetTomcatLoggedPost() {
		// TODO: Check the destination tomcat to see if it fired (TBC on
		// tomcat)  // check access log for a 200 response with a timestamp of last 20 secs
		
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

	private void assertXmlFileHasValidChecksum() {
		InputStream is1;
		InputStream is2;
		
		try {
			is1 = Utils.getClasspathContentAsStream(XML_FILENAME);
			byte[] bytes1;
			bytes1 = IOUtils.toByteArray(is1);
			
			Checksum checksum = new CRC32();
			checksum.update(bytes1, 0, bytes1.length);
			long resourceChecksumValue = checksum.getValue();
			logger.info("Checksum1 = " + resourceChecksumValue);
			
			String fileSystemRoot = testingProperties.getProperty("tomcat.replication.path");
			String filepath = fileSystemRoot + XML_FILENAME_URI;
			File file = new File(filepath);
			is2 = new FileInputStream(file);
			byte[] bytes2 = IOUtils.toByteArray(is2);
			
			checksum = new CRC32();
			checksum.update(bytes2, 0, bytes2.length);
			long fileSystemChecksumValue = checksum.getValue();
			logger.info("Checksum2 = " + fileSystemChecksumValue);
			
			Assert.assertEquals("Checksums are not equal", resourceChecksumValue, fileSystemChecksumValue);
			
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
	}

	private void assertXmlWrittenToFileSystem() {
		// TODO Auto-generated method stub
		String fileSystemRoot = testingProperties.getProperty("tomcat.replication.path");
		String filepath = fileSystemRoot + XML_FILENAME_URI;
		logger.info("check to see if file written to path: " + filepath);
		File xmlFile = new File(filepath);
		Assert.assertTrue("XML file not found on file system", xmlFile.exists());
	}

	private void assertXmlWrittenAndReplicatedPerMaster() {
		try {
			Assert.assertEquals("true", xccHelper.executeXquery(String.format(
					"xdmp:exists(fn:doc(\"%s\"))", XML_FILENAME_URI)));
			String iso8601DateString = xccHelper.executeXquery(String.format(
					"xdmp:document-properties(\"%s\")//*:last-success/text()",
					XML_FILENAME_URI));

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

	private void assertBinaryWrittenAndReplicatedPerMaster() {
		try {
			Assert.assertEquals("true", xccHelper.executeXquery(String.format(
					"xdmp:exists(fn:doc(\"%s\"))", NON_CHUNKING_FILENAME_URI)));
			String iso8601DateString = xccHelper.executeXquery(String.format(
					"xdmp:document-properties(\"%s\")//*:last-success/text()",
					NON_CHUNKING_FILENAME_URI));

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

	@After
	public void clearMarkLogicTmpAndFilesystemDirectory() {

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

	@Ignore
	@Test
	public void testChunkingReplication() {
		fail("Not yet implemented");
	}

	private void insertNonChunkingBinary()
			throws FileSystemApplyInsertException {
		try {
			ContentCreateOptions options = new ContentCreateOptions();
			options.setFormatBinary();
			Content content = ContentFactory.newContent(
					NON_CHUNKING_FILENAME_URI,
					Utils.getClasspathContentAsStream(NON_CHUNKING_FILENAME),
					options);
			xccHelper.insertBinary(content);
		} catch (XccHelperException | IOException e) {
			throw new FileSystemApplyInsertException(e);
		}
	}
	
	private void insertXmlFile()
			throws FileSystemApplyInsertException {
		try {
			ContentCreateOptions options = new ContentCreateOptions();
			options.setFormatXml();
			Content content = ContentFactory.newContent(
					XML_FILENAME_URI,
					Utils.getClasspathContentAsStream(XML_FILENAME),
					options);
			xccHelper.insertXml(content);
		} catch (XccHelperException | IOException e) {
			throw new FileSystemApplyInsertException(e);
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

}
