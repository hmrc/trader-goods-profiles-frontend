package models.audits

import uk.gov.hmrc.auth.core.AffinityGroup

import java.time.LocalDate
 case class OttAuditDetails(eori: String,
                                                   affinityGroup: AffinityGroup,
                                                   recordId: Option[String],
                                                   commodityCode: String,
                                                   countryOfOrigin: String,
                                                   dateOfTrade: LocalDate,
                                                                         journey: String
                           )

//case class AuditValidateCommodityCodeDetails(eori: String,
//                                             affinityGroup: AffinityGroup,
//                                             recordId: Option[String],
//                                             commodityCode: String,
//                                             journey: String
//                                            ) extends OttAuditDetails()
//
//case class AuditGetCategorisationAssessmentDetails(eori: String,
//                                                   affinityGroup: AffinityGroup,
//                                                   recordId: Option[String],
//                                                   commodityCode: String,
//                                                   countryOfOrigin: String,
//                                                   dateOfTrade: LocalDate) extends OttAuditDetails()
