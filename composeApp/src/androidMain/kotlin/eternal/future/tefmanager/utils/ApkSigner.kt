package eternal.future.tefmanager.utils

import android.content.Context
import com.android.apksig.ApkSigner
import com.android.apksig.KeyConfig
import java.io.File
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

/*******************************************************************************
 * TEFManager - ApkSigner
 * Copyright (C) 2026 eternalfuture-e38299
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Author: eternalfuture-e38299
 * GitHub: https://github.com/eternalfuture-e38299
 * Created: 2026/2/27
 *******************************************************************************/

class ApkSigner(private val context: Context) {

    companion object {
        private const val STORE_PASSWORD = "EternalFuture@2026"
        private const val KEY_ALIAS = "TEFManager"
        private const val KEY_PASSWORD = "EternalFuture@2026"
        private const val KEYSTORE_FILE = "tefmanager.bks"
    }

    fun signApkInPlace(apkFile: File): Boolean {
        return try {
            val tempFile = File.createTempFile("temp_signed", ".apk", apkFile.parentFile)
            val success = signApk(apkFile, tempFile)

            if (success) {
                if (apkFile.delete()) {
                    if (tempFile.renameTo(apkFile)) {
                        return true
                    } else {
                        tempFile.renameTo(apkFile)
                        return false
                    }
                } else {
                    tempFile.delete()
                    return false
                }
            } else {
                tempFile.delete()
                return false
            }

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun signApk(inputApk: File, outputApk: File): Boolean {
        return try {
            val keyStore = loadBksKeyStore()

            val privateKey = keyStore.getKey(KEY_ALIAS, KEY_PASSWORD.toCharArray()) as PrivateKey
            val certificateChain = keyStore.getCertificateChain(KEY_ALIAS)
                .map { it as X509Certificate }
                .toTypedArray()

            val signerConfig = ApkSigner.SignerConfig.Builder(
                KEY_ALIAS,
                KeyConfig.Jca(privateKey),
                certificateChain.asList()
            ).build()

            val apkSigner = ApkSigner.Builder(listOf(signerConfig))
                .setInputApk(inputApk)
                .setOutputApk(outputApk)
                .setV1SigningEnabled(false)  // 启用 V1 签名
                .setV2SigningEnabled(true)  // 启用 V2 签名
                .setV3SigningEnabled(false)  // 启用 V3 签名
                .setMinSdkVersion(24)
                .build()

            apkSigner.sign()
            true

        } catch (e: Exception) {
            AppLogger.e("Sign apk failed: ${e.message}", e)  // 添加日志记录
            false
        }
    }

    private fun loadBksKeyStore(): KeyStore {
        val keyStore = KeyStore.getInstance("BKS")
        context.assets.open(KEYSTORE_FILE).use { inputStream ->
            keyStore.load(inputStream, STORE_PASSWORD.toCharArray())
        }
        return keyStore
    }
}