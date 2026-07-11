#include <jni.h>
#include <string>
#include "blackjack_game.h"

using namespace std;

// TODO: change this to match wherever you put the BlackjackEngine.kt
// class -- slashes, not dots (e.g. "com/yourname/app/BlackjackEngine")
static const char* kEngineClassPath = "com/example/blackjackscreentime/BlackjackEngine";

// wallet lives on the C++ side; Kotlin just holds onto this pointer
// (as a jlong) and hands it back to us on every call
static BlackjackGame* asGame(jlong handle) {
    return reinterpret_cast<BlackjackGame*>(handle);
}

// packs an outcome into "resultCode|delta|walletAfter|description" so
// Kotlin can split() it into something usable without us having to
// construct a Java object from native code
static jstring outcomeToJString(JNIEnv* env, const RoundOutcome& o) {
    string packed = to_string(static_cast<int>(o.result)) + "|" +
                     to_string(o.walletDelta) + "|" +
                     to_string(o.walletAfter) + "|" +
                     o.description;
    return env->NewStringUTF(packed.c_str());
}

static jlong nativeCreate(JNIEnv*, jobject, jint startingMinutes) {
    auto* game = new BlackjackGame(startingMinutes);
    return reinterpret_cast<jlong>(game);
}

static void nativeDestroy(JNIEnv*, jobject, jlong handle) {
    delete asGame(handle);
}

static jboolean nativePlaceBet(JNIEnv*, jobject, jlong handle, jint minutes) {
    return asGame(handle)->placeBet(minutes) ? JNI_TRUE : JNI_FALSE;
}

static jstring nativeDealInitial(JNIEnv* env, jobject, jlong handle) {
    return outcomeToJString(env, asGame(handle)->dealInitial());
}

static jstring nativeHit(JNIEnv* env, jobject, jlong handle) {
    return outcomeToJString(env, asGame(handle)->hit());
}

static jstring nativeStand(JNIEnv* env, jobject, jlong handle) {
    return outcomeToJString(env, asGame(handle)->stand());
}

static void nativeNewRound(JNIEnv*, jobject, jlong handle) {
    asGame(handle)->newRound();
}

static jint nativeGetWallet(JNIEnv*, jobject, jlong handle) {
    return asGame(handle)->getWallet();
}

static jint nativeGetState(JNIEnv*, jobject, jlong handle) {
    return static_cast<jint>(asGame(handle)->getState());
}

static jboolean nativeIsBrokeOut(JNIEnv*, jobject, jlong handle) {
    return asGame(handle)->isBrokeOut() ? JNI_TRUE : JNI_FALSE;
}

static jstring nativeGetPlayerHand(JNIEnv* env, jobject, jlong handle) {
    return env->NewStringUTF(asGame(handle)->getPlayerHand().toString().c_str());
}

// revealAll = false hides everything but the dealer's first card,
// matching how a real table looks before the round is settled
static jstring nativeGetDealerHand(JNIEnv* env, jobject, jlong handle, jboolean revealAll) {
    const Hand& dealer = asGame(handle)->getDealerHand();
    if (revealAll) {
        return env->NewStringUTF(dealer.toString().c_str());
    }
    const auto& cards = dealer.getCards();
    string upCard = cards.empty() ? "?" : cards[0].toString();
    return env->NewStringUTF((upCard + " ??").c_str());
}

// method table -- name/signature has to match the `external fun`
// declarations in BlackjackEngine.kt exactly
static const JNINativeMethod kMethods[] = {
    { "nativeCreate",         "(I)J",                     (void*)nativeCreate },
    { "nativeDestroy",        "(J)V",                     (void*)nativeDestroy },
    { "nativePlaceBet",       "(JI)Z",                    (void*)nativePlaceBet },
    { "nativeDealInitial",    "(J)Ljava/lang/String;",    (void*)nativeDealInitial },
    { "nativeHit",            "(J)Ljava/lang/String;",    (void*)nativeHit },
    { "nativeStand",          "(J)Ljava/lang/String;",    (void*)nativeStand },
    { "nativeNewRound",       "(J)V",                     (void*)nativeNewRound },
    { "nativeGetWallet",      "(J)I",                     (void*)nativeGetWallet },
    { "nativeGetState",       "(J)I",                     (void*)nativeGetState },
    { "nativeIsBrokeOut",     "(J)Z",                     (void*)nativeIsBrokeOut },
    { "nativeGetPlayerHand",  "(J)Ljava/lang/String;",    (void*)nativeGetPlayerHand },
    { "nativeGetDealerHand",  "(JZ)Ljava/lang/String;",   (void*)nativeGetDealerHand },
};

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass clazz = env->FindClass(kEngineClassPath);
    if (clazz == nullptr) {
        return JNI_ERR; // class path above doesn't match BlackjackEngine.kt's location
    }

    if (env->RegisterNatives(clazz, kMethods, sizeof(kMethods) / sizeof(kMethods[0])) != JNI_OK) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}
