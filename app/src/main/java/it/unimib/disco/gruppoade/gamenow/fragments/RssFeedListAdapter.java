package it.unimib.disco.gruppoade.gamenow.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.List;

import it.unimib.disco.gruppoade.gamenow.R;

public class RssFeedListAdapter extends RecyclerView.Adapter<RssFeedListAdapter.FeedModelViewHolder> {

    private final FragmentActivity mContext;
    private List<PieceOfNews> mRssFeedModels;
    private final static String TAG = "RssFeedListAdapter";

    public RssFeedListAdapter(Context mContext, List<PieceOfNews> rssFeedModels) {
        this.mContext = (FragmentActivity) mContext;
        mRssFeedModels = rssFeedModels;
    }

    @Override
    public FeedModelViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.layout_singlenews_card, parent, false);
        return new FeedModelViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(FeedModelViewHolder holder, int position) {
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