package com.olivierbda.omnivigie.`data`.local.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.olivierbda.omnivigie.`data`.local.entities.EmailEntity
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
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
public class EmailDao_Impl(
  __db: RoomDatabase,
) : EmailDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfEmailEntity: EntityInsertAdapter<EmailEntity>

  private val __deleteAdapterOfEmailEntity: EntityDeleteOrUpdateAdapter<EmailEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfEmailEntity = object : EntityInsertAdapter<EmailEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `emails` (`id`,`receivedDate`,`sender`,`subject`,`bodyHtml`,`isProcessed`) VALUES (?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: EmailEntity) {
        statement.bindText(1, entity.id)
        statement.bindLong(2, entity.receivedDate)
        statement.bindText(3, entity.sender)
        statement.bindText(4, entity.subject)
        statement.bindText(5, entity.bodyHtml)
        val _tmp: Int = if (entity.isProcessed) 1 else 0
        statement.bindLong(6, _tmp.toLong())
      }
    }
    this.__deleteAdapterOfEmailEntity = object : EntityDeleteOrUpdateAdapter<EmailEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `emails` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: EmailEntity) {
        statement.bindText(1, entity.id)
      }
    }
  }

  public override suspend fun insertEmail(email: EmailEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfEmailEntity.insert(_connection, email)
  }

  public override suspend fun deleteEmail(email: EmailEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfEmailEntity.handle(_connection, email)
  }

  public override fun getAllEmails(): Flow<List<EmailEntity>> {
    val _sql: String = "SELECT * FROM emails ORDER BY receivedDate DESC"
    return createFlow(__db, false, arrayOf("emails")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfReceivedDate: Int = getColumnIndexOrThrow(_stmt, "receivedDate")
        val _columnIndexOfSender: Int = getColumnIndexOrThrow(_stmt, "sender")
        val _columnIndexOfSubject: Int = getColumnIndexOrThrow(_stmt, "subject")
        val _columnIndexOfBodyHtml: Int = getColumnIndexOrThrow(_stmt, "bodyHtml")
        val _columnIndexOfIsProcessed: Int = getColumnIndexOrThrow(_stmt, "isProcessed")
        val _result: MutableList<EmailEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: EmailEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpReceivedDate: Long
          _tmpReceivedDate = _stmt.getLong(_columnIndexOfReceivedDate)
          val _tmpSender: String
          _tmpSender = _stmt.getText(_columnIndexOfSender)
          val _tmpSubject: String
          _tmpSubject = _stmt.getText(_columnIndexOfSubject)
          val _tmpBodyHtml: String
          _tmpBodyHtml = _stmt.getText(_columnIndexOfBodyHtml)
          val _tmpIsProcessed: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsProcessed).toInt()
          _tmpIsProcessed = _tmp != 0
          _item = EmailEntity(_tmpId,_tmpReceivedDate,_tmpSender,_tmpSubject,_tmpBodyHtml,_tmpIsProcessed)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getEmailById(id: String): EmailEntity? {
    val _sql: String = "SELECT * FROM emails WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfReceivedDate: Int = getColumnIndexOrThrow(_stmt, "receivedDate")
        val _columnIndexOfSender: Int = getColumnIndexOrThrow(_stmt, "sender")
        val _columnIndexOfSubject: Int = getColumnIndexOrThrow(_stmt, "subject")
        val _columnIndexOfBodyHtml: Int = getColumnIndexOrThrow(_stmt, "bodyHtml")
        val _columnIndexOfIsProcessed: Int = getColumnIndexOrThrow(_stmt, "isProcessed")
        val _result: EmailEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpReceivedDate: Long
          _tmpReceivedDate = _stmt.getLong(_columnIndexOfReceivedDate)
          val _tmpSender: String
          _tmpSender = _stmt.getText(_columnIndexOfSender)
          val _tmpSubject: String
          _tmpSubject = _stmt.getText(_columnIndexOfSubject)
          val _tmpBodyHtml: String
          _tmpBodyHtml = _stmt.getText(_columnIndexOfBodyHtml)
          val _tmpIsProcessed: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsProcessed).toInt()
          _tmpIsProcessed = _tmp != 0
          _result = EmailEntity(_tmpId,_tmpReceivedDate,_tmpSender,_tmpSubject,_tmpBodyHtml,_tmpIsProcessed)
        } else {
          _result = null
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
