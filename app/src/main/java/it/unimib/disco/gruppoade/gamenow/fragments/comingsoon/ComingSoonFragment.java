package it.unimib.disco.gruppoade.gamenow.fragments.comingsoon;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import it.unimib.disco.gruppoade.gamenow.R;
import it.unimib.disco.gruppoade.gamenow.adapters.IncomingAdapter;
import it.unimib.disco.gruppoade.gamenow.fragments.comingsoon.utils.ApiClient;
import it.unimib.disco.gruppoade.gamenow.fragments.comingsoon.utils.Constants;
import it.unimib.disco.gruppoade.gamenow.models.Game;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.view.View.GONE;

public class ComingSoonFragment extends Fragment {

    private static final String TAG = "ComingSoonFragment";
    private ComingSoonViewModel comingSoonViewModel;

    private long todayInSecs = (new Date().getTime()/1000);

    private LottieAnimationView lottieAnimationView;
    private String body;
    private List<Game> mGames = new ArrayList<>();
    private ImageButton ps4Btn, xboxBtn, pcBtn, switchBtn;
    private Button allBtn;
    private RecyclerView recyclerView;
    private Observer<List<Game>> observer;
    private LiveData<List<Game>> gamesList;

    final Gson gson = new Gson();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_comingsoon, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        allBtn = view.findViewById(R.id.button_all);
        ps4Btn = view.findViewById(R.id.button_ps4);
        xboxBtn = view.findViewById(R.id.button_xbox);
        pcBtn = view.findViewById(R.id.button_pc);
        switchBtn = view.findViewById(R.id.button_switch);
        recyclerView = view.findViewById(R.id.recyclerview);

        comingSoonViewModel = new ViewModelProvider(requireActivity()).get(ComingSoonViewModel.class);


        observer = new Observer<List<Game>>() {
            @Override
            public void onChanged(List<Game> games) {
                Log.d(TAG, "initRecyclerView: Init RecyclerView");
                IncomingAdapter incomingAdapter = new IncomingAdapter(getActivity(), games, new IncomingAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(Game game) {
                        ComingSoonFragmentDirections.DisplayGameInfo action = ComingSoonFragmentDirections.displayGameInfo(game);
                        Navigation.findNavController(view).navigate(action);
                    }
                });
                recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                recyclerView.setAdapter(incomingAdapter);
                lottieAnimationView.setVisibility(GONE);
            }
        };

        resetBody();
        gamesList = comingSoonViewModel.getGames(body);
        gamesList.observe(getViewLifecycleOwner(), observer);

        //Buttons Listeners
        ps4Btn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                chooseButton(ps4Btn.getId());
            }
        });

        allBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                chooseButton(allBtn.getId());
            }
        });

        xboxBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi( api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                chooseButton(xboxBtn.getId());
            }
        });

        pcBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                chooseButton(pcBtn.getId());
            }
        });

        switchBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                chooseButton(switchBtn.getId());
            }
        });

        lottieAnimationView = view.findViewById(R.id.animation_view);

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void chooseButton(int buttonId){

        switch (buttonId){
            case R.id.button_ps4:
                ps4Btn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_tint));

                //reset other btn colors to off
                allBtn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_off_tint));
                xboxBtn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_off_tint));
                pcBtn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_off_tint));
                switchBtn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_off_tint));

                body ="fields name,cover.url,platforms.abbreviation,first_release_date,summary,storyline,total_rating, videos.video_id;\n" +
                        "where category = 0 & platforms= {48}& first_release_date > "+ todayInSecs +";\n" +
                        "sort first_release_date asc;\nlimit " + Constants.PAGE_SIZE + ";\n";
                retrieveJson(body);
                break;

            case R.id.button_xbox:
                xboxBtn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_tint));

                //reset other btn colors to off
                ps4Btn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_off_tint));
                allBtn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_off_tint));
                pcBtn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_off_tint));
                switchBtn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_off_tint));
                body ="fields name,cover.url,platforms.abbreviation,first_release_date,summary,storyline,total_rating, videos.video_id;\n" +
                        "where category = 0 & platforms= {49}& first_release_date > "+ todayInSecs +";\n" +
                        "sort first_release_date asc;\nlimit " + Constants.PAGE_SIZE + ";\n";
                retrieveJson(body);
                break;
            case R.id.button_pc:
                pcBtn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_tint));

                //reset other btn colors to off
                ps4Btn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_off_tint));
                xboxBtn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_off_tint));
                allBtn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_off_tint));
                switchBtn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_off_tint));
                body ="fields name,cover.url,platforms.abbreviation,first_release_date,summary,storyline,total_rating, videos.video_id;\n" +
                        "where category = 0 & platforms= {6}& first_release_date > "+ todayInSecs +";\n" +
                        "sort first_release_date asc;\nlimit " + Constants.PAGE_SIZE + ";\n";
                retrieveJson(body);
                break;
            case R.id.button_switch:
                switchBtn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_tint));

                //reset other btn colors to off
                ps4Btn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_off_tint));
                xboxBtn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_off_tint));
                pcBtn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_off_tint));
                allBtn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_off_tint));
                body ="fields name,cover.url,platforms.abbreviation,first_release_date,summary,storyline,total_rating, videos.video_id;\n" +
                        "where category = 0 & platforms= {130}& first_release_date > "+ todayInSecs +";\n" +
                        "sort first_release_date asc;\nlimit " + Constants.PAGE_SIZE + ";\n";
                retrieveJson(body);
                break;
            default:
                allBtn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_tint));

                //reset other btn colors to off
                ps4Btn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_off_tint));
                xboxBtn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_off_tint));
                pcBtn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_off_tint));
                switchBtn.setBackgroundTintList(getResources().getColorStateList(R.color.bg_off_tint));
                resetBody();
                retrieveJson(body);
        }

    }

    private void resetBody(){
        body ="fields name,cover.url,platforms.abbreviation,first_release_date,summary,storyline,total_rating, videos.video_id;\n" +
                "where category = 0 & platforms= (130,49,48,6) & first_release_date > "+ todayInSecs +";\n" +
                "sort first_release_date asc;\nlimit " + Constants.PAGE_SIZE + ";\n";
    }

    private void retrieveJson(String body){


        Call<List<Game>> call = ApiClient.getInstance().getApi().getGames(body);
        call.enqueue(new Callback<List<Game>>() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onResponse(Call<List<Game>> call, Response<List<Game>> response) {
                if(response.isSuccessful() && response.body() != null){
                    mGames.clear();
                    mGames = response.body();
                    Log.d(TAG, "onResponse: Response Body = "+ gson.toJson(mGames));
                    initRecyclerView();
                    lottieAnimationView.setVisibility(GONE);
                }
            }

            @Override
            public void onFailure(Call<List<Game>> call, Throwable t) {
                Toast.makeText(getActivity(), t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                Log.d(TAG, "onFailure: Error " + t.getLocalizedMessage());
            }
        });

    }

    private void initRecyclerView() {
        Log.d(TAG, "initRecyclerView: Init RecyclerView");

        IncomingAdapter incomingAdapter = new IncomingAdapter(getActivity(), mGames, new IncomingAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Game game) {
                ComingSoonFragmentDirections.DisplayGameInfo action = ComingSoonFragmentDirections.displayGameInfo(game);
                Navigation.findNavController(getView()).navigate(action);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(incomingAdapter);

    }

}
