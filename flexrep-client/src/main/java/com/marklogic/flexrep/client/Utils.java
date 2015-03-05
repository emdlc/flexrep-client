package com.marklogic.flexrep.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Utils {

	public static InputStream getClasspathContentAsStream(String path) {
		return Utils.class.getResourceAsStream(path);
	}
	
	public static Properties getPropertiesFromClasspath(String path)
			throws IOException {
		Properties properties = new Properties();
		properties.load(getClasspathContentAsStream(path));
		return properties;
	}
	
}
