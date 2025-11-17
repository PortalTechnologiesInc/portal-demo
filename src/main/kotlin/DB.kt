package cc.getportal.demo

import cc.getportal.model.Currency
import cc.getportal.model.Profile
import com.google.gson.Gson
import com.sun.tools.example.debug.expr.Token
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
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
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID


object DB {
    private val gson = Gson()

    lateinit var _database : Database
    fun connect(dbPath : String, dbFile : String) {
        run{
            val folder = File(dbPath)
            if(!folder.exists()) {
                folder.mkdirs()
            }
        }

        _database = Database.connect("jdbc:sqlite:$dbPath/$dbFile", "org.sqlite.JDBC")


        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        // or Connection.TRANSACTION_READ_UNCOMMITTED

        transaction {
            SchemaUtils.create(Sessions, Payments, Subscriptions)
        }

    }

    fun disconnect() {
        TransactionManager.closeAndUnregister(_database)
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
        val currency = varchar("currency", 10)
        val amount = long("amount")
        val description = text("description")
        val paid = bool("paid").nullable()
        val updatedAt = timestamp("updated_at").nullable()
        val portalSubscriptionId = text("portal_subscription_id").nullable()

        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    }

    fun registerPayment(pubkey: String, currency: Currency, amount: Long, description: String, portalSubscriptionId : String?) : UUID {
        return transaction {
            Payments.insertAndGetId {
                it[Payments.pubkey] = pubkey
                it[Payments.currency] = currency.code
                it[Payments.amount] = amount
                it[Payments.description] = description
                it[Payments.portalSubscriptionId] = portalSubscriptionId
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
                .where { (Payments.pubkey eq user) and (Payments.portalSubscriptionId.isNull()) }
                .orderBy(Payments.createdAt, order = SortOrder.DESC)
                .map { UserPayment(
                    id= it[Payments.id].value,
                    currency = it[Payments.currency],
                    amount = it[Payments.amount],
                    description = it[Payments.description],
                    paid = it[Payments.paid],
                    createdAt = DateTimeFormatter.ISO_INSTANT.format(it[Payments.createdAt]),
                    updateAt = it[Payments.updatedAt]?.let { updatedAt -> DateTimeFormatter.ISO_INSTANT.format(updatedAt) }
                ) }
        }
    }

    object Subscriptions : UUIDTable("subscriptions") {
        val pubkey = varchar("pubkey", PUBKEY_LENGTH_MAX)
        val currency = varchar("currency", 10)
        val amount = long("amount")
        val frequency = varchar("frequency", 100)
        val description = text("description")
        val status = varchar("status",15)
        val lastPaymentAt = timestamp("last_payment_at").nullable()
        val nextPaymentAt = timestamp("next_payment_at")
        val portalSubscriptionId = text("portal_subscription_id")

        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    }

    fun registerSubscription(pubkey: String, currency: Currency, amount: Long, frequency: String, description: String, nextPaymentAt: java.time.Instant, portalSubscriptionId: String) : UUID {
        return transaction {
            Subscriptions.insertAndGetId {
                it[Subscriptions.pubkey] = pubkey
                it[Subscriptions.currency] = currency.code
                it[Subscriptions.amount] = amount
                it[Subscriptions.frequency] = frequency
                it[Subscriptions.description] = description
                it[Subscriptions.status] = SubscriptionStatus.ACTIVE.name
                it[Subscriptions.nextPaymentAt] = nextPaymentAt
                it[Subscriptions.portalSubscriptionId] = portalSubscriptionId
            }.value
        }
    }

    fun getSubscriptionsHistory(user: String) : List<UserSubscription> {
        return transaction {
            Subscriptions.selectAll()
                .where { Subscriptions.pubkey eq user }
                .orderBy(Subscriptions.createdAt, order = SortOrder.DESC)
                .map {
                    UserSubscription(
                        id = it[Subscriptions.id].value,
                        currency = it[Subscriptions.currency],
                        amount = it[Subscriptions.amount],
                        frequency = it[Subscriptions.frequency],
                        description = it[Subscriptions.description],
                        status = SubscriptionStatus.valueOf(it[Subscriptions.status]),
                        portalSubscriptionId = it[Subscriptions.portalSubscriptionId],
                        createdAt = DateTimeFormatter.ISO_INSTANT.format(it[Subscriptions.createdAt]),
                    )
                }
        }
    }

    fun getDueSubscriptions(now : Instant) : List<Subscription> {
        return transaction {
            Subscriptions.selectAll()
                .where { (Subscriptions.status eq SubscriptionStatus.ACTIVE.name) and (Subscriptions.nextPaymentAt less now) }
                .map {
                    val data = UserSubscription(
                        id = it[Subscriptions.id].value,
                        currency = it[Subscriptions.currency],
                        amount = it[Subscriptions.amount],
                        frequency = it[Subscriptions.frequency],
                        description = it[Subscriptions.description],
                        status = SubscriptionStatus.valueOf(it[Subscriptions.status]),
                        portalSubscriptionId = it[Subscriptions.portalSubscriptionId],
                        createdAt = DateTimeFormatter.ISO_INSTANT.format(it[Subscriptions.createdAt]),
                    )
                    Subscription(it[Subscriptions.pubkey], data)
                }
        }
    }

    fun updateSubscriptionLastPayment(id: UUID, lastPayment: Instant, nextPaymentAt: Instant) {
        transaction {
            Subscriptions.update({ Subscriptions.id eq id}) {

                it[Subscriptions.lastPaymentAt] = lastPayment
                it[Subscriptions.nextPaymentAt] = nextPaymentAt
            }
        }
    }

    fun getSubscriptionByPortalId(portalSubscriptionId: String) : Subscription? {
        return transaction {
            Subscriptions.selectAll()
                .where { (Subscriptions.status eq SubscriptionStatus.ACTIVE.name) and (Subscriptions.portalSubscriptionId eq portalSubscriptionId) }
                .limit(1)
                .firstOrNull()
                ?.let{
                    val data = UserSubscription(
                        id = it[Subscriptions.id].value,
                        currency = it[Subscriptions.currency],
                        amount = it[Subscriptions.amount],
                        frequency = it[Subscriptions.frequency],
                        description = it[Subscriptions.description],
                        status = SubscriptionStatus.valueOf(it[Subscriptions.status]),
                        portalSubscriptionId = it[Subscriptions.portalSubscriptionId],
                        createdAt = DateTimeFormatter.ISO_INSTANT.format(it[Subscriptions.createdAt]),
                    )
                    Subscription(it[Subscriptions.pubkey], data)
                }
        }
    }

    fun updateSubscriptionStatus(id: UUID, status: SubscriptionStatus) {
        transaction {
            Subscriptions.update({ Subscriptions.id eq id}) {

                it[Subscriptions.status] = status.name
            }
        }
    }

    fun getSubscriptionRecentPayments(pubkey: String, portalSubscriptionId: String, limit: Int) : List<UserPayment> {
        return transaction {
            Payments.selectAll()
                .where { (Payments.pubkey eq pubkey) and (Payments.portalSubscriptionId eq portalSubscriptionId) }
                .limit(limit)
                .orderBy(Payments.createdAt, SortOrder.DESC)
                .map { UserPayment(
                    id= it[Payments.id].value,
                    currency = it[Payments.currency],
                    amount = it[Payments.amount],
                    description = it[Payments.description],
                    paid = it[Payments.paid],
                    createdAt = DateTimeFormatter.ISO_INSTANT.format(it[Payments.createdAt]),
                    updateAt = it[Payments.updatedAt]?.let { updatedAt -> DateTimeFormatter.ISO_INSTANT.format(updatedAt) }
                ) }
        }
    }

    fun getSubscriptionAllPayments(pubkey: String, portalSubscriptionId: String) : List<UserPayment> {
        return transaction {
            Payments.selectAll()
                .where { (Payments.pubkey eq pubkey) and (Payments.portalSubscriptionId eq portalSubscriptionId) }
                .orderBy(Payments.createdAt, SortOrder.DESC)
                .map { UserPayment(
                    id= it[Payments.id].value,
                    currency = it[Payments.currency],
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

data class UserPayment(val id: UUID, val currency: String, val amount: Long, val description: String, val paid: Boolean?, val createdAt: String, val updateAt: String?)

enum class SubscriptionStatus {
    ACTIVE,
    CANCELLED,
    FAILED
}

data class Subscription(val user: String, val data: UserSubscription)
data class UserSubscription(val id: UUID, val currency: String, val amount: Long, val frequency: String, val description: String, val status: SubscriptionStatus, val portalSubscriptionId : String, val createdAt: String)


