#include <jni.h>
#include <cstring>

// Mask disimpan dalam memori native, disetel setelah verifikasi server.
// Tersembunyi dari inspeksi bytecode Kotlin/Java biasa (butuh disassembler ARM).
static long cachedMask = 0L;

// Hash signature APK resmi (SHA-256), diisi saat build release.
static const unsigned char EXPECTED_SIG[32] = {
    0x00 /* TODO: isi hash signature keystore produksi saat build release */
};

extern "C" JNIEXPORT void JNICALL
Java_com_docapp_core_security_NativeBridge_setCachedMask(JNIEnv* env, jobject, jlong mask) {
    cachedMask = static_cast<long>(mask);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_docapp_core_security_NativeBridge_chk(JNIEnv* env, jobject, jint featureId) {
    return ((cachedMask >> featureId) & 1L) != 0;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_docapp_core_security_NativeBridge_expectedSig(JNIEnv* env, jobject) {
    jbyteArray result = env->NewByteArray(32);
    env->SetByteArrayRegion(result, 0, 32, reinterpret_cast<const jbyte*>(EXPECTED_SIG));
    return result;
}
