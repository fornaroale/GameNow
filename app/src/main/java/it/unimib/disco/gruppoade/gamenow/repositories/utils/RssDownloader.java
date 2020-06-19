package it.unimib.disco.gruppoade.gamenow.repositories.utils;

import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.unimib.disco.gruppoade.gamenow.models.NewsProvider;
import it.unimib.disco.gruppoade.gamenow.models.PieceOfNews;
import it.unimib.disco.gruppoade.gamenow.repositories.ProvidersRepository;

public class RssDownloader implements Runnable {
    private Resources resources;
    private MutableLiveData<ArrayList<PieceOfNews>> news;
    private final static String TAG = "RssDownloaderRunnable";

    public RssDownloader(Resources res, MutableLiveData<ArrayList<PieceOfNews>> news){
        this.resources = res;
        this.news = news;
    }

    @Override
    public void run() {
        ProvidersRepository.getInstance(resources);
        List<NewsProvider> providers = ProvidersRepository.loadProviders();

        // Array temporaneo per news
        ArrayList<PieceOfNews> newsFromProviders = new ArrayList<>();
        for (NewsProvider provider : providers) {
            URL urlLink = provider.getRssUrl();

            if (!TextUtils.isEmpty(urlLink.toString())) {
                try {
                    // Creo connessione con URL
                    InputStream inputStream = urlLink.openConnection().getInputStream();

                    // Eseguo il parsing XML -> oggetti PieceOfNews
                    parseFeed(inputStream, provider, newsFromProviders);
                } catch (IOException e) {
                    Log.e(TAG, "Error [IO EXCEPTION] ", e);
                } catch (XmlPullParserException e) {
                    Log.e(TAG, "Error [XML PULL PARS.] ", e);
                }
            }
        }

        // Ordino notizie in base alla data di pubblicazione
        Collections.sort(newsFromProviders);
        Collections.reverse(newsFromProviders);

        // Aggiorno l'oggetto LiveData news
        news.postValue(newsFromProviders);
    }

    private void parseFeed(InputStream inputStream, NewsProvider provider, ArrayList<PieceOfNews> newsFromProviders)
            throws XmlPullParserException, IOException {
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
                        Log.d("PARSER PRE!", "Prima della checkNewsPresence su news con guid: " + guid);
                        Log.d("PARSER PRE!", guid + " --- " + newsFromProviders);
                        if (!checkNewsPresence(guid, provider.getPlatform(), newsFromProviders)) {
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
                            newsFromProviders.add(item);
                        }
                    }
                }
                eventType = xpp.next();
            }
        } finally {
            inputStream.close();
        }
    }

//    public boolean checkNewsPresence(String guid, String platform, ArrayList<PieceOfNews> alreadyPresentNews){
//        boolean added = false;
//        for(PieceOfNews article : alreadyPresentNews){
//            String articleGuid = article.getGuid();
//            if(guid.equalsIgnoreCase(articleGuid)){
//                String articleTags = article.getProvider().getPlatform();
//                if(!articleTags.contains(platform)){
//                    article.getProvider().addPlatform(platform);
//                    added = true;
//                }
//            }
//        }
//
//        return added;
//    }

    public boolean checkNewsPresence(String guid, String platform, ArrayList<PieceOfNews> newsToCheck) {
        // Controlla che non vi sia già una news uguale ma con tag diversi,
        // in tal caso aggiunge a quella
        for (PieceOfNews pieceOfNews : newsToCheck) {
            if (pieceOfNews.getGuid().equals(guid)) {
                // Se è già presente, aggiungo il tag (ammesso che questo non vi sia già)
                String tmpPlatform = pieceOfNews.getProvider().getPlatform();
                Log.d("PARSER", "Trovata news equivalente con guid: " + pieceOfNews.getGuid() +"-*-*-**-**-"+ tmpPlatform);
                Log.d("PARSER", "Tag presenti: " + pieceOfNews.getProvider().getPlatform() + "------" + pieceOfNews.getTitle() + "------" + pieceOfNews.getGuid());
                Log.d("PARSER", "Tag da aggiungere: " + platform + "------" + pieceOfNews.getTitle() + "------" + pieceOfNews.getGuid());

                if (!tmpPlatform.toLowerCase().contains(platform.toLowerCase())) {
                    pieceOfNews.getProvider().setPlatform(tmpPlatform + "," + platform);
                    return true;
                }
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
