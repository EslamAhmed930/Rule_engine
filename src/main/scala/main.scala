import java.io.{FileWriter, PrintWriter}
import java.sql.{Connection, DriverManager, PreparedStatement}
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import scala.io.{BufferedSource, Source}
import scala.util.Using

case class Order(
                  timestamp: String,
                  productName: String,
                  expiryDate: String,
                  quantity: Int,
                  unitPrice: Double,
                  channel: String,
                  paymentMethod: String
                )

case class Result(
                   order: Order,
                   discounts: List[Double],
                   averageDiscount: Double,
                   finalPrice: Double
                 )

object RuleEngine extends App {
  new java.io.File("logs").mkdirs()
  val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  val logFile = new PrintWriter(new FileWriter("logs/rule_engine.log", true))

  def log(level: String, message: String): Unit = {
    val timestamp = java.time.LocalDateTime.now()
    logFile.write(s"$timestamp  $level  $message\n")
    logFile.flush()
  }

  def loadCSV(path: String): List[Order] = {
    Using.resource(Source.fromFile(path)) { source =>
      source.getLines().drop(1).toList.map { line =>
        val cols = line.split(",").map(_.trim)
        Order(
          cols(0), cols(1), cols(2),
          cols(3).toInt, cols(4).toDouble,
          cols(5), cols(6)
        )
      }
    }
  }

  def toDate(dateStr: String): LocalDate = LocalDate.parse(dateStr.take(10))

  def daysToExpiry(order: Order): Long = ChronoUnit.DAYS.between(toDate(order.timestamp), LocalDate.parse(order.expiryDate))

  def discountByExpiry(order: Order): Double = {
    val days = daysToExpiry(order)
    if (days < 30 && days > 0) 30 - days else 0
  }

  def discountByProduct(order: Order): Double = {
    val name = order.productName.toLowerCase
    if (name.contains("cheese")) 10
    else if (name.contains("wine")) 5
    else 0
  }

  def discountByDate(order: Order): Double = {
    val date = toDate(order.timestamp)
    if (date.getMonthValue == 3 && date.getDayOfMonth == 23) 50 else 0
  }

  def discountByQuantity(order: Order): Double = order.quantity match {
    case q if q >= 6 && q <= 9  => 5
    case q if q >= 10 && q <= 14 => 7
    case q if q > 15 => 10
    case _ => 0
  }

  def discountByAppChannel(order: Order): Double = {
    if (order.channel.toLowerCase == "app") {
      val roundedQty = ((order.quantity + 4) / 5) * 5
      (roundedQty / 5) * 5.0
    } else 0
  }

  def discountByVisa(order: Order): Double = {
    if (order.paymentMethod.toLowerCase == "visa") 5.0 else 0
  }

  def top2Average(discounts: List[Double]): Double = {
    val sorted = discounts.sorted(Ordering[Double].reverse)
    if (sorted.size >= 2) (sorted(0) + sorted(1)) / 2
    else sorted.headOption.getOrElse(0.0)
  }

  def calculateDiscount(order: Order): Result = {
    val discounts = List(
      discountByExpiry(order),
      discountByProduct(order),
      discountByDate(order),
      discountByQuantity(order),
      discountByAppChannel(order),
      discountByVisa(order)
    ).filter(_ > 0)

    val avgDiscount = top2Average(discounts)
    val finalPrice = order.unitPrice * order.quantity * (1 - avgDiscount / 100)

    log("INFO", s"Processed order for '${order.productName}' with discounts $discounts")

    Result(order, discounts, avgDiscount, finalPrice)
  }

  def saveToDatabase(results: List[Result]): Unit = {
    val url = "jdbc:postgresql://localhost:5432/retail_discounts"
    val user = "postgres"
    val password = "123"
    val connection: Connection = DriverManager.getConnection(url, user, password)

    val sql =
      """
        |INSERT INTO summary_orders
        |(product_name, discount, final_price)
        |VALUES (?, ?, ?)
        |""".stripMargin

    val statement: PreparedStatement = connection.prepareStatement(sql)

    results.foreach { result =>
      val o = result.order
      statement.setString(1, o.productName)
      statement.setDouble(2, result.averageDiscount)
      statement.setDouble(3, result.finalPrice)
      statement.executeUpdate()
    }

    connection.close()
    log("INFO", "Finished writing to summary_orders table in PostgreSQL database.")
  }

  val orders = loadCSV("src/main/resources/TRX1000.csv")
  val results = orders.map(calculateDiscount)
  saveToDatabase(results)
  log("INFO", "All orders processed.")
  logFile.close()
}


