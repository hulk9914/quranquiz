/****
* Copyright (C) 2011-2013 Quran Quiz Net 
* Tarek Eldeeb <tarekeldeeb@gmail.com>
* License: see LICENSE.txt
****/
package net.quranquiz.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.quranquiz.R;
import net.quranquiz.storage.QQProfile;
import net.quranquiz.storage.QQProfileHandler;
import net.quranquiz.util.QQUtils;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;
//import com.google.analytics.tracking.android.EasyTracker;

public class QQLastScreenActivity extends SherlockActivity {

	private TextView tvScore, tvShare;
	private QQProfileHandler ProfileHandler;
	private String conclusionMessage;
	private String ExtraInfo = "\n\n"
			+ "يمكنك مشاركة اصدقاءك عن طريق احد الطرق بالقائمة العلوية،"
			+ " فالمشاركة تشجع التنافس في الطاعة كما انها تساعد على نشر البرنامج";

	private Intent createShareIntent() {
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		//shareIntent.putExtra(Intent.EXTRA_SUBJECT, "نقاط جديدة في اختبار القران");
		shareIntent.putExtra(Intent.EXTRA_TEXT, conclusionMessage
				+ " http://quranquiz.net");
		return shareIntent;
	}

	private String getMessageFromProfile(QQProfile currentProfile) {
		String msg;
		msg = "لقد حصلت على " + currentProfile.getScore()
				+ "  نقطة في #اختبار_القران، من ينافسنى؟";
		return msg;
	}

	  @Override
	  public void onStart() {
	    super.onStart();
	    //EasyTracker.getInstance().activityStart(this); // Add this method.
	  }

	  @Override
	  public void onStop() {
	    super.onStop();
	    //EasyTracker.getInstance().activityStop(this); // Add this method.
	  }
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.lastscreen_layout);

		// Get the passed QQProfile
		ProfileHandler = (QQProfileHandler) getIntent().getSerializableExtra(
				"ProfileHandler");
		conclusionMessage = getMessageFromProfile(ProfileHandler.getProfile());
		tvScore = (TextView) findViewById(R.id.textViewScore);
		tvShare = (TextView) findViewById(R.id.textViewShare);

		tvScore.setText(conclusionMessage);
		tvShare.setText(ExtraInfo);

		// If score history needs update, do so then save and post it
		if (ProfileHandler.getProfile().updateScoreRecord() ||
			QQUtils.QQDebug==1) {
			new UserPostAnonymous().execute(ProfileHandler);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate your menu.
		getSupportMenuInflater().inflate(R.menu.share_action_provider, menu);

		// Set file with share history to the provider and set the share intent.
		MenuItem actionItem = menu
				.findItem(R.id.menu_item_share_action_provider_action_bar);
		ShareActionProvider actionProvider = (ShareActionProvider) actionItem
				.getActionProvider();
		actionProvider
				.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
		// Note that you can set/change the intent any time,
		// say when the user has selected an image.
		actionProvider.setShareIntent(createShareIntent());

		return true;
	}

    private class UserPostAnonymous extends AsyncTask<QQProfileHandler, Void, String> {

        @Override
        protected String doInBackground(QQProfileHandler... params) {
    		String uid, score, juz2, qcount, avglevel, md5;
    		String[] ids;
    		String response="";
    		QQProfile currentProfile = params[0].getProfile();
    		
    		/******************* 1- Save profile *****************/
			ProfileHandler.saveProfile();
			
    		/******************* 2- Post User Data ***************/
    		uid = currentProfile.getuid();
    		ids = uid.split("\\+");
    		
    		score = String.valueOf(currentProfile.getScore());
    		juz2 = String
    				.valueOf((double) (currentProfile.getTotalStudyLength() * 300 / QQUtils.QuranWords) / 10);
    		qcount = String.valueOf(currentProfile.getTotalQuesCount());
    		avglevel = String.valueOf(currentProfile.getTotAvgLevel());
    		md5 = QQUtils.md5(QQUtils.QQ_MD5_KEY + ids[0] + "-" + ids[1] + "-" + ids[2] + "-"
    							+ ids[3] + "-" + ids[4]
    							+ "-" + score + "-" + juz2 + "-" + qcount
    							+ "-" + avglevel);

    		// Create a new HttpClient and Post Header
    		HttpClient httpclient = new DefaultHttpClient();
    		HttpPost httppost = new HttpPost(
    				"http://post.quranquiz.net/updateUserData.php");

    		try {
    			// Add user data
    			List<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>(10);
    			nameValuePairs.add(new BasicNameValuePair("uid_gl", ids[0]));
    			nameValuePairs.add(new BasicNameValuePair("uid_fb", ids[1]));
    			nameValuePairs.add(new BasicNameValuePair("uid_tw", ids[2]));
    			nameValuePairs.add(new BasicNameValuePair("uid_ap", ids[3]));
    			nameValuePairs.add(new BasicNameValuePair("uid_ot", ids[4]));
    			nameValuePairs.add(new BasicNameValuePair("s", score));
    			nameValuePairs.add(new BasicNameValuePair("j", juz2));
    			nameValuePairs.add(new BasicNameValuePair("q", qcount));
    			nameValuePairs.add(new BasicNameValuePair("l", avglevel));
    			nameValuePairs.add(new BasicNameValuePair("m", md5));
    			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

    			// Execute HTTP Post Request
    			HttpResponse res = httpclient.execute(httppost);
    			response += EntityUtils.toString(res.getEntity()); 
    		} catch (ClientProtocolException e) {
    		} catch (IOException e) {
    		}
    		return response;
        }      

        @Override
        protected void onPostExecute(String result) {
    		//TextView tvShare = (TextView) findViewById(R.id.textViewShare);
			//tvShare.append(result);
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
  } 
}
