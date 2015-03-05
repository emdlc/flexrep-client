package com.marklogic.flexrep.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.InputSource;

/**
 * Servlet implementation class ReplicateServlet
 */
@WebServlet("/apply.xqy")
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 2, // 2MB
maxFileSize = 1024 * 1024 * 10, // 10MB
maxRequestSize = 1024 * 1024 * 50)
// 50MB
public class ApplyServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private String currentDesignation = null;
	private String currentContentType = null;
	private Integer currentContentLength = null;
	private boolean isProcessingContent = false;
	private String currentUpdateFormat = null;
	private String currentUri = null;

	private static NamespaceContext CONTEXT = new NamespaceContextMap("doc",
			"xdmp:document-load", "flexrep",
			"http://marklogic.com/xdmp/flexible-replication");

	/**
	 * Default constructor.
	 */
	public ApplyServlet() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		OutputStream out = response.getOutputStream();
		out.write("test this222".getBytes("UTF-8"));
		out.flush();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		FileOutputStream out = null;
		String boundary = null;
		String contentType = request.getHeader("content-type");
		for (String f : contentType.split(" *; *")) {
			String[] toks = f.split(" *= *");
			if ("boundary".equals(toks[0]))
				boundary = toks[1];
		}
		// boundary = "--"+boundary+"[\r\n-]*";
		boundary = "--" + boundary + "\r\n";
		String domainId = request.getHeader("X-Flexrep-Domain-ID");
		String domainName = request.getHeader("X-Flexrep-Domain-Name");
		String targetId = request.getHeader("X-Flexrep-Target-ID");
		String targetName = request.getHeader("X-Flexrep-Target-Name");

		try {
			// BufferedReader bodyReader = request.getReader();
			// StringWriter writer = new StringWriter();
			// IOUtils.copy(bodyReader, writer);
			// System.out.println("body="+writer.toString());
			// byte[] body = request.

			System.out.println("boundary=" + boundary);
			System.out.println("domainId=" + domainId);
			System.out.println("domainName=" + domainName);
			System.out.println("targetId=" + targetId);
			System.out.println("targetName=" + targetName);
			System.out.println("contentType=" + request.getContentType());

			InputStream in = request.getInputStream();

			StringBuilder line = new StringBuilder();
			int ch = 0;
			boolean reading = false;
			while ((ch = in.read()) != -1) {
				line.append((char) ch);
				// End of line
				if (line.toString().endsWith("\r\n")) {
					if (line.toString().endsWith(boundary))
						reading = !reading;
					if (reading) {
						processLine(line.toString());
						if (isProcessingContent) {
							if (currentUri == null)
								out = new FileOutputStream("c:\\Temp\\out.xlsx");

							else {
								String fullPath = "c:\\Temp\\"
										+ formatUri(currentUri);
								File file = new File(fullPath.substring(0,
										fullPath.lastIndexOf("\\")));
								file.mkdirs();
								out = new FileOutputStream(fullPath);
							}
							readContent(in, out);
							reading = false;
						}
					}
					line = new StringBuilder();
				}
				if (reading) {

				}
			}

		} catch (Exception e) {
			e.printStackTrace(System.out);
		} finally {
			out.flush();
			out.close();
		}
	}

	private static String formatUri(String currentUri) {
		return currentUri.replaceAll("/", "\\\\");
	}

	private void processXml(String xml) {
		try {
			XPath xpath = XPathFactory.newInstance().newXPath();
			xpath.setNamespaceContext(CONTEXT);
			currentUpdateFormat = xpath.compile("/flexrep:update/doc:format")
					.evaluate(new InputSource(new StringReader(xml)));
			currentUri = xpath.compile("/flexrep:update/doc:uri").evaluate(
					new InputSource(new StringReader(xml)));
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}

	}

	private void processLine(String line) {
		System.out.println("line=" + line);
		if (line.startsWith("X-FlexRep-Designation: ")) {
			currentContentType = "";
			currentDesignation = line.replaceFirst("X-FlexRep-Designation: ",
					"").trim();
		}
		if (line.startsWith("Content-Length: ")) {
			currentContentLength = Integer.valueOf(line.replaceFirst(
					"Content-Length: ", "").trim());
			isProcessingContent = true;
		}
		if (line.startsWith("Content-Type: "))
			currentContentType = line.replaceFirst("Content-Type: ", "").trim();
	}

	private void readContent(InputStream in, OutputStream out) {
		byte[] bytes = new byte[1024];
		Integer sizeRead = 0;
		Integer sizeRemaining = currentContentLength - sizeRead;
		// Remove extra whitespace and line breaks
		try {
			for (int j = 0; j < 2; j++)
				in.read();

			while (sizeRemaining != 0) {
				int howMuch = sizeRemaining > 1024 ? 1024 : sizeRemaining;
				int byteCt = in.read(bytes, 0, howMuch);
				if ("document".equals(currentDesignation))
					out.write(bytes, 0, byteCt);
				sizeRead += byteCt;
				sizeRemaining = currentContentLength - sizeRead;
			}
			System.out.println("sizeRead=" + sizeRead);
			if ("update".equals(currentDesignation)) {
				String update = new String(bytes, "UTF-8");
				processXml(update.trim());
			}
			if (!"document".equals(currentDesignation)) {
				String update = new String(bytes, "UTF-8");
				System.out.println("bytes=" + update);
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
		isProcessingContent = false;
	}

}
