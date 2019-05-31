package net.codefudge.showcasedigix.deploy

import com.customer.cloudscripts.core.config.DefaultJvmConfig
import com.customer.cloudscripts.core.config.ImageConfig
import com.customer.cloudscripts.core.config.InstanceTypeConfig
import com.customer.cloudscripts.core.config.ResourceConfig
import com.customer.cloudscripts.core.config.servicediscovery.ConsulServiceDiscoveryConfig
import com.customer.cloudscripts.core.config.storage.EbsStsStorageConfig
import com.customer.cloudscripts.core.creation.UnitFactory
import com.customer.cloudscripts.core.k8s.K8sClientSetup
import com.customer.cloudscripts.core.spec.HttpStuff
import com.customer.cloudscripts.core.spec.WaitForApp
import com.customer.cloudscripts.core.spec.WaitForHttp
import com.customer.cloudscripts.core.spec.WaitForK8s
import com.customer.cloudscripts.core.unit.PvcClaimUnit
import com.customer.cloudscripts.extensions.unit.*
import groovyx.net.http.ContentType
import io.fabric8.kubernetes.api.model.EnvVar
import spock.lang.Specification

class ShowCaseSpec extends Specification {
    def client = new K8sClientSetup().createK8sDevClient()
    def wait4App = new WaitForApp(client)
    def wait4Http = new WaitForHttp(new HttpStuff(), 500)
    def wait4k8s = new WaitForK8s(client)

    def 'test'() {
        when:
        new PvcClaimUnit('test', 'test', '10Gi').create(client)

        then:
        true
    }

    def 'elastic test 6.8.0'() {
        when:
        new ElasticsearchStatefulSetUnit(
                new ElasticConfig(
                        name: 'elastic-db',
                        imageConfig: new ImageConfig(tag: '6.8.0'),
                        replicas: 1,
                        resourceConfig: new ResourceConfig(cpu: '0.6', mem: '3Gi'),
                        minimumMasterNodes: 1,
                        instanceTypeConfig: InstanceTypeConfig.DEFAULT,
                        serviceDiscoveryConfig: ConsulServiceDiscoveryConfig.DISABLED_STS,
                        storageClaim: new EbsStsStorageConfig(size: '30Gi'),
                        jvmConfig: new DefaultJvmConfig(javaMem: '2000M')
                )
        ).apply(client)

        then:
        wait4k8s.waitForPodsStatusHavingLabel('Running', 'app', 'elastic-db', 1, 180000)
    }

    def 'logstash test 6.8.0'() {
        when:
        new LogstashDeployUnit(new LogstashConfig(
                name: 'ls-heartbeat',
                replicas: 1,
                jvmConfig: new DefaultJvmConfig(javaMem: '1000M'),
                imageConfig: new ImageConfig(tag: '6.8.0'),
                confResource: '/analysis-stack/logstash-heartbeat.conf',
                resourceConfig: new ResourceConfig(
                        cpu: '0.6',
                        mem: '1100M'
                ),
                instanceTypeConfig: InstanceTypeConfig.DEFAULT,
                envVars: [

                ],
                serviceDiscoveryConfig: ConsulServiceDiscoveryConfig.DISABLED_DEPLOY
        )).apply(client)

        then:
        wait4k8s.waitForPodsStatusHavingLabel('Running', 'app', 'ls-heartbeat', 1, 180000)
        wait4Http.waitForUrl('http://ls-heartbeat.dixday.codefudge.net/_node', ContentType.TEXT, 200, 180000)
    }

    def 'logstash shipper 6.8.0'() {
        when:
        new LogstashDeployUnit(new LogstashConfig(
                name: 'ls-shipper',
                replicas: 1,
                jvmConfig: new DefaultJvmConfig(javaMem: '1000M'),
                imageConfig: new ImageConfig(tag: '6.8.0'),
                confResource: '/analysis-stack/logstash-shipper.conf',
                resourceConfig: new ResourceConfig(
                        cpu: '0.6',
                        mem: '1100M'
                ),
                instanceTypeConfig: InstanceTypeConfig.DEFAULT,
                envVars: [

                ],
                serviceDiscoveryConfig: ConsulServiceDiscoveryConfig.DISABLED_DEPLOY
        )).apply(client)

        then:
        wait4k8s.waitForPodsStatusHavingLabel('Running', 'app', 'ls-shipper', 1, 180000)
        wait4Http.waitForUrl('http://ls-heartbeat.dixday.codefudge.net/_node', ContentType.TEXT, 200, 180000)
    }

    def 'kibana test 6.8.0'() {
        when:
        new KibanaDeploymentUnit(new KibanaConfig(
                name: 'kibana',
                imageConfig: new ImageConfig(tag: '6.8.0'),
                replicas: 1,
                resourceConfig: new ResourceConfig(cpu: '0.3', mem: '900M'),
                instanceTypeConfig: InstanceTypeConfig.DEFAULT,
                serviceDiscoveryConfig: ConsulServiceDiscoveryConfig.DISABLED_DEPLOY,
                dbHost: 'elastic-db',
                dbPort: 9200,
                envVars: [
                        new EnvVar('ELASTICSEARCH_USERNAME', "kibana", null),
                        new EnvVar('ELASTICSEARCH_PASSWORD', "dixday19", null)
                ]
        )).apply(client)

        then:
        wait4k8s.waitForPodsStatusHavingLabel('Running', 'app', 'kibana', 1, 180000)
    }

    def 'nginx ingress setup'() {
        def name = 'ingress'

        def nginxDeploy = new NginxDeploymentUnit(
                new NginxConfig(
                        name: name,
                        replicas: 1,
                        resourceConfig: new ResourceConfig(cpu: '0.1', mem: '250M'),
                        instanceTypeConfig: InstanceTypeConfig.DEFAULT,
                        configResource: '/nginx/ingress-dixday',
                        serviceDiscoveryConfig: ConsulServiceDiscoveryConfig.DISABLED_DEPLOY
                )
        )

        when:
        nginxDeploy.apply(client)

        then:
        wait4k8s.waitForPodsStatusHavingLabel('Running', 'app', 'ingress', 1, 180000)
    }

    def 'nginx test setup'() {
        def name = 'nginx'

        def nginxDeploy = new NginxDeploymentUnit(
                new NginxConfig(
                        name: name,
                        replicas: 1,
                        resourceConfig: new ResourceConfig(cpu: '0.1', mem: '250M'),
                        instanceTypeConfig: InstanceTypeConfig.DEFAULT,
                        configResource: '/nginx/nginx',
                        serviceDiscoveryConfig: ConsulServiceDiscoveryConfig.DISABLED_DEPLOY
                )
        )

        when:
        nginxDeploy.apply(client)

        then:
        wait4k8s.waitForPodsStatusHavingLabel('Running', 'app', 'nginx', 1, 180000)
        wait4Http.waitForUrl('http://nginx.dixday.codefudge.net', ContentType.TEXT, 200, 180000)
    }

    def 'go crazy'() {
        def name = 'go-crazy'

        def goDeploy = new GoDeployUnit(
                new GoConfig(
                        name: name,
                        replicas: 1,
                        resourceConfig: new ResourceConfig(cpu: '0.1', mem: '300M'),
                        instanceTypeConfig: InstanceTypeConfig.DEFAULT,
                        confResource: '/go/script.go',
                        serviceDiscoveryConfig: ConsulServiceDiscoveryConfig.DISABLED_DEPLOY,
                )
        )

        when:
        goDeploy.apply(client)

        then:
        wait4k8s.waitForPodsStatusHavingLabel('Running', 'app', 'go-crazy', 1, 180000)
    }

    def 'lb test'() {
        when:
        new LoadBalancerUnit('elastic-db-ext', 'elastic-db', 9200, new UnitFactory())
                .apply(client)

        then:
        true
    }

    def 'kibana lb test'() {
        when:
        new LoadBalancerUnit('kibana-ext', 'kibana', 5601, new UnitFactory())
                .apply(client)

        then:
        true
    }

    def 'nginx lb test'() {
        when:
        new LoadBalancerUnit('ingress-ext', 'ingress', 80, new UnitFactory())
                .apply(client)

        then:
        true
    }
}
