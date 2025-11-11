package cc.getportal.demo

import cc.getportal.model.Profile
import com.google.gson.Gson
import com.sun.tools.example.debug.expr.Token
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.sql.Connection
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.time.Instant

object DB {
    private val gson = Gson()

    fun connect(dbPath : String, dbFile : String) {
        run{
            val folder = File(dbPath)
            if(!folder.exists()) {
                folder.mkdirs()
            }
        }

        Database.connect("jdbc:sqlite:$dbPath/$dbFile", "org.sqlite.JDBC")


        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        // or Connection.TRANSACTION_READ_UNCOMMITTED

        transaction {
            SchemaUtils.create(Sessions, Payments)
        }

    }


    const val PUBKEY_LENGTH_MAX = 255
    // public record Profile(@Nullable String name, @Nullable String display_name, @Nullable String picture, @Nullable String nip05) {

    object Sessions : IntIdTable("sessions") {
        val token = text("token")
        val pubkey = varchar("pubkey", PUBKEY_LENGTH_MAX)
        val profile = text("profile")
    }

    fun getUserByToken(token : String) : UserSession? {
        return transaction {
            val row = Sessions.selectAll()
                .where { Sessions.token eq token }
                .limit(1)
                .firstOrNull()

            if(row == null) {
                return@transaction null
            }
            val pubkey = row[Sessions.pubkey]
            val profile = gson.fromJson(row[Sessions.profile], Profile::class.java)
            UserSession(pubkey, profile)
        }
    }

    fun insertUserToken(token: String, session: UserSession) {
        transaction {
            Sessions.insert {
                it[Sessions.token] = token
                it[Sessions.pubkey] = session.key
                it[Sessions.profile] = gson.toJson(session.profile)
            }
        }
    }

    object Payments : UUIDTable("payments") {
        val pubkey = varchar("pubkey", PUBKEY_LENGTH_MAX)
        val amount = long("amount")
        val description = text("description")
        val paid = bool("paid").nullable()
        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
        val updatedAt = timestamp("updated_at").nullable()
    }

    fun registerPayment(pubkey: String, amount: Long, description: String) : UUID {
        return transaction {
            Payments.insertAndGetId {
                it[Payments.pubkey] = pubkey
                it[Payments.amount] = amount
                it[Payments.description] = description
            }.value
        }
    }

    fun updatePaymentStatus(id: UUID, paid: Boolean) {
        transaction {
            Payments.update({Payments.id eq id}) {
                it[Payments.paid] = paid
                it[Payments.updatedAt] = java.time.Instant.now()
            }
        }
    }

    fun getPaymentsHistory(user: String) : List<UserPayment> {
        return transaction {
            Payments.selectAll()
                .where { Payments.pubkey eq user }
                .orderBy(Payments.createdAt, order = SortOrder.DESC)
                .map { UserPayment(
                    id= it[Payments.id].value,
                    amount = it[Payments.amount],
                    description = it[Payments.description],
                    paid = it[Payments.paid],
                    createdAt = DateTimeFormatter.ISO_INSTANT.format(it[Payments.createdAt]),
                    updateAt = it[Payments.updatedAt]?.let { updatedAt -> DateTimeFormatter.ISO_INSTANT.format(updatedAt) }
                ) }
        }
    }


}

data class UserSession(val key: String, val profile: Profile?)

data class UserPayment(val id: UUID, val amount: Long, val description: String, val paid: Boolean?, val createdAt: String, val updateAt: String?)