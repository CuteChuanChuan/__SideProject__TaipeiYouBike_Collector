package helper

import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ConfigHelperSpec extends AnyFlatSpec with Matchers {

  val testConfigText: String =
    """
      |api {
      |    url = "https://example.com"
      |    fetch-interval = 5 minutes
      |}
      |
      |mongodb {
      |    uri = "mongodb://fake_usr:fakee_pwd@localhost:27017/fake_db?authSource=fake_db"
      |    database = "fake_db"
      |    collections = {
      |        dim = "dim"
      |        fct = "fct"
      |    }
      |    retention = {
      |        fact-data-days = 30
      |        enable-cleanup = true
      |        cleanup-interval = 1 day
      |    }
      |}
      |""".stripMargin

  "ConfigHelper" should "load correct configuration values" in {

    val testConfig = ConfigFactory.parseString(testConfigText)

    object TestConfigHelper extends ConfigHelper {
      override def loadConfig(): Config = testConfig
    }

    TestConfigHelper.Api.url shouldBe "https://example.com"
    TestConfigHelper.Api.fetchInterval shouldBe 300000
    TestConfigHelper.MongoDB.uri shouldBe "mongodb://fake_usr:fakee_pwd@localhost:27017/fake_db?authSource=fake_db"
    TestConfigHelper.MongoDB.database shouldBe "fake_db"
    TestConfigHelper.MongoDB.dimCollection shouldBe "dim"
    TestConfigHelper.MongoDB.fctCollection shouldBe "fct"
  }
}
