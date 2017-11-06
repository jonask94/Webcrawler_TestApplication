import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import com.google.common.collect.ImmutableList;

import Exceptions.RobotsTXTNotFoundException;

public class RobotsInterpreter {

	private static final ImmutableList<Integer> HTTP_REDIRECTION_CODES = ImmutableList.of(301, 302, 303, 307, 308);
	private static final String GZIP_CONTENT_TYPE = "gzip";
	private static final String ALLOW_KEY = "allow";
	private static final String DISALLOW_KEY = "disallow";
	private HashMap<String, ArrayList<String>> disallowListCache = new HashMap<String, ArrayList<String>>();
	private HashMap<String, ArrayList<String>> allowListCache = new HashMap<String, ArrayList<String>>();

	/**
	 * This method looks up whether the rules of the needed robots.txt are
	 * already cached or not. If the rules from robots.txt are not cached in the
	 * list, the method downloads and caches them.
	 * 
	 * @param url
	 *            - url that has to be checked
	 * @return whether the given url is allowed by robots.txt or not
	 */
	public boolean checkUrlByRobotsTxt(String url) {

		URL checkUrl;
		// by default everything is allowed
		boolean allowedByRobotsTxtResult = true;

		try {
			// get host name
			checkUrl = new URL(url);
			String host = checkUrl.getHost();

			// retrieve host's disallow list from cache
			ArrayList<String> disallowList = (ArrayList<String>) disallowListCache.get(host);

			// retrieve host's allow list from cache
			ArrayList<String> allowList = (ArrayList<String>) allowListCache.get(host);

			// download and cache lists if they were not already cached
			if (disallowList == null || allowList == null) {
				try {
					// get URL to robots.txt
					URL robotsFileUrl = getRobotsFileUrl(host);
					System.out.println("**ROBOTS** Creating new rules from: " + robotsFileUrl);

					// read robots.txt and add new disallowList and allowList to
					// cache
					HashMap<String, ArrayList<String>> readResult = readRobotsTxt(robotsFileUrl);
					disallowList = readResult.get(DISALLOW_KEY);
					allowList = readResult.get(ALLOW_KEY);
					disallowListCache.put(host, disallowList);
					allowListCache.put(host, allowList);

					/*
					// testing
					System.out.println("***Disallow:***");
					for (String rule : disallowList) {
						System.out.println(rule);
					}
					System.out.println("***Allow:***");
					for (String rule : allowList) {
						System.out.println(rule);
					}
					*/

				} catch (RobotsTXTNotFoundException e) {
					e.printStackTrace();
					// if the robots.txt could not be reached or read the
					// checkUrl is allowed
					return allowedByRobotsTxtResult;
				}
			}

			HashMap<String, ArrayList<String>> setOfRules = new HashMap<String, ArrayList<String>>();
			setOfRules.put(DISALLOW_KEY, disallowList);
			setOfRules.put(ALLOW_KEY, allowList);

			// check the robots-rules
			allowedByRobotsTxtResult = urlIsAllowed(checkUrl, setOfRules);

		} catch (MalformedURLException e) {
			e.printStackTrace();
			System.out.println("**ERROR** Unexpected Error while creating url from String: " + url);
			// if the url could not be created the checkUrl is allowed
			return allowedByRobotsTxtResult;
		}

		return allowedByRobotsTxtResult;
	}

	/**
	 * This method checks the given url against a list of rules (these are
	 * excluded from the robots.txt)
	 * 
	 * @param checkUrl
	 *            - url that has to be checked
	 * @param setOfRules
	 *            - including the rules that disallow an url and that allow an
	 *            url
	 * @return whether the url is allowed or disallowed
	 */
	private boolean urlIsAllowed(URL checkUrl, HashMap<String, ArrayList<String>> setOfRules) {

		// by default every url is allowed
		boolean urlIsAllowed = true;

		// get single list of rules
		ArrayList<String> disallowList = setOfRules.get(DISALLOW_KEY);
		ArrayList<String> allowList = setOfRules.get(ALLOW_KEY);

		// get the file-path (without host part)
		String file = checkUrl.getFile();

		// check all rules, that are cached in the dissalowList
		for (int i = 0; i < disallowList.size(); i++) {

			String disallowRule = (String) disallowList.get(i);

			// check if file matches rule
			if (FileMatchesRule(file, disallowRule)) {
				urlIsAllowed = false;
				break;
			}
		}

		// check all rules, that are cached in the allowList (only needed if an
		// url was disallowed before)
		if (urlIsAllowed == false) {
			for (int i = 0; i < allowList.size(); i++) {

				String allowRule = (String) allowList.get(i);

				// check if file matches rule
				if (FileMatchesRule(file, allowRule)) {
					urlIsAllowed = true;
					break;
				}
			}
		}

		if (urlIsAllowed == false) {
			System.out.println("**ROBOTS** Die Website " + checkUrl + " darf nicht durchsucht werden.");
		}

		return urlIsAllowed;
	}

	/**
	 * This method compares a given file to a given rule and checks if they fit
	 * 
	 * @param file
	 * @param rule
	 * @return whether file and rules matches or not
	 */
	private boolean FileMatchesRule(String file, String rule) {

		// rule without wildcards
		// file matches if it starts with the given rule
		if (!rule.contains("*") && !rule.endsWith("$")) {
			if (file.startsWith(rule)) {
				return true;
			}

			// file without "*" but containing "$"
			// file matches if it starts and ends with the given rule
		} else if (!rule.contains("*")) {
			// cut the wildcard at the end
			String wholePath = rule.substring(0, rule.length() - 1);
			if (file.startsWith(wholePath) && file.endsWith(wholePath)) {
				return true;
			}

			// file containing "*"
		} else {
			// modify rule depending on the existing wildcards
			String path = rule;
			if (rule.endsWith("$")) {
				path = rule.substring(0, rule.length() - 1);
			} else {
				path = rule.concat("*");
			}

			// create pattern for regular expression
			String regPattern = path.replace(".", "[.]");
			regPattern = regPattern.replace("?", "[?]");
			regPattern = regPattern.replace("+", "[+]");
			regPattern = regPattern.replace("*", ".*");

			// check if file fits to the pattern
			if (Pattern.matches(regPattern, file)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * this method reads the disallow rules from the robots.txt for the
	 * user-agent: *
	 * 
	 * @param robotsFileUrl
	 *            - url to robots.txt
	 * @return two lists in a map. One list contains all disallow rules that
	 *         were read from the robots.txt, the other list contains all allow
	 *         rules
	 */
	private HashMap<String, ArrayList<String>> readRobotsTxt(URL robotsFileUrl) {

		ArrayList<String> disallowList = new ArrayList<String>();
		ArrayList<String> allowList = new ArrayList<String>();
		HashMap<String, ArrayList<String>> readResult = new HashMap<String, ArrayList<String>>();
		boolean foundSetOfRules = false;
		boolean NextUserAgentIsFollowingLine = false;
		BufferedReader reader = null;

		try {
			// Open connection to robots file for reading
			URLConnection conn = robotsFileUrl.openConnection();

			// Some servers might offer an gzip-InputStream --> this can not be
			// read by normal InputStream
			if (GZIP_CONTENT_TYPE.equals(conn.getContentEncoding())) {
				GZIPInputStream inputStream = new GZIPInputStream(conn.getInputStream());
				reader = new BufferedReader(new InputStreamReader(inputStream));
			} else {
				reader = new BufferedReader(new InputStreamReader(robotsFileUrl.openStream()));
			}

			// Read robots.txt
			String line = null;
			while ((line = reader.readLine()) != null) {

				// some hosts use "User-agent" other hosts use "User-Agent" -->
				// only use lowercases
				String lineLowerCases = line.toLowerCase();

				// find the right passage starting with "user-agent: *"
				if (foundSetOfRules == false && lineLowerCases.indexOf("user-agent: *") != 0) {
					continue;
				} else if (foundSetOfRules == false) {
					foundSetOfRules = true;
					NextUserAgentIsFollowingLine = true;
				}

				// it is possible to use the the command "user-agent" in
				// following line
				// in this case the command doesn't define the end of the wanted
				// passage
				if (NextUserAgentIsFollowingLine
						&& (lineLowerCases.indexOf("user-agent:") == 0 || line.indexOf("#") == 0)) {
					continue;
				} else {
					NextUserAgentIsFollowingLine = false;
				}

				// a new passage (defining rules for another user-agent) is
				// always introduced by the command "user-agent"
				// this command defines the end of the wanted passage
				if (foundSetOfRules == true && lineLowerCases.indexOf("user-agent:") == 0
						&& NextUserAgentIsFollowingLine == false) {
					foundSetOfRules = false;
					continue;
				}

				// get the lines that begin with "Disallow:"
				if (lineLowerCases.indexOf("disallow:") == 0) {
					String disallowPath = line.substring("disallow:".length());

					// check disallow path on comments and remove if
					// present
					int commentIndex = disallowPath.indexOf("#");
					if (commentIndex != -1) {
						disallowPath = disallowPath.substring(0, commentIndex);
					}

					// remove leading or trailing spaces from disallow
					// path
					disallowPath = disallowPath.trim();

					// Add disallowPath to list (empty disallowPath =
					// allow everything --> does not have to be cached)
					if (!disallowPath.equals("")) {
						disallowList.add(disallowPath);
					}
				}

				// get the line that begin with "Allow:"
				if (lineLowerCases.indexOf("allow:") == 0) {
					String allowPath = line.substring("allow:".length());

					// check allowPath on comments and remove if present
					int commentIndex = allowPath.indexOf("#");
					if (commentIndex != -1) {
						allowPath = allowPath.substring(0, commentIndex);
					}

					// remove leading or trailing spaces from allowPath
					allowPath = allowPath.trim();

					// Add allowPath to list ( "/" or "" = allow everything -->
					// does not have to be cached)
					if (!allowPath.equals("") && !allowPath.equals("/")) {
						allowList.add(allowPath);
					}
				}
			}

			if (disallowList.isEmpty() && allowList.isEmpty()) {
				System.out.println("**ROBOTS** Es konnten keine Regeln aus der robots.txt gelesen werden.");
				System.out.println("**ROBOTS** Bitte überprüfen Sie: " + robotsFileUrl);
			}

			// fill readResult
			readResult.put(DISALLOW_KEY, disallowList);
			readResult.put(ALLOW_KEY, allowList);

		} catch (IOException e) {
			// catch Exception if creating the BufferedReader failed
			System.out.println("**ERROR** Unexpected Error while reading: " + robotsFileUrl);
			e.printStackTrace();
		}

		return readResult;
	}

	/**
	 * this method creates the Url to the required robot.txt file
	 * 
	 * @param host
	 *            - host of required robots.txt
	 * @return the url to the required robots.txt
	 * @throws RobotsTXTNotFoundException
	 *             if robots.txt could not be found or read
	 */
	private URL getRobotsFileUrl(String host) throws RobotsTXTNotFoundException {

		try {
			// create url to robots.txt from hostname
			URL robotsFileUrl = new URL("http://" + host + "/robots.txt");

			Response response;

			try {
				// check if the robots.txt is reachable (status code 200 of
				// connection)
				// if the file has moved (status code 3**) find the new Url
				do {
					response = Jsoup.connect(robotsFileUrl.toString()).followRedirects(false).execute();

					if (RobotsInterpreter.HTTP_REDIRECTION_CODES.contains(response.statusCode())
							&& response.hasHeader("location")) {
						// robots.txt has moved --> read new Url from location
						// field
						robotsFileUrl = new URL(response.header("location"));
					} else if (response.statusCode() != 200) {
						throw new RobotsTXTNotFoundException("**ROBOTS** Die Robots.txt für " + host
								+ " konnte nicht gefunden werden. Ursache: Robots.txt liegt nicht in der ermittelten URL!");
					}
				} while (response.statusCode() != 200);

			} catch (IOException e) {
				// catch exception if command "Jsoup.connect" failed
				e.printStackTrace();
				throw new RobotsTXTNotFoundException("**ROBOTS** Die Robots.txt für " + host
						+ " konnte nicht gefunden werden. Ursache: Fehler beim Aufbau einer Verbindung mit ermittelter URL!");
			}

			return robotsFileUrl;

		} catch (MalformedURLException e) {
			// catch exception if command "new URL (String)" failed
			e.printStackTrace();
			throw new RobotsTXTNotFoundException("**ROBOTS** Die Robots.txt für " + host
					+ " konnte nicht gefunden werden. Ursache: Fehler beim Erstellen der URL!");
		}

	}
}
