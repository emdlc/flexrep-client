package com.marklogic.flexrep.client;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

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

	private static Properties testingProperties = null;

	private static XccHelper xccHelper = null;

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
			// tomcat)
			// TODO: Verify the file was written to filesystem
			// TODO: Checksum on file to make sure it went over successfully
		} catch (FileSystemApplyInsertException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		HttpGet get = new HttpGet(String.format("http://%s:%s/apply.xqy",
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
