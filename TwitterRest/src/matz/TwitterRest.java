package matz;

import java.io.*;
import java.util.ArrayList;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterRest {
	private static boolean forceCursorReset = false;
	private static boolean forceFileRefresh = false;
	private static boolean forceInitialize = false;
	private final static String cursorResetOption = "-cr";
	private final static String fileRefreshOption = "-fr";
	private final static String initializeOption = "-init";
	
	public static ArrayList<String> queryKeyWordsList = new ArrayList<String>();
	public static File currentIdFile = new File("currentId.txt");
	public static File searchLog = new File("searchLog.txt");
	public static File userListFile = new File("userList.txt");
	public static File userDir = new File("userDir");
	public static ArrayList<String> userList = new ArrayList<String>();
	public static ConfigurationBuilder cb = new ConfigurationBuilder();
	
	public static void setQueryKeyWordsList(File file) {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String line = new String();
			while((line = br.readLine()) != null) {
				queryKeyWordsList.add(line);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void extractUsers(String queryKeyWords, long currentId, Twitter twitter)
			throws FileNotFoundException, TwitterException, IOException {
		Query query = new Query(queryKeyWords);
		
		OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(userListFile, forceFileRefresh? false : true));
		BufferedWriter bw = new BufferedWriter(osw);
		
		query.setSinceId(forceCursorReset? 0 : currentId);
		query.setCount(100);
		QueryResult result = twitter.search(query);

		long since = result.getSinceId();
		long until = result.getMaxId();
		
		System.out.println("Since " + since + " until " + until);
		for (Status status : result.getTweets()) { //pickup relevant user
			User thisone = status.getUser();
			String thisoneId = String.format("%d", thisone.getId());
			if (!userList.contains(thisoneId)) {
				userList.add(thisoneId);
				bw.write(thisoneId);
				bw.newLine();
			}
		}
		
		bw.close();
		osw.close();
		
		OutputStreamWriter osw1 = new OutputStreamWriter(new FileOutputStream(currentIdFile));
		BufferedWriter bw1 = new BufferedWriter(osw1);
		
		long nextSince = until + 1;
		bw1.write(Long.toString(nextSince));
		bw1.close();
		osw1.close();
	}
	
	public static void main (String[] args) {
		String queryKeyWords = "Ž©–¯“}";
		for (int i=0; i<args.length; i++) {
			if(args[i].matches(cursorResetOption)) {
				forceCursorReset = true;
			} else if(args[i].matches(fileRefreshOption)) {
				forceFileRefresh = true;
			} else if(args[i].matches(initializeOption)){
				forceInitialize = true;
			} else if(args[i] == "queries.txt" && new File(args[i]).exists()) {
				setQueryKeyWordsList(new File(args[i]));
			} else {
				queryKeyWords = args[i];
			}
		}
		long currentId = 0;
		try { //userList loading
			BufferedReader ulbr = new BufferedReader(new InputStreamReader(new FileInputStream(userListFile)));
			String userid = new String();
			while((userid = ulbr.readLine()) != null) {
				if (!userList.contains(userid)) userList.add(userid);
			}
			ulbr.close();
		} catch (FileNotFoundException e) {
			//ignore
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (forceInitialize) {
			currentIdFile.delete();
			userListFile.delete();
			File oldUserDir = new File("userDir" + System.currentTimeMillis());
			userDir.renameTo(oldUserDir);
			File oldLogFile = new File("searchLog" + System.currentTimeMillis() + ".txt");
			searchLog.renameTo(oldLogFile);
			return;
		}
		
		//ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		.setOAuthConsumerKey("KuG34YZ202uEZCHgHQhQ")
		.setOAuthConsumerSecret("5R9nqymC0Zl5pF5KSDr80gCOTHqkaPS9sehrGoMpI")
		.setOAuthAccessToken("352840258-QrB0NPIYNtGiNUt3vnetmnMumnppJETnvccdZVJa")
		.setOAuthAccessTokenSecret("PkuqzpWNCWglAGKtMfUeg1UVCRQqjqesmlOuyrVA")
	    .setJSONStoreEnabled(true);
		
		TwitterFactory tf = new TwitterFactory(cb.build());
		Twitter twitter = tf.getInstance();
		
		try {
			InputStreamReader isr = new InputStreamReader(new FileInputStream(currentIdFile));
			BufferedReader br = new BufferedReader(isr);
			
			String line = new String();
			while((line = br.readLine()) != null) {
				currentId = Long.parseLong(line);
			}
			br.close();
			isr.close();
		} catch (Exception e) {
			// ignore
		}
		

		try {
			//search for relevant users
			extractUsers(queryKeyWords, currentId, twitter);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
