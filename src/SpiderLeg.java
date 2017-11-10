import java.io.IOException;
import java.util.*;

import org.jsoup.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class SpiderLeg {
	
	//use a fake USER_AGENT so the web server thinks the robot is a normal web browser
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";
	
	private List<String> links = new LinkedList<String>();
	private Document htmlDocument;
	
	/**
	 * This method makes an HTTP request, checks the response and then gathers up all the links on the page.
	 * @param url - the url to visit
	 * @return whether or not the crawler was successful
	 */
	public boolean crawl(String url)
	{
		try
		{
			//execute http request and get html document
			Connection connection = Jsoup.connect(url).userAgent(USER_AGENT);
			Document htmlDocument = connection.get();
			this.htmlDocument = htmlDocument;
			
			//check if status code of http request is ok
			if(connection.response().statusCode() == 200) // 200 is the HTTP OK status code, indicating that everything is great
			{
				System.out.println("**Visiting** Received web page at " + url);	
			}
			else
			{
				System.out.println("**ERROR** unsuccessful http-request. statusCode != 200");
				return false;
			}
			
			//check if response is html or something else
			if(!connection.response().contentType().contains("text/html"))
			{
				System.out.println("**ERROR** Retrieved something other than HTML");
				return false;
			}
			
			//get all links from the page
			//select("a[href]") returns all complete a-Tags with href-attributes
			Elements linksOnPage = htmlDocument.select("a[href]");
			System.out.println("Found (" + linksOnPage.size() + ") links");
			//extract the absolute Urls from the collected a-Tags
			for(Element link : linksOnPage)
			{
				this.links.add(link.absUrl("href"));
			}
			
			//TODO: kommentieren
			//TODO: auf moegliche null Werte pruefen
			//TODO: auslagern auf kickstarterInterpreter
			//TODO: vervollstaendigen und fuer aktuell laufende Projekte anpassen
			//crawling fields from kickstarter project page (page of an project that is already finished)
			if (url.contains("www.kickstarter.com/projects/")) {
				String title = htmlDocument.select("h2.project-profile__title").first().getElementsByTag("a").first().ownText();
				String shortDescription = htmlDocument.select("div.NS_project_profiles__blurb").first().select("span.content").first().ownText();
				String creatorName = htmlDocument.select("div.creator-name").first().getElementsByTag("a").first().ownText();
				int positionOfCreatorId = url.indexOf("/projects/")+"/projects/".length();
				String creatorId = url.substring(positionOfCreatorId);
				creatorId = creatorId.split("/", 2)[0];
				String NumberOfBackers = htmlDocument.select("div.NS_campaigns__spotlight_stats").first().getElementsByTag("b").first().ownText().split("\\s", 2)[0];
				if (NumberOfBackers.contains(",")) {
					NumberOfBackers = NumberOfBackers.replaceAll("[,]", ".");			
				}
				Element helperElementForFundingGoal = htmlDocument.select("div.NS_projects__category_location").first();
				while(helperElementForFundingGoal!=null){
					helperElementForFundingGoal = helperElementForFundingGoal.parent();
					if(helperElementForFundingGoal.hasClass("row")){
					break;
					}
				}
				helperElementForFundingGoal = helperElementForFundingGoal.getElementsMatchingOwnText("pledged of goal").first();
				String fundingGoal = helperElementForFundingGoal.select("span.money").first().ownText().replaceAll("[^0-9,.]", "");
				if(fundingGoal.contains(",") || fundingGoal.contains(".")){
					fundingGoal = fundingGoal.replaceAll("[.]", "#");
					fundingGoal = fundingGoal.replaceAll("[,]", ".");
					fundingGoal = fundingGoal.replaceAll("[#]", ",");
				}
				String totalFunding = htmlDocument.select("div.NS_campaigns__spotlight_stats").first().select("span.money").first().ownText().replaceAll("[^0-9,.]", "");
				if(totalFunding.contains(",") || totalFunding.contains(".")){
					totalFunding = totalFunding.replaceAll("[.]", "#");
					totalFunding = totalFunding.replaceAll("[,]", ".");
					totalFunding = totalFunding.replaceAll("[#]", ",");
				}
				Element helperElementForLocationAndCategory = htmlDocument.select("div.NS_projects__category_location").first();
				Elements locationAndCategoryElements = helperElementForLocationAndCategory.getElementsByTag("a");
				String location = "not found";
				String category = "not found";
				for (Element element : locationAndCategoryElements) {
					if (!element.getElementsByClass("ksr-icon__location").isEmpty()) {
						location = element.ownText();
					}
					if (!element.getElementsByClass("ksr-icon__tag").isEmpty()) {
						category = element.ownText();
					}
				}
				System.out.println("Titel: " + title);
				System.out.println("Kurzbeschreibung: " + shortDescription);
				System.out.println("Projektgründer: " + creatorName + " (Id: " + creatorId + ")");
				System.out.println("Anzahl Unterstützer: " + NumberOfBackers);
				System.out.println("Finanzierungsziel ($): " + fundingGoal);
				System.out.println("Gesamtsumme ($): " + totalFunding);
				System.out.println("Ort: " + location);
				System.out.println("Kategorie: " + category);
				
			}
			
			return true;
		}
		catch(IOException ioe)
		{
			// We were not successful in our HTTP request
			System.out.println("**ERROR** Unexpected Error during HTTP request " + ioe);
			return false;
		}
	}
	
	/**
	 * this method performs a search on the body of the HTML document that is retrieved.
	 * this method should only be called after a successful crawl
	 * @param searchWord  - the word or string to look for
	 * @return whether or not the word was found
	 */
	public boolean searchForWord(String searchWord)
	{
		//defensive coding: this method should only be used after a successful crawl
		if(this.htmlDocument == null)
		{
			System.out.println("**ERROR** there was no successful crawl to perform the search function");
			return false;
		}
		System.out.println("Searching for the word >>" + searchWord + "<< ...");
		String bodyText = this.htmlDocument.body().text();
		return bodyText.toLowerCase().contains(searchWord.toLowerCase());
	}
	
	// get all the links found on the current page
	public List<String> getLinks()
	{
		return this.links;
	}

}
