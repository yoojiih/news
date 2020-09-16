package com.example.mynews;

import android.app.ProgressDialog;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.content.Intent;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static String rssUrl = "https://news.sbs.co.kr/news/SectionRssFeed.do?sectionId=14";

    ProgressDialog progressDialog;
    Handler handler = new Handler();

    RSSListView list;
    RSSListAdapter adapter;
    ArrayList<RSSNewsItem> newsItemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create a ListView instance
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        list = new RSSListView(this);

        adapter = new RSSListAdapter(this);
        list.setAdapter(adapter);
        list.setOnDataSelectionListener(new OnDataSelectionListener() {
            public void onDataSelected(AdapterView parent, View v, int position, long id) {
                RSSNewsItem curItem = (RSSNewsItem) adapter.getItem(position);
                String url = curItem.getLink();

                Uri uri = Uri.parse(url);
                Intent it  = new Intent(Intent.ACTION_VIEW,uri);
                startActivity(it);
            }
        });

        newsItemList = new ArrayList<RSSNewsItem>();

        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.mainLayout);
        mainLayout.addView(list, params);
        showRSS(rssUrl);
    }

    private void showRSS(String urlStr) {
        try {
            progressDialog = ProgressDialog.show(this, "RSS Refresh", "RSS 정보 업데이트 중...", true, true);

            RefreshThread thread = new RefreshThread(urlStr);
            thread.start();

        } catch (Exception e) {
            Log.e(TAG, "Error", e);
        }
    }

    class RefreshThread extends Thread {
        String urlStr;

        public RefreshThread(String str) {
            urlStr = str;
        }

        public void run() {

            try {

                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();

                URL urlForHttp = new URL(urlStr);

                InputStream instream = getInputStreamUsingHTTP(urlForHttp);
                Document document = builder.parse(instream);
                int countItem = processDocument(document);
                Log.d(TAG, countItem + " news item processed.");

                // post for the display of fetched RSS info.
                handler.post(updateRSSRunnable);

            } catch(Exception ex) {
                ex.printStackTrace();
            }

        }
    }


    public InputStream getInputStreamUsingHTTP(URL url)
            throws Exception
    {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);

        int resCode = conn.getResponseCode();
        Log.d(TAG, "Response Code : " + resCode);

        InputStream instream = conn.getInputStream();

        return instream;
    }

    /**
     * process DOM document for RSS
     *
     * @param doc
     */
    private int processDocument(Document doc) {
        newsItemList.clear();

        Element docEle = doc.getDocumentElement();
        NodeList nodelist = docEle.getElementsByTagName("item");
        int count = 0;
        if ((nodelist != null) && (nodelist.getLength() > 0)) {
            for (int i = 0; i < nodelist.getLength(); i++) {
                RSSNewsItem newsItem = dissectNode(nodelist, i);
                if (newsItem != null) {
                    newsItemList.add(newsItem);
                    count++;
                }
            }
        }

        return count;
    }

    private RSSNewsItem dissectNode(NodeList nodelist, int index) {
        RSSNewsItem newsItem = null;

        try {
            Element entry = (Element) nodelist.item(index);

            Element title = (Element) entry.getElementsByTagName("title").item(0);
            Element link = (Element) entry.getElementsByTagName("link").item(0);
            Element description = (Element) entry.getElementsByTagName("description").item(0);


            String titleValue = null;
            if (title != null) {
                Node firstChild = title.getFirstChild();
                if (firstChild != null) {
                    titleValue = firstChild.getNodeValue();
                }
            }
            String linkValue = null;
            if (link != null) {
                Node firstChild = link.getFirstChild();
                if (firstChild != null) {
                    linkValue = firstChild.getNodeValue();
                }
            }

            String descriptionValue = null;
            if (description != null) {
                Node firstChild = description.getFirstChild();
                if (firstChild != null) {
                    descriptionValue = firstChild.getNodeValue();
                }
            }

            Log.d(TAG, "item node : " + titleValue + ", " + linkValue + ", " + descriptionValue);

            newsItem = new RSSNewsItem(titleValue, linkValue, descriptionValue);

        } catch (DOMException e) {
            e.printStackTrace();
        }

        return newsItem;
    }


    Runnable updateRSSRunnable = new Runnable() {
        public void run() {

            try {

                Resources res = getResources();
                Drawable rssIcon = res.getDrawable(R.drawable.love);
                for (int i = 0; i < newsItemList.size(); i++) {
                    RSSNewsItem newsItem = (RSSNewsItem) newsItemList.get(i);
                    newsItem.setIcon(rssIcon);
                    adapter.addItem(newsItem);
                }

                adapter.notifyDataSetChanged();

                progressDialog.dismiss();
            } catch(Exception ex) {
                ex.printStackTrace();
            }

        }
    };

}


