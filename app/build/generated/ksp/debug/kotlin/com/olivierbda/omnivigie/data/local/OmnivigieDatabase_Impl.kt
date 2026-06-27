package com.olivierbda.omnivigie.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.olivierbda.omnivigie.`data`.local.dao.ArticleDao
import com.olivierbda.omnivigie.`data`.local.dao.ArticleDao_Impl
import com.olivierbda.omnivigie.`data`.local.dao.EmailDao
import com.olivierbda.omnivigie.`data`.local.dao.EmailDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class OmnivigieDatabase_Impl : OmnivigieDatabase() {
  private val _emailDao: Lazy<EmailDao> = lazy {
    EmailDao_Impl(this)
  }

  private val _articleDao: Lazy<ArticleDao> = lazy {
    ArticleDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1, "1b4f3dcca68089902a95dd64fd9cc590", "18161689f5f72a2ba3354313d3b78c73") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `emails` (`id` TEXT NOT NULL, `receivedDate` INTEGER NOT NULL, `sender` TEXT NOT NULL, `subject` TEXT NOT NULL, `bodyHtml` TEXT NOT NULL, `isProcessed` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `articles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `emailId` TEXT NOT NULL, `title` TEXT NOT NULL, `url` TEXT NOT NULL, `source` TEXT NOT NULL, `readingTime` TEXT NOT NULL, `summary` TEXT NOT NULL, `isSponsor` INTEGER NOT NULL, `aiInterest` INTEGER, `aiThemes` TEXT NOT NULL, `aiExplanation` TEXT, `isQualified` INTEGER NOT NULL, `isSentToNotebook` INTEGER NOT NULL, FOREIGN KEY(`emailId`) REFERENCES `emails`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_articles_emailId` ON `articles` (`emailId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '1b4f3dcca68089902a95dd64fd9cc590')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `emails`")
        connection.execSQL("DROP TABLE IF EXISTS `articles`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsEmails: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsEmails.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEmails.put("receivedDate", TableInfo.Column("receivedDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEmails.put("sender", TableInfo.Column("sender", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEmails.put("subject", TableInfo.Column("subject", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEmails.put("bodyHtml", TableInfo.Column("bodyHtml", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEmails.put("isProcessed", TableInfo.Column("isProcessed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysEmails: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesEmails: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoEmails: TableInfo = TableInfo("emails", _columnsEmails, _foreignKeysEmails, _indicesEmails)
        val _existingEmails: TableInfo = read(connection, "emails")
        if (!_infoEmails.equals(_existingEmails)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |emails(com.olivierbda.omnivigie.data.local.entities.EmailEntity).
              | Expected:
              |""".trimMargin() + _infoEmails + """
              |
              | Found:
              |""".trimMargin() + _existingEmails)
        }
        val _columnsArticles: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsArticles.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsArticles.put("emailId", TableInfo.Column("emailId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsArticles.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsArticles.put("url", TableInfo.Column("url", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsArticles.put("source", TableInfo.Column("source", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsArticles.put("readingTime", TableInfo.Column("readingTime", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsArticles.put("summary", TableInfo.Column("summary", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsArticles.put("isSponsor", TableInfo.Column("isSponsor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsArticles.put("aiInterest", TableInfo.Column("aiInterest", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsArticles.put("aiThemes", TableInfo.Column("aiThemes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsArticles.put("aiExplanation", TableInfo.Column("aiExplanation", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsArticles.put("isQualified", TableInfo.Column("isQualified", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsArticles.put("isSentToNotebook", TableInfo.Column("isSentToNotebook", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysArticles: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysArticles.add(TableInfo.ForeignKey("emails", "CASCADE", "NO ACTION", listOf("emailId"), listOf("id")))
        val _indicesArticles: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesArticles.add(TableInfo.Index("index_articles_emailId", false, listOf("emailId"), listOf("ASC")))
        val _infoArticles: TableInfo = TableInfo("articles", _columnsArticles, _foreignKeysArticles, _indicesArticles)
        val _existingArticles: TableInfo = read(connection, "articles")
        if (!_infoArticles.equals(_existingArticles)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |articles(com.olivierbda.omnivigie.data.local.entities.ArticleEntity).
              | Expected:
              |""".trimMargin() + _infoArticles + """
              |
              | Found:
              |""".trimMargin() + _existingArticles)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "emails", "articles")
  }

  public override fun clearAllTables() {
    super.performClear(true, "emails", "articles")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(EmailDao::class, EmailDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ArticleDao::class, ArticleDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun emailDao(): EmailDao = _emailDao.value

  public override fun articleDao(): ArticleDao = _articleDao.value
}
