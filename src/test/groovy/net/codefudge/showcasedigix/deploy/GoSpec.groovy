package net.codefudge.showcasedigix.deploy

import com.customer.cloudscripts.core.config.InstanceTypeConfig
import com.customer.cloudscripts.core.config.ResourceConfig
import com.customer.cloudscripts.core.config.servicediscovery.ConsulServiceDiscoveryConfig
import spock.lang.Specification

import java.time.Instant

import static com.customer.cloudscripts.core.spec.ResourceStuff.readResource
import static com.customer.cloudscripts.core.spec.SpecHelper.prettyYaml

class GoSpec extends Specification {
    def 'check go-crazy'() {
        when:
        GoDeployUnit go = Spy(GoDeployUnit.class, constructorArgs: [new GoConfig(
                name: 'some-name',
                replicas: 1,
                resourceConfig: new ResourceConfig(),
                serviceDiscoveryConfig: ConsulServiceDiscoveryConfig.DISABLED_DEPLOY,
                instanceTypeConfig: InstanceTypeConfig.DEFAULT,
                confResource: '/go/script.go',

        )])

        go.getNow() >> Instant.parse('2019-05-25T13:00:00.000Z')

        then:
        prettyYaml(go.define().build()) == readResource('/spec/go-crazy.yaml')
    }
}
