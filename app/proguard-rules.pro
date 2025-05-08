# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Se seu projeto usa WebView com JavaScript, descomente e especifique a classe:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preservar informações de depuração
-keepattributes SourceFile,LineNumberTable

# Esconder nome do arquivo original
-renamesourcefileattribute SourceFile

# Manter todas as classes e métodos do Glide
-keep class com.bumptech.glide.** { *; }

# Manter todas as classes e métodos específicos do projeto
-keep class com.video.vidbr.model.UserModel { *; }
-keep class com.video.vidbr.ProfileActivity { *; }
-keep class com.video.vidbr.adapter.VideoListAdapter { *; }

# Manter todas as classes e métodos nos pacotes especificados
-keep class com.video.vidbr.model.** { *; }
-keep class com.video.vidbr.adapter.** { *; }
-keep class com.video.vidbr.viewmodel.** { *; }
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.auth.** { *; }

# Manter as informações genéricas do Gson para evitar erros com TypeToken
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

# Evitar minificação e remoção de classes Gson e Firebase necessárias
-keep class com.google.gson.** { *; }
-keep class com.google.firebase.** { *; }

# Melhorar compatibilidade com ViewPager2 e RecyclerView
-keep class androidx.viewpager2.** { *; }
-keep class androidx.recyclerview.** { *; }
