package viewmodels.checkAnswers

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.ReasonForWithdrawAdvicePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object ReasonForWithdrawAdviceSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(ReasonForWithdrawAdvicePage).map {
      answer =>

        SummaryListRowViewModel(
          key     = "reasonForWithdrawAdvice.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlFormat.escape(answer).toString),
          actions = Seq(
            ActionItemViewModel("site.change", routes.ReasonForWithdrawAdviceController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("reasonForWithdrawAdvice.change.hidden"))
          )
        )
    }
}
