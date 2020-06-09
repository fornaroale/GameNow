package it.unimib.disco.gruppoade.gamenow.fragments.feed;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.unimib.disco.gruppoade.gamenow.R;
import it.unimib.disco.gruppoade.gamenow.adapters.RssListAdapter;
import it.unimib.disco.gruppoade.gamenow.database.FbDatabase;
import it.unimib.disco.gruppoade.gamenow.models.NewsProvider;
import it.unimib.disco.gruppoade.gamenow.models.PieceOfNews;
import it.unimib.disco.gruppoade.gamenow.models.User;

public class FeedFragment extends Fragment {

    private static final String TAG = "DiscoverFragment";

    private RecyclerView mRecyclerView;
    private TextView mEmptyTV;
    private SwipeRefreshLayout mSwipeLayout;
    private List<PieceOfNews> mFeedModelList;
    private FeedViewModel feedViewModel;
    private RssListAdapter adapter;
    private User user;
    private boolean recyclerViewInitialized;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        feedViewModel =
                ViewModelProviders.of(this).get(FeedViewModel.class);
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerViewInitialized = false;

        // Binding elementi visuali
        mSwipeLayout = getView().findViewById(R.id.feed_swipe_refresh);
        mRecyclerView = getView().findViewById(R.id.feed_recycler_view);
        mEmptyTV = getView().findViewById(R.id.feed_empty_view);

        // Recupero dati database
        FbDatabase.getUserReference().addListenerForSingleValueEvent(postListenerFirstUserData);
        FbDatabase.getUserReference().addValueEventListener(postListenerUserData);
    }

    private ValueEventListener postListenerFirstUserData = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            user = dataSnapshot.getValue(User.class);

            if(user!=null) {
                initializeRecyclerView();
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            throw databaseError.toException();
        }
    };

    private ValueEventListener postListenerUserData = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            user = dataSnapshot.getValue(User.class);

            if(user!=null) {
                if(!recyclerViewInitialized){

                    initializeRecyclerView();
                }

                // Controllo la presenza o meno di informazioni per mostrare un messaggio di stato
                if (mFeedModelList.isEmpty()) {
                    mRecyclerView.setVisibility(View.GONE);
                    mEmptyTV.setVisibility(View.VISIBLE);
                } else {
                    mRecyclerView.setVisibility(View.VISIBLE);
                    mEmptyTV.setVisibility(View.GONE);
                }

                adapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            throw databaseError.toException();
        }
    };

    private void initializeRecyclerView() {

        // Recupero il recyclerview dal layout xml e imposto l'adapter
        mFeedModelList = new ArrayList<>();
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.setHasFixedSize(true);
        adapter = new RssListAdapter(getActivity(), mFeedModelList, user);
        mRecyclerView.setAdapter(adapter);

        new ProcessInBackground().execute(readProvidersCsv());

        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new ProcessInBackground().execute(readProvidersCsv());
            }
        });

        recyclerViewInitialized = true;

    }

    public class ProcessInBackground extends AsyncTask<List<NewsProvider>, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            mSwipeLayout.setRefreshing(true);
        }

        @Override
        protected Boolean doInBackground(List<NewsProvider>... providers) {

            // Pulisco array di notizie
            mFeedModelList.clear();

            // Messaggio caricamento notizie
            mEmptyTV.setText(R.string.news_loading);

            for (NewsProvider provider : providers[0]) {
                URL urlLink = provider.getRssUrl();

                if (TextUtils.isEmpty(urlLink.toString()))
                    return false;

                try {
                    // Creo connessione con URL
                    InputStream inputStream = urlLink.openConnection().getInputStream();

                    // Eseguo il parsing XML -> oggetti PieceOfNews
                    parseFeed(inputStream, provider);
                } catch (IOException e) {
                    Log.e(TAG, "Error [IO EXCEPTION] ", e);
                } catch (XmlPullParserException e) {
                    Log.e(TAG, "Error [XML PULL PARS.] ", e);
                }
            }

            // Parsing XML avvenuto correttamente
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mSwipeLayout.setRefreshing(false);

            if (success) {
                // Ordino notizie in base alla data di pubblicazione
                Collections.sort(mFeedModelList);
                Collections.reverse(mFeedModelList);

                // Controllo la presenza o meno di informazioni per mostrare un messaggio di stato
                if (mFeedModelList.isEmpty()) {
                    mRecyclerView.setVisibility(View.GONE);
                    mEmptyTV.setText(R.string.no_data_available_feed);
                    mEmptyTV.setVisibility(View.VISIBLE);
                } else {
                    mRecyclerView.setVisibility(View.VISIBLE);
                    mEmptyTV.setVisibility(View.GONE);
                }

                // Riempo la RecyclerView con le schede notizie
                adapter.notifyDataSetChanged();
            } else {
                Log.d("RSS URL", "NOT valid Rss feed url");
            }
        }

    }

    public void parseFeed(InputStream inputStream, NewsProvider provider) throws XmlPullParserException, IOException {
        // Variabili temporanee per valori xml
        String title = null;
        String link = null;
        String description = null;
        String guid = null;
        LocalDateTime pubDate = null;
        String contentEncoded = null;

        // mi trovo all'interno della notizia?
        boolean insideItem = false;

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xpp = factory.newPullParser();
            // supporto per namespace xml a false
            factory.setNamespaceAware(false);
            xpp.setInput(inputStream, "UTF_8");

            // tipo dell'evento (inizio doc, fine doc, inizio tag, fine tag, ecc.)
            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().equalsIgnoreCase("item")) {
                        insideItem = true;
                    } else if (insideItem && xpp.getName().equalsIgnoreCase("title")) {
                        title = xpp.nextText();
                    } else if (insideItem && xpp.getName().equalsIgnoreCase("link")) {
                        link = xpp.nextText();
                    } else if (insideItem && xpp.getName().equalsIgnoreCase("description")) {
                        description = xpp.nextText();
                    } else if (insideItem && xpp.getName().equalsIgnoreCase("guid")) {
                        guid = xpp.nextText();
                    } else if (insideItem && xpp.getName().equalsIgnoreCase("pubDate")) {
                        pubDate = LocalDateTime.parse(xpp.nextText(), DateTimeFormatter.RFC_1123_DATE_TIME);
                    } else if (insideItem && xpp.getName().equalsIgnoreCase("content:encoded")) {
                        contentEncoded = xpp.nextText();
                    }
                } else if (eventType == XmlPullParser.END_TAG && xpp.getName().equalsIgnoreCase("item")) {
                    insideItem = false;
                    if (title != null && link != null && description != null && pubDate != null) {
                        if (!checkNewsPresence(guid, provider.getPlatform())) {
                            String imgUrl = extractImageUrl(description);
                            String contentImgUrl = null;
                            if(imgUrl.isEmpty() && contentEncoded!=null) {
                                contentImgUrl = extractImageUrl(contentEncoded);
                            }

                            PieceOfNews item;
                            if(contentImgUrl!=null) {
                                item = new PieceOfNews(title, description, link, pubDate, contentImgUrl, guid, provider);
                            } else {
                                item = new PieceOfNews(title, description, link, pubDate, imgUrl, guid, provider);
                            }
                            mFeedModelList.add(item);
                        }
                    }
                }
                eventType = xpp.next();
            }
        } finally {
            inputStream.close();
        }
    }

    private List<NewsProvider> readProvidersCsv() {
        List<NewsProvider> providers = new ArrayList<>();
        InputStream is = getResources().openRawResource(R.raw.providers);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        String line = "";

        try {
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("@@@");
                if (user.getTags() != null) {
                    if (user.getTags().contains(tokens[0]))
                        providers.add(new NewsProvider(tokens[0], tokens[1], tokens[2], tokens[3]));
                }
            }
        } catch (IOException e) {
            Log.e("CSV ERROR LOG ----->>> ", "Error: " + e);
        }

        return providers;
    }

    public boolean checkNewsPresence(String guid, String platform) {
        // Controlla che non vi sia già una news uguale ma con tag diversi,
        // in tal caso aggiunge a quella
        for (PieceOfNews pieceOfNews : mFeedModelList) {
            if (pieceOfNews.getGuid().equals(guid)) {
                // Se è già presente, aggiungo il tag (ammesso che questo non vi sia già)
                String tmpPlatform = pieceOfNews.getProvider().getPlatform();
                if (tmpPlatform.indexOf(platform) == -1)
                    pieceOfNews.getProvider().setPlatform(tmpPlatform + "," + platform);
                return true;
            }
        }
        return false;
    }

    private String extractImageUrl(String description) {
        Document document = Jsoup.parse(description);
        Elements imgs = document.select("img");

        for (Element img : imgs) {
            if (img.hasAttr("src")) {
                return img.attr("src").replace("http:", "https:");
            }
        }

        // no image URL
        return "";
    }
}