package com.castrelos.tixola.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.castrelos.tixola.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static SharedPreferences mPrefUser;
    private static final String mDataUser = "user_data";

    private static final int RC_SIGN_IN = 1213;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set Content View
        setContentView(R.layout.activity_auth);

        // Preferencias user_data
        mPrefUser = this
                .getSharedPreferences(mDataUser, MODE_PRIVATE);

        // Init Google SigIn Button
        SignInButton googleSignInBtn = (SignInButton) findViewById(R.id.sign_in_button);
        // Activate views
        EditText username = findViewById(R.id.et_username);
        EditText password = findViewById(R.id.et_password);
        TextView login = findViewById(R.id.btn_login);

        // Init google sign-in
        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.oAuthClientID))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        //OnClick FireBase Login button
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user = username.getText().toString();
                String pass = password.getText().toString();

                signIn(user,pass);
            }
        });

        //OnClick Google SignIn Button
        googleSignInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signInGoogle();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check for existing Google Sign In account or Firebase user logIn
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser != null){
            updateUIFirebase(currentUser);
        }else if (account != null){
            updateUIGoogle(account);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if( requestCode == RC_SIGN_IN){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("oAuth2", "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account);

            } catch (ApiException e){
                // Google Sign In failed, update UI appropriately
                Log.w("oAuth2", "Google sign in failed", e);
            }
        }
    }

    private void signInGoogle(){
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signIn(String email, String password) {
        // Init SharedPreferences Editor
        SharedPreferences.Editor editor = mPrefUser.edit();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("FireBase SignIn", "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            assert user != null;
                            editor.putString("UserName", user.getDisplayName());
                            editor.putString("UserMail", user.getEmail());
                            editor.putString("UserPhone", user.getPhoneNumber());
                            editor.putString("UserPhotoUrl", String.valueOf(user.getPhotoUrl()));
                            editor.apply();
                            updateUIFirebase(user);

                            Log.e("URL PHOTO", "URL PHOTO USER"+ user.getPhotoUrl());

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("FireBase SignIn Fail", "signInWithEmail:failure", task.getException());

                            Toast.makeText(AuthActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUIFirebase(null);
                        }
                    }
                });
        // [END sign_in_with_email]
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount idToken){
        // Init SharedPreferences Editor
        SharedPreferences.Editor editor = mPrefUser.edit();
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken.getIdToken(),null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("oAuth2", "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            assert user != null;
                            editor.putString("UserName", user.getDisplayName());
                            editor.putString("UserMail", user.getEmail());
                            editor.putString("UserPhone", user.getPhoneNumber());
                            editor.putString("UserPhotoUrl", String.valueOf(user.getPhotoUrl()));
                            editor.apply();

                            updateUIFirebase(user);

                            Log.e("URL PHOTO", "URL PHOTO USER"+ user.getPhotoUrl());
                        }else{
                            // If sign in fails, display a message to the user.
                            Log.w("oAuth2", "signInWithCredential:failure", task.getException());
                            updateUIFirebase(null);
                        }
                    }
                });
    }

    private void updateUIGoogle(GoogleSignInAccount user){
        startActivity(new Intent(AuthActivity.this, MainActivity.class)
                .putExtra("user", user.getDisplayName())
        );
        finish();
    }

    private void updateUIFirebase(FirebaseUser user) {
        if (user != null){

            startActivity(new Intent(AuthActivity.this, MainActivity.class)
                    .putExtra("user", user.getDisplayName())
            );
            finish();
        }
    }
}
