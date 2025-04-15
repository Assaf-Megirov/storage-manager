# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class com.google.gson.TypeAdapter { *; }
-keep class com.google.gson.TypeAdapterFactory { *; }
-keepnames @com.google.gson.annotations.SerializedName class *
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class com.google.gson.reflect.TypeToken
-keep class com.awindyendprod.storage_manager.model.Item {*;}
-keep class com.awindyendprod.storage_manager.model.ExportData {*;}
-keep class com.awindyendprod.storage_manager.model.Settings {*;}
-keep class com.awindyendprod.storage_manager.model.Shelf {*;}
-keep class com.awindyendprod.storage_manager.model.ShelfSection {*;}
-keep class com.awindyendprod.storage_manager.model.AppLanguage {*;}
-keep class com.awindyendprod.storage_manager.model.DateDisplayFormat {*;}
-keep class com.awindyendprod.storage_manager.model.FontSize {*;}
-keep class com.awindyendprod.storage_manager.model.SectionDateType {*;}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep,allowobfuscation class com.google.gson.TypeAdapter
-keep,allowobfuscation class com.google.gson.TypeAdapterFactory
-keep,allowobfuscation class com.google.gson.JsonSerializer
-keep,allowobfuscation class com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * implements com.google.gson.TypeAdapterFactory {
    public <init>();
}