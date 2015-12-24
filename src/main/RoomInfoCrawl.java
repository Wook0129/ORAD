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

		FileWriter fw = new FileWriter("C://Users/�¿�/Desktop/��������.csv");
		String vars = "������;�� ����;����;�ǹ�����;��Ϲ�ȣ;��/�ǹ�����;������;����;������ �����׸�;����������;ũ��(��);�ɼ�;���ְ�����;�ּ�;�α���ö��;����� ����;���";
		
		int cnt = 0;

		if(useTextFeatures) ke = new KeywordExtractor();

		for(String link : links) {
			data.add(getRoomInfo(link, useTextFeatures));
			cnt++;
			if(cnt % 10 == 0) System.out.println(cnt+"��° ���� ���� ó�� ��..");
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
		System.setProperty("webdriver.chrome.driver", "C://Users/�¿�/Desktop/chromedriver.exe");
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
				case "������":
					value = value.replaceAll("����", "").trim();
					if(value.contains("��������")){
						value = value.replaceAll("  \\(��������\\)", "");
						etc += "��������"+",";
					}
					break;
				case "����":
					value = value.replaceAll("����", "").trim();
					break;
				case "ũ��":
					value = value.split("\\(")[1].split("P")[0];
					break;
				case "������":
					value = value.replaceAll("����", "");
					if(value.equals("����")) value = "0";
					break;
				case "����":
					if(value.equals("�Ұ���")) value = "N";
					else value = "Y";
					break;
				case "����������":
					if(value.equals("����")) value = "N";
					else value = "Y";
					break;
				case "����":
					etc += var+":"+value+",";
					continue;
				case "�ݷ�����":
					etc += var+":"+value+",";
					continue;
				case "��������":
					etc += var+":"+value+",";
					continue;
				case "�󼼼���":
					if(useTextFeatures){

						//Text Preprocessing
						value = value.replaceAll("\n", " ")
								.replaceAll(" ~+", " ").replaceAll(" -+", " ")
								.replaceAll("~+ ", " ").replaceAll("-+ ", " ")
								.replaceAll("[^��-�R 0-9,/%-]", " ").replaceAll(" +", " ");

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
				case "�ǹ�����":
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
