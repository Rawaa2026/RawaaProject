package com.example.rawaaproject.data;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.example.rawaaproject.LinkToDb.DALAppWriteConnection;
import com.example.rawaaproject.models.UserProfile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * وحدة مسؤولة عن جلب وتحديث الملف الشخصي من/إلى قاعدة البيانات.
 */
public class ProfileRepository {

    private static final String TABLE_PROFILES = "profiles";

    private final Context context;
    private final DALAppWriteConnection dal;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ProfileRepository(Context context) {
        this.context = context.getApplicationContext();
        this.dal = new DALAppWriteConnection(this.context);
    }

    /**
     * جلب الملف الشخصي حسب معرف المستخدم.
     */
    public void getProfile(String userId, ProfileCallback callback) {
        new Thread(() -> {
            DALAppWriteConnection.OperationResult<UserProfile> result =
                    dal.getDataById(TABLE_PROFILES, userId, null, UserProfile.class);
            mainHandler.post(() -> callback.onResult(result));
        }).start();
    }

    /**
     * تحديث الملف الشخصي. إن وُجد photoUri تُرفع الصورة أولاً ثم يُحدَّث الحقل photoUrl.
     */
    public void updateProfile(UserProfile profile, Uri photoUri, ProfileCallback callback) {
        new Thread(() -> {
            if (photoUri != null) {
                byte[] bytes = readUriToBytes(photoUri);
                if (bytes != null && bytes.length > 0) {
                    DALAppWriteConnection.OperationResult<DALAppWriteConnection.FileInfo> upload =
                            dal.uploadFile(bytes, "profile_" + profile.userId + ".jpg", "image/jpeg", null);
                    if (upload.success && upload.data != null) {
                        profile.photoUrl = upload.data.fileUrl;
                    }
                }
            }
            DALAppWriteConnection.OperationResult<UserProfile> updateResult;
            DALAppWriteConnection.OperationResult<UserProfile> existing = dal.getDataById(TABLE_PROFILES, profile.userId, null, UserProfile.class);
            if (existing != null && existing.success && existing.data != null) {
                updateResult = dal.updateData(profile, TABLE_PROFILES, profile.userId, null);
            } else {
                DALAppWriteConnection.OperationResult<java.util.ArrayList<UserProfile>> createResult = dal.saveData(profile, TABLE_PROFILES, null);
                updateResult = (createResult != null && createResult.success)
                        ? new DALAppWriteConnection.OperationResult<>(true, "تم الحفظ بنجاح", profile)
                        : new DALAppWriteConnection.OperationResult<>(false, createResult != null ? createResult.message : "فشل الحفظ", null);
            }
            mainHandler.post(() -> callback.onResult(updateResult != null ? updateResult : new DALAppWriteConnection.OperationResult<>(false, "فشل التحديث", null)));
        }).start();
    }

    private byte[] readUriToBytes(Uri uri) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int n;
            while ((n = is.read(chunk)) != -1) buffer.write(chunk, 0, n);
            return buffer.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public interface ProfileCallback {
        void onResult(DALAppWriteConnection.OperationResult<UserProfile> result);
    }
}
