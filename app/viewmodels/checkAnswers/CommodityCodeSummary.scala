package viewmodels.checkAnswers

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.CommodityCodePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object CommodityCodeSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(CommodityCodePage).map {
      answer =>

        SummaryListRowViewModel(
          key     = "commodityCode.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlFormat.escape(answer).toString),
          actions = Seq(
            ActionItemViewModel("site.change", routes.CommodityCodeController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("commodityCode.change.hidden"))
          )
        )
    }
}
