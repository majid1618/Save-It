package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Folder::class, SavedUrl::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun savedUrlDao(): SavedUrlDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "saveit_database"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate default folders on database creation
                        CoroutineScope(Dispatchers.IO).launch {
                            val folderDao = getDatabase(context).folderDao()
                            folderDao.insertFolder(Folder(name = "📥 Inbox", iconName = "Inbox"))
                            folderDao.insertFolder(Folder(name = "📖 Read Later", iconName = "MenuBook"))
                            folderDao.insertFolder(Folder(name = "💡 Inspiration", iconName = "Lightbulb"))
                            folderDao.insertFolder(Folder(name = "💼 Work", iconName = "Work"))
                            folderDao.insertFolder(Folder(name = "🛒 Shopping", iconName = "ShoppingCart"))
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
