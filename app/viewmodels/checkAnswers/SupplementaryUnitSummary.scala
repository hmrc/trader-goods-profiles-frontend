package viewmodels.checkAnswers

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.SupplementaryUnitPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object SupplementaryUnitSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(SupplementaryUnitPage).map {
      answer =>

        SummaryListRowViewModel(
          key     = "supplementaryUnit.checkYourAnswersLabel",
          value   = ValueViewModel(answer.toString),
          actions = Seq(
            ActionItemViewModel("site.change", routes.SupplementaryUnitController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("supplementaryUnit.change.hidden"))
          )
        )
    }
}
