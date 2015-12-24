package main;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.snu.ids.ha.index.Keyword;
import org.snu.ids.ha.index.KeywordExtractor;
import org.snu.ids.ha.index.KeywordList;


public class RoomInfoCrawl {

	static WebDriver driver;
	static ArrayList<String> data = new ArrayList<String>();

	static HashMap<String, Integer> textFeatures = new HashMap<String, Integer>(); //Whole term dictionary
	static ArrayList<HashMap<String, Integer>> termFreqs = new ArrayList<HashMap<String, Integer>>(); //Index -> (Term -> Freq)
	static KeywordExtractor ke;
	static boolean useTextFeatures = true;
	static int termFreqCutoff = 10;

	public static void main(String[] args) throws Exception{

		String naksung = "lat=37.476287841796875&lng=126.9583740234375";
		String daehak = "lat=37.47060012817383&lng=126.93685913085938";
		String seorim = "lat=37.47504425048828&lng=126.93497467041016";
		String inhun = "lat=37.475128173828125&lng=126.96526336669922";

		String[] locations = {naksung, daehak, seorim, inhun};
		//		String[] locations = {naksung};

		HashSet<String> links = new HashSet<String>();

		initCrawler();

		for(String location : locations) addLink(links, location);
		System.out.println(links.size());
		driver.quit();

		FileWriter fw = new FileWriter("C://Users/ÅÂ¿í/Desktop/¿ø·ëÁ¤º¸.csv");
		String vars = "º¸Áõ±Ý;¹æ ±¸Á¶;¿ù¼¼;°Ç¹°ÇüÅÂ;µî·Ï¹øÈ£;Ãþ/°Ç¹°Ãþ¼ö;°ü¸®ºñ;ÁÖÂ÷;°ü¸®ºñ Æ÷ÇÔÇ×¸ñ;¿¤·¹º£ÀÌÅÍ;Å©±â(Æò);¿É¼Ç;ÀÔÁÖ°¡´ÉÀÏ;ÁÖ¼Ò;ÀÎ±ÙÀüÃ¶¿ª;µî·ÏÀÚ Á¤º¸;ºñ°í";
		
		int cnt = 0;

		if(useTextFeatures) ke = new KeywordExtractor();

		for(String link : links) {
			data.add(getRoomInfo(link, useTextFeatures));
			cnt++;
			if(cnt % 10 == 0) System.out.println(cnt+"¹øÂ° ¿ø·ë Á¤º¸ Ã³¸® Áß..");
		}

		if(useTextFeatures){

			HashSet<String> dic = new HashSet<String>();
			for(String textFeature : textFeatures.keySet())
				if(textFeatures.get(textFeature) > termFreqCutoff) dic.add(textFeature);
			for(String textFeature : dic)
				vars += ";"+textFeature;

			for(int i = 0; i<data.size(); i++){
				String room_data = data.get(i);
				HashMap<String, Integer> termFreq = termFreqs.get(i);
				for(String term : dic)
					if(termFreq.containsKey(term)) room_data += termFreq.get(term)+";";
					else room_data += 0+";";
				room_data = room_data.substring(0, room_data.length() - 1);
				data.set(i,room_data);
			}
		}
		
		fw.write(vars+"\n");
		for(String roomData : data) {
			fw.write(roomData + "\n");
			cnt++;
		}
		fw.close();

	}

	public static void initCrawler(){
		System.setProperty("webdriver.chrome.driver", "C://Users/ÅÂ¿í/Desktop/chromedriver.exe");
		driver = new ChromeDriver();
		driver.manage().timeouts().pageLoadTimeout(60, TimeUnit.SECONDS);
	}

	public static void addLink(Set<String> links, String location) throws InterruptedException{
		driver.navigate().to("https://m.zigbang.com/search/map?"+location+"&zoom=5");
		Thread.sleep(5000);
		String page = driver.getPageSource();
		String[] rooms = page.split("onclick=\"window.open\\('");
		for(String room : rooms){
			String link = room.split("'\\)")[0];
			if(link.length() == 15) links.add(link); /// "/items1/#######" form
		}
	}

	public static String getRoomInfo(String link, boolean useTextFeatures) throws IOException{
		String url = "https://www.zigbang.com"+link;
		Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(0).get();
		String raw_roomInfo = doc.getElementsByClass("detail-list").first().toString();
		String[] raw_attrs = raw_roomInfo.split("<th>");

		String values = "";
		String etc = "";

		for(String raw_attr : raw_attrs){
			if(!raw_attr.startsWith("<")){
				String var = raw_attr.split("</th>")[0].replaceAll("&.*;", "").trim();
				String value = raw_attr.split("</th>")[1].replaceAll("<.*?>", "").replaceAll("&.*;", "").trim();

				switch(var){
				case "º¸Áõ±Ý":
					value = value.replaceAll("¸¸¿ø", "").trim();
					if(value.contains("Àü¼¼°¡´É")){
						value = value.replaceAll("  \\(Àü¼¼°¡´É\\)", "");
						etc += "Àü¼¼°¡´É"+",";
					}
					break;
				case "¿ù¼¼":
					value = value.replaceAll("¸¸¿ø", "").trim();
					break;
				case "Å©±â":
					value = value.split("\\(")[1].split("P")[0];
					break;
				case "°ü¸®ºñ":
					value = value.replaceAll("¸¸¿ø", "");
					if(value.equals("¾øÀ½")) value = "0";
					break;
				case "ÁÖÂ÷":
					if(value.equals("ºÒ°¡´É")) value = "N";
					else value = "Y";
					break;
				case "¿¤¸®º£ÀÌÅÍ":
					if(value.equals("¾øÀ½")) value = "N";
					else value = "Y";
					break;
				case "¹æÇâ":
					etc += var+":"+value+",";
					continue;
				case "¹Ý·Áµ¿¹°":
					etc += var+":"+value+",";
					continue;
				case "Àü¼¼´ëÃâ":
					etc += var+":"+value+",";
					continue;
				case "»ó¼¼¼³¸í":
					if(useTextFeatures){

						//Text Preprocessing
						value = value.replaceAll("\n", " ")
								.replaceAll(" ~+", " ").replaceAll(" -+", " ")
								.replaceAll("~+ ", " ").replaceAll("-+ ", " ")
								.replaceAll("[^°¡-ÆR 0-9,/%-]", " ").replaceAll(" +", " ");

						//Keyword Extraction(Kokoma)
						KeywordList kl = ke.extractKeyword(value, true);
						HashMap<String, Integer> termFreq = new HashMap<String, Integer>();
						for( int i = 0; i < kl.size(); i++ ) {
							Keyword kwrd = kl.get(i);
							String keyword = kwrd.getString();
							if(keyword.length() == 2 || keyword.length() == 3){
								if(textFeatures.containsKey(keyword)) textFeatures.put(keyword, textFeatures.get(keyword) + kwrd.getCnt());
								else textFeatures.put(keyword, kwrd.getCnt());
								termFreq.put(keyword, kwrd.getCnt());
							}
						}
						termFreqs.add(termFreq);
					}
					continue;
				case "°Ç¹°Á¤º¸":
					continue;
				}
				if(value.length() != 0) values += value+";";
			}
		}
		if(etc.length() > 0) values += etc.substring(0, etc.length() - 1)+";";
		else values += "-"+";";
		return values;
	}
}
