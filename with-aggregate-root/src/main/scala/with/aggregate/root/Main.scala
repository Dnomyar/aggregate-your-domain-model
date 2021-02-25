package `with`.aggregate.root

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import scalikejdbc.{AutoSession, ConnectionPool}
import scalikejdbc._

import java.time.LocalDate

object Main {

  def main(args: Array[String]): Unit = {

    case class Invoice(invoiceId: String, date: LocalDate, invoiceLines: List[InvoiceLine])

    case class InvoiceLine(description: String, quantity: Double, unitPrice: Double)

    trait GetInvoiceRepository {
      def get(invoiceId: String): Option[Invoice]
    }

    trait SaveInvoiceRepository {
      def save(invoice: Invoice): Unit
    }

    object InvoiceSerialisation {
      def serialize(invoice: Invoice): String = invoice.asJson.noSpaces
      def deserialize(maybeInvoiceJson: String): Invoice = decode[Invoice](maybeInvoiceJson) match {
        case Left(error) => throw error // not very scala that simplify the example
        case Right(invoice) => invoice
      }
    }

    object SQLRepositoryImplementation extends GetInvoiceRepository with SaveInvoiceRepository {
      Class.forName("org.h2.Driver")
      ConnectionPool.singleton("jdbc:h2:mem:hello", "user", "pass")

      private implicit val session = AutoSession

      sql"""
      create table invoices (
        invoice_id varchar(100) not null primary key,
        json json not null
      )""".execute.apply()

      override def get(invoiceId: String): Option[Invoice] = DB readOnly { implicit session =>
        sql"""select json
              from invoices
              where invoice_id = ${invoiceId}"""
          .map { rs =>
            InvoiceSerialisation.deserialize(rs.string("json").replace("\\\"", "\"").drop(1).dropRight(1))
          }
          .first
          .apply()
      }

      override def save(invoice: Invoice): Unit = {
        val invoiceSerialised = InvoiceSerialisation.serialize(invoice)
        println(invoiceSerialised)
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
