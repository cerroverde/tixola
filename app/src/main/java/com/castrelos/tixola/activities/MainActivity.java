package com.castrelos.tixola.activities;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.Navigation;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.castrelos.tixola.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;



public class MainActivity extends AppCompatActivity {
    /**
     *  Init Firebase Authentication
     */
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    /**
     * Menu pestaña flotante
     */
    private DrawerLayout drawerLayout;
    private Drawable drawable;

    /**
     * Variables para el swicth del NavegationView Menu
     */
    private static final int vProfile = R.id.nav_profile;
    private static final int vLogout = R.id.nav_logout;
    private static final int vInicio = R.id.nav_inicio;
    private static final int vShare = R.id.nav_share;
    private static final int vReview = R.id.nav_rate;
    private static final int vNewPoint = R.id.nav_new_point;

    public MainActivity() {
        //TODO lista de whishlist (Add, Remove, Edit)
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        switch (getResources().getConfiguration().orientation){
            case Configuration.ORIENTATION_PORTRAIT:

                setContentView(R.layout.activity_main);
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                setContentView(R.layout.activity_main_landscape);
                break;
        }

        // Initialize toolbar
        Toolbar mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);

        final ActionBar mActionBar = getSupportActionBar();
        assert mActionBar != null;
        if (Objects.equals(mActionBar.getTitle(), "Tixola")){
            mActionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        }
        mActionBar.setDisplayHomeAsUpEnabled(true);


        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        //Init drawerLayout "menu Flotante"
        drawerLayout = findViewById(R.id.main_activity_drawer);

        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            prepararDrawer(navigationView);
            //Seleccionar item por defecto
            //seleccionarItem(navigationView.getMenu().getItem(0));

        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            Log.i("User ID", "Show User Name:" + currentUser.getEmail());
            reload();
        }else {
            startActivity(new Intent(this,AuthActivity.class));
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_newpoint, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int menuID = item.getItemId();


        switch (menuID){
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                break;

            case vInicio:
                Navigation.findNavController(this, R.id.fragmentContainerView).navigate(R.id.mainFragment);
                break;

            case vProfile:
                Navigation.findNavController(this, R.id.fragmentContainerView).navigate(R.id.profileFragment);
                break;

            case vLogout:
                FirebaseUser user = mAuth.getCurrentUser();
                mGoogleSignInClient = getGoogleSigInInstance();

                mAuth.signOut();
                mGoogleSignInClient.signOut();

                startActivity(new Intent(this, AuthActivity.class));
                finish();
                break;

            case vShare:
                sharedApp();
                break;

            case vReview:
                rateApp();
                break;

            case vNewPoint:
                Navigation.findNavController(this, R.id.fragmentContainerView).navigate(R.id.newPointFragment);
                break;
        }
        if (menuID == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
        return super.onOptionsItemSelected(item);
    }

    private void rateApp(){
        //TODO esto no se puede continuar hasta tener el acceso a Play Store
        /*
        ReviewManager manager = ReviewManagerFactory.create(this);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // We can get the ReviewInfo object
                ReviewInfo reviewInfo = task.getResult();

            } else {
                // There was some problem, log or handle the error code.
                //@ReviewErrorCode int reviewErrorCode = ((TaskException) task.getException()).getErrorCode();
            }
        });

         */
        showToast("Función no disponible en estos momentos");
    }
    private void sharedApp(){
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "VCL Media Player");
            String shareMessage= "Te recomiendo esta App\n\nTixola, Organizador de sitios de interes\n\n";
            shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=org.videolan.vlc&gl=ES";
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, "choose one"));
        } catch(Exception e) {
            Log.e("Error ", "Something goes wrong " + e.toString());
        }


    }

    /**
     * funcion necesaria para cargar la instancia de GoogleSigIn para poder cerrar la session
     * @return mGoogleSignInClient
     */
    private GoogleSignInClient getGoogleSigInInstance(){
        // Init google sign-in
        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.oAuthClientID))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        return mGoogleSignInClient;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void prepararDrawer(NavigationView navigationView) {
        for (int i=0; navigationView.getMenu().size() > i; i++){
            navigationView.getMenu().getItem(i).getTitle();

        }

        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    menuItem.setChecked(true);
                    onOptionsItemSelected(menuItem);
                    drawerLayout.closeDrawers();
                    return true;
                });

        if (getClass().getName().equals("com.castrelos.tixola.activities.MainActivity")){
            navigationView.getMenu().getItem(0).setChecked(true);
            Toast.makeText(this,"Inicio isChecked",Toast.LENGTH_LONG).show();
        }else{
            navigationView.getMenu().getItem(0).setChecked(false);
            Toast.makeText(this,"Inicio notChecked",Toast.LENGTH_LONG).show();
        }
    }

    private void reload() { }

    private void showToast(String message){
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();
    }
}