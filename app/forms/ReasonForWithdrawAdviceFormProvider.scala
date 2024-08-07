package forms

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form

class ReasonForWithdrawAdviceFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("reasonForWithdrawAdvice.error.required")
        .verifying(maxLength(512, "reasonForWithdrawAdvice.error.length"))
    )
}
