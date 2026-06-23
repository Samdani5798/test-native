# Add project specific ProGuard rules here.
# By default, the active rules are defined in the file(s) in the list above.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep JavascriptInterface methods accessible
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
