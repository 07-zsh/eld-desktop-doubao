# 默认不收缩数据模型与 Room 生成代码，避免运行时反射问题
-keep class com.elder.desktop.data.model.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**
