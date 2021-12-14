package org.dspace.ldn;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

public class LDNServiceCheckAuthorization {
	/** Logger */
	private static Logger logger = Logger.getLogger(LDNServiceCheckAuthorization.class);

	private static List<String> authorizedIpList;

	private static boolean isLocalhostTrustedByDefault;

	static {
		isLocalhostTrustedByDefault = ConfigurationManager.getBooleanProperty("ldn-trusted-services",
				"ldn-trusted.localhost.default");
		String authorisedIpString = ConfigurationManager.getProperty("ldn-trusted-services", "ldn-trusted.from.ip");
		String authorisedHostnameString = ConfigurationManager.getProperty("ldn-trusted-services",
				"ldn-trusted.from.hostname");

		List<String> tmpList = new LinkedList<>();

		// Authorized IP Addresses
		if (StringUtils.isNotEmpty(authorisedIpString)) {
			if (authorisedIpString.contains(",")) {
				String[] ipArray = authorisedIpString.split(",");
				for (String tmpIp : ipArray) {
					tmpList.add(tmpIp);
				}
			} else {
				tmpList.add(authorisedIpString);
			}
		}

		// Authorized Hostnames
		if (StringUtils.isNotEmpty(authorisedHostnameString)) {
			if (authorisedHostnameString.contains(",")) {
				String[] hostnameArray = authorisedHostnameString.split(",");
				for (String hostname : hostnameArray) {
					tmpList.add(parseHostnameToString(hostname));
				}
			} else {
				tmpList.add(parseHostnameToString(authorisedHostnameString));
			}
		}

		authorizedIpList = tmpList;
	}

	public static boolean isHostAuthorized(HttpServletRequest request) {
		String ipFromRequest = request.getRemoteAddr();
		if (isLocalhostTrustedByDefault) {
			try {
				InetAddress ip = InetAddress.getByName(ipFromRequest);
				return authorizedIpList.contains(ipFromRequest) || ip.isLoopbackAddress();
			} catch (UnknownHostException e) {
				logger.error(e);
			}
		}
		return authorizedIpList.contains(ipFromRequest);
	}

	public static String parseHostnameToString(String hostname) {
		String tmpIpAddress = null;
		try {
			tmpIpAddress = InetAddress.getByName(hostname).getHostAddress();
		} catch (UnknownHostException e) {
			logger.error("Hostname " + hostname + " is unknown ", e);
		}
		return tmpIpAddress;
	}

}
