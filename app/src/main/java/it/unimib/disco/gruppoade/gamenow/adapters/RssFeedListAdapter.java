package it.unimib.disco.gruppoade.gamenow.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import it.unimib.disco.gruppoade.gamenow.R;
import it.unimib.disco.gruppoade.gamenow.models.PieceOfNews;
import it.unimib.disco.gruppoade.gamenow.models.User;

public class RssFeedListAdapter extends RecyclerView.Adapter<RssFeedListAdapter.FeedModelViewHolder> {

    private final FragmentActivity mContext;
    private List<PieceOfNews> mRssFeedModels;
    private User user;
    private List<PieceOfNews> locallySavedNews;
    private final static String TAG = "RssFeedListAdapter";

    public RssFeedListAdapter(Context mContext, List<PieceOfNews> rssFeedModels, User user, List<PieceOfNews> locallySavedNews) {
        this.mContext = (FragmentActivity) mContext;
        this.mRssFeedModels = rssFeedModels;
        this.user = user;
        this.locallySavedNews = locallySavedNews;
    }

    @Override
    public FeedModelViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.layout_singlenews_card, parent, false);
        return new FeedModelViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final FeedModelViewHolder holder, int position) {
        final PieceOfNews rssFeedModel = mRssFeedModels.get(position);

        // Immagine
        String imgUrl = rssFeedModel.getImage();
        if (!imgUrl.isEmpty())
            Picasso.get()
                    .load(imgUrl)
                    .fit()
                    .centerCrop()
                    .into((ImageView) holder.rssFeedView.findViewById(R.id.newsImage));

        // Titolo
        ((TextView) holder.rssFeedView.findViewById(R.id.newsTitle)).setText(rssFeedModel.getTitle());

        // Provider della notizia
        ((TextView) holder.rssFeedView.findViewById(R.id.newsProvider)).setText(rssFeedModel.getProvider().getName());

        // Descrizione
        String plainDesc = Html.fromHtml(rssFeedModel.getDesc().replaceAll("<img.+/(img)*>", "")).toString();
        ((TextView) holder.rssFeedView.findViewById(R.id.newsDesc)).setText(plainDesc);

        // Data di pubblicazione
        LocalDateTime osDateTime = rssFeedModel.getPubDate();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yy, HH:mm");
        ((TextView) holder.rssFeedView.findViewById(R.id.newsPubDate)).setText(dtf.format(osDateTime));

        // Configurazione link
        holder.rssFeedView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(rssFeedModel.getLink()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                view.getContext().startActivity(intent);
            }
        });

        // ToggleButton bookmark
        final ToggleButton favButton = holder.rssFeedView.findViewById(R.id.saveNewsImg);
        if(checkSavedPieceofNews(locallySavedNews, rssFeedModel))
            favButton.setChecked(true);
        else
            favButton.setChecked(false);

        favButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(favButton.isPressed()) {
                    if (isChecked) {
                        user.savePieceOfNews(locallySavedNews, mRssFeedModels.get(holder.getAdapterPosition()));
                        Log.d(TAG, "NEWS: Notizie salvate in locale: " + locallySavedNews.size());
                        Snackbar snackbar = Snackbar.make(buttonView, "News salvata!", Snackbar.LENGTH_LONG);
                        snackbar.show();
                    } else {
                        user.removeSavedPieceOfNews(locallySavedNews, mRssFeedModels.get(holder.getAdapterPosition()));
                        Snackbar snackbar = Snackbar.make(buttonView, "News rimossa da 'News salvate'", Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                }
            }
        });

        // News' tags chips
        ChipGroup chipGroup = holder.rssFeedView.findViewById(R.id.newsTagsChipGroup);
        chipGroup.removeAllViews();
        for (String newsTag : rssFeedModel.getProvider().getPlatform().split(",")) {
            final Chip chip = (Chip) LayoutInflater.from(mContext).inflate(R.layout.chip_tag_layout, chipGroup, false);
            chip.setText(newsTag);
            chipGroup.addView(chip);
            setNewsTagsIcon(chip, holder);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String chipTagText = chip.getText().toString();
                    if (user.getTags().contains(chipTagText)) {
                        user.removeTag(chipTagText);
                        chip.setChipIcon(ContextCompat.getDrawable(view.getContext(), R.drawable.heart));
                        Snackbar snackbar = Snackbar.make(view, "Tag rimosso dai tag utente!", Snackbar.LENGTH_LONG);
                        snackbar.show();
                    } else {
                        user.addTag(chipTagText);
                        chip.setChipIcon(ContextCompat.getDrawable(view.getContext(), R.drawable.heart_pressed));
                        Snackbar snackbar = Snackbar.make(view, "Tag aggiunto ai tag utente!", Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                }
            });
        }
    }

    private void setNewsTagsIcon(Chip chip, FeedModelViewHolder holder) {
        if (user.getTags().contains(chip.getText().toString())) {
            chip.setChipIcon(ContextCompat.getDrawable(holder.rssFeedView.getContext(), R.drawable.heart_pressed));
        } else {
            chip.setChipIcon(ContextCompat.getDrawable(holder.rssFeedView.getContext(), R.drawable.heart));
        }
    }

    private boolean checkSavedPieceofNews(List<PieceOfNews> locallySavedNews, PieceOfNews PON){
        Boolean savedNews = false;
        for(PieceOfNews pon : locallySavedNews){
            if(pon.equals(PON))
                savedNews = true;
        }
        return savedNews;
    }

    @Override
    public int getItemCount() {
        return mRssFeedModels.size();
    }

    public static class FeedModelViewHolder extends RecyclerView.ViewHolder {
        private View rssFeedView;

        public FeedModelViewHolder(View v) {
            super(v);
            rssFeedView = v;
        }
    }
}