package it.unimib.disco.gruppoade.gamenow.fragments.comingsoon;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


import java.util.List;

import it.unimib.disco.gruppoade.gamenow.models.Game;
import it.unimib.disco.gruppoade.gamenow.repositories.GameRepository;

public class SearchViewModel extends ViewModel {

    private static final String TAG = "ComingSoonViewModel";

    private MutableLiveData<List<Game>> mGames;
    private int offset = 0;
    private int currentResults;
    private boolean isLoading;


    public LiveData<List<Game>> getGames(String body) {
        if(mGames == null){
            mGames = new MutableLiveData<>();
            GameRepository.getInstance().getGames(mGames, body);
        }
        return mGames;
    }

    public MutableLiveData<List<Game>> getmGamesLiveData(){
        return mGames;
    }

    public LiveData<List<Game>> getMoreGames(String body) {
        GameRepository.getInstance().getGames(mGames, body);
        return mGames;
    }

    public LiveData<List<Game>> resetGames() {
        mGames = null;
        return mGames;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getCurrentResults() {
        return currentResults;
    }

    public void setCurrentResults(int currentResults) {
        this.currentResults = currentResults;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
    }

    public void setmGames(MutableLiveData<List<Game>> mGames) {
        this.mGames = mGames;
    }

    @Override
    public String toString() {
        return "ComingSoonViewModel{" +
                "mGames=" + mGames +
                ", offset=" + offset +
                ", currentResults=" + currentResults +
                ", isLoading=" + isLoading +
                '}';
    }
}