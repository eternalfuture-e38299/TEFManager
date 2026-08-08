package eternal.future.tefmanager.utils

import com.android.tools.build.apkzlib.zip.ZFile
import com.android.tools.build.apkzlib.zip.ZFileOptions
import com.apk.axml.aXMLDecoder
import com.apk.axml.aXMLEncoder
import com.wind.meditor.core.ManifestEditor
import com.wind.meditor.property.AttributeItem
import com.wind.meditor.property.ModificationProperty
import eternal.future.tefmanager.MainActivity
import eternal.future.tefmanager.strings.StringsResource.Strings
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/*******************************************************************************
 * TEFManager - Patcher
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

object Patcher {
    enum class PatchProgress {
        PREPARING,
        OPENING_APK,
        UNZIP_DEX,
        READING_MANIFEST,
        MODIFYING_APPLICATION,
        MODIFYING_APP_COMPONENT_FACTORY,
        ADDING_METADATA,
        ADDING_PROVIDER,
        REPLACING_MANIFEST,
        ADDING_DEX,
        REPACKAGING_APK,
        SIGNING_APK,
        COMPLETED,
        FAILED;

        override fun toString(): String = when (this) {
            PREPARING -> Strings.home.patch.progress.preparing
            OPENING_APK -> Strings.home.patch.progress.openApk
            UNZIP_DEX -> Strings.home.patch.progress.unzipDex
            READING_MANIFEST -> Strings.home.patch.progress.readingManifest
            MODIFYING_APPLICATION -> Strings.home.patch.progress.modifyingApplication
            MODIFYING_APP_COMPONENT_FACTORY -> Strings.home.patch.progress.modifyingAppComponentFactory
            ADDING_METADATA -> Strings.home.patch.progress.addingMetadata
            ADDING_PROVIDER -> Strings.home.patch.progress.addingProvider
            REPLACING_MANIFEST -> Strings.home.patch.progress.replacingManifest
            ADDING_DEX -> Strings.home.patch.progress.addingDex
            REPACKAGING_APK -> Strings.home.patch.progress.repackagingApk
            SIGNING_APK -> Strings.home.patch.progress.signingApk
            COMPLETED -> Strings.home.patch.progress.completed
            FAILED -> Strings.home.patch.progress.failed
        }
    }

    enum class PatchOption {
        APPLICATION,
        APP_COMPONENT_FACTORY
    }

    fun patch(apkPath: String, option: PatchOption, progressCallback: (PatchProgress, String?) -> Unit) {
        progressCallback(PatchProgress.PREPARING, null)
        AppLogger.i("Starting patch for APK: $apkPath")

        val apkFile = File(apkPath)
        AppLogger.i("APK file exists: ${apkFile.exists()}, size: ${apkFile.length()}")

        try {
            progressCallback(PatchProgress.OPENING_APK, null)
            ZFile.openReadWrite(apkFile, ZFileOptions()).use { dstZFile ->
                progressCallback(PatchProgress.UNZIP_DEX, null)
                val dexCount = getDexCount(dstZFile)
                AppLogger.i("Current dex count: $dexCount")

                val manifestEntry = dstZFile.get("AndroidManifest.xml")
                if (manifestEntry == null) {
                    AppLogger.e("AndroidManifest.xml not found in APK")
                    progressCallback(PatchProgress.FAILED, "AndroidManifest.xml not found in APK")
                    return@use
                }

                progressCallback(PatchProgress.READING_MANIFEST, null)
                manifestEntry.open().use { manifestStream ->
                    progressCallback(when(option) {
                        PatchOption.APPLICATION -> PatchProgress.MODIFYING_APPLICATION
                        PatchOption.APP_COMPONENT_FACTORY -> PatchProgress.MODIFYING_APP_COMPONENT_FACTORY
                    }, null)

                    progressCallback(PatchProgress.ADDING_METADATA, null)
                    progressCallback(PatchProgress.ADDING_PROVIDER, null)

                    val manifest = modifyManifest(manifestStream, option)
                    AppLogger.i("Manifest modified, size: ${manifest.size}")

                    progressCallback(PatchProgress.REPLACING_MANIFEST, null)
                    dstZFile.add("AndroidManifest.xml", ByteArrayInputStream(manifest))
                    AppLogger.i("Replaced AndroidManifest.xml")

                    progressCallback(PatchProgress.ADDING_DEX, null)
                    val dexAsset = MainActivity.context?.assets?.open("tefloader.dex")
                    if (dexAsset == null) {
                        AppLogger.e("tefloader.dex not found in assets")
                        progressCallback(PatchProgress.FAILED, "tefloader.dex not found in assets")
                        return@use
                    }

                    dstZFile.add("classes$dexCount.dex", dexAsset)
                    AppLogger.i("Added classes$dexCount.dex")
                }

                progressCallback(PatchProgress.REPACKAGING_APK, null)
            }

            AppLogger.i("APK patching completed, starting signing...")
            progressCallback(PatchProgress.SIGNING_APK, null)

            if (!ApkSigner(MainActivity.context!!).signApkInPlace(apkFile)) {
                AppLogger.e("Signature failed")
                progressCallback(PatchProgress.FAILED, "Signature failed")
            } else {
                AppLogger.i("Signature successful")
                progressCallback(PatchProgress.COMPLETED, null)
            }

        } catch (e: Exception) {
            AppLogger.e("Patching failed", e)
            progressCallback(PatchProgress.FAILED, e.toString())
        }
    }

    private fun modifyManifest(inputStream: InputStream, option: PatchOption): ByteArray {
        AppLogger.i("Starting manifest modification")

        try {
            val axmlStr = aXMLDecoder(inputStream).decodeAsString()
            AppLogger.d("Manifest decoded, length: ${axmlStr.length}")

            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()

            val document = builder.parse(ByteArrayInputStream(axmlStr.toByteArray(Charsets.UTF_8)))
            document.documentElement.normalize()
            AppLogger.d("Manifest parsed successfully")

            val property = ModificationProperty()

            addProvider(document)

            property.addApplicationAttribute(
                AttributeItem(
                    if (option == PatchOption.APPLICATION) "name" else "appComponentFactory",
                    "eternal.future.tefkernel.${if (option == PatchOption.APPLICATION) "TefLoaderApplication" else "TefLoaderAppComponentFactory"}")
            )

            addMetaData(document, "TEFManager-Patch", "true")
            addMetaData(document, "TEFManager-Patch-Version", "1.0.0")

            AppLogger.d("Manifest modifications applied")

            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
            transformer.setOutputProperty(OutputKeys.METHOD, "xml")
            transformer.setOutputProperty(OutputKeys.INDENT, "no")
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            val writer = StringWriter()
            transformer.transform(DOMSource(document), StreamResult(writer))
            val result = writer.toString()
            AppLogger.d("Modified manifest XML generated, length: ${result.length}")

            val encoded = aXMLEncoder().encodeString(result, MainActivity.context!!)
            AppLogger.d("Manifest encoded to binary AXML, size: ${encoded.size}")

            encoded.inputStream().use {
                ByteArrayOutputStream().use { os ->
                    ManifestEditor(it, os, property).processManifest()
                    return os.toByteArray()
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to modify manifest", e)
            throw e
        }
    }

    private fun addMetaData(document: Document, name: String, value: String) {
        val manifest: Element = document.documentElement
        val application = manifest.getElementsByTagName("application").item(0) as? Element

        val metaData = document.createElement("meta-data")
        metaData.setAttribute("android:name", name)
        metaData.setAttribute("android:value", value)

        application?.appendChild(metaData)
    }

    private fun addProvider(document: Document) {
        val queries: Element = document.createElement("queries")
        val provider: Element = document.createElement("provider")
        provider.setAttribute("android:authorities", "eternal.future.tefkernel.fileprovider")
        queries.appendChild(provider)

        val manifest: Element = document.documentElement
        val application = manifest.getElementsByTagName("application").item(0) as Element?
        manifest.insertBefore(queries, application)
    }

    private fun getDexCount(zFile: ZFile): Int {
        return zFile.entries()
            .count { entry ->
                val name = entry.centralDirectoryHeader.name
                name.startsWith("classes") && name.endsWith(".dex")
            } + 1
    }
}