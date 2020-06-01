package it.unimib.disco.gruppoade.gamenow.handlers;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

import it.unimib.disco.gruppoade.gamenow.models.FbDatabase;

public class FirebaseHandler extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        FbDatabase.FbDatabase();
    }
}
