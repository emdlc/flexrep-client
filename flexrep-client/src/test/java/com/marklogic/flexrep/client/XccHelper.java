package com.marklogic.flexrep.client;

import java.net.URI;
import java.net.URISyntaxException;

import com.marklogic.xcc.AdhocQuery;
import com.marklogic.xcc.Content;
import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.ContentSourceFactory;
import com.marklogic.xcc.ResultSequence;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.exceptions.RequestException;
import com.marklogic.xcc.exceptions.XccConfigException;

public class XccHelper {

	private ContentSource contentSource = null;
	private String xccUrl;

	public XccHelper(String xccUrl) throws XccHelperException {
		this.xccUrl = xccUrl;
		try {
			contentSource = ContentSourceFactory.newContentSource(new URI(
					xccUrl));
		} catch (XccConfigException | URISyntaxException e) {
			throw new XccHelperException(e);
		}
	}
	
	public void insertBinary(Content content) throws XccHelperException {			
		contentSource.setAuthenticationPreemptive(true);
		Session session = contentSource.newSession();
		try {
			session.insertContent(content);
		} catch (RequestException e) {
			throw new XccHelperException(e);
		}
	}
	
	public String executeXquery(String xquery) throws XccHelperException {
		Session session = contentSource.newSession();
		AdhocQuery query = session.newAdhocQuery(xquery);
		ResultSequence sequence;
		try {
			sequence = session.submitRequest(query);
			return sequence.asString();			
		} catch (RequestException e) {
			throw new XccHelperException(e);
		}
	}

}
