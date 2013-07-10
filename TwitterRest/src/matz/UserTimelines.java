package matz;

import java.io.*;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.json.DataObjectFactory;

public class UserTimelines extends TwitterRest {
	private static int timeLineAuthHead = 1;
	private static int timeLineAuthTail;
	
	private static void setTimeLineAuthTail() {
		timeLineAuthTail = OAuthList.length - 1;
	}

	private static Paging setPaging(File thisUsersCurr) throws Exception {
		Paging paging = new Paging();
		paging.setCount(200);
		if(thisUsersCurr.exists()) {
			BufferedReader cbr = new BufferedReader(new InputStreamReader(new FileInputStream(thisUsersCurr)));
			String curr = cbr.readLine();
			paging.setSinceId(Long.parseLong(curr));
			cbr.close();
		}
		return paging;
	}
	
	public static boolean findAvailableAuth()  {
		//int groundId = currentAuthId;
		//twitter = buildTwitterIns((groundId  == timeLineAuthTail) ? timeLineAuthHead : groundId + 1);
		/*while(!authAvailabilityCheck()) {
			int nextId = (currentAuthId == timeLineAuthTail)? timeLineAuthHead : currentAuthId + 1;
			if (nextId == groundId) return false;
			
			twitter = buildTwitterIns(nextId);
		}*/
		if (!authAvailabilityCheck()) {
			int nextId = (currentAuthId == timeLineAuthTail)? timeLineAuthHead : currentAuthId + 1;
			twitter = buildTwitterIns(nextId);
			saveAuthInfo();
			if (!authAvailabilityCheck()) return false;
		}		
		return true;
	}

	public static void main(String[] args) {
		loadAuthInfo();
		setTimeLineAuthTail();
		
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
		
		//Twitter twitter = buildTwitterIns(timeLineAuthHead); //matz_0001, init
		twitter = buildTwitterIns(timeLineAuthHead); //matz_0001, init
		
		try {	//for every recorded users, get their recent tweets
			for (String id : userList) {
				while (!findAvailableAuth()) {
					sleepUntilReset();
				}
				
				File thisUsersFile = new File(userDir,id + ".txt");
				File thisUsersCurr = new File(userDir,id + ".curr.txt");
				long userIdLong = Long.parseLong(id);
				long maxid = 1;
				
				Paging paging = setPaging(thisUsersCurr);

				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(thisUsersFile, true)));
				
				ResponseList<Status> timeLine = null;
				try {
					timeLine = twitter.getUserTimeline(userIdLong, paging);
				} catch (TwitterException twe) {
					System.err.println(twe.getRateLimitStatus().toString());
					System.err.println();
					saveAuthInfo();
					// getSecondsUntilReset returns seconds until limit reset in Integer
					int secondsUntilReset = twe.getRateLimitStatus().getSecondsUntilReset();
					long retryAfter = (long)(secondsUntilReset * 1000);
					if (secondsUntilReset <= 0) retryAfter = authLimitWindow;
					retryAfter += authRetryMargin;
					sleepUntilReset(retryAfter);
					timeLine = twitter.getUserTimeline(userIdLong, paging);
				} finally {
					callCount(currentAuthId);
					for (Status status : timeLine) {
						long tmpid = status.getId(); 
						maxid = (tmpid > maxid)? tmpid : maxid;
						String rawJSON = DataObjectFactory.getRawJSON(status);
						bw.write(rawJSON);
						bw.newLine();
					}
				}
				
				bw.close();
				
				BufferedWriter cbw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(thisUsersCurr)));
				cbw.write(Long.toString(maxid));
				cbw.close();
				System.out.println(thisUsersFile);
				//break;
			}
		} catch (Exception e) {
			saveAuthInfo();
			e.printStackTrace();
		}

		saveAuthInfo();
		System.out.println("Done.");
	}

}
