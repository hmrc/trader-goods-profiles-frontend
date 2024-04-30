
## Trader Goods Profile

Trader Goods Profile (TGP) supports UK Internal Market Scheme (UKIMS) registered traders moving goods from Great Britain to Northern Ireland that are not-at-risk of moving into the European Union.

TGP stores goods information allowing traders or intermediaries to use the information to create, update and remove declarations.

### Dependencies

- Scala 2.13.12
- Java 11
- SBT > 1.9.7
- Mongo 4.4
- [Service Manager](https://github.com/hmrc/sm2)

### Setup

Run this service from your console using: `sbt run`

Use service manager to start up supporting services: `sm2 --start TGP_FE` 

### Testing

Run this to check unit tests: `sbt test`

Run this to check integration tests: `sbt it/test`

Run this to check accessibility tests: `sbt clean a11y:test`

Run this to check code coverage: `sbt clean coverage test`

Run this to get code coverage report: `sbt coverageReport`

### Formatting

Run this to check all scala files are formatted: `sbt scalafmtCheckAll`

Run this to format none test scala files: `sbt scalafmt`

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
