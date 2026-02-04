package com.example.rawaaproject.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.rawaaproject.LinkToDb.DALAppWriteConnection;
import com.example.rawaaproject.R;
import com.example.rawaaproject.data.ProfileRepository;
import com.example.rawaaproject.data.SessionManager;
import com.example.rawaaproject.models.UserProfile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * صفحة حسابي: عرض الملف الشخصي، تحريره، وتغيير الصورة (كاميرا أو مكتبة).
 */
public class ProfileFragment extends Fragment {

    private SessionManager sessionManager;
    private ProfileRepository profileRepository;
    private UserProfile currentProfile;
    private Uri currentPhotoUri; // صورة محليّة لم تُرفع بعد
    private boolean isEditMode = false;

    private ImageView profilePhoto;
    private View profilePhotoPlaceholder;
    private TextView profilePhotoAddLabel;
    private View profilePhotoClick;
    private TextView profileName;
    private EditText profileNameEdit;
    private TextView profileEmail;
    private TextView profileDescription;
    private EditText profileDescriptionEdit;
    private LinearLayout profileBirthSection;
    private TextView profileBirthDate;
    private EditText profileBirthDateEdit;
    private Button profileEditBtn;
    private Button profileSaveBtn;

    private ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) return;
                Uri uri = result.getData().getData();
                if (uri != null) setPhotoFromUri(uri);
            });

    private final ActivityResultLauncher<String> requestCameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) launchCamera();
                else Toast.makeText(requireContext(), getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show();
            });

    private ActivityResultLauncher<Intent> takePicture = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != android.app.Activity.RESULT_OK) return;
                Intent data = result.getData();
                if (currentPhotoUri != null) {
                    try {
                        if (contentUriHasData(currentPhotoUri)) {
                            setPhotoFromUri(currentPhotoUri);
                            return;
                        }
                    } catch (Exception ignored) { }
                }
                if (data != null && data.getData() != null) {
                    setPhotoFromUri(data.getData());
                } else if (data != null && data.getExtras() != null && data.getExtras().get("data") instanceof android.graphics.Bitmap) {
                    saveBitmapAndSetPhoto((android.graphics.Bitmap) data.getExtras().get("data"));
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        profileRepository = new ProfileRepository(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profilePhoto = view.findViewById(R.id.profile_photo);
        profilePhotoPlaceholder = view.findViewById(R.id.profile_photo_placeholder);
        profilePhotoAddLabel = view.findViewById(R.id.profile_photo_add_label);
        profilePhotoClick = view.findViewById(R.id.profile_photo_click);
        profileName = view.findViewById(R.id.profile_name);
        profileNameEdit = view.findViewById(R.id.profile_name_edit);
        profileEmail = view.findViewById(R.id.profile_email);
        profileDescription = view.findViewById(R.id.profile_description);
        profileDescriptionEdit = view.findViewById(R.id.profile_description_edit);
        profileBirthSection = view.findViewById(R.id.profile_birth_section);
        profileBirthDate = view.findViewById(R.id.profile_birth_date);
        profileBirthDateEdit = view.findViewById(R.id.profile_birth_date_edit);
        profileEditBtn = view.findViewById(R.id.profile_edit_btn);
        profileSaveBtn = view.findViewById(R.id.profile_save_btn);

        profilePhotoClick.setOnClickListener(v -> { if (isEditMode) showPhotoSourceDialog(); });
        profilePhotoAddLabel.setOnClickListener(v -> { if (isEditMode) showPhotoSourceDialog(); });
        updatePhotoAreaClickable(false);

        profileEditBtn.setOnClickListener(v -> setEditMode(true));
        profileSaveBtn.setOnClickListener(v -> saveProfile());

        String userId = sessionManager.getUserId();
        String email = sessionManager.getUserEmail();
        if (userId == null || userId.isEmpty()) {
            profileEmail.setText(email != null ? email : "");
            profileName.setText("");
            return;
        }
        profileEmail.setText(email);
        profileRepository.getProfile(userId, result -> {
            if (result.success && result.data != null) {
                currentProfile = result.data;
                fillProfileViews(currentProfile);
            } else {
                currentProfile = new UserProfile();
                currentProfile.userId = userId;
                currentProfile.role = sessionManager.getUserRole();
                currentProfile.fullName = "";
                currentProfile.description = "";
                currentProfile.birthDate = "";
                currentProfile.photoUrl = null;
                profileName.setText("");
            }
        });
    }

    private void fillProfileViews(UserProfile p) {
        profileName.setText(p.fullName != null ? p.fullName : "");
        profileDescription.setText(p.description != null ? p.description : "");
        profileDescription.setVisibility(View.VISIBLE);
        profileDescriptionEdit.setVisibility(View.GONE);
        if ("student".equals(p.role) && p.birthDate != null && !p.birthDate.isEmpty()) {
            profileBirthSection.setVisibility(View.VISIBLE);
            profileBirthDate.setText(p.birthDate);
        } else {
            profileBirthSection.setVisibility(View.GONE);
        }
        if (p.photoUrl != null && !p.photoUrl.isEmpty()) {
            profilePhotoAddLabel.setVisibility(View.GONE);
            profilePhotoPlaceholder.setVisibility(View.GONE);
            profilePhoto.setVisibility(View.VISIBLE);
            loadProfilePhotoFromUrl(p.photoUrl, profilePhoto);
        } else {
            profilePhotoAddLabel.setVisibility(View.VISIBLE);
            profilePhotoPlaceholder.setVisibility(View.GONE);
            profilePhoto.setVisibility(View.GONE);
        }
    }

    /** تحميل الصورة من رابط (مع رؤوس Appwrite إن لزم). skipCache=true لعدم استخدام الكاش بعد تحديث الصورة. */
    private void loadProfilePhotoFromUrl(String photoUrl, ImageView into) {
        loadProfilePhotoFromUrl(photoUrl, into, false);
    }

    private void loadProfilePhotoFromUrl(String photoUrl, ImageView into, boolean skipCache) {
        if (photoUrl == null || into == null) return;
        try {
            String urlToLoad = photoUrl;
            if (skipCache && photoUrl.contains("appwrite")) {
                urlToLoad = photoUrl + (photoUrl.contains("?") ? "&" : "?") + "v=" + System.currentTimeMillis();
            }
            if (urlToLoad.contains("appwrite")) {
                GlideUrl glideUrl = new GlideUrl(urlToLoad, new LazyHeaders.Builder()
                        .addHeader("X-Appwrite-Project", DALAppWriteConnection.getProjectId())
                        .addHeader("X-Appwrite-Key", DALAppWriteConnection.getApiKey())
                        .build());
                if (skipCache) {
                    Glide.with(requireContext()).load(glideUrl).centerCrop()
                            .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into(into);
                } else {
                    Glide.with(requireContext()).load(glideUrl).centerCrop().into(into);
                }
            } else {
                if (skipCache) {
                    Glide.with(requireContext()).load(urlToLoad).centerCrop()
                            .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into(into);
                } else {
                    Glide.with(requireContext()).load(urlToLoad).centerCrop().into(into);
                }
            }
        } catch (Exception e) {
            into.setImageDrawable(null);
        }
    }

    private void setPhotoFromUri(Uri uri) {
        currentPhotoUri = uri;
        profilePhoto.setImageURI(uri);
        profilePhoto.setVisibility(View.VISIBLE);
        profilePhotoAddLabel.setVisibility(View.GONE);
        profilePhotoPlaceholder.setVisibility(View.GONE);
    }

    private void showPhotoSourceDialog() {
        String[] options = {
                getString(R.string.photo_from_camera),
                getString(R.string.photo_from_gallery)
        };
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.change_photo)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) openCamera();
                    else openGallery();
                })
                .show();
    }

    private void openGallery() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        pickImage.launch(i);
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            File dir = requireContext().getFilesDir();
            File photoFile = new File(dir, "profile_camera_" + System.currentTimeMillis() + ".jpg");
            if (!photoFile.exists()) photoFile.createNewFile();
            currentPhotoUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            takePicture.launch(intent);
        } catch (IOException e) {
            Toast.makeText(requireContext(), getString(R.string.change_photo), Toast.LENGTH_SHORT).show();
        }
    }

    /** التحقق من أن الـ Uri يحتوي على بيانات (ملف غير فارغ) */
    private boolean contentUriHasData(Uri uri) throws IOException {
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
            if (is == null) return false;
            return is.read() != -1;
        }
    }

    /** حفظ البيتماب من الكاميرا (بعض الأجهزة ترجع thumbnail فقط) وعرضه */
    private void saveBitmapAndSetPhoto(android.graphics.Bitmap bitmap) {
        try {
            File dir = requireContext().getFilesDir();
            File photoFile = new File(dir, "profile_camera_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream out = new FileOutputStream(photoFile)) {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out);
            }
            currentPhotoUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile);
            setPhotoFromUri(currentPhotoUri);
        } catch (IOException e) {
            Toast.makeText(requireContext(), getString(R.string.change_photo), Toast.LENGTH_SHORT).show();
        }
    }

    private void setEditMode(boolean edit) {
        isEditMode = edit;
        updatePhotoAreaClickable(edit);
        profileEditBtn.setVisibility(edit ? View.GONE : View.VISIBLE);
        profileSaveBtn.setVisibility(edit ? View.VISIBLE : View.GONE);
        profileName.setVisibility(edit ? View.GONE : View.VISIBLE);
        profileNameEdit.setVisibility(edit ? View.VISIBLE : View.GONE);
        profileDescription.setVisibility(edit ? View.GONE : View.VISIBLE);
        profileDescriptionEdit.setVisibility(edit ? View.VISIBLE : View.GONE);
        if (edit) {
            profileNameEdit.setText(profileName.getText());
            profileDescriptionEdit.setText(profileDescription.getText());
            if (profileBirthSection.getVisibility() == View.VISIBLE) {
                profileBirthDate.setVisibility(View.GONE);
                profileBirthDateEdit.setVisibility(View.VISIBLE);
                profileBirthDateEdit.setText(profileBirthDate.getText());
            }
        } else {
            if (profileBirthSection.getVisibility() == View.VISIBLE) {
                profileBirthDate.setVisibility(View.VISIBLE);
                profileBirthDateEdit.setVisibility(View.GONE);
            }
        }
    }

    /** تفعيل أو تعطيل النقر على منطقة الصورة (الكاميرا/المكتبة فقط في وضع التحرير) */
    private void updatePhotoAreaClickable(boolean clickable) {
        profilePhotoClick.setClickable(clickable);
        profilePhotoClick.setFocusable(clickable);
        profilePhotoAddLabel.setClickable(clickable);
    }

    private void saveProfile() {
        if (currentProfile == null) {
            currentProfile = new UserProfile();
            currentProfile.userId = sessionManager.getUserId();
            currentProfile.role = sessionManager.getUserRole();
        }
        currentProfile.fullName = profileNameEdit.getText() != null ? profileNameEdit.getText().toString().trim() : currentProfile.fullName;
        currentProfile.description = profileDescriptionEdit.getText() != null ? profileDescriptionEdit.getText().toString().trim() : "";
        if (profileBirthDateEdit.getVisibility() == View.VISIBLE) {
            currentProfile.birthDate = profileBirthDateEdit.getText() != null ? profileBirthDateEdit.getText().toString().trim() : "";
        }
        profileSaveBtn.setEnabled(false);
        profileRepository.updateProfile(currentProfile, currentPhotoUri, result -> {
            profileSaveBtn.setEnabled(true);
            if (result.success) {
                currentPhotoUri = null;
                if (result.data != null) currentProfile = result.data;
                setEditMode(false);
                fillProfileViews(currentProfile);
                if (currentProfile != null && currentProfile.photoUrl != null && !currentProfile.photoUrl.isEmpty()) {
                    loadProfilePhotoFromUrl(currentProfile.photoUrl, profilePhoto, true);
                }
                Toast.makeText(requireContext(), getString(R.string.profile_updated), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), result.message != null ? result.message : getString(R.string.save), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
