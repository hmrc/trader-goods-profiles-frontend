
package logging

import org.slf4j.{Logger, LoggerFactory}

trait Logging {

  protected val logger: Logger =
    LoggerFactory.getLogger("application." + getClass.getCanonicalName)
}
