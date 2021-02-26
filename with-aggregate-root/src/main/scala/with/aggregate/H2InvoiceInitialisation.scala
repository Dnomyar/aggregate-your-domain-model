package `with`.aggregate

import scalikejdbc._

trait H2InvoiceInitialisation {

  Class.forName("org.h2.Driver")
  ConnectionPool.singleton("jdbc:h2:mem:hello", "user", "pass")

  protected implicit val session = AutoSession

  sql"""
      create table invoices (
        invoice_id varchar(100) not null primary key,
        json json not null
      )""".execute.apply()

}
