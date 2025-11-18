
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

Use service manager to start up supporting services: `sm2 --start TGP_FE_ALL`

### Testing

Run this to check unit tests: `sbt test`

Run this to check integration tests: `sbt it/test`

Run this to run all tests, check code coverage, and generate a report: `sbt clean coverage test it/test coverageReport`

### Formatting

Run this to check all scala files are formatted: `sbt scalafmtCheckAll`

Run this to format non-test scala files: `sbt scalafmt`

Run this to format test scala files: `sbt test:scalafmt`

### SBT Alias Commands

To run all unit and IT tests, check code coverage, and generate a report: Use `sbt testAndCoverage`

Before raising a PR, to run all unit and IT tests, format the code, check code coverage, and generate a report: Use `sbt prePR`

Before merging, to run all unit and IT tests, verify code formatting, check code coverage, and generate a report: Use `sbt preMerge`

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").