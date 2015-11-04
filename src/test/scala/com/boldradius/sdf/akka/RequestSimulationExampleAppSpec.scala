package com.boldradius.sdf.akka

import akka.testkit.TestProbe


class RequestSimulationExampleAppSpec extends BaseAkkaSpec {
  "Creating the App" should {
    "result in creating top-level actor 'consumer'" in {
      new RequestSimulationExampleApp(system)
      TestProbe().expectActor("/user/consumer")
    }
  }
}
