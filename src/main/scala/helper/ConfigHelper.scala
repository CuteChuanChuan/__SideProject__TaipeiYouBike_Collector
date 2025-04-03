package helper

import com.typesafe.config.{ Config, ConfigFactory }
import org.slf4j.{ Logger, LoggerFactory }

import scala.concurrent.duration.{ DurationLong, FiniteDuration }

trait ConfigHelper {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def loadConfig(): Config = ConfigFactory.load()

  private lazy val config: Config = loadConfig()

  private val mongoUri: String = sys.env.get("MONGODB_URI").orElse(sys.props.get("mongodb.uri")).getOrElse {
    val mongoConfig: Config = config.getConfig("mongodb")
    mongoConfig.getString("uri")
  }

  object Api extends ApiConfig {
    private val apiConfig: Config         = config.getConfig("api")
    val url:               String         = apiConfig.getString("url")
    val fetchInterval:     FiniteDuration = apiConfig.getDuration("fetch-interval").toNanos.nanos
  }

  object MongoDB extends MongoDBConfig {
    private val mongoConfig: Config = config.getConfig("mongodb")
    val uri:                 String = mongoUri
    logger.info(s"Using MongoDB URI: $uri")
    val database:            String = mongoConfig.getString("database")

    val collections:   Config = mongoConfig.getConfig("collections")
    val dimCollection: String = collections.getString("dim")
    val fctCollection: String = collections.getString("fct")
  }
}

trait ApiConfig {
  def url:           String
  def fetchInterval: FiniteDuration
}

trait MongoDBConfig {
  def uri:           String
  def database:      String
  def dimCollection: String
  def fctCollection: String
}

object ConfigHelper extends ConfigHelper
