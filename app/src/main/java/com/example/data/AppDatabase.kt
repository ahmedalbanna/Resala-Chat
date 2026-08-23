package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.util.EncryptionEngine
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [User::class, Chat::class, Message::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Phase 0: Removed fallbackToDestructiveMigration() which wiped user data on version bump.
         * Added explicit MIGRATION_1_2 (no-op for now — schema unchanged, only hardening).
         * Phase 3 TODO: Switch to SQLCipher (SupportFactory with passphrase from EncryptedSharedPreferences / Android Keystore).
         *   Example: Room.databaseBuilder(...).openHelperFactory(SupportFactory(SQLiteDatabase.getBytes(passphrase)))
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No schema change in Phase 0; placeholder to prevent destructive migration.
                // Future Phase 3: add columns for Signal preKeys, SQLCipher rekey, etc.
            }
        }

        /** Phase 1: DB creation callback for seeding — replaces ViewModel users.take(1) race. */
        private fun seedCallback(context: Context) = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getDatabase(context)
                    // Insert seed users/chats/messages once per DB creation
                    val seedUsers = listOf(
                        User(id = "user_bot", fullName = "المساعد الذكي المشفر 🤖", phone = "+966 54 000 0000", bio = "روبوت ذكي للرد التلقائي وإثبات التشفير الفوري", publicKey = "IK-robo-secure-bot-key-83ad", isFavorite = true, lastSeen = "متصل الآن"),
                        User(id = "user_mona", fullName = "منى الكردي 🛡️", phone = "+966 50 123 4567", bio = "متوفرة | مفتاح الهوية المعتمد نشط", publicKey = "IK-98df-892c-bef4-c321", isFavorite = true, lastSeen = "نشط منذ ٤ د"),
                        User(id = "user_khaled", fullName = "المهندس خالد ⚙️", phone = "+966 55 987 6543", bio = "في اجتماع آمن مغلق 🔒 المراسلة مشفرة", publicKey = "IK-a47f-8be3-91cd-fb92", isFavorite = false, lastSeen = "متصل الآن"),
                        User(id = "user_sulaiman", fullName = "سليمان الفهيد 🏡", phone = "+966 53 456 7890", bio = "تراسل معي لحماية خصوصيتك", publicKey = "IK-38bc-9fdb-4328-da62", isFavorite = false, lastSeen = "نشط أمس")
                    )
                    database.userDao().insertUsers(seedUsers)
                    val chatBot = Chat(id = "chat_user_bot", peerId = "user_bot", peerName = "المساعد الذكي المشفر 🤖", lastMessageText = "دردشة مشفرة تامة (End-to-End)", lastMessageTime = System.currentTimeMillis() - 7200000, unreadCount = 1)
                    val chatMona = Chat(id = "chat_user_mona", peerId = "user_mona", peerName = "منى الكردي 🛡️", lastMessageText = "أهلاً بك! تم إنشاء الجلسة الآمنة بيننا بنجاح.", lastMessageTime = System.currentTimeMillis() - 3600000, unreadCount = 0)
                    database.chatDao().insertChat(chatBot)
                    database.chatDao().insertChat(chatMona)
                    val msg1 = Message(id = UUID.randomUUID().toString(), chatId = "chat_user_bot", senderId = "user_bot", contentCipher = EncryptionEngine.encrypt("أهلاً بك في رِسالة! 👋 هذا التطبيق مشفر بالكامل ببروتوكول يحمي بياناتك محلياً وفي السقيفة السحابية. جرّب إرسال نصوص، رسائل صوتية، أو مواقع جغرافية، أو تجربة المكالمات الآمنة!", "IK-robo-secure-bot-key-83ad"), timestamp = System.currentTimeMillis() - 7200000)
                    val msg2 = Message(id = UUID.randomUUID().toString(), chatId = "chat_user_mona", senderId = "user_mona", contentCipher = EncryptionEngine.encrypt("أهلاً بك! تم إنشاء الجلسة الآمنة بيننا بنجاح.", "IK-98df-892c-bef4-c321"), timestamp = System.currentTimeMillis() - 3600000)
                    database.messageDao().insertMessage(msg1)
                    database.messageDao().insertMessage(msg2)
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "resala_secure_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(seedCallback(context.applicationContext))
                    .fallbackToDestructiveMigrationOnDowngrade()
                 .build()
                INSTANCE = instance
                instance
            }
        }

        /** For tests: in-memory instance without migrations */
        fun getInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        }
    }
}
