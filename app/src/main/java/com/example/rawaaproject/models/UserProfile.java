package com.example.rawaaproject.models;

/**
 * نموذج بيانات الملف الشخصي للمستخدم (مدرس أو طالب).
 * يُحفظ في قاعدة البيانات في مجموعة "profiles".
 */
public class UserProfile {

    public String userId;
    /** "teacher" أو "student" */
    public String role;
    public String fullName;
    public String description;
    /** تاريخ الميلاد (للطالب) بصيغة نصية مثل 2005-03-15 */
    public String birthDate;
    /** رابط الصورة بعد الرفع إلى التخزين */
    public String photoUrl;

    public UserProfile() {
    }

    public UserProfile(String userId, String role, String fullName, String description, String birthDate, String photoUrl) {
        this.userId = userId;
        this.role = role;
        this.fullName = fullName;
        this.description = description;
        this.birthDate = birthDate;
        this.photoUrl = photoUrl;
    }
}
