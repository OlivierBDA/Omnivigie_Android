package com.olivierbda.omnivigie.`data`.local.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.olivierbda.omnivigie.`data`.local.Converters
import com.olivierbda.omnivigie.`data`.local.entities.ArticleEntity
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ArticleDao_Impl(
  __db: RoomDatabase,
) : ArticleDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfArticleEntity: EntityInsertAdapter<ArticleEntity>

  private val __converters: Converters = Converters()

  private val __deleteAdapterOfArticleEntity: EntityDeleteOrUpdateAdapter<ArticleEntity>

  private val __updateAdapterOfArticleEntity: EntityDeleteOrUpdateAdapter<ArticleEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfArticleEntity = object : EntityInsertAdapter<ArticleEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `articles` (`id`,`emailId`,`title`,`url`,`source`,`readingTime`,`summary`,`isSponsor`,`aiInterest`,`aiThemes`,`aiExplanation`,`isQualified`,`isSentToNotebook`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ArticleEntity) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.emailId)
        statement.bindText(3, entity.title)
        statement.bindText(4, entity.url)
        statement.bindText(5, entity.source)
        statement.bindText(6, entity.readingTime)
        statement.bindText(7, entity.summary)
        val _tmp: Int = if (entity.isSponsor) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        val _tmpAiInterest: Boolean? = entity.aiInterest
        val _tmp_1: Int? = _tmpAiInterest?.let { if (it) 1 else 0 }
        if (_tmp_1 == null) {
          statement.bindNull(9)
        } else {
          statement.bindLong(9, _tmp_1.toLong())
        }
        val _tmp_2: String = __converters.fromStringList(entity.aiThemes)
        statement.bindText(10, _tmp_2)
        val _tmpAiExplanation: String? = entity.aiExplanation
        if (_tmpAiExplanation == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpAiExplanation)
        }
        val _tmp_3: Int = if (entity.isQualified) 1 else 0
        statement.bindLong(12, _tmp_3.toLong())
        val _tmp_4: Int = if (entity.isSentToNotebook) 1 else 0
        statement.bindLong(13, _tmp_4.toLong())
      }
    }
    this.__deleteAdapterOfArticleEntity = object : EntityDeleteOrUpdateAdapter<ArticleEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `articles` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ArticleEntity) {
        statement.bindLong(1, entity.id.toLong())
      }
    }
    this.__updateAdapterOfArticleEntity = object : EntityDeleteOrUpdateAdapter<ArticleEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `articles` SET `id` = ?,`emailId` = ?,`title` = ?,`url` = ?,`source` = ?,`readingTime` = ?,`summary` = ?,`isSponsor` = ?,`aiInterest` = ?,`aiThemes` = ?,`aiExplanation` = ?,`isQualified` = ?,`isSentToNotebook` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ArticleEntity) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.emailId)
        statement.bindText(3, entity.title)
        statement.bindText(4, entity.url)
        statement.bindText(5, entity.source)
        statement.bindText(6, entity.readingTime)
        statement.bindText(7, entity.summary)
        val _tmp: Int = if (entity.isSponsor) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        val _tmpAiInterest: Boolean? = entity.aiInterest
        val _tmp_1: Int? = _tmpAiInterest?.let { if (it) 1 else 0 }
        if (_tmp_1 == null) {
          statement.bindNull(9)
        } else {
          statement.bindLong(9, _tmp_1.toLong())
        }
        val _tmp_2: String = __converters.fromStringList(entity.aiThemes)
        statement.bindText(10, _tmp_2)
        val _tmpAiExplanation: String? = entity.aiExplanation
        if (_tmpAiExplanation == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpAiExplanation)
        }
        val _tmp_3: Int = if (entity.isQualified) 1 else 0
        statement.bindLong(12, _tmp_3.toLong())
        val _tmp_4: Int = if (entity.isSentToNotebook) 1 else 0
        statement.bindLong(13, _tmp_4.toLong())
        statement.bindLong(14, entity.id.toLong())
      }
    }
  }

  public override suspend fun insertArticle(article: ArticleEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfArticleEntity.insert(_connection, article)
  }

  public override suspend fun insertArticles(articles: List<ArticleEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfArticleEntity.insert(_connection, articles)
  }

  public override suspend fun deleteArticle(article: ArticleEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfArticleEntity.handle(_connection, article)
  }

  public override suspend fun updateArticle(article: ArticleEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfArticleEntity.handle(_connection, article)
  }

  public override fun getAllArticles(): Flow<List<ArticleEntity>> {
    val _sql: String = "SELECT * FROM articles ORDER BY id DESC"
    return createFlow(__db, false, arrayOf("articles")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfEmailId: Int = getColumnIndexOrThrow(_stmt, "emailId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfUrl: Int = getColumnIndexOrThrow(_stmt, "url")
        val _columnIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _columnIndexOfReadingTime: Int = getColumnIndexOrThrow(_stmt, "readingTime")
        val _columnIndexOfSummary: Int = getColumnIndexOrThrow(_stmt, "summary")
        val _columnIndexOfIsSponsor: Int = getColumnIndexOrThrow(_stmt, "isSponsor")
        val _columnIndexOfAiInterest: Int = getColumnIndexOrThrow(_stmt, "aiInterest")
        val _columnIndexOfAiThemes: Int = getColumnIndexOrThrow(_stmt, "aiThemes")
        val _columnIndexOfAiExplanation: Int = getColumnIndexOrThrow(_stmt, "aiExplanation")
        val _columnIndexOfIsQualified: Int = getColumnIndexOrThrow(_stmt, "isQualified")
        val _columnIndexOfIsSentToNotebook: Int = getColumnIndexOrThrow(_stmt, "isSentToNotebook")
        val _result: MutableList<ArticleEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ArticleEntity
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpEmailId: String
          _tmpEmailId = _stmt.getText(_columnIndexOfEmailId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpUrl: String
          _tmpUrl = _stmt.getText(_columnIndexOfUrl)
          val _tmpSource: String
          _tmpSource = _stmt.getText(_columnIndexOfSource)
          val _tmpReadingTime: String
          _tmpReadingTime = _stmt.getText(_columnIndexOfReadingTime)
          val _tmpSummary: String
          _tmpSummary = _stmt.getText(_columnIndexOfSummary)
          val _tmpIsSponsor: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsSponsor).toInt()
          _tmpIsSponsor = _tmp != 0
          val _tmpAiInterest: Boolean?
          val _tmp_1: Int?
          if (_stmt.isNull(_columnIndexOfAiInterest)) {
            _tmp_1 = null
          } else {
            _tmp_1 = _stmt.getLong(_columnIndexOfAiInterest).toInt()
          }
          _tmpAiInterest = _tmp_1?.let { it != 0 }
          val _tmpAiThemes: List<String>
          val _tmp_2: String
          _tmp_2 = _stmt.getText(_columnIndexOfAiThemes)
          _tmpAiThemes = __converters.toStringList(_tmp_2)
          val _tmpAiExplanation: String?
          if (_stmt.isNull(_columnIndexOfAiExplanation)) {
            _tmpAiExplanation = null
          } else {
            _tmpAiExplanation = _stmt.getText(_columnIndexOfAiExplanation)
          }
          val _tmpIsQualified: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfIsQualified).toInt()
          _tmpIsQualified = _tmp_3 != 0
          val _tmpIsSentToNotebook: Boolean
          val _tmp_4: Int
          _tmp_4 = _stmt.getLong(_columnIndexOfIsSentToNotebook).toInt()
          _tmpIsSentToNotebook = _tmp_4 != 0
          _item = ArticleEntity(_tmpId,_tmpEmailId,_tmpTitle,_tmpUrl,_tmpSource,_tmpReadingTime,_tmpSummary,_tmpIsSponsor,_tmpAiInterest,_tmpAiThemes,_tmpAiExplanation,_tmpIsQualified,_tmpIsSentToNotebook)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getInterestingArticles(): Flow<List<ArticleEntity>> {
    val _sql: String = "SELECT * FROM articles WHERE aiInterest = 1 ORDER BY id DESC"
    return createFlow(__db, false, arrayOf("articles")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfEmailId: Int = getColumnIndexOrThrow(_stmt, "emailId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfUrl: Int = getColumnIndexOrThrow(_stmt, "url")
        val _columnIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _columnIndexOfReadingTime: Int = getColumnIndexOrThrow(_stmt, "readingTime")
        val _columnIndexOfSummary: Int = getColumnIndexOrThrow(_stmt, "summary")
        val _columnIndexOfIsSponsor: Int = getColumnIndexOrThrow(_stmt, "isSponsor")
        val _columnIndexOfAiInterest: Int = getColumnIndexOrThrow(_stmt, "aiInterest")
        val _columnIndexOfAiThemes: Int = getColumnIndexOrThrow(_stmt, "aiThemes")
        val _columnIndexOfAiExplanation: Int = getColumnIndexOrThrow(_stmt, "aiExplanation")
        val _columnIndexOfIsQualified: Int = getColumnIndexOrThrow(_stmt, "isQualified")
        val _columnIndexOfIsSentToNotebook: Int = getColumnIndexOrThrow(_stmt, "isSentToNotebook")
        val _result: MutableList<ArticleEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ArticleEntity
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpEmailId: String
          _tmpEmailId = _stmt.getText(_columnIndexOfEmailId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpUrl: String
          _tmpUrl = _stmt.getText(_columnIndexOfUrl)
          val _tmpSource: String
          _tmpSource = _stmt.getText(_columnIndexOfSource)
          val _tmpReadingTime: String
          _tmpReadingTime = _stmt.getText(_columnIndexOfReadingTime)
          val _tmpSummary: String
          _tmpSummary = _stmt.getText(_columnIndexOfSummary)
          val _tmpIsSponsor: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsSponsor).toInt()
          _tmpIsSponsor = _tmp != 0
          val _tmpAiInterest: Boolean?
          val _tmp_1: Int?
          if (_stmt.isNull(_columnIndexOfAiInterest)) {
            _tmp_1 = null
          } else {
            _tmp_1 = _stmt.getLong(_columnIndexOfAiInterest).toInt()
          }
          _tmpAiInterest = _tmp_1?.let { it != 0 }
          val _tmpAiThemes: List<String>
          val _tmp_2: String
          _tmp_2 = _stmt.getText(_columnIndexOfAiThemes)
          _tmpAiThemes = __converters.toStringList(_tmp_2)
          val _tmpAiExplanation: String?
          if (_stmt.isNull(_columnIndexOfAiExplanation)) {
            _tmpAiExplanation = null
          } else {
            _tmpAiExplanation = _stmt.getText(_columnIndexOfAiExplanation)
          }
          val _tmpIsQualified: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfIsQualified).toInt()
          _tmpIsQualified = _tmp_3 != 0
          val _tmpIsSentToNotebook: Boolean
          val _tmp_4: Int
          _tmp_4 = _stmt.getLong(_columnIndexOfIsSentToNotebook).toInt()
          _tmpIsSentToNotebook = _tmp_4 != 0
          _item = ArticleEntity(_tmpId,_tmpEmailId,_tmpTitle,_tmpUrl,_tmpSource,_tmpReadingTime,_tmpSummary,_tmpIsSponsor,_tmpAiInterest,_tmpAiThemes,_tmpAiExplanation,_tmpIsQualified,_tmpIsSentToNotebook)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getArticlesByEmail(emailId: String): List<ArticleEntity> {
    val _sql: String = "SELECT * FROM articles WHERE emailId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, emailId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfEmailId: Int = getColumnIndexOrThrow(_stmt, "emailId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfUrl: Int = getColumnIndexOrThrow(_stmt, "url")
        val _columnIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _columnIndexOfReadingTime: Int = getColumnIndexOrThrow(_stmt, "readingTime")
        val _columnIndexOfSummary: Int = getColumnIndexOrThrow(_stmt, "summary")
        val _columnIndexOfIsSponsor: Int = getColumnIndexOrThrow(_stmt, "isSponsor")
        val _columnIndexOfAiInterest: Int = getColumnIndexOrThrow(_stmt, "aiInterest")
        val _columnIndexOfAiThemes: Int = getColumnIndexOrThrow(_stmt, "aiThemes")
        val _columnIndexOfAiExplanation: Int = getColumnIndexOrThrow(_stmt, "aiExplanation")
        val _columnIndexOfIsQualified: Int = getColumnIndexOrThrow(_stmt, "isQualified")
        val _columnIndexOfIsSentToNotebook: Int = getColumnIndexOrThrow(_stmt, "isSentToNotebook")
        val _result: MutableList<ArticleEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ArticleEntity
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpEmailId: String
          _tmpEmailId = _stmt.getText(_columnIndexOfEmailId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpUrl: String
          _tmpUrl = _stmt.getText(_columnIndexOfUrl)
          val _tmpSource: String
          _tmpSource = _stmt.getText(_columnIndexOfSource)
          val _tmpReadingTime: String
          _tmpReadingTime = _stmt.getText(_columnIndexOfReadingTime)
          val _tmpSummary: String
          _tmpSummary = _stmt.getText(_columnIndexOfSummary)
          val _tmpIsSponsor: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsSponsor).toInt()
          _tmpIsSponsor = _tmp != 0
          val _tmpAiInterest: Boolean?
          val _tmp_1: Int?
          if (_stmt.isNull(_columnIndexOfAiInterest)) {
            _tmp_1 = null
          } else {
            _tmp_1 = _stmt.getLong(_columnIndexOfAiInterest).toInt()
          }
          _tmpAiInterest = _tmp_1?.let { it != 0 }
          val _tmpAiThemes: List<String>
          val _tmp_2: String
          _tmp_2 = _stmt.getText(_columnIndexOfAiThemes)
          _tmpAiThemes = __converters.toStringList(_tmp_2)
          val _tmpAiExplanation: String?
          if (_stmt.isNull(_columnIndexOfAiExplanation)) {
            _tmpAiExplanation = null
          } else {
            _tmpAiExplanation = _stmt.getText(_columnIndexOfAiExplanation)
          }
          val _tmpIsQualified: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfIsQualified).toInt()
          _tmpIsQualified = _tmp_3 != 0
          val _tmpIsSentToNotebook: Boolean
          val _tmp_4: Int
          _tmp_4 = _stmt.getLong(_columnIndexOfIsSentToNotebook).toInt()
          _tmpIsSentToNotebook = _tmp_4 != 0
          _item = ArticleEntity(_tmpId,_tmpEmailId,_tmpTitle,_tmpUrl,_tmpSource,_tmpReadingTime,_tmpSummary,_tmpIsSponsor,_tmpAiInterest,_tmpAiThemes,_tmpAiExplanation,_tmpIsQualified,_tmpIsSentToNotebook)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
