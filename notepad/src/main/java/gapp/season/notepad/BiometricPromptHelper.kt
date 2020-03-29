package gapp.season.notepad

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import gapp.season.encryptlib.hash.HashUtil
import gapp.season.util.log.LogUtil
import gapp.season.util.task.OnTaskDone
import gapp.season.util.task.ThreadPoolExecutor
import java.security.Key
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator


//need: android.permission.USE_BIOMETRIC
object BiometricPromptHelper {
    private const val CIPHER_KEY = "gapp.season.notepad.Cipher"
    private fun log(msg: String) {
        LogUtil.d("BiometricPromptHelper", msg)
    }

    fun authenticate(context: Context, listener: OnTaskDone<String>?): Boolean {
        val executor = ThreadPoolExecutor.getInstance().executorService
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("身份验证")
                .setSubtitle(null)
                .setDescription("使用私密便签时需要验证本人的身份")
                .setNegativeButtonText("取消")
                .build()
        val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                //处理异常、主动点击取消或关闭弹窗等会回调到onAuthenticationError
                log("onAuthenticationError $errorCode $errString")
                listener?.onTaskDone(OnTaskDone.CODE_FAIL, errString.toString(), null)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                //指纹验证成功
                log("onAuthenticationSucceeded ${result.cryptoObject}")
                /*try {
                    val cipher = result.cryptoObject?.cipher
                    val data = HexUtil.toHexStr(cipher?.doFinal(CIPHER_KEY.toByteArray()))
                    listener?.onTaskDone(OnTaskDone.CODE_SUCCESS, null, data)
                } catch (e: Exception) {
                    e.printStackTrace()
                }*/
                listener?.onTaskDone(OnTaskDone.CODE_SUCCESS, null, HashUtil.md5(CIPHER_KEY))
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                //指纹验证失败
                log("onAuthenticationFailed")
                listener?.onTaskDone(OnTaskDone.CODE_STATE_1, null, null)
            }
        }
        if (context is FragmentActivity) {
            val biometricPrompt = BiometricPrompt(context, executor, authenticationCallback)
            val cipher = createCipher()
            if (cipher != null) {
                val cryptoObject = BiometricPrompt.CryptoObject(cipher)
                biometricPrompt.authenticate(promptInfo, cryptoObject)
            } else {
                biometricPrompt.authenticate(promptInfo)
            }
            return true
        }
        return false
    }

    private fun createCipher(): Cipher? {
        var cipher: Cipher? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cipher = CipherHelper.createCipher(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return cipher
    }

    //BiometricPrompt中只是包裹了一下Cipher，具体内部安全验证逻辑未知
    @TargetApi(Build.VERSION_CODES.M)
    object CipherHelper {
        // This can be key name you want. Should be unique for the app.
        private const val KEY_NAME = CIPHER_KEY
        // We always use this keystore on Android.
        private const val KEYSTORE_NAME = "AndroidKeyStore"
        // Should be no need to change these values.
        private const val KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
        private const val TRANSFORMATION = "$KEY_ALGORITHM/$BLOCK_MODE/$ENCRYPTION_PADDING"

        /**
         * 创建一个Cipher，用于 CryptoObject 的初始化
         * https://developer.android.google.cn/reference/javax/crypto/Cipher.html
         */
        @Throws(Exception::class)
        fun createCipher(retry: Boolean): Cipher {
            val keyStore = KeyStore.getInstance(KEYSTORE_NAME)
            keyStore.load(null)
            val key = getKey(keyStore)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            try {
                val opMode = Cipher.ENCRYPT_MODE //Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE
                //val parameterSpec = IvParameterSpec(iv)
                cipher.init(opMode, key)
            } catch (e: KeyPermanentlyInvalidatedException) {
                keyStore.deleteEntry(KEY_NAME)
                if (retry) {
                    return createCipher(false)
                }
                throw Exception("Could not create the cipher for CryptoObject.", e)
            }
            return cipher
        }

        @Throws(Exception::class)
        private fun getKey(keyStore: KeyStore): Key {
            if (!keyStore.isKeyEntry(KEY_NAME)) {
                createKey()
            }
            return keyStore.getKey(KEY_NAME, null)
        }

        @Throws(Exception::class)
        private fun createKey() {
            val keyGen = KeyGenerator.getInstance(KEY_ALGORITHM, KEYSTORE_NAME)
            val purposes = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            val keyGenSpec = KeyGenParameterSpec.Builder(KEY_NAME, purposes)
                    .setBlockModes(BLOCK_MODE)
                    .setEncryptionPaddings(ENCRYPTION_PADDING)
                    .setUserAuthenticationRequired(true)
                    .build()
            keyGen.init(keyGenSpec)
            keyGen.generateKey()
        }
    }
}
