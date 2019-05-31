package net.codefudge.showcasedigix.deploy


import com.customer.cloudscripts.core.config.unit.DeploymentServiceConfig
import io.fabric8.kubernetes.api.model.EnvVar

class GoConfig extends DeploymentServiceConfig {
    String confResource
    List<EnvVar> envVars
}
