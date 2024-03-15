package com.example.chatapp;

import static java.lang.invoke.VarHandle.AccessMode.GET;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class RegisterActivity extends AppCompatActivity {
    private CircleImageView circleimgview;
    private TextInputEditText editTextemailreg,editTextpassreg,editTextuserreg;
    private Button buttonreg;
    boolean imagecheck = false;

    FirebaseAuth auth;
    FirebaseDatabase database;
    DatabaseReference reference;

    FirebaseStorage firebaseStorage;
    StorageReference storageReference;

    Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        circleimgview = findViewById(R.id.circleImageView);
        editTextemailreg = findViewById(R.id.editxtEmailSignup);
        editTextpassreg = findViewById(R.id.editxtSignUpPassword);
        editTextuserreg = findViewById(R.id.editxtSignUpUserName);
        buttonreg = findViewById(R.id.buttonRedgister);
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        reference = database.getReference();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        circleimgview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgChooser();
            }
        });
        buttonreg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editTextemailreg.getText().toString();
                String password = editTextpassreg.getText().toString();
                String userName = editTextuserreg.getText().toString();
                if (!email.equals("") && !password.equals("") && !userName.equals("")){
                    signUp(email,password,userName);
                }
            }
        });
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
            Picasso.get().load(imageUri).into(circleimgview);
            imagecheck = true;
        }else {
            imagecheck = false;
        }
    }
    public void signUp(String email,String password,String username){
        auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    reference.child("Users").child(auth.getUid()).child("userName").setValue(username);

                    if (imagecheck) {
                        UUID randomId = UUID.randomUUID();
                        final String imageName = "image/"+ randomId +".jpg";
                        storageReference.child(imageName).putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            StorageReference myStorageRef = firebaseStorage.getReference(imageName);
                            myStorageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String filePath = uri.toString();
                                    reference.child("Users").child(auth.getUid()).child("image")
                                            .setValue(filePath).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    Toast.makeText(RegisterActivity.this,
                                                            "Write to database is successful.", Toast.LENGTH_SHORT).show();
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(RegisterActivity.this,
                                                            "Write to database is not successful.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            });
                            }
                        });
                    }
                    else {
                        reference.child("Users").child(auth.getUid()).child("image").setValue("null");
                    }
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
                else {
                    Toast.makeText(RegisterActivity.this,"An error occurred."+task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

}