package viewmodels.checkAnswers

import base.SpecBase
import base.TestConstants.testRecordId
import controllers.routes
import models.NormalMode
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages

class SupplementaryUnitSummarySpec extends SpecBase with MockitoSugar {

  implicit private val messages: Messages = messages(applicationBuilder().build())

  "SupplementaryUnitSummary.row" - {

    "must return a SummaryListRow when category is 2" in {
      val row = SupplementaryUnitSummary.row(
        Some(2),
        Some(BigDecimal(100)),
        Some("kg"),
        testRecordId,
        recordLocked = false
      )

      row mustBe defined
      row.get.actions.value.items.head.href mustEqual routes.SupplementaryUnitController
        .onPageLoadUpdate(NormalMode, testRecordId)
        .url
    }

    "must not return a SummaryListRow when category is not 2" in {
      val row = SupplementaryUnitSummary.row(
        Some(1),
        Some(BigDecimal(100)),
        Some("kg"),
        testRecordId,
        recordLocked = false
      )

      row mustBe None
    }
  }
}