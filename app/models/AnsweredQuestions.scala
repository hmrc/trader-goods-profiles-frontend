package models

import models.ott.CategoryAssessment

case class AnsweredQuestions(index: Int, question: CategoryAssessment, answer: Option[AssessmentAnswer2])
