package forms

import forms.mappings.Mappings
import javax.inject.Inject
import play.api.data.Form

class SupplementaryUnitFormProvider @Inject() extends Mappings {

  def apply(): Form[Int] =
    Form(
      "value" -> int(
        "supplementaryUnit.error.required",
        "supplementaryUnit.error.wholeNumber",
        "supplementaryUnit.error.nonNumeric")
          .verifying(inRange(0, Int.MaxValue, "supplementaryUnit.error.outOfRange"))
    )
}
