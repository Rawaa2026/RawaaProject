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
 * وحدة مسؤولة عن عمليات المصادقة فقط: تسجيل الدخول والتسجيل.
 * تستخدم DAL للاتصال بقاعدة البيانات وتنفّذ العمليات في خيط خلفي ثم تُرجع النتيجة للواجهة.
 */
public class AuthRepository {

    private static final String TABLE_PROFILES = "profiles";

    private final Context context;
    private final DALAppWriteConnection dal;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AuthRepository(Context context) {
        this.context = context.getApplicationContext();
        this.dal = new DALAppWriteConnection(this.context);
    }

    /**
     * تسجيل الدخول بالبريد وكلمة المرور.
     * الاستدعاء يتم في خيط خلفي؛ النتيجة تُعاد عبر callback على الخيط الرئيسي.
     */
    public void login(String email, String password, AuthCallback callback) {
        new Thread(() -> {
            DALAppWriteConnection.OperationResult<DALAppWriteConnection.UserData> result =
                    dal.loginUser(email, password);
            notifyOnMain(result, callback);
        }).start();
    }

    /**
     * تسجيل مستخدم جديد (مدرس أو طالب) مع الملف الشخصي.
     * الخطوات: إنشاء الحساب في Appwrite → رفع الصورة إن وُجدت → حفظ الملف الشخصي في جدول profiles.
     */
    public void register(String role, String fullName, String email, String password,
                         String description, String birthDate, Uri photoUri, AuthCallback callback) {
        new Thread(() -> {
            DALAppWriteConnection.OperationResult<DALAppWriteConnection.UserData> result = doRegister(
                    role, fullName, email, password, description, birthDate, photoUri);
            notifyOnMain(result, callback);
        }).start();
    }

    private void notifyOnMain(DALAppWriteConnection.OperationResult<DALAppWriteConnection.UserData> result,
                              AuthCallback callback) {
        mainHandler.post(() -> callback.onResult(result));
    }

    private DALAppWriteConnection.OperationResult<DALAppWriteConnection.UserData> doRegister(
            String role, String fullName, String email, String password,
            String description, String birthDate, Uri photoUri) {

        // 1) إنشاء المستخدم في Appwrite (الاسم الكامل في الحقل الأول)
        DALAppWriteConnection.OperationResult<DALAppWriteConnection.UserData> createResult =
                dal.createDefaultUser(email, password, fullName, "", "");

        if (!createResult.success || createResult.data == null) {
            return createResult;
        }

        String userId = createResult.data.userId;
        String photoUrl = null;

        // 2) رفع الصورة إن وُجدت
        if (photoUri != null) {
            byte[] bytes = readUriToBytes(photoUri);
            if (bytes != null && bytes.length > 0) {
                DALAppWriteConnection.OperationResult<DALAppWriteConnection.FileInfo> uploadResult =
                        dal.uploadFile(bytes, "profile_" + userId + ".jpg", "image/jpeg", null);
                if (uploadResult.success && uploadResult.data != null) {
                    photoUrl = uploadResult.data.fileUrl;
                }
            }
        }

        // 3) حفظ الملف الشخصي في جدول profiles
        UserProfile profile = new UserProfile(userId, role, fullName, description, birthDate, photoUrl);
        DALAppWriteConnection.OperationResult<java.util.ArrayList<UserProfile>> saveResult =
                dal.saveData(profile, TABLE_PROFILES, null);

        if (!saveResult.success) {
            return new DALAppWriteConnection.OperationResult<>(false,
                    "تم إنشاء الحساب لكن فشل حفظ الملف الشخصي: " + saveResult.message,
                    createResult.data);
        }

        return new DALAppWriteConnection.OperationResult<>(true, "تم التسجيل بنجاح", createResult.data);
    }

    /** قراءة محتوى Uri إلى مصفوفة بايت (لرفع الصورة) */
    private byte[] readUriToBytes(Uri uri) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int n;
            while ((n = is.read(chunk)) != -1) {
                buffer.write(chunk, 0, n);
            }
            return buffer.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}
