package com.marklogic.flexrep.client;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

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

public class ApplyServletTest {

	private static final String NON_CHUNKING_FILENAME = "canyon-single.jpg";

	@Test
	public void testNonChunkingReplication() {

		// TODO: Check that tomcat flexrep client is alive

		try {
			Session session;
			ContentCreateOptions options = new ContentCreateOptions();
			
			Properties props = Utils
					.getPropertiesFromClasspath("/ml.properties");
			String xccUrl = props.getProperty("ml.flexrep.xcc.url");
			System.out.println("url=" + xccUrl);
			
			ContentSource cs = ContentSourceFactory.newContentSource(new URI(
					xccUrl));
			cs.setAuthenticationPreemptive(true);
			assertMarkLogicIsAlive(cs);
			
			options.setFormatBinary();

			Content content = ContentFactory.newContent("/tmp/"
					+ NON_CHUNKING_FILENAME, ApplyServletTest.class
					.getClassLoader()
					.getResourceAsStream(NON_CHUNKING_FILENAME), options);

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
			Assert.assertEquals("I'm alive", sequence.asString());
		} catch (RequestException e) {
			Assert.fail("MarkLogic server is unreachable");
		}

	}

}
