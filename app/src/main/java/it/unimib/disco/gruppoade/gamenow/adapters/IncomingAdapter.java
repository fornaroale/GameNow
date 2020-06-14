package it.unimib.disco.gruppoade.gamenow.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.List;

import it.unimib.disco.gruppoade.gamenow.R;
import it.unimib.disco.gruppoade.gamenow.models.Game;

public class IncomingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private static final int GAME_VIEW_TYPE = 0;
    private static final int LOADING_VIEW_TYPE = 1;


    public interface OnItemClickListener{
        void onItemClick(Game game);
    }

    private static final String TAG = "Adapter";

    final Gson gson = new Gson();

    private Context mContext;
    private List<Game> mGames;
    private OnItemClickListener onItemClickListener;

    public IncomingAdapter(Context mContext, List<Game> mResults, OnItemClickListener onItemClickListener) {
        this.mContext = mContext;
        this.mGames = mResults;
        this.onItemClickListener = onItemClickListener;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if(viewType == GAME_VIEW_TYPE)
        {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_carditem, parent, false);
            return new ViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_loading_games, parent, false);
            return new LoadingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        final Game game = mGames.get(position);
        if(holder instanceof ViewHolder)
            ((ViewHolder) holder).bind(game,this.onItemClickListener);
        else if(holder instanceof LoadingViewHolder)
            ((LoadingViewHolder) holder).loadingGame.setIndeterminate(true);

    }

    @Override
    public int getItemCount() {
        if (mGames != null)
            return mGames.size();
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        if(mGames.get(position) == null)
            return LOADING_VIEW_TYPE;
        else
            return GAME_VIEW_TYPE;
    }

    public void setData(List<Game> gameList){
        if (gameList != null) {
            this.mGames = gameList;
            notifyDataSetChanged();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView itemTitle;
        ImageView imageView;
        CardView cardView;
        RecyclerView recyclerView;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.incoming_image);
            itemTitle = itemView.findViewById(R.id.incoming_title);
            cardView = itemView.findViewById(R.id.incoming_cardview);
            recyclerView = itemView.findViewById(R.id.card_recyclerview);
        }

        public void bind(final Game game, final OnItemClickListener onItemClickListener){

            Log.d(TAG, "Incoming onBindViewHolder: Game = " + game.toString());
            String gameString = gson.toJson(game.toString()).split(",")[0].replace("\"Game{", "");
            Log.d(TAG, "Incoming bind: gameString " + gameString);

            if (game.getDate() != null)
                itemTitle.setText(constructTitle(game.getName(), game.getDate()));
             else if(game.getName() == null)
                itemTitle.setText("N/A");
             else
                itemTitle.setText(game.getName());

            final String coverBig, url;

            if(game.getCover() != null) {
                coverBig = game.getCover().getUrl().replace("t_thumb", "t_cover_big");
                url = "https:" + coverBig;
                Picasso.get()
                        .load(url)
                        .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.cover_na);
                url = "";
            }

            //Console Recycler
            ConsoleAdapter consoleAdapter = new ConsoleAdapter(game.getPlatforms(),mContext);
            recyclerView.setAdapter(consoleAdapter);
            recyclerView.setLayoutManager(new GridLayoutManager(mContext, 3));

            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onItemClick(game);
                }
            });
        }
    }

    public class LoadingViewHolder extends RecyclerView.ViewHolder{

        ProgressBar loadingGame;

        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            loadingGame = itemView.findViewById(R.id.loading_game);
        }
    }

    private String constructTitle(String name, Integer date){
        long dateInMillis = date * 1000L;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        String stringDate = sdf.format(dateInMillis);
        if(name.length() > 35) {
            return name.substring(0, 35) + "... - " + stringDate;
        }
        return name + " - " + stringDate;
    }

}
