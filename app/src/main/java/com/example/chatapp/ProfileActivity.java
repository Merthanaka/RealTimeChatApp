package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.Objects;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {
    private CircleImageView imageViewCircleProfile;
    private TextInputEditText editTextUsername;
    private Button buttonEditProfile;

    FirebaseDatabase database;
    DatabaseReference reference;
    FirebaseAuth auth;
    FirebaseUser firebaseUser;
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;

    Uri imageUri;
    boolean imageControl = false;
    String image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        imageViewCircleProfile = findViewById(R.id.circleImageViewProfile);
        editTextUsername = findViewById(R.id.editxtProfileUserName);
        buttonEditProfile = findViewById(R.id.buttonUpdate);

        database = FirebaseDatabase.getInstance();
        reference = database.getReference();
        auth = FirebaseAuth.getInstance();
        firebaseUser = auth.getCurrentUser();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        if (firebaseUser == null) {
            // Kullanıcı oturumu açmamışsa, oturum açma ekranına yönlendir
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
            finish(); // ProfileActivity'i kapat
        } else {
            // Kullanıcı oturumu açıksa, profil bilgilerini getir
            getUserinfo();
            imageViewCircleProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    imgChooser();
                }
            });

            buttonEditProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateProfile();
                }
            });
        }
    }

    public void imgChooser(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction (Intent.ACTION_GET_CONTENT);
        startActivityForResult (intent,1);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultcode, @Nullable Intent data){
        super.onActivityResult(requestCode,requestCode,data);
        if (requestCode == 1 && resultcode == RESULT_OK && data != null){
            imageUri = data.getData();
            Picasso.get().load(imageUri).into(imageViewCircleProfile);
            imageControl = true;
        }else {
            imageControl = false;
        }
    }
    public void updateProfile(){
        String userName = Objects.requireNonNull(editTextUsername.getText()).toString();
        reference.child("Users").child(firebaseUser.getUid()).child("userName").setValue(userName);
        if (imageControl){
            UUID randomId = UUID.randomUUID();
            final String imageName = "image/"+ randomId +".jpg";
            storageReference.child(imageName).putFile(imageUri).addOnSuccessListener
                    (new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    StorageReference myStorageRef = firebaseStorage.getReference(imageName);
                    myStorageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String filePath = uri.toString();
                            reference.child("Users").child(Objects.requireNonNull(auth.getUid())).child("image")
                                    .setValue(filePath).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            Toast.makeText(ProfileActivity.this,
                                                    "Write to database is successful.", Toast.LENGTH_SHORT).show();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(ProfileActivity.this,
                                                    "Write to database is not successful.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    });
                }
            });
        }
        else {
            reference.child("Users").child(Objects.requireNonNull(auth.getUid())).child("image").setValue(image);
        }
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.putExtra("userName", userName);
        startActivity(intent);
        finish();
    }
    public void getUserinfo() {
        reference.child("Users").child(firebaseUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("userName").getValue(String.class);
                    image = snapshot.child("image").getValue(String.class);
                    if (name != null && image != null && !image.equals("null")) {
                        editTextUsername.setText(name);
                        Picasso.get().load(image).into(imageViewCircleProfile);
                    }else {
                        imageViewCircleProfile.setImageResource(R.drawable.emtpp);
                    }
                } else {
                    // Veri bulunamadı veya snapshot boşsa, varsayılan değerler atanabilir veya hata mesajı gösterilebilir.
                    Toast.makeText(ProfileActivity.this, "Data not found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Hata durumunda yapılacak işlemler buraya yazılabilir.
                Toast.makeText(ProfileActivity.this, "Data could not get: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}