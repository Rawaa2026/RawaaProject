package com.example.rawaaproject.data;

import com.example.rawaaproject.LinkToDb.DALAppWriteConnection;

/**
 * واجهة بسيطة لإرجاع نتيجة عملية المصادقة (تسجيل دخول أو تسجيل) إلى واجهة المستخدم.
 */
public interface AuthCallback {

    /**
     * تُستدعى عند انتهاء العملية (نجاح أو فشل).
     * يُفضّل استدعاؤها من الخيط الرئيسي (UI thread).
     */
    void onResult(DALAppWriteConnection.OperationResult<DALAppWriteConnection.UserData> result);
}
