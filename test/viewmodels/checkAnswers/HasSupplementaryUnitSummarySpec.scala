package viewmodels.checkAnswers

import base.SpecBase
import base.TestConstants.testRecordId
import controllers.routes
import models.NormalMode
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Actions

import java.time.Instant

class HasSupplementaryUnitSummarySpec extends SpecBase {

  implicit private val messages: Messages = messages(applicationBuilder().build())

  private val recordWithSupplementaryUnit = goodsRecordResponseWithSupplementaryUnit(
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z")
  ).copy(recordId = testRecordId, category = Some(1))

  private val recordWithSupplementaryUnitCat2 = goodsRecordResponseWithSupplementaryUnit(
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z")
  ).copy(recordId = testRecordId, category = Some(2))

  "HasSupplementaryUnitSummary.row" - {

    "must not return a SummaryListRow when not Category 2" in {

      val row = HasSupplementaryUnitSummary.row(
        recordWithSupplementaryUnit,
        testRecordId,
        recordLocked = true
      )

      row mustBe None
    }

    "must not return a SummaryListRow when no measurement unit" in {

      val row = HasSupplementaryUnitSummary.row(
        goodsRecordResponse(Instant.now, Instant.now),
        testRecordId,
        recordLocked = false
      )

      row mustBe None
    }

    "must return a SummaryListRow without change links when record is locked and is Category 2" in {

      val row = HasSupplementaryUnitSummary.row(
        recordWithSupplementaryUnitCat2,
        testRecordId,
        recordLocked = true
      )

      row.get.actions mustBe Some(Actions("", List()))
    }

    "must return a SummaryListRow with change links when record is not locked and is Category 2" in {

      val row = HasSupplementaryUnitSummary.row(
        recordWithSupplementaryUnitCat2,
        testRecordId,
        recordLocked = false
      )

      row.get.actions mustBe defined
      row.get.actions.value.items.head.href mustEqual routes.HasSupplementaryUnitController
        .onPageLoadUpdate(NormalMode, testRecordId)
        .url
    }
  }

}
