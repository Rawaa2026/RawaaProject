package com.example.rawaaproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.widget.Toolbar;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.rawaaproject.data.AuthRepository;
import com.example.rawaaproject.data.SessionManager;
import com.example.rawaaproject.ui.HomeFragment;
import com.example.rawaaproject.ui.MessagesFragment;
import com.example.rawaaproject.ui.ProfileFragment;
import com.example.rawaaproject.ui.SearchFragment;

public class MainActivity extends AppCompatActivity {

    private FrameLayout contentFrame;
    private BottomNavigationView bottomNav;
    private View authView;
    private Toolbar toolbarMain;

    private SessionManager sessionManager;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        authRepository = new AuthRepository(this);

        toolbarMain = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbarMain);
        contentFrame = findViewById(R.id.content_frame);
        bottomNav = findViewById(R.id.bottom_nav);

        if (sessionManager.isLoggedIn()) {
            showMainContent();
        } else {
            showAuth();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn() && authView != null) {
            showMainContent();
        }
    }

    private void showAuth() {
        if (toolbarMain != null) toolbarMain.setTitle(R.string.login);
        bottomNav.setVisibility(View.GONE);
        contentFrame.removeAllViews();
        authView = getLayoutInflater().inflate(R.layout.layout_auth, contentFrame, false);
        contentFrame.addView(authView);

        EditText email = authView.findViewById(R.id.auth_email);
        EditText password = authView.findViewById(R.id.auth_password);
        Button loginBtn = authView.findViewById(R.id.auth_login_btn);
        View goRegister = authView.findViewById(R.id.auth_go_register);

        loginBtn.setOnClickListener(v -> {
            String e = email.getText() != null ? email.getText().toString().trim() : "";
            String p = password.getText() != null ? password.getText().toString() : "";
            if (e.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, getString(R.string.email) + " / " + getString(R.string.password), Toast.LENGTH_SHORT).show();
                return;
            }
            loginBtn.setEnabled(false);
            authRepository.login(e, p, result -> {
                loginBtn.setEnabled(true);
                if (result.success && result.data != null) {
                    sessionManager.saveLogin(result.data.userId, result.data.email, "");
                    showMainContent();
                } else {
                    Toast.makeText(this, result.message != null ? result.message : getString(R.string.login), Toast.LENGTH_SHORT).show();
                }
            });
        });

        goRegister.setOnClickListener(v -> startActivityForResult(new Intent(this, RegisterActivity.class), 200));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RegisterActivity.RESULT_REGISTER_OK) {
            showMainContent();
        }
    }

    private void showMainContent() {
        if (authView != null) {
            contentFrame.removeAllViews();
            authView = null;
        }
        bottomNav.setVisibility(View.VISIBLE);

        if (getSupportFragmentManager().findFragmentById(R.id.content_frame) == null) {
            setToolbarTitle(R.string.nav_home);
            showFragment(new HomeFragment());
        }
        bottomNav.setSelectedItemId(R.id.nav_home);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { setToolbarTitle(R.string.nav_home); showFragment(new HomeFragment()); }
            else if (id == R.id.nav_search) { setToolbarTitle(R.string.nav_search); showFragment(new SearchFragment()); }
            else if (id == R.id.nav_messages) { setToolbarTitle(R.string.nav_messages); showFragment(new MessagesFragment()); }
            else if (id == R.id.nav_profile) { setToolbarTitle(R.string.nav_profile); showFragment(new ProfileFragment()); }
            return true;
        });
    }

    private void setToolbarTitle(int titleRes) {
        if (toolbarMain != null) toolbarMain.setTitle(titleRes);
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
    }
}
