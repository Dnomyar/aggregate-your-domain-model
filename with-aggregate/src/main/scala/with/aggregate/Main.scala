package `with`.aggregate

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import scalikejdbc._

import java.time.LocalDate

object Main {

  def main(args: Array[String]): Unit = {

    case class Invoice(invoiceId: String, date: LocalDate, invoiceLines: List[InvoiceLine])

    case class InvoiceLine(description: String, quantity: Double, unitPrice: Double)

    trait Repository {
      def get(invoiceId: String): Option[Invoice]

      def save(invoice: Invoice): Unit
    }

    object InvoiceSerialisation {
      def serialize(invoice: Invoice): String = invoice.asJson.noSpaces

      def deserialize(maybeInvoiceJson: String): Invoice = decode[Invoice](maybeInvoiceJson) match {
        case Left(error) => throw error // not very scala but simplifies the example
        case Right(invoice) => invoice
      }
    }

    object SQLRepositoryImplementation extends Repository with H2InvoiceInitialisation {

      override def get(invoiceId: String): Option[Invoice] = DB readOnly { implicit session =>
        sql"""select json
              from invoices
              where invoice_id = ${invoiceId}"""
          .map(rs =>
            InvoiceSerialisation.deserialize(
              rs.string("json")
                .replace("\\\"", "\"")
                .drop(1).dropRight(1)
              // ^^^ haven't found the proper way of extracting json from H2
            )
          )
          .first
          .apply()
      }

      override def save(invoice: Invoice): Unit = {
        val invoiceSerialised = InvoiceSerialisation.serialize(invoice)
        sql"""
        insert into invoices (invoice_id, json)
        values (${invoice.invoiceId}, ${invoiceSerialised})""".execute.apply()
      }
    }

    val invoice = Invoice(
      "I1234567",
      LocalDate.of(2021, 2, 24),
      List(
        InvoiceLine("Pen", 10, 0.5),
        InvoiceLine("Book", 5, 10),
      )
    )
    SQLRepositoryImplementation.save(invoice)
    SQLRepositoryImplementation.get("I1234567")

  }


}
