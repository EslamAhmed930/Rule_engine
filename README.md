# Rule-engine
#  Retail Discount Rule Engine (Scala)

This project is a **pure functional rule engine** built in Scala to process retail transactions, calculate discounts based on configurable rules, log all activity, and store the results in a PostgreSQL database.

---

##  Features

- Load and parse retail order data from CSV
- Apply multiple discount rules:
  - Expiry date proximity
  - Product type (e.g., cheese, wine)
  - Special event date (March 23)
  - Quantity tiers
  - Sales channel (app/web)
  - Payment method (Visa)
- Log events with timestamped messages
- Save processed results (discount + final price) to PostgreSQL
- Organized and easy-to-extend design using case classes and functional transformations

---

##  Discount Logic

| Rule                         | Description                                              |
|-----------------------------|----------------------------------------------------------|
| Expiry Proximity            | 30 - days to expiry if less than 30 days remain         |
| Product-Based               | Cheese → 10%, Wine → 5%                                 |
| Special Date (Mar 23)       | 50% if order is on March 23                            |
| Quantity-Based              | 6–9 → 5%, 10–14 → 7%, >15 → 10%                        |
| App Channel Bonus           | 5% per 5 items ordered via app                         |
| Visa Payment Bonus          | Extra 5% discount if Visa is used                      |

 Final price is calculated using the **average of the top 2 discounts** applied.


##  Technologies Used

- **Scala 2.13**
- **JDBC + PostgreSQL**
- **Scala I/O & Utilities** (`scala.io.Source`, `scala.util.Using`)
- **Logging to File** (`java.io.FileWriter`)
- **Functional Programming** (case classes, pure methods)
---

## Project Structure


├── src
│   └── main
│       └── scala
│           └── RuleEngine.scala
├── resources
│   └── TRX1000.csv
├── logs
│   └── rule_engine.log
├── build.sbt
└── README.md
