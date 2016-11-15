[![Build Status](https://travis-ci.org/skbkontur/OctoPnP.svg?branch=master)](https://travis-ci.org/skbkontur/OctoPnP)

# OctoPnP TeamCity plugin

OctoPnP provides "Pack and Publish" functionality for Octopus-based deployments automation.

## Build and test
 1. Open `src` project in IntelliJ Idea.
 2. Issue `mvn package` command from the root project to build the plugin.
 3. Resulting package `octo-pnp.zip` will be placed in `src\target` directory.
 4. To deploy the plugin to locally installed TeamCity run `.\deploy-plugin.cmd` script.

Initial implementation of OctoPnP was inspired by [Octopus-TeamCity](https://github.com/OctopusDeploy/Octopus-TeamCity) project.
