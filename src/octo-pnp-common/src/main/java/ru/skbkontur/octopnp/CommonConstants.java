package ru.skbkontur.octopnp;

public class CommonConstants {
    public static final String OCTOPNP_PACK_AND_PUBLISH_RUNNER_TYPE = "octopnp.pack.and.publish";

    public String getOctopusServerUrlKey() {
        return "octopus_server_url";
    }

    public String getOctopusApiKey() {
        return "octopus_api_key";
    }

    public String getNuspecPathsKey() {
        return "nuspec_paths";
    }

    public String getPackageVersionKey() {
        return "package_version";
    }

    public String getPushConcurrencyKey() {
        return "push_concurrency";
    }
}
