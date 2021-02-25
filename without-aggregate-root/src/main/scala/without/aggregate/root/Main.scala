package without.aggregate.root

import scalikejdbc._

import java.time.LocalDate

object Main {

  def main(args: Array[String]): Unit = {

    case class Invoice(invoiceId: String, date: LocalDate)

    case class InvoiceLine(invoiceId: String, description: String, quantity: Double, unitPrice: Double)

    trait GetInvoice {
      def get(invoiceId: String): Option[(Invoice, List[InvoiceLine])]
    }

    trait SaveInvoice {
      def saveInvoice(invoice: Invoice): Unit
    }

    trait SaveInvoiceLine {
      def saveInvoiceLines(invoiceLines: List[InvoiceLine]): Unit
    }

    trait DeleteInvoiceLinesByInvoiceId {
      def deleteInvoiceLinesByInvoiceId(invoiceId: String): Unit
    }

    object SQLRepositoryImplementation extends GetInvoice with SaveInvoice with SaveInvoiceLine with DeleteInvoiceLinesByInvoiceId {

      Class.forName("org.h2.Driver")
      ConnectionPool.singleton("jdbc:h2:mem:hello", "user", "pass")

      private implicit val session = AutoSession

      sql"""
      create table invoices (
        invoice_id varchar(100) not null primary key,
        date timestamp not null
      )""".execute.apply()

      sql"""
      create table invoice_lines (
        index varchar(100) not null,
        invoice_id varchar(100) not null,
        description varchar(1000) not null,
        quantity double not null,
        unit_price double not null,
        PRIMARY KEY(index, invoice_id)
      )""".execute.apply()


      override def get(invoiceId: String): Option[(Invoice, List[InvoiceLine])] = DB readOnly { implicit session =>
        val rows =
          sql"""select invoices.invoice_id, invoices.date, invoice_lines.index, invoice_lines.description, invoice_lines.quantity, invoice_lines.unit_price
              from invoices
              left join invoice_lines on invoices.invoice_id = invoice_lines.invoice_id
              where invoices.invoice_id = ${invoiceId}"""
            .map { rs =>
              val invoiceLine = InvoiceLine(
                invoiceId = rs.string("invoice_id"),
                description = rs.string("description"),
                quantity = rs.double("quantity"),
                unitPrice = rs.double("unit_price")
              )
              ((rs.string("invoice_id"), rs.localDate("date")), rs.int("index"), invoiceLine)
            }.list.apply()

        if(rows.isEmpty) None
        else {
          val (invoiceId, date) = rows.head._1
          Some((Invoice(invoiceId, date), rows.sortBy(_._2).map(_._3)))
        }
      }

      override def saveInvoice(invoice: Invoice): Unit =
        sql"""
        insert into invoices (invoice_id, date)
        values (${invoice.invoiceId}, ${invoice.date})""".execute.apply()

      override def saveInvoiceLines(invoiceLines: List[InvoiceLine]): Unit = {
        val batch: Seq[Seq[(String, Any)]] =
          invoiceLines.zipWithIndex.map { case (invoiceLine, index) =>
            Seq(
              "index" -> index,
              "invoice_id" -> invoiceLine.invoiceId,
              "description" -> invoiceLine.description,
              "quantity" -> invoiceLine.quantity,
              "unit_price" -> invoiceLine.unitPrice
            )
          }

        sql"""
        insert into invoice_lines (index, invoice_id, description, quantity, unit_price)
        values ({index}, {invoice_id}, {description}, {quantity}, {unit_price})"""
          .batchByName(batch: _*)
          .apply()
      }

      override def deleteInvoiceLinesByInvoiceId(invoiceId: String): Unit =
        sql"delete from invoice_lines where invoice_id = ${invoiceId}".update.apply()
    }

    val invoice = Invoice("I1234567", LocalDate.of(2021, 2, 24))
    val invoiceLines = List(
      InvoiceLine("I1234567", "Pen", 10, 0.5),
      InvoiceLine("I1234567", "Book", 5, 10),
    )
    SQLRepositoryImplementation.saveInvoice(invoice)
    SQLRepositoryImplementation.deleteInvoiceLinesByInvoiceId("I1234567")
    SQLRepositoryImplementation.saveInvoiceLines(invoiceLines)

    println(SQLRepositoryImplementation.get("I1234567"))


  }

}
