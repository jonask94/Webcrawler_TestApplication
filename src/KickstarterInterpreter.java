import java.util.ArrayList;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import Helper.KickstarterIdentifier;

public class KickstarterInterpreter {

	//TODO: vervollstaendigen und fuer aktuell laufende Projekte anpassen
	/**
	 * this method crawls values from a kickstarter project page
	 * @param htmlDocument - html document of the project
	 * @param url - url of the project
	 * @return a list of all crawled values of the current project
	 */
	public ArrayList<String> getProjectValues(Document htmlDocument, String url) {

		ArrayList<String> projectValues = new ArrayList<String>();

		// initialize all values to "not found"
		String title = "not found";
		String shortDescription = "not found";
		String creatorName = "not found";
		String creatorId = "not found";
		String numberOfBackers = "not found";
		String fundingGoal = "not found";
		String totalFunding = "not found";
		String location = "not found";
		String category = "not found";

		// get title of project
		Element titleParentElement = htmlDocument.select(KickstarterIdentifier.KICKSTARTER_TITLE_IDENTIFIER).first();
		if (titleParentElement != null) {
			Element titleElement = titleParentElement.getElementsByTag("a").first();
			if (titleElement != null) {
				title = titleElement.ownText();
			}
		}
		projectValues.add(title);

		// get short description of project
		Element shortDescriptionParentElement = htmlDocument
				.select(KickstarterIdentifier.KICKSTARTER_SHORTDESCRIPTION_IDENTIFIER).first();
		if (shortDescriptionParentElement != null) {
			Element shortDescriptionElement = shortDescriptionParentElement.select("span.content").first();
			if (shortDescriptionElement != null) {
				shortDescription = shortDescriptionElement.ownText();
			}
		}
		projectValues.add(shortDescription);

		// get creator name of project
		Element creatorNameParentElement = htmlDocument.select(KickstarterIdentifier.KICKSTARTER_CREATORNAME_IDENTIFIER)
				.first();
		if (creatorNameParentElement != null) {
			Element creatorNameElement = creatorNameParentElement.getElementsByTag("a").first();
			if (creatorNameElement != null) {
				creatorName = creatorNameElement.ownText();
			}
		}
		projectValues.add(creatorName);

		// get creator id of project
		// the id is extracted from the url instead of reading it from the html
		// document
		int positionOfCreatorId = url.indexOf("/projects/") + "/projects/".length();
		creatorId = url.substring(positionOfCreatorId);
		creatorId = creatorId.split("/", 2)[0];
		projectValues.add(creatorId);

		// get number of backers of project
		Element numberOfBackersParentElement = htmlDocument.select(KickstarterIdentifier.KICKSTARTER_BACKERS_IDENTIFIER)
				.first();
		if (numberOfBackersParentElement != null) {
			Element numberOfBackersElement = numberOfBackersParentElement.getElementsByTag("b").first();
			if (numberOfBackersElement != null) {
				numberOfBackers = numberOfBackersElement.ownText().split("\\s", 2)[0];
				// receiving number in American style --> convert to German
				// number style
				if (numberOfBackers.contains(",")) {
					numberOfBackers = numberOfBackers.replaceAll("[,]", ".");
				}
			}
		}
		projectValues.add(numberOfBackers);

		// get funding goal of project
		// problem: there is no identifier for the funding goal
		// solution: the next element with an clear identifier is the element of
		// category and location
		// --> take the way from this element to the searched goal element
		Element helperElementForFundingGoal = htmlDocument
				.select(KickstarterIdentifier.KICKSTARTER_CATEGORYANDLOCATION_IDENTIFIER).first();
		if (helperElementForFundingGoal != null) {

			// find the parent element that includes both: the
			// categoryAndLocation element and the goal element
			while (helperElementForFundingGoal != null) {
				helperElementForFundingGoal = helperElementForFundingGoal.parent();
				if (helperElementForFundingGoal.hasClass("row")) {
					break;
				}
			}

			if (helperElementForFundingGoal != null) {

				// identify the goal element by its own text "pledged of goal"
				helperElementForFundingGoal = helperElementForFundingGoal.getElementsMatchingOwnText("pledged of goal")
						.first();

				if (helperElementForFundingGoal != null) {

					Element fundingGoalElement = helperElementForFundingGoal.select("span.money").first();

					if (fundingGoalElement != null) {
						fundingGoal = fundingGoalElement.ownText().replaceAll("[^0-9,.]", "");

						// receiving money in American style --> convert to
						// German
						// money style
						if (fundingGoal.contains(",") || fundingGoal.contains(".")) {
							fundingGoal = fundingGoal.replaceAll("[.]", "#");
							fundingGoal = fundingGoal.replaceAll("[,]", ".");
							fundingGoal = fundingGoal.replaceAll("[#]", ",");
						}
					}
				}
			}
		}
		projectValues.add(fundingGoal);

		// get total funding of project
		Element totalFundingParentElement = htmlDocument
				.select(KickstarterIdentifier.KICKSTARTER_TOTALFUNDING_IDENTIFIER).first();
		if (totalFundingParentElement != null) {
			Element totalFundingElement = totalFundingParentElement.select("span.money").first();
			if (totalFundingElement != null) {
				totalFunding = totalFundingElement.ownText().replaceAll("[^0-9,.]", "");

				// receiving money in American style --> convert to German
				// money style
				if (totalFunding.contains(",") || totalFunding.contains(".")) {
					totalFunding = totalFunding.replaceAll("[.]", "#");
					totalFunding = totalFunding.replaceAll("[,]", ".");
					totalFunding = totalFunding.replaceAll("[#]", ",");
				}
			}
		}
		projectValues.add(totalFunding);

		// get category and location from project
		Element helperElementForLocationAndCategory = htmlDocument
				.select(KickstarterIdentifier.KICKSTARTER_CATEGORYANDLOCATION_IDENTIFIER).first();
		if (helperElementForLocationAndCategory != null) {
			Elements locationAndCategoryElements = helperElementForLocationAndCategory.getElementsByTag("a");
			for (Element element : locationAndCategoryElements) {
				if (!element.getElementsByClass(KickstarterIdentifier.KICKSTARTER_LOCATION_IDENTIFIER).isEmpty()) {
					location = element.ownText();
				}
				if (!element.getElementsByClass(KickstarterIdentifier.KICKSTARTER_CATEGORY_IDENTIFIER).isEmpty()) {
					category = element.ownText();
				}
			}
		}
		projectValues.add(location);
		projectValues.add(category);

		// testing results
		System.out.println("Titel: " + title);
		System.out.println("Kurzbeschreibung: " + shortDescription);
		System.out.println("Projektgründer: " + creatorName + " (Id: " + creatorId + ")");
		System.out.println("Anzahl Unterstützer: " + numberOfBackers);
		System.out.println("Finanzierungsziel ($): " + fundingGoal);
		System.out.println("Gesamtsumme ($): " + totalFunding);
		System.out.println("Ort: " + location);
		System.out.println("Kategorie: " + category);

		return projectValues;
	}

}
