import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

//TODO store results in file
//TODO create github repo
//TODO translate all outputs in english

public class Spider {
	private Set<String> pagesVisited = new HashSet<String>();
	private List<String> pagesToVisit = new LinkedList<String>();
	private boolean wordFound = false;
	private int counter = 1;
	private RobotsInterpreter robotsInterpreter = new RobotsInterpreter();

	/**
	 * Returns the next Url to visit (in the order that they were found -->
	 * breadth first search). We also do a check to make sure this method
	 * doesn't return a Url that has already been visited, that doesn't fit to
	 * the localization variable or that is forbidden by the corresponding
	 * robots.txt
	 * 
	 * @return nextURL to visit
	 */
	private String nextUrl(String localization) {
		String nextUrl;
		do {
			if (this.pagesToVisit.isEmpty()) {
				nextUrl = "";
				break;
			}
			nextUrl = this.pagesToVisit.remove(0);
		} while (this.pagesVisited.contains(nextUrl) || !nextUrl.contains(localization)
				|| robotsInterpreter.checkUrlByRobotsTxt(nextUrl) == false);
		return nextUrl;
	}

	/**
	 * The main launching point for the Spider's functionality Internally it
	 * creates spider legs that make an HTTP request and parse the response (the
	 * web page).
	 * 
	 * @param startUrl
	 *            - the starting point of the spider
	 * @param searchWord
	 *            - the word or string the spider is searching for
	 * @param maxPagesToSearch
	 *            - the maximum of pages that will be searched
	 * @param localization
	 *            - String that defines the searching domain
	 */
	public void search(String startUrl, String searchWord, int maxPagesToSearch, String localization) {

		while (this.pagesVisited.size() < maxPagesToSearch) {

			String currentUrl;
			SpiderLeg leg = new SpiderLeg();

			// set currentUrl to startUrl in first iteration
			if (counter == 1) {

				// check if startUrl is allowed by robots.txt
				if (robotsInterpreter.checkUrlByRobotsTxt(startUrl) == false) {
					break;
				}

				currentUrl = startUrl;
			} else {
				// set the currentUrl to the next Url from the list:
				// pagesToVisit
				currentUrl = this.nextUrl(localization);
			}
			if (currentUrl == "") {
				System.out.println("Es wurden alle möglichen Webseiten durchsucht.");
				break;
			}
			// crawl the page
			System.out.println("SEARCHSTEP: " + counter);
			boolean crawlSuccess = leg.crawl(currentUrl);
			if (crawlSuccess) {
				// search for word at the current page
				boolean searchSuccess = leg.searchForWord(searchWord);
				// check on success and break if word was found
				if (searchSuccess) {
					System.out.println("**Success** Word >>" + searchWord + "<< found at " + currentUrl);
					wordFound = true;
					this.pagesVisited.add(currentUrl);
					break;
				}
				// store the new links at the corresponding list
				this.pagesToVisit.addAll(leg.getLinks());
			}
			this.pagesVisited.add(currentUrl);
			this.counter++;
			System.out.println("###########################################################");
		}
		// print result
		System.out.println(String.format("**Done** Visited " + this.pagesVisited.size() + " page(s)"));
		if (!wordFound) {
			System.out.println("**Failure** could not find the word >>" + searchWord + "<< during this progress");
		}
	}
}
