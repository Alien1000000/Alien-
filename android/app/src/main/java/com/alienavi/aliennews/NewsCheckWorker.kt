package com.alienavi.aliennews

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NewsCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repository = NewsRepository(applicationContext)
        if (!repository.notificationsEnabled()) return@withContext Result.success()
        runCatching {
            val article = repository.fetch(repository.language()).firstOrNull() ?: return@runCatching
            val previous = repository.lastArticleId()
            repository.setLastArticleId(article.id)
            if (previous != null && previous != article.id) showNotification(article)
        }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }

    private fun showNotification(article: Article) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("new_stories", "כתבות חדשות", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "עדכונים על כתבות חדשות ב-Alien News" })
        if (Build.VERSION.SDK_INT >= 33 && applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val intent = PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, ArticleReaderActivity::class.java).apply { putExtra("url", article.articleUrl); putExtra("title", article.title) }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(applicationContext, "new_stories")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Alien News · כתבה חדשה")
            .setContentText(article.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(article.title))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(article.id.hashCode(), notification)
    }
}
