## Understanding what Tradeleg is:

Trade Leg:
A trade leg is a single component or side of a financial trade. Complex trades (like swaps or multi-party deals) are often broken down into multiple legs, each representing a distinct obligation, payment, or position. For example, in a currency swap, one leg might be the payment in USD, and the other leg the payment in EUR. Each leg specifies details like amount, direction (buy/sell), counterparty, and settlement terms.

## Understanding what Trade book is:

Trade Book:
A trade book (or simply “book”) is a collection or grouping of trades managed together, typically by a trading desk, portfolio manager, or business unit. Books help organise trades for risk management, reporting, and performance tracking. For example, a trader might have a “Fixed Income Book” containing all bond trades, or a “FX Book” for currency trades. Books are used to monitor exposures, P&L, and compliance for a set of related trades.

## Entity Relationships (Model Classes Only)

### Trade TradeLeg

Type: One-to-Many
Description: A single Trade can have multiple TradeLegs. Each TradeLeg references its parent Trade.
Java Mapping: @OneToMany(mappedBy = "trade") in Trade, @ManyToOne in TradeLeg.

### Trade User

Type: Many-to-One
Description: Many Trades can be booked by one User. Each Trade references the User who booked it.
Java Mapping: @ManyToOne in Trade.

### Trade Book

Type: Many-to-One
Description: Many Trades can belong to one Book. Each Trade references the Book it belongs to.
Java Mapping: @ManyToOne in Trade.

### Trade Cashflow

Type: One-to-Many
Description: A Trade can generate multiple Cashflow records. Each Cashflow references its parent Trade.
Java Mapping: @OneToMany(mappedBy = "trade") in Trade, @ManyToOne in Cashflow.

### Trade AdditionalInfo

Type: One-to-One or One-to-Many
Description: A Trade may have one or more AdditionalInfo records for extra metadata.
Java Mapping: @OneToOne or @OneToMany in Trade.
