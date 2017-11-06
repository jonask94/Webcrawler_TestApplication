import java.util.Scanner;

public class SpiderTest {

	public static void main(String[] args) {
		//TODO use GUI
		/*
		 * interaction with user
		 * needed arguments:
		 * - start URL
		 * - word (string) the program should search
		 * - maximum of pages that are searched
		 * - searching domain (optional)
		 */
		Scanner scanner = new Scanner(System.in);
		System.out.println("Bitte geben Sie die Start-URL als vollst�ndige http-Adresse an (z.B. http://www.uni-siegen.de/start/):");
		String startUrl = scanner.next();
		System.out.println("Bitte geben Sie die Zeichenkette ein, nach der Sie suchen m�chten:");
		String searchWord = scanner.next();
		System.out.println("Bitte geben Sie die maximale Anzahl an Webseiten ein, die Sie durchsuchen m�chten:");
		int maxPagesToSearch = scanner.nextInt();
		String localization;
		System.out.println("Sie k�nnen eine Eingrenzung des Suchraums vornehmen.");
		System.out.println("Dadurch werden nur Webseiten durchsucht, die eine von Ihnen eingegebene Zeichenkette enthalten (z.B. www.uni-siegen.de)");
		System.out.println("M�chten Sie eine solche Eingrenzung vornehmen? j/n");
		while(true)
		{
			String localizationResponse = scanner.next();
			if(localizationResponse.equals("j"))
			{
				System.out.println("Bitte geben Sie eine Zeichenkette ein:");
				localization = scanner.next();
				break;
			}
			else if(localizationResponse.equals("n"))
			{
				localization = "";
				break;
			}
			else{
				System.out.println("Ihre Antwort konnte nicht interpretiert werden. Bitte antworten Sie mit 'j' oder 'n'");
			}
		}
		scanner.close();
		//starting the webcrawler
		Spider spider = new Spider();
		spider.search(startUrl, searchWord, maxPagesToSearch, localization);
	}

}
