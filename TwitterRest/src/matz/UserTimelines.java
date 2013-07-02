package matz;

import java.io.*;
import java.util.ArrayList;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.json.DataObjectFactory;

public class UserTimelines {
	public static File userListFile = new File("userList.txt");
	public static ArrayList<String> userList = new ArrayList<String>();
	public static ConfigurationBuilder cb = new ConfigurationBuilder();
	
	public static File userDir = new File("userDir");

	public static void main(String[] args) {
		try { //userList loading
			BufferedReader ulbr = new BufferedReader(new InputStreamReader(new FileInputStream(userListFile)));
			String userid = new String();
			while((userid = ulbr.readLine()) != null) {
				if (!userList.contains(userid)) userList.add(userid);
			}
			ulbr.close();
			
			//userDir
			if(!userDir.isDirectory()) userDir.mkdir();
		} catch (FileNotFoundException e) {
			//ignore
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		cb.setDebugEnabled(true)
		.setOAuthConsumerKey("KuG34YZ202uEZCHgHQhQ")
		.setOAuthConsumerSecret("5R9nqymC0Zl5pF5KSDr80gCOTHqkaPS9sehrGoMpI")
		.setOAuthAccessToken("352840258-QrB0NPIYNtGiNUt3vnetmnMumnppJETnvccdZVJa")
		.setOAuthAccessTokenSecret("PkuqzpWNCWglAGKtMfUeg1UVCRQqjqesmlOuyrVA")
	    .setJSONStoreEnabled(true);
		
		TwitterFactory tf = new TwitterFactory(cb.build());
		Twitter twitter = tf.getInstance();
		
		//for every recorded users, get their recent tweets
		try {
			for (String id : userList) {
				File thisUsersFile = new File(userDir,id + ".txt");
				File thisUsersCurr = new File(userDir,id + ".curr.txt");
				long idLong = Long.parseLong(id);
				Paging paging = new Paging();
				paging.setCount(200);
				long maxid = 0;
				if(thisUsersCurr.exists()) {
					BufferedReader cbr = new BufferedReader(new InputStreamReader(new FileInputStream(thisUsersCurr)));
					String curr = cbr.readLine();
					paging.setSinceId(Long.parseLong(curr));
					cbr.close();
				}
				
				ResponseList<Status> timeLine = twitter.getUserTimeline(idLong, paging);
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(thisUsersFile, true)));
				for (Status status : timeLine) {
					maxid = (status.getId() > maxid)? status.getId() : maxid;
					String rawJSON = DataObjectFactory.getRawJSON(status);
					bw.write(rawJSON);
					bw.newLine();
				}
				bw.close();
				
				BufferedWriter cbw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(thisUsersCurr)));
				cbw.write(Long.toString(maxid + 1));
				cbw.close();
				System.out.println(thisUsersFile);
				//break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		

	}

}
