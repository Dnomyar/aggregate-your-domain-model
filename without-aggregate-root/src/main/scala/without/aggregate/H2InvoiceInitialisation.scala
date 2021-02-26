package without.aggregate

import scalikejdbc._

trait H2InvoiceInitialisation {

  Class.forName("org.h2.Driver")
  ConnectionPool.singleton("jdbc:h2:mem:hello", "user", "pass")

  protected implicit val session = AutoSession

  sql"""
      create table invoices (
        invoice_id varchar(100) not null primary key,
        date timestamp not null
      )""".execute.apply()

  sql"""
      create table invoice_lines (
        index int not null,
        invoice_id varchar(100) not null,
        description varchar(1000) not null,
        quantity double not null,
        unit_price double not null,
        PRIMARY KEY(index, invoice_id),
        FOREIGN KEY (invoice_id) references invoices(invoice_id)
      )""".execute.apply()


}
