package viewmodels.checkAnswers

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.HasGoodDescriptionChangePage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object HasGoodDescriptionChangeSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(HasGoodDescriptionChangePage).map { answer =>
      val value = if (answer) "site.yes" else "site.no"

      SummaryListRowViewModel(
        key = "hasGoodDescriptionChange.checkYourAnswersLabel",
        value = ValueViewModel(value),
        actions = Seq(
          ActionItemViewModel("site.change", routes.HasGoodDescriptionChangeController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("hasGoodDescriptionChange.change.hidden"))
        )
      )
    }
}
