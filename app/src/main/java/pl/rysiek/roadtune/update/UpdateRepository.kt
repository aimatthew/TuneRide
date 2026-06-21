package pl.rysiek.roadtune.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class UpdateRepository(private val context: Context) {

    fun check(currentVersion: String): AppRelease? {
        val connection = openConnection(LATEST_RELEASE_URL)
        val body = connection.run {
            try {
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) return null
                check(responseCode in 200..299) { "GitHub zwrócił kod $responseCode" }
                inputStream.bufferedReader().use { it.readText() }
            } finally {
                disconnect()
            }
        }

        val json = JSONObject(body)
        val tag = json.getString("tag_name").removePrefix("v")
        if (compareVersions(tag, currentVersion) <= 0) return null

        val assets = json.getJSONArray("assets")
        val apk = (0 until assets.length())
            .map { assets.getJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
            ?: error("W wydaniu $tag nie znaleziono pliku APK")

        return AppRelease(
            versionName = tag,
            title = json.optString("name").ifBlank { "TuneRide $tag" },
            notes = json.optString("body"),
            pageUrl = json.optString("html_url"),
            apkUrl = apk.getString("browser_download_url"),
            apkName = apk.getString("name"),
            sha256 = apk.optString("digest")
                .removePrefix("sha256:")
                .takeIf { it.matches(Regex("[a-fA-F0-9]{64}")) }
        )
    }

    fun download(release: AppRelease, onProgress: (Int) -> Unit): File {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        directory.listFiles()?.forEach { it.delete() }
        val target = File(directory, safeFileName(release.apkName))
        val connection = openConnection(release.apkUrl)

        try {
            check(connection.responseCode in 200..299) {
                "Pobieranie APK zakończyło się kodem ${connection.responseCode}"
            }
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        if (total > 0) onProgress(((downloaded * 100) / total).toInt())
                    }
                }
            }
        } catch (error: Throwable) {
            target.delete()
            throw error
        } finally {
            connection.disconnect()
        }

        release.sha256?.let { expected ->
            val actual = sha256(target)
            check(actual.equals(expected, ignoreCase = true)) {
                "Suma kontrolna pobranego APK jest nieprawidłowa"
            }
        }
        val archive = context.packageManager.getPackageArchiveInfo(target.absolutePath, 0)
        check(archive?.packageName == context.packageName) {
            "Pobrany APK nie należy do aplikacji TuneRide"
        }
        return target
    }

    fun requestInstall(file: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        return true
    }

    private fun openConnection(address: String): HttpURLConnection {
        return (URL(address).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "TuneRide-Android")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
    }

    private fun compareVersions(left: String, right: String): Int {
        val a = left.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val b = right.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        repeat(maxOf(a.size, b.size)) { index ->
            val result = (a.getOrNull(index) ?: 0).compareTo(b.getOrNull(index) ?: 0)
            if (result != 0) return result
        }
        return 0
    }

    private fun safeFileName(value: String): String =
        value.replace(Regex("[^A-Za-z0-9._-]"), "_").let {
            if (it.endsWith(".apk", true)) it else "TuneRide-update.apk"
        }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val REPOSITORY = "aimatthew/TuneRide"
        const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/$REPOSITORY/releases/latest"
    }
}
