plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

val terraformPath: String? by extra // Specify different binary from the default with -PterraformPath=...


fun Project.registerTerraformTask(name: String, vararg args: String) {
    tasks.register(name, Exec::class) {
        workingDir(File("${rootProject.projectDir}/infra"))
        commandLine(terraformPath ?: "/usr/local/bin/terraform", *args)
    }
}

// Register top-level scripts here:
registerTerraformTask("tfi", "init")
registerTerraformTask("tfp", "plan")
registerTerraformTask("tfa", "apply", "-auto-approve")
