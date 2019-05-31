package net.codefudge.showcasedigix.deploy

import com.customer.cloudscripts.core.config.DefaultConfigMapConfig
import com.customer.cloudscripts.core.unit.DeploymentUnit
import io.fabric8.kubernetes.api.model.ConfigMapBuilder
import io.fabric8.kubernetes.api.model.Container
import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.ServiceBuilder
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder

import java.time.Instant

class GoDeployUnit extends DeploymentUnit<GoConfig> {
    private DefaultConfigMapConfig configMap

    GoDeployUnit(GoConfig config) {
        super(config)

        configMap = createConfigMap()
    }

    @Override
    ServiceBuilder defineService() {
        return unitFac.svcFac.defaultHeadless(name, name, 9000)
    }

    DefaultConfigMapConfig createConfigMap() {
        def configMap = new DefaultConfigMapConfig(
                namePrefix: name
        )

        configMap.addResource2Data('script.go', config.confResource)
        configMap.add2Mounts('/mnt/go', null)


        configMap
    }

    @Override
    protected DeploymentBuilder define() {
        def builder = config.unitFac.deployFac.defaultDeploymentBuilder(name, config.replicas, config.instanceTypeConfig)
                .editSpec().editTemplate().editSpec()
                .withContainers(createLogstashContainer())
                .endSpec().endTemplate().endSpec()

        configMap.apply(builder)
    }

    @Override
    ConfigMapBuilder defineConfigMap() {
        return configMap.createConfigMap()
    }

    Container createLogstashContainer() {
        def builder = config.unitFac.conFac.createBaseContainerBuilder(
                'go',
                "golang",
                9000)
                .withCommand('sh', '-c', "go run /mnt/go/script.go | xargs -I '%' curl 'http://ls-shipper:8080' -XPOST -d '%'")
                .withEnv(createEnv())

        builder = config.resourceConfig.apply(builder)
        builder = configMap.apply(builder)

        builder.build()
    }

    List<EnvVar> createEnv() {
        def vars = [
                new EnvVar('ALWAYS_REDEPLOY', getNow().toString(), null),
        ]

        if (config.envVars) {
            vars.addAll(config.envVars)
        }

        vars
    }

    Instant getNow() {
        Instant.now()
    }
}
