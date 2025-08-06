import models.ReviewReason
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._

class ReviewReasonSpec extends AnyWordSpec with Matchers {

  "ReviewReason JSON serialization" should {

    "serialize each ReviewReason to the correct JsString" in {
      Json.toJson[ReviewReason](ReviewReason.Commodity) shouldBe JsString("commodity")
      Json.toJson[ReviewReason](ReviewReason.Country) shouldBe JsString("country")
      Json.toJson[ReviewReason](ReviewReason.Inadequate) shouldBe JsString("inadequate")
      Json.toJson[ReviewReason](ReviewReason.Unclear) shouldBe JsString("unclear")
      Json.toJson[ReviewReason](ReviewReason.Measure) shouldBe JsString("measure")
      Json.toJson[ReviewReason](ReviewReason.Mismatch) shouldBe JsString("mismatch")
    }
  }

  "ReviewReason JSON deserialization" should {

    "deserialize valid JSON strings to the correct ReviewReason" in {
      Json.fromJson[ReviewReason](JsString("commodity")).get shouldBe ReviewReason.Commodity
      Json.fromJson[ReviewReason](JsString("country")).get shouldBe ReviewReason.Country
      Json.fromJson[ReviewReason](JsString("inadequate")).get shouldBe ReviewReason.Inadequate
      Json.fromJson[ReviewReason](JsString("unclear")).get shouldBe ReviewReason.Unclear
      Json.fromJson[ReviewReason](JsString("measure")).get shouldBe ReviewReason.Measure
      Json.fromJson[ReviewReason](JsString("mismatch")).get shouldBe ReviewReason.Mismatch
    }

    "be case-insensitive when deserializing" in {
      Json.fromJson[ReviewReason](JsString("CoMmodiTy")).get shouldBe ReviewReason.Commodity
      Json.fromJson[ReviewReason](JsString("COUNTRY")).get shouldBe ReviewReason.Country
    }

    "fail to deserialize unknown strings" in {
      val result = Json.fromJson[ReviewReason](JsString("unknown"))
      result.isError shouldBe true
      result.asInstanceOf[JsError].errors.head._2.head.message should include("unknown ReviewReason")
    }

    "fail to deserialize non-string JSON values" in {
      val result = Json.fromJson[ReviewReason](JsNumber(123))
      result.isError shouldBe true
    }
  }
}
