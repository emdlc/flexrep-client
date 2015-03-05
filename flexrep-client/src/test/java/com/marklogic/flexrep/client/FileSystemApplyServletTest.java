package com.marklogic.flexrep.client;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.marklogic.xcc.AdhocQuery;
import com.marklogic.xcc.Content;
import com.marklogic.xcc.ContentCreateOptions;
import com.marklogic.xcc.ContentFactory;
import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.ContentSourceFactory;
import com.marklogic.xcc.ResultSequence;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.exceptions.RequestException;
import com.marklogic.xcc.exceptions.XccConfigException;

public class FileSystemApplyServletTest {

	private static final Logger logger = Logger
			.getLogger(FileSystemApplyServletTest.class.getName());

	private static final String NON_CHUNKING_FILENAME = "/canyon-single.jpg";

	@Test
	public void testNonChunkingReplication() {

		try {

			Properties props = Utils
					.getPropertiesFromClasspath("/flexrep-client.properties");

			// Check that tomcat flexrep client is alive
			assertTomcatFlexRepReplicaIsAlive(props);
			
			String xccUrl = props.getProperty("ml.flexrep.xcc.url");
			logger.info("url=" + xccUrl);

			Session session;
			ContentCreateOptions options = new ContentCreateOptions();
			ContentSource cs = ContentSourceFactory.newContentSource(new URI(
					xccUrl));
			cs.setAuthenticationPreemptive(true);
			assertMarkLogicIsAlive(cs);

			options.setFormatBinary();

			Content content = ContentFactory.newContent("/tmp"
					+ NON_CHUNKING_FILENAME,
					Utils.getClasspathContentAsStream(NON_CHUNKING_FILENAME),
					options);

			session = cs.newSession();
			session.insertContent(content);

			// TODO: Check to confirm the file was written
			// TODO: Check the destination tomcat to see if it fired (TBC on
			// tomcat)
			// TODO: Verify the file was written to filesystem
			// TODO: Checksum on file to make sure it went over successfully
		} catch (RequestException | SecurityException | IOException
				| XccConfigException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void assertTomcatFlexRepReplicaIsAlive(Properties props) {
		HttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet(String.format("http://%s:%s/apply.xqy",
				props.getProperty("tomcat.http.host"),
				props.getProperty("tomcat.http.port")));
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

	private void assertMarkLogicIsAlive(ContentSource cs) {
		Session session = cs.newSession();
		AdhocQuery query = session.newAdhocQuery("'I''m alive'");
		ResultSequence sequence;
		try {
			sequence = session.submitRequest(query);
			Assert.assertEquals("MarkLogic server is unreachable", "I'm alive",
					sequence.asString());
		} catch (RequestException e) {
			Assert.fail("MarkLogic server is unreachable");
		}

	}

}
