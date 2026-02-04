package com.example.rawaaproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.card.MaterialCardView;
import com.example.rawaaproject.data.AuthRepository;
import com.example.rawaaproject.data.SessionManager;

/**
 * شاشة التسجيل: اختيار الدور (مدرس/طالب) ثم الاسم، الصورة، الوصف، وتاريخ الميلاد للطالب.
 * مربوطة بـ AuthRepository وقاعدة البيانات.
 */
public class RegisterActivity extends AppCompatActivity {

    public static final int RESULT_REGISTER_OK = 100;

    private boolean isStudent = false;
    private Uri photoUri;

    private MaterialCardView cardTeacher;
    private MaterialCardView cardStudent;
    private EditText fullName;
    private EditText emailInput;
    private EditText passwordInput;
    private ImageView photoView;
    private View addPhotoLabel;
    private View photoClickArea;
    private EditText description;
    private LinearLayout birthDateSection;
    private EditText birthDate;
    private View continueBtn;
    private View goLogin;

    private AuthRepository authRepository;
    private SessionManager sessionManager;

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    photoUri = result.getData().getData();
                    if (photoUri != null) {
                        photoView.setImageURI(photoUri);
                        photoView.setVisibility(View.VISIBLE);
                        addPhotoLabel.setVisibility(View.GONE);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Toolbar toolbar = findViewById(R.id.toolbar_register);
        setSupportActionBar(toolbar);

        authRepository = new AuthRepository(this);
        sessionManager = new SessionManager(this);

        cardTeacher = findViewById(R.id.card_teacher);
        cardStudent = findViewById(R.id.card_student);
        fullName = findViewById(R.id.register_full_name);
        photoView = findViewById(R.id.register_photo);
        addPhotoLabel = findViewById(R.id.register_add_photo_label);
        photoClickArea = findViewById(R.id.register_photo_click_area);
        description = findViewById(R.id.register_description);
        birthDateSection = findViewById(R.id.register_birth_date_section);
        birthDate = findViewById(R.id.register_birth_date);
        continueBtn = findViewById(R.id.register_continue_btn);
        goLogin = findViewById(R.id.register_go_login);

        emailInput = findViewById(R.id.register_email);
        passwordInput = findViewById(R.id.register_password);

        cardTeacher.setOnClickListener(v -> setRole(false));
        cardStudent.setOnClickListener(v -> setRole(true));

        photoClickArea.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            pickImage.launch(i);
        });
        addPhotoLabel.setOnClickListener(v -> photoClickArea.performClick());

        continueBtn.setOnClickListener(v -> submitRegister());
        goLogin.setOnClickListener(v -> finish());
    }

    private void setRole(boolean student) {
        isStudent = student;
        int strokePx = getResources().getDimensionPixelSize(R.dimen.stroke_selected);
        cardTeacher.setStrokeWidth(student ? 0 : strokePx);
        cardStudent.setStrokeWidth(student ? strokePx : 0);
        if (student) {
            birthDateSection.setVisibility(View.VISIBLE);
            description.setHint(R.string.description_hint_student);
        } else {
            birthDateSection.setVisibility(View.GONE);
            description.setHint(R.string.description_hint_teacher);
        }
    }

    private void submitRegister() {
        String name = fullName.getText() != null ? fullName.getText().toString().trim() : "";
        String email = emailInput != null && emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput != null ? passwordInput.getText().toString() : "";
        if (name.isEmpty()) {
            Toast.makeText(this, getString(R.string.full_name_hint), Toast.LENGTH_SHORT).show();
            return;
        }
        if (email.isEmpty()) {
            Toast.makeText(this, getString(R.string.email), Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, getString(R.string.password_min_length), Toast.LENGTH_SHORT).show();
            return;
        }
        if (isStudent) {
            String dob = birthDate.getText() != null ? birthDate.getText().toString().trim() : "";
            if (dob.isEmpty()) {
                Toast.makeText(this, getString(R.string.birth_date_hint), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String role = isStudent ? "student" : "teacher";
        String desc = description.getText() != null ? description.getText().toString().trim() : "";
        String dob = isStudent && birthDate.getText() != null ? birthDate.getText().toString().trim() : null;

        continueBtn.setEnabled(false);
        authRepository.register(role, name, email, password, desc, dob, photoUri, result -> {
            continueBtn.setEnabled(true);
            if (result.success && result.data != null) {
                sessionManager.saveLogin(result.data.userId, result.data.email, role);
                setResult(RESULT_REGISTER_OK);
                finish();
            } else {
                Toast.makeText(this, result.message != null ? result.message : getString(R.string.register_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
