package eternal.future.tefmanager

import android.content.ContentProvider
import android.os.Bundle
import android.os.ParcelFileDescriptor
import eternal.future.tefmanager.utils.AppLogger
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile

/*******************************************************************************
 * TEFManager - FileProvider
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

class FileProvider : ContentProvider() {
    companion object {
        // Call方法名称
        const val METHOD_OPEN = "open"
        const val METHOD_UNLINK = "unlink"
        const val METHOD_RMDIR = "rmdir"
        const val METHOD_MKDIR = "mkdir"
        const val METHOD_RENAME = "rename"
        const val METHOD_STAT = "stat"
        const val METHOD_ACCESS = "access"
        const val METHOD_OPENDIR = "opendir"
        const val METHOD_REALPATH = "realpath"
        const val METHOD_TRUNCATE = "truncate"

        const val METHOD_GET_INTERNAL_DIR = "getInternalDir"
        const val METHOD_GET_EXTERNAL_DIR = "getExternalDir"
        const val METHOD_GET_CACHE_DIR = "getCacheDir"
        const val METHOD_GET_EXTERNAL_CACHE_DIR = "getExternalCacheDir"

        // Bundle键名
        const val KEY_PATH = "path"
        const val KEY_PATH2 = "path2"
        const val KEY_MODE = "mode"
        const val KEY_SIZE = "size"
        const val KEY_SUCCESS = "success"
        const val KEY_RESULT = "result"
        const val KEY_FD = "fd"
        const val KEY_FILES = "files"
        const val KEY_IS_DIR = "is_dir"
        const val KEY_LAST_MODIFIED = "last_modified"
        const val KEY_FILE_SIZE = "file_size"
        const val KEY_ERRNO = "errno"
        const val KEY_DIR_PATH = "dir_path"
    }

    override fun onCreate(): Boolean {
        AppLogger.d("TEFManager FileProvider created")
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        AppLogger.d("call method: $method")

        if (extras == null) AppLogger.w("extras is null for method: $method")

        return try {
            if (extras == null) {
                when (method) {
                    METHOD_GET_INTERNAL_DIR -> handleGetInternalDir()
                    METHOD_GET_EXTERNAL_DIR -> handleGetExternalDir()
                    METHOD_GET_CACHE_DIR -> handleGetCacheDir()
                    METHOD_GET_EXTERNAL_CACHE_DIR -> handleGetExternalCacheDir()
                    else -> errorResult("Unknown method: $method", -1)
                }
            } else {
                when(method) {
                    METHOD_OPEN -> handleOpen(extras)
                    METHOD_UNLINK -> handleUnlink(extras)
                    METHOD_RMDIR -> handleRmdir(extras)
                    METHOD_MKDIR -> handleMkdir(extras)
                    METHOD_RENAME -> handleRename(extras)
                    METHOD_STAT -> handleStat(extras)
                    METHOD_ACCESS -> handleAccess(extras)
                    METHOD_OPENDIR -> handleOpendir(extras)
                    METHOD_REALPATH -> handleRealpath(extras)
                    METHOD_TRUNCATE -> handleTruncate(extras)
                    else -> errorResult("Unknown method: $method", -1)
                }
            }
        } catch (e: Exception) {
            AppLogger.e("call method failed: $method", e)
            errorResult(e.message ?: "Unknown error", -1)
        }
    }

    private fun handleGetInternalDir(): Bundle {
        return try {
            val context = context ?: return errorResult("Context is null", -1)

            Bundle().apply {
                putBoolean(KEY_SUCCESS, true)
                putString(KEY_DIR_PATH, context.filesDir.absolutePath)
                AppLogger.d("Internal dir: ${context.filesDir}")
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to get internal dir", e)
            errorResult(e.message ?: "Failed to get internal dir", -1)
        }
    }

    private fun handleGetExternalDir(): Bundle {
        return try {
            val context = context ?: return errorResult("Context is null", -1)

            // 获取外部存储的私有目录
            val externalDir = context.getExternalFilesDir(null) ?: return Bundle().apply {
                putBoolean(KEY_SUCCESS, false)
                putString(KEY_RESULT, "External storage not available")
                putInt(KEY_ERRNO, -2)
            }

            Bundle().apply {
                putBoolean(KEY_SUCCESS, true)
                putString(KEY_DIR_PATH, externalDir.absolutePath)
                AppLogger.d("External dir: ${externalDir.absolutePath}")
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to get external dir", e)
            errorResult(e.message ?: "Failed to get external dir", -1)
        }
    }

    private fun handleGetExternalCacheDir(): Bundle {
        return try {
            val context = context ?: return errorResult("Context is null", -1)

            val externalCacheDir = context.externalCacheDir ?: return Bundle().apply {
                putBoolean(KEY_SUCCESS, false)
                putString(KEY_RESULT, "External Cache not available")
                putInt(KEY_ERRNO, -2)
            }

            Bundle().apply {
                putBoolean(KEY_SUCCESS, true)
                putString(KEY_DIR_PATH, externalCacheDir.absolutePath)
                AppLogger.d("External Cache dir: $externalCacheDir")
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to get external cache dir", e)
            errorResult(e.message ?: "Failed to get cache dir", -1)
        }
    }

    private fun handleGetCacheDir(): Bundle {
        return try {
            val context = context ?: return errorResult("Context is null", -1)

            Bundle().apply {
                putBoolean(KEY_SUCCESS, true)
                putString(KEY_DIR_PATH, context.cacheDir.absolutePath)
                AppLogger.d("Cache dir: ${context.cacheDir}")
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to get cache dir", e)
            errorResult(e.message ?: "Failed to get cache dir", -1)
        }
    }

    private fun handleOpen(extras: Bundle): Bundle {
        val path = extras.getString(KEY_PATH)
        val mode = extras.getString(KEY_MODE, "r")

        if (path.isNullOrEmpty()) {
            return errorResult("path is null or empty", -1)
        }

        return try {
            val file = File(path)
            val modeBits = ParcelFileDescriptor.parseMode(mode)
            val pfd = ParcelFileDescriptor.open(file, modeBits)

            Bundle().apply {
                putBoolean(KEY_SUCCESS, true)
                putParcelable(KEY_FD, pfd)
            }
        } catch (e: FileNotFoundException) {
            AppLogger.e("File not found", e)
            errorResult("File not found", -2)
        } catch (e: Exception) {
            AppLogger.e("Open failed", e)
            errorResult(e.message ?: "Open failed", -13)
        }
    }

    private fun handleUnlink(extras: Bundle): Bundle {
        val path = extras.getString(KEY_PATH)
        if (path.isNullOrEmpty()) {
            AppLogger.e("path is null or empty")
            return errorResult("path is null or empty", -1)
        }

        val file = File(path)
        val success = file.delete()

        return Bundle().apply {
            putBoolean(KEY_SUCCESS, success)
            if (!success) {
                putInt(KEY_ERRNO, if (file.exists()) -13 else -2)
            }
        }
    }

    private fun handleRmdir(extras: Bundle): Bundle {
        return handleUnlink(extras) // 实现相同
    }

    private fun handleMkdir(extras: Bundle): Bundle {
        val path = extras.getString(KEY_PATH)
        if (path.isNullOrEmpty()) {
            return errorResult("path is null or empty", -1)
        }

        val dir = File(path)
        val success = dir.mkdirs()

        return Bundle().apply {
            putBoolean(KEY_SUCCESS, success)
            if (!success) {
                putInt(KEY_ERRNO, -13)
            }
        }
    }

    private fun handleRename(extras: Bundle): Bundle {
        val oldPath = extras.getString(KEY_PATH)
        val newPath = extras.getString(KEY_PATH2)

        if (oldPath.isNullOrEmpty() || newPath.isNullOrEmpty()) {
            return errorResult("oldPath or newPath is null or empty", -1)
        }

        val oldFile = File(oldPath)
        val newFile = File(newPath)

        // 确保新文件父目录存在
        val parent = newFile.parentFile
        parent?.takeIf { !it.exists() }?.mkdirs()

        val success = oldFile.renameTo(newFile)

        return Bundle().apply {
            putBoolean(KEY_SUCCESS, success)
            if (!success) {
                putInt(KEY_ERRNO, -13)
            }
        }
    }

    private fun handleStat(extras: Bundle): Bundle {
        val path = extras.getString(KEY_PATH)
        if (path.isNullOrEmpty()) {
            AppLogger.e("path is null or empty")
            return errorResult("path is null or empty", -1)
        }

        val file = File(path)
        if (!file.exists()) {
            AppLogger.e("File not exists")
            return errorResult("File not exists", -2)
        }

        return Bundle().apply {
            putBoolean(KEY_SUCCESS, true)
            putLong(KEY_FILE_SIZE, file.length())
            putLong(KEY_LAST_MODIFIED, file.lastModified())
            putBoolean(KEY_IS_DIR, file.isDirectory)
        }
    }

    private fun handleAccess(extras: Bundle): Bundle {
        val path = extras.getString(KEY_PATH)

        if (path.isNullOrEmpty()) {
            return errorResult("path is null or empty", -1)
        }

        val file = File(path)
        val exists = file.exists()

        return Bundle().apply {
            putBoolean(KEY_SUCCESS, exists)
            if (!exists) {
                putInt(KEY_ERRNO, -2)
            }
        }
    }

    private fun handleOpendir(extras: Bundle): Bundle {
        val path = extras.getString(KEY_PATH)
        if (path.isNullOrEmpty()) {
            return errorResult("path is null or empty", -1)
        }

        val dir = File(path)
        if (!dir.isDirectory) {
            return errorResult("Not a directory", -20)
        }

        val files = dir.list()
        if (files == null) {
            AppLogger.e("Cannot read directory")
            return errorResult("Cannot read directory", -13)
        }

        // 排序
        files.sort()

        return Bundle().apply {
            putBoolean(KEY_SUCCESS, true)
            putStringArray(KEY_FILES, files)
        }
    }

    private fun handleRealpath(extras: Bundle): Bundle {
        val path = extras.getString(KEY_PATH)
        if (path.isNullOrEmpty()) {
            return errorResult("path is null or empty", -1)
        }

        return try {
            val file = File(path)
            val canonicalPath = file.canonicalPath

            Bundle().apply {
                putBoolean(KEY_SUCCESS, true)
                putString(KEY_RESULT, canonicalPath)
            }
        } catch (e: Exception) {
            AppLogger.e("Realpath failed", e)
            errorResult(e.message ?: "Realpath failed", -2)
        }
    }

    private fun handleTruncate(extras: Bundle): Bundle {
        val path = extras.getString(KEY_PATH)
        val size = extras.getLong(KEY_SIZE, 0)

        if (path.isNullOrEmpty()) {
            return errorResult("path is null or empty", -1)
        }

        return try {
            RandomAccessFile(path, "rw").use { raf ->
                raf.setLength(size)
            }

            Bundle().apply {
                putBoolean(KEY_SUCCESS, true)
            }
        } catch (e: Exception) {
            AppLogger.e("Truncate failed", e)
            errorResult(e.message ?: "Truncate failed", -13)
        }
    }

    private fun errorResult(message: String, errno: Int): Bundle {
        return Bundle().apply {
            putBoolean(KEY_SUCCESS, false)
            putString(KEY_RESULT, message)
            putInt(KEY_ERRNO, errno)
        }
    }

    // 空实现其他必要方法
    override fun openFile(uri: android.net.Uri, mode: String): ParcelFileDescriptor? {
        return null
    }

    override fun delete(
        uri: android.net.Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0

    override fun getType(uri: android.net.Uri): String? = null

    override fun insert(
        uri: android.net.Uri,
        values: android.content.ContentValues?
    ): android.net.Uri? = null

    override fun query(
        uri: android.net.Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): android.database.Cursor? = null

    override fun update(
        uri: android.net.Uri,
        values: android.content.ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0
}