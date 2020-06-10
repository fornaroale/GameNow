package it.unimib.disco.gruppoade.gamenow.fragments.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;
import it.unimib.disco.gruppoade.gamenow.R;
import it.unimib.disco.gruppoade.gamenow.database.FbDatabase;

public class ModifyProfileFragment extends Fragment {


    private static final int PICK_IMAGE_REQUEST = 234;

    private Button salvaModifiche;
    private Button eliminaFoto;
    private Button modificaFoto;
    private EditText editUsername;
    private CircleImageView profilePicture;

    // dati
    private File dbFile;
    private Boolean eliminaFotoPremuto;
    //a Uri object to store file path
    private Uri filePath;

    public ModifyProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);




    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_modify_profile, container, false);




    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        eliminaFotoPremuto = false;
        salvaModifiche = getView().findViewById(R.id.btn_salva_modifiche);
        editUsername = getView().findViewById(R.id.edit_username);

        eliminaFoto = getView().findViewById(R.id.btn_delete);
        modificaFoto = getView().findViewById(R.id.btn_change);

        // setto la foto profilo con quella iniziale
        setProfilePhoto();

        // setto il testo dell'edit text con l'username corrente
        editUsername.setText(FbDatabase.getUserAuth().getDisplayName());

        // setto elimina foto
        eliminaFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!eliminaFotoPremuto){
                    eliminaFotoPremuto = true;
                    filePath = null;
                }

            }
        });

        modificaFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileChooser();
            }
        });



        salvaModifiche.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();

            }
        });

    }


    private void delteProfilePhotoFromDB(){
        // creo un riferimento allo storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference imagesRef = storage.getReference().child("images").child(FbDatabase.getUserAuth().getUid() + ".jpg");

        // Delete the file
        imagesRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // File deleted successfully
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Uh-oh, an error occurred!
            }
        });
    }

    private void setProfilePhoto(){
        profilePicture = getView().findViewById(R.id.profile_image_change);

        // creo un riferimento allo storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference imagesRef = storage.getReference().child("images").child(FbDatabase.getUserAuth().getUid() + ".jpg");

        dbFile = null;

        try {
            dbFile = File.createTempFile("images", "jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // se riesco a scaricare la foto profilo la mostro con picasso
        imagesRef.getFile(dbFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {

                Picasso.get()
                        .load(dbFile)
                        .fit()
                        .centerCrop()
                        .into((CircleImageView) profilePicture);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
            }
        });

    }

    //method to show file chooser
    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == -1 && data != null && data.getData() != null) {

            // prendo il persocroso della foto
            filePath = data.getData();

            // la stampo usando picasso
            if(filePath != null){
                Picasso.get()
                        .load(filePath)
                        .fit()
                        .centerCrop()
                        .into((CircleImageView) profilePicture);

                profilePicture.setVisibility(View.VISIBLE);
            }
        }
    }
}